package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.IdempotencyService;
import com.paymentSystem.project.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")  // ← Add this
@SpringBootTest
class EdgeCaseTest {

    @Autowired
    TestDataHelper helper;
    @Autowired
    PaymentService paymentService;
    @Autowired
    WalletRepository walletRepository;
    @MockBean
    IdempotencyService idempotencyService;

    @BeforeEach
    void stubIdempotencyCache() {
        // Unblock the Redis call in verifyPayment; DB idempotency record + version lock still apply.
        when(idempotencyService.getCachedResponse(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void testWalletOverflowHandling() {
        // Crediting the payee would push it past MAX_WALLET_BALANCE (500,000).
        // Payment stays within the per-transaction limit (100,000) so it reaches the credit step.
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","1234"), 2_00_000d);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","2345"),
                4_50_000d);  // 50,000 below the 500,000 cap

        Payments payment = helper.createAndSavePayment(
            payer.getId(),
            payee.getId(),
            80_000d,  // 450,000 + 80,000 = 530,000 -> 30,000 over the cap
            PaymentStatus.AUTH_PENDING
        );

        PaymentResponse response = paymentService.verifyPayment(
            new VerifyPaymentRequest(payment.getId(), "1234"),
            "key1"
        );

        // Only the amount up to the cap is credited; the excess is reversed -> PARTIAL_SUCCESS
        assertEquals(PaymentStatus.PARTIAL_SUCCESS.toString(), response.getStatus());

        Wallet finalPayee = walletRepository.findById(payee.getId()).orElseThrow();
        assertTrue(finalPayee.getBalance() <= 5_00_000d,
            "Payee balance should not exceed max");
        assertEquals(5_00_000d, finalPayee.getBalance(),
            "Payee should be credited exactly up to the cap");
    }
    
    @Test
    void testDailyLimitExceeded() {
        // Real daily limit is 300,000; default per-transaction cap is 100,000.
        // Raise this payer's per-transaction limit so two 200,000 payments are individually
        // allowed but together (400,000) breach the daily cap on the second.
        User user = helper.createAndSaveUser("user@test.com","1234");
        Wallet wallet = helper.createAndSaveWallet(user, 5_00_000d);
        wallet.setPerTransLimit(2_00_000d);
        walletRepository.save(wallet);

        User user2 = helper.createAndSaveUser("user2@test.com","2345");
        Wallet wallet2 = helper.createAndSaveWallet(user2, 0d);

        // ===== First payment (200,000): daily total 200,000 <= 300,000 -> SUCCESS =====

        Payments payment1 = helper.createAndSavePayment(
                wallet.getId(),
                wallet2.getId(),
                2_00_000d,
                PaymentStatus.AUTH_PENDING);

        PaymentResponse response1 = paymentService.verifyPayment(
                new VerifyPaymentRequest(payment1.getId(), "1234"),
                "daily-limit-key-1"
        );

        assertEquals(PaymentStatus.SUCCESS.toString(), response1.getStatus());
        Wallet walletAfterFirst = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(3_00_000d, walletAfterFirst.getBalance());  // 500,000 - 200,000

        // ===== Second payment (200,000): daily total 400,000 > 300,000 -> FAILED =====

        Payments payment2= helper.createAndSavePayment(
                wallet.getId(),
                wallet2.getId(),
                2_00_000d,
                PaymentStatus.AUTH_PENDING);

        PaymentResponse response2 = paymentService.verifyPayment(
                new VerifyPaymentRequest(payment2.getId(), "1234"),
                "daily-limit-key-2"
        );

        // ===== ASSERT =====

        assertEquals(PaymentStatus.FAILED.toString(), response2.getStatus());
        assertEquals("Daily Transaction Limit reached", response2.getMessage());

        // Wallet should NOT be debited for the second payment
        Wallet walletAfterSecond = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(3_00_000d, walletAfterSecond.getBalance(),
                "Balance should remain unchanged after failed daily limit check");

        // Payee wallet should only have the first payment credited
        Wallet wallet2After = walletRepository.findById(wallet2.getId()).orElseThrow();
        assertEquals(2_00_000d, wallet2After.getBalance());  // 0 + 200,000
    }
    
    @Test
    void testPerTransactionLimitExceeded() {
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com","1234"), 100_000.0);
        wallet.setPerTransLimit(1000.0);
        walletRepository.save(wallet);
        
        Payments payment = helper.createAndSavePayment(
            wallet.getId(),
            helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","2345"), 0d).getId(),
            2000.0,  // Exceeds per-trans limit
            PaymentStatus.AUTH_PENDING
        );
        
        PaymentResponse response = paymentService.verifyPayment(
            new VerifyPaymentRequest(payment.getId(), "1234"),
            "key1"
        );
        
        assertEquals("Per Transaction Limit Breached", response.getMessage());
    }
}