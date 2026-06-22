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
import org.springframework.test.context.ActiveProfiles;

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
    
    @Test
    void testWalletOverflowHandling() {
        // Test partial success scenario
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","123"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"),
            9_999_000.0);  // Near max (10M)
        
        Payments payment = helper.createAndSavePayment(
            payer.getId(),
            payee.getId(),
            2_000_000.0,  // Would overflow payee
            PaymentStatus.AUTH_PENDING
        );
        
        PaymentResponse response = paymentService.verifyPayment(
            new VerifyPaymentRequest(payment.getId(), "pin"),
            "key1"
        );
        
        // Should be PARTIAL_SUCCESS, not complete loss
        assertEquals(PaymentStatus.PARTIAL_SUCCESS, response.getStatus());
        
        Wallet finalPayee = walletRepository.findById(payee.getId()).orElseThrow();
        assertTrue(finalPayee.getBalance() <= 10_000_000.0, 
            "Payee balance should not exceed max");
    }
    
    @Test
    void testDailyLimitExceeded() {
        User user = helper.createAndSaveUser("user@test.com","123");
        Wallet wallet = helper.createAndSaveWallet(user, 100_000.0);

        User user2 = helper.createAndSaveUser("user2@test.com","234");
        Wallet wallet2 = helper.createAndSaveWallet(user2, 1000.0);

        // ===== ACT - First Payment (3000) - Should SUCCEED =====

        Payments payment1 = helper.createAndSavePayment(
                wallet.getId(),
                wallet2.getId(),
                3000.0,
                PaymentStatus.AUTH_PENDING);

        PaymentResponse response1 = paymentService.verifyPayment(
                new VerifyPaymentRequest(payment1.getId(), "correct_pin"),
                "daily-limit-key-1"
        );

        // Verify first payment succeeded
        assertEquals(PaymentStatus.SUCCESS, response1.getStatus());

        Wallet walletAfterFirst = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(97_000.0, walletAfterFirst.getBalance());  // 100,000 - 3,000

        // ===== ACT - Second Payment (3000) - Should FAIL (total 6000 > 5000 limit) =====

        Payments payment2= helper.createAndSavePayment(
                wallet.getId(),
                wallet2.getId(),
                3000.0,
                PaymentStatus.AUTH_PENDING);

        PaymentResponse response2 = paymentService.verifyPayment(
                new VerifyPaymentRequest(payment2.getId(), "correct_pin"),
                "daily-limit-key-2"
        );

        // ===== ASSERT =====

        // Second payment should fail
        assertEquals(PaymentStatus.FAILED, response2.getStatus());
        assertEquals("Daily Transaction Limit reached", response2.getMessage());

        // Wallet should NOT be debited for second payment
        Wallet walletAfterSecond = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(97_000.0, walletAfterSecond.getBalance(),
                "Balance should remain unchanged after failed daily limit check");

        // Payee wallet should only have first payment credited
        Wallet wallet2After = walletRepository.findById(wallet2.getId()).orElseThrow();
        assertEquals(4000.0, wallet2After.getBalance());  // 1000 + 300
    }
    
    @Test
    void testPerTransactionLimitExceeded() {
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com","123"), 100_000.0);
        wallet.setPerTransLimit(1000.0);
        walletRepository.save(wallet);
        
        Payments payment = helper.createAndSavePayment(
            wallet.getId(),
            helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0d).getId(),
            2000.0,  // Exceeds per-trans limit
            PaymentStatus.AUTH_PENDING
        );
        
        PaymentResponse response = paymentService.verifyPayment(
            new VerifyPaymentRequest(payment.getId(), "pin"),
            "key1"
        );
        
        assertEquals("Per Transaction Limit Breached", response.getMessage());
    }
}