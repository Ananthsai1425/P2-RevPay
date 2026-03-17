package com.revature.revpay.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserConstructorsAndGettersSetters() {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPhone("1234567890");
        user.setUsername("johndoe");
        user.setPasswordHash("hash123");
        user.setRole("USER");
        user.setAccountType("PERSONAL");
        user.setWalletBalance(new BigDecimal("100.50"));
        user.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertEquals(1L, user.getUserId());
        assertEquals("John Doe", user.getFullName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("1234567890", user.getPhone());
        assertEquals("johndoe", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("USER", user.getRole());
        assertEquals("PERSONAL", user.getAccountType());
        assertEquals(new BigDecimal("100.50"), user.getWalletBalance());
        assertTrue(user.isActive());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());

        User userWithArgs = new User(2L, "Jane Doe", "jane@example.com", "0987654321", "janedoe", "hash456", "ADMIN",
                "BUSINESS", new BigDecimal("500.00"), false);
        assertEquals(2L, userWithArgs.getUserId());
        assertFalse(userWithArgs.isActive());
    }

    @Test
    void testUserHelperMethods() {
        User user = new User();

        user.setAccountType("PERSONAL");
        assertTrue(user.isPersonal());
        assertFalse(user.isBusiness());

        user.setAccountType("BUSINESS");
        assertTrue(user.isBusiness());
        assertFalse(user.isPersonal());

        user.setRole("ADMIN");
        assertTrue(user.isAdmin());

        user.setRole("USER");
        assertFalse(user.isAdmin());

        user.setWalletBalance(null);
        assertEquals(BigDecimal.ZERO, user.getWalletBalance());
    }

    @Test
    void testToString() {
        User user = new User();
        user.setUserId(5L);
        user.setUsername("testuser");
        user.setRole("USER");
        user.setAccountType("PERSONAL");
        String result = user.toString();
        assertTrue(result.contains("5"));
        assertTrue(result.contains("testuser"));
        assertTrue(result.contains("USER"));
        assertTrue(result.contains("PERSONAL"));
    }
}
