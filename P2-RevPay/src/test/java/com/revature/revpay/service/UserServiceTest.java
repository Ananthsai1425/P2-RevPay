package com.revature.revpay.service;

import com.revature.revpay.dao.BusinessProfileDAO;
import com.revature.revpay.dao.NotificationDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.BusinessProfile;
import com.revature.revpay.model.User;
import com.revature.revpay.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private BusinessProfileDAO businessProfileDAO;

    @Mock
    private NotificationDAO notificationDAO;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setFullName("Test User");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPhone("1234567890");
        sampleUser.setUsername("testuser");
        sampleUser.setPasswordHash(PasswordUtil.hash("password123"));
        sampleUser.setRole("USER");
        sampleUser.setAccountType("PERSONAL");
        sampleUser.setWalletBalance(new BigDecimal("100.00"));
    }

    @Test
    void testFindByEmailOrPhone() {
        when(userDAO.findByEmailOrPhone("test@example.com")).thenReturn(sampleUser);
        User result = userService.findByEmailOrPhone("test@example.com");
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userDAO).findByEmailOrPhone("test@example.com");
    }

    @Test
    void testFindByUsernameOrEmailOrPhone() {
        when(userDAO.findByUsernameOrEmailOrPhone("testuser")).thenReturn(sampleUser);
        User result = userService.findByUsernameOrEmailOrPhone("testuser");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userDAO).findByUsernameOrEmailOrPhone("testuser");
    }

    @Test
    void testFindById() {
        when(userDAO.findById(1L)).thenReturn(sampleUser);
        User result = userService.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(userDAO).findById(1L);
    }

    @Test
    void testRegisterPersonal_Success() {
        when(userDAO.emailExists("new@example.com")).thenReturn(false);
        when(userDAO.usernameExists("newuser")).thenReturn(false);
        when(userDAO.save(any(User.class))).thenReturn(2L);

        Long newId = userService.registerPersonal("New User", "new@example.com", "0987654321", "newuser", "pass123");
        assertEquals(2L, newId);
        
        verify(userDAO).save(any(User.class));
        verify(notificationDAO, times(6)).savePreference(any());
    }

    @Test
    void testRegisterPersonal_EmailExists() {
        when(userDAO.emailExists("test@example.com")).thenReturn(true);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            userService.registerPersonal("Test", "test@example.com", "123", "test", "pass")
        );
        assertEquals("Email already registered", ex.getMessage());
        verify(userDAO, never()).save(any(User.class));
    }

    @Test
    void testRegisterBusiness_Success() {
        when(userDAO.emailExists("biz@example.com")).thenReturn(false);
        when(userDAO.usernameExists("bizuser")).thenReturn(false);
        when(userDAO.save(any(User.class))).thenReturn(3L);
        
        // Mock admin retrieval for notifications
        User admin = new User();
        admin.setUserId(99L);
        when(userDAO.findByRole("ADMIN")).thenReturn(Arrays.asList(admin));

        Long newId = userService.registerBusiness("Biz User", "biz@example.com", "111", "bizuser", "pass123", 
                "My Business", "Retail", "TAX123", "123 Main St");
        
        assertEquals(3L, newId);
        
        verify(userDAO).save(any(User.class));
        verify(businessProfileDAO).save(any(BusinessProfile.class));
        verify(notificationDAO, times(6)).savePreference(any()); // 6 default prefs
        verify(notificationDAO).save(any(com.revature.revpay.model.Notification.class)); // 1 admin notif
    }

    @Test
    void testChangePassword_Success() {
        when(userDAO.findById(1L)).thenReturn(sampleUser);
        
        boolean result = userService.changePassword(1L, "password123", "newPassword456");
        
        assertTrue(result);
        verify(userDAO).updatePassword(eq(1L), anyString());
    }

    @Test
    void testChangePassword_WrongPassword() {
        when(userDAO.findById(1L)).thenReturn(sampleUser);
        
        boolean result = userService.changePassword(1L, "wrongpass", "newPassword456");
        
        assertFalse(result);
        verify(userDAO, never()).updatePassword(anyLong(), anyString());
    }
}
