package com.revature.revpay.controller;

import com.revature.revpay.model.User;
import com.revature.revpay.service.PaymentMethodService;
import com.revature.revpay.service.TransactionService;
import com.revature.revpay.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(com.revature.revpay.config.SecurityConfig.class)
class TransactionControllerTest {

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
    private TransactionService transactionService;

    @MockBean
    private UserService userService;

    @MockBean
    private PaymentMethodService paymentMethodService;

    // We also need to mock JwtUtil and UserDetailsService because SecurityConfig
    // pulls them in
    @MockBean
    private com.revature.revpay.util.JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@test.com");
    }

    @Test
    @WithMockUser("test@test.com")
    void testSendPage() throws Exception {
        when(userService.findByEmailOrPhone("test@test.com")).thenReturn(mockUser);
        when(paymentMethodService.getByUser(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/transactions/send"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/send"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("paymentMethods"));
    }

    @Test
    @WithMockUser("test@test.com")
    void testDoSend_NoPaymentMethods() throws Exception {
        when(userService.findByEmailOrPhone("test@test.com")).thenReturn(mockUser);
        when(paymentMethodService.getByUser(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/transactions/send")
                .param("recipient", "bob@example.com")
                .param("amount", "50.00")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment-methods"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    @Test
    @WithMockUser("test@test.com")
    void testDoSend_Success() throws Exception {
        when(userService.findByEmailOrPhone("test@test.com")).thenReturn(mockUser);
        when(paymentMethodService.getByUser(1L)).thenReturn(Collections.singletonList(new com.revature.revpay.model.PaymentMethod()));
        when(paymentMethodService.verifyPin(any(), anyString(), any())).thenReturn(true);
        when(transactionService.sendMoney(any(), anyString(), any(), any())).thenReturn("SUCCESS");

        mockMvc.perform(post("/transactions/send")
                .param("recipient", "bob@example.com")
                .param("amount", "50.00")
                .param("methodId", "10")
                .param("pin", "1234")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("successMsg"));
    }
}
