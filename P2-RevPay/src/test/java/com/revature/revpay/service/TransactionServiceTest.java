package com.revature.revpay.service;

import com.revature.revpay.dao.PaymentMethodDAO;
import com.revature.revpay.dao.TransactionDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.PaymentMethod;
import com.revature.revpay.model.Transaction;
import com.revature.revpay.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionDAO transactionDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private PaymentMethodDAO paymentMethodDAO;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setUserId(1L);
        sender.setWalletBalance(new BigDecimal("500.00"));

        receiver = new User();
        receiver.setUserId(2L);
        receiver.setWalletBalance(new BigDecimal("100.00"));
    }

    @Test
    void testSendMoney_Success() {
        when(userDAO.findById(1L)).thenReturn(sender);
        when(userDAO.findByUsernameOrEmailOrPhone("receiver_id")).thenReturn(receiver);

        String result = transactionService.sendMoney(1L, "receiver_id", new BigDecimal("100.00"), "Lunch");

        assertEquals("SUCCESS", result);
        verify(userDAO).updateBalance(1L, new BigDecimal("400.00"));
        verify(userDAO).updateBalance(2L, new BigDecimal("200.00"));
        verify(transactionDAO).save(any(Transaction.class));
        verify(notificationService, times(2)).sendTransactionNotification(anyLong(), anyString(), anyString());
    }

    @Test
    void testSendMoney_InsufficientFunds() {
        when(userDAO.findById(1L)).thenReturn(sender);
        when(userDAO.findByUsernameOrEmailOrPhone("receiver_id")).thenReturn(receiver);

        String result = transactionService.sendMoney(1L, "receiver_id", new BigDecimal("600.00"), "Too expensive");

        assertEquals("INSUFFICIENT_FUNDS", result);
        verify(userDAO, never()).updateBalance(anyLong(), any());
        verify(transactionDAO, never()).save(any());
    }

    @Test
    void testSendMoney_CannotSendSelf() {
        when(userDAO.findById(1L)).thenReturn(sender);
        when(userDAO.findByUsernameOrEmailOrPhone("myself")).thenReturn(sender);

        String result = transactionService.sendMoney(1L, "myself", new BigDecimal("50.00"), "To me");

        assertEquals("CANNOT_SEND_SELF", result);
    }

    @Test
    void testAddFunds_Success() {
        when(userDAO.findById(1L)).thenReturn(sender);
        
        PaymentMethod card = new PaymentMethod();
        card.setUserId(1L);
        card.setBalance(new BigDecimal("1000.00"));
        when(paymentMethodDAO.findById(10L)).thenReturn(card);

        String result = transactionService.addFunds(1L, 10L, new BigDecimal("200.00"));

        assertEquals("SUCCESS", result);
        verify(paymentMethodDAO).updateBalance(10L, new BigDecimal("800.00"));
        verify(userDAO).updateBalance(1L, new BigDecimal("700.00"));
        verify(transactionDAO).save(any(Transaction.class));
    }

    @Test
    void testWithdraw_Success() {
        when(userDAO.findById(1L)).thenReturn(sender);
        
        PaymentMethod card = new PaymentMethod();
        card.setUserId(1L);
        card.setBalance(new BigDecimal("1000.00"));
        when(paymentMethodDAO.findById(10L)).thenReturn(card);
        
        String result = transactionService.withdraw(1L, 10L, new BigDecimal("150.00"));
        
        assertEquals("SUCCESS", result);
        verify(userDAO).updateBalance(1L, new BigDecimal("350.00"));
        verify(paymentMethodDAO).updateBalance(10L, new BigDecimal("1150.00"));
        verify(transactionDAO).save(any(Transaction.class));
    }
}
