package com.revature.revpay.service;

import com.revature.revpay.dao.PaymentMethodDAO;
import com.revature.revpay.model.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodDAO paymentMethodDAO;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    @Test
    void testAddCard_MakeDefault() {
        PaymentMethod existingCard = new PaymentMethod();
        existingCard.setDefault(true);
        when(paymentMethodDAO.findByUserId(1L)).thenReturn(Collections.singletonList(existingCard));
        when(paymentMethodDAO.save(any(PaymentMethod.class))).thenReturn(10L);

        Long cardId = paymentMethodService.addCard(1L, "CREDIT", "1234567890123456", "John Doe", 12, 2025, "123 St",
                true);

        assertEquals(10L, cardId);
        verify(paymentMethodDAO).setDefault(-1L, 1L); // clears old defaults
        verify(paymentMethodDAO).save(any(PaymentMethod.class));
        verify(notificationService).send(eq(1L), anyString(), anyString(), eq("CARD_CHANGE"));
    }

    @Test
    void testGetByUser() {
        List<PaymentMethod> mockList = Collections.singletonList(new PaymentMethod());
        when(paymentMethodDAO.findByUserId(1L)).thenReturn(mockList);

        List<PaymentMethod> result = paymentMethodService.getByUser(1L);

        assertEquals(1, result.size());
        verify(paymentMethodDAO).findByUserId(1L);
    }

    @Test
    void testDelete_Authorized() {
        PaymentMethod card = new PaymentMethod();
        card.setUserId(1L);
        when(paymentMethodDAO.findById(10L)).thenReturn(card);

        paymentMethodService.delete(10L, 1L);

        verify(paymentMethodDAO).delete(10L);
        verify(notificationService).send(eq(1L), anyString(), anyString(), eq("CARD_CHANGE"));
    }

    @Test
    void testDelete_Unauthorized() {
        PaymentMethod card = new PaymentMethod();
        card.setUserId(2L); // Different user
        when(paymentMethodDAO.findById(10L)).thenReturn(card);

        paymentMethodService.delete(10L, 1L);

        verify(paymentMethodDAO, never()).delete(anyLong());
        verify(notificationService, never()).send(anyLong(), anyString(), anyString(), anyString());
    }
}
