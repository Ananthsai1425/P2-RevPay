package com.revature.revpay.controller;

import com.revature.revpay.service.UserService;
import com.revature.revpay.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
// Import security context if needed, or disable it
@Import(com.revature.revpay.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.revature.revpay.dao.NotificationDAO notificationDAO;

    @MockBean
    private com.revature.revpay.dao.MoneyRequestDAO moneyRequestDAO;

    @MockBean
    private com.revature.revpay.dao.InvoiceDAO invoiceDAO;

    @MockBean
    private com.revature.revpay.dao.LoanApplicationDAO loanApplicationDAO;

    @MockBean
    private com.revature.revpay.dao.BusinessProfileDAO businessProfileDAO;

    @MockBean
    private com.revature.revpay.service.NotificationService notificationService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void testRegisterPage() throws Exception {
        mockMvc.perform(get("/auth/register").param("type", "personal"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("accountType"));
    }

    @Test
    void testRegisterPersonal_Success() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("accountType", "PERSONAL")
                .param("fullName", "John Doe")
                .param("email", "john@example.com")
                .param("phone", "1234567890")
                .param("username", "johndoe")
                .param("password", "pass123")
                .param("confirmPassword", "pass123")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMsg"));
    }

    @Test
    void testRegisterPersonal_PasswordMismatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("accountType", "PERSONAL")
                .param("fullName", "John")
                .param("email", "john@example.com")
                .param("phone", "12345")
                .param("username", "john")
                .param("password", "pass123")
                .param("confirmPassword", "wrongpass")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register?type=PERSONAL"))
                .andExpect(flash().attributeExists("errorMsg"));
    }
}
