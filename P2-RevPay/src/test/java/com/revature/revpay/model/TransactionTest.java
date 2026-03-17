package com.revature.revpay.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testTransactionGettersAndSetters() {
        Transaction tx = new Transaction();
        tx.setTxnId(1L);
        tx.setSenderId(10L);
        tx.setReceiverId(20L);
        tx.setSenderName("Alice");
        tx.setReceiverName("Bob");
        tx.setAmount(new BigDecimal("50.00"));
        tx.setTxnType("SEND");
        tx.setStatus("COMPLETED");
        tx.setNote("Dinner");
        tx.setReferenceId("REF123");
        LocalDateTime now = LocalDateTime.now();
        tx.setTxnTimestamp(now);

        assertEquals(1L, tx.getTxnId());
        assertEquals(10L, tx.getSenderId());
        assertEquals(20L, tx.getReceiverId());
        assertEquals("Alice", tx.getSenderName());
        assertEquals("Bob", tx.getReceiverName());
        assertEquals(new BigDecimal("50.00"), tx.getAmount());
        assertEquals("SEND", tx.getTxnType());
        assertEquals("COMPLETED", tx.getStatus());
        assertEquals("Dinner", tx.getNote());
        assertEquals("REF123", tx.getReferenceId());
        assertEquals(now, tx.getTxnTimestamp());
    }

    @Test
    void testIsDebit() {
        Transaction tx = new Transaction();
        tx.setSenderId(10L);
        tx.setReceiverId(20L);

        tx.setTxnType("SEND");
        assertTrue(tx.isDebit(10L));

        tx.setTxnType("WITHDRAWAL");
        assertTrue(tx.isDebit(10L));

        tx.setTxnType("LOAN_REPAYMENT");
        assertTrue(tx.isDebit(10L));

        tx.setTxnType("PAYMENT");
        assertTrue(tx.isDebit(10L)); // Current user is sender
        assertFalse(tx.isDebit(20L)); // Current user is receiver
    }

    @Test
    void testIsCredit() {
        Transaction tx = new Transaction();
        tx.setSenderId(10L);
        tx.setReceiverId(20L);

        tx.setTxnType("ADD_FUNDS");
        assertTrue(tx.isCredit(20L));

        tx.setTxnType("RECEIVE");
        assertTrue(tx.isCredit(20L));

        tx.setTxnType("PAYMENT");
        assertTrue(tx.isCredit(20L)); // Current user is receiver
        assertFalse(tx.isCredit(10L)); // Current user is sender
    }
}
