package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.service.IdempotencyService;
import com.paymentSystem.project.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentVerificationServiceTest {

    @Autowired
    PaymentService paymentService;
    @Autowired
    TestDataHelper helper;
    @MockBean
    IdempotencyService idempotencyService;

    @BeforeEach
    void stubIdempotencyCache() {
        // Unblock the Redis call in verifyPayment; the real flow runs against the in-memory DB.
        when(idempotencyService.getCachedResponse(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void testInvalidPinReturnsFailure() {
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com", "1234"), 5000.0);
        Payments payment = helper.createAndSavePayment(
                wallet.getId(),
                helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com", "2345"), 0.0).getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );

        // Wrong PIN (stored hash is for "1234") -> fails at the PIN check before any money movement.
        PaymentResponse response = paymentService.verifyPayment(
                new VerifyPaymentRequest(payment.getId(), "0000"), "key-invalid-pin");

        assertEquals(PaymentStatus.FAILED.toString(), response.getStatus());
        assertEquals("INVALID PIN", response.getMessage());
    }

    @Test
    void testInsufficientBalanceReturnsFailure() {
        // Correct PIN ("1243"), but balance (100) < amount (1000) -> underflow check fails.
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com", "1243"), 100.0);
        Payments payment = helper.createAndSavePayment(
                wallet.getId(),
                helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com", "2345"), 0.0).getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );

        PaymentResponse response = paymentService.verifyPayment(
                new VerifyPaymentRequest(payment.getId(), "1243"), "key-insufficient");

        assertEquals(PaymentStatus.FAILED.toString(), response.getStatus());
        assertEquals("INSUFFICIENT BALANCE", response.getMessage());
    }
}
