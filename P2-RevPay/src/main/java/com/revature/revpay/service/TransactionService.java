package com.revature.revpay.service;

import com.revature.revpay.dao.TransactionDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.Transaction;
import com.revature.revpay.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LogManager.getLogger(TransactionService.class);

    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;
    private final com.revature.revpay.dao.PaymentMethodDAO paymentMethodDAO;
    private final NotificationService notificationService;

    public TransactionService(TransactionDAO transactionDAO, UserDAO userDAO,
            com.revature.revpay.dao.PaymentMethodDAO paymentMethodDAO, NotificationService notificationService) {
        this.transactionDAO = transactionDAO;
        this.userDAO = userDAO;
        this.paymentMethodDAO = paymentMethodDAO;
        this.notificationService = notificationService;
    }

    /**
     * Send money from sender to receiver
     */
    @Transactional
    public String sendMoney(Long senderId, String recipientIdentifier, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";

        User sender = userDAO.findById(senderId);
        if (sender == null)
            return "SENDER_NOT_FOUND";

        User receiver = userDAO.findByUsernameOrEmailOrPhone(recipientIdentifier);
        if (receiver == null)
            return "RECEIVER_NOT_FOUND";

        if (sender.getUserId().equals(receiver.getUserId()))
            return "CANNOT_SEND_SELF";

        // Execute atomic transfer via Stored Procedure
        String result = transactionDAO.transferMoneySP(senderId, receiver.getUserId(), amount, note);
        
        if (!"SUCCESS".equals(result)) {
            return result;
        }

        // Notifications (Executed after successful commit)
        notificationService.sendTransactionNotification(senderId,
                "Money Sent", "You sent ₹" + amount + " to " + receiver.getFullName());
        notificationService.sendTransactionNotification(receiver.getUserId(),
                "Money Received", "You received ₹" + amount + " from " + sender.getFullName());

        log.info("Transfer (SP): {} -> {} amount={}", senderId, receiver.getUserId(), amount);
        return "SUCCESS";
    }

    /**
     * Process an invoice payment
     */
    @Transactional
    public String payInvoiceTransaction(Long senderId, String recipientIdentifier, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";

        User sender = userDAO.findById(senderId);
        if (sender == null)
            return "SENDER_NOT_FOUND";

        User receiver = userDAO.findByUsernameOrEmailOrPhone(recipientIdentifier);
        if (receiver == null)
            return "RECEIVER_NOT_FOUND";

        if (sender.getUserId().equals(receiver.getUserId()))
            return "CANNOT_SEND_SELF";

        // Execute atomic transfer via Stored Procedure
        String result = transactionDAO.transferMoneySP(senderId, receiver.getUserId(), amount, note);
        
        if (!"SUCCESS".equals(result)) {
            return result;
        }

        // Notifications
        notificationService.sendTransactionNotification(senderId,
                "Invoice Paid", "You paid ₹" + amount + " to " + receiver.getFullName());
        notificationService.sendTransactionNotification(receiver.getUserId(),
                "Payment Received", "Payment of ₹" + amount + " received from " + sender.getFullName());

        log.info("Invoice Payment: {} -> {} amount={}", senderId, receiver.getUserId(), amount);
        return "SUCCESS";
    }

    /**
     * Add funds to wallet from a payment method
     */
    @Transactional
    public String addFunds(Long userId, Long methodId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";
        User user = userDAO.findById(userId);
        if (user == null)
            return "USER_NOT_FOUND";

        com.revature.revpay.model.PaymentMethod card = paymentMethodDAO.findById(methodId);
        if (card == null || !card.getUserId().equals(userId))
            return "CARD_NOT_FOUND";

        if (card.getBalance().compareTo(amount) < 0)
            return "INSUFFICIENT_CARD_FUNDS";

        // Deduct from card, add to wallet
        paymentMethodDAO.updateBalance(methodId, card.getBalance().subtract(amount));
        java.math.BigDecimal walletBalance = user.getWalletBalance() != null ? user.getWalletBalance() : java.math.BigDecimal.ZERO;
        userDAO.updateBalance(userId, walletBalance.add(amount));

        Transaction txn = new Transaction();
        txn.setReceiverId(userId);
        txn.setAmount(amount);
        txn.setTxnType("ADD_FUNDS");
        txn.setStatus("COMPLETED");
        txn.setNote("Added funds from card " + card.getLastFour());
        transactionDAO.save(txn);

        notificationService.sendTransactionNotification(userId,
                "Funds Added", "₹" + amount + " added to your wallet from ending in " + card.getLastFour());
        return "SUCCESS";
    }

    /**
     * Withdraw from wallet to bank
     */
    @Transactional
    public String withdraw(Long userId, Long methodId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return "INVALID_AMOUNT";
        User user = userDAO.findById(userId);
        if (user == null)
            return "USER_NOT_FOUND";

        com.revature.revpay.model.PaymentMethod card = paymentMethodDAO.findById(methodId);
        if (card == null || !card.getUserId().equals(userId))
            return "CARD_NOT_FOUND";

        java.math.BigDecimal walletBal = user.getWalletBalance() != null ? user.getWalletBalance() : java.math.BigDecimal.ZERO;
        if (walletBal.compareTo(amount) < 0)
            return "INSUFFICIENT_FUNDS";

        // Deduct from wallet, add to card
        userDAO.updateBalance(userId, walletBal.subtract(amount));
        paymentMethodDAO.updateBalance(methodId, card.getBalance().add(amount));

        Transaction txn = new Transaction();
        txn.setSenderId(userId);
        txn.setAmount(amount);
        txn.setTxnType("WITHDRAWAL");
        txn.setStatus("COMPLETED");
        txn.setNote("Withdrawal to card " + card.getLastFour());
        transactionDAO.save(txn);

        notificationService.sendTransactionNotification(userId,
                "Withdrawal Processed", "₹" + amount + " withdrawn to card ending in " + card.getLastFour());

        return "SUCCESS";
    }

    public List<Transaction> getHistory(Long userId, String type, LocalDate from, LocalDate to,
            BigDecimal minAmt, BigDecimal maxAmt, String search, int page) {
        return transactionDAO.findByUserId(userId, type, from, to, minAmt, maxAmt, search, page, 20);
    }

    public List<Transaction> getRecent(Long userId, int limit) {
        return transactionDAO.findRecentByUserId(userId, limit);
    }

    public List<Transaction> getAllForExport(Long userId) {
        return transactionDAO.findAllByUserId(userId);
    }

    public BigDecimal getTotalSent(Long userId) {
        return transactionDAO.getTotalByType(userId, "SEND");
    }

    public BigDecimal getTotalReceived(Long userId) {
        return transactionDAO.getTotalReceived(userId);
    }
}
