package com.revature.revpay.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodTest {

    @Test
    void testPaymentMethodGettersAndSetters() {
        PaymentMethod pm = new PaymentMethod();
        pm.setMethodId(1L);
        pm.setUserId(10L);
        pm.setCardType("CREDIT");
        pm.setCardNumberEnc("encryptedData");
        pm.setLastFour("1234");
        pm.setExpiryMonth(12);
        pm.setExpiryYear(2025);
        pm.setCardholderName("John Doe");
        pm.setBillingAddress("123 Main St");
        pm.setPinHash("pinHash123");
        pm.setDefault(true);
        LocalDateTime now = LocalDateTime.now();
        pm.setCreatedAt(now);
        pm.setBalance(new BigDecimal("1000.00"));

        assertEquals(1L, pm.getMethodId());
        assertEquals(10L, pm.getUserId());
        assertEquals("CREDIT", pm.getCardType());
        assertEquals("encryptedData", pm.getCardNumberEnc());
        assertEquals("1234", pm.getLastFour());
        assertEquals(12, pm.getExpiryMonth());
        assertEquals(2025, pm.getExpiryYear());
        assertEquals("John Doe", pm.getCardholderName());
        assertEquals("123 Main St", pm.getBillingAddress());
        assertEquals("pinHash123", pm.getPinHash());
        assertTrue(pm.isDefault());
        assertEquals(now, pm.getCreatedAt());
        assertEquals(new BigDecimal("1000.00"), pm.getBalance());
    }

    @Test
    void testGetMaskedNumber() {
        PaymentMethod pm = new PaymentMethod();
        pm.setLastFour("9876");
        assertEquals("**** **** **** 9876", pm.getMaskedNumber());
    }

    @Test
    void testGetExpiryFormatted() {
        PaymentMethod pm = new PaymentMethod();
        pm.setExpiryMonth(5);
        pm.setExpiryYear(2028);
        assertEquals("05/2028", pm.getExpiryFormatted());

        PaymentMethod pmEmpty = new PaymentMethod();
        assertEquals("", pmEmpty.getExpiryFormatted());
    }
}
