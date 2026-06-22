package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
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

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentPaymentTest {
    
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    TestDataHelper helper;
    
    @Test
    void testConcurrentPaymentAttemptsOnlyOneSucceeds() throws Exception {
        // Arrange
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com","123"), 5000.0);
        Payments payment = helper.createAndSavePayment(
            wallet.getId(),
            helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0.0).getId(),
            1000.0,
            PaymentStatus.AUTH_PENDING
        );
        
        VerifyPaymentRequest request = new VerifyPaymentRequest(payment.getId(), "123");
        
        // Act - 5 concurrent attempts
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<PaymentResponse> responses = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    PaymentResponse response = paymentService.verifyPayment(request, "key123");
                    responses.add(response);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Assert - Only ONE should succeed
        long successCount = responses.stream()
            .filter(r -> r.getStatus().equals(PaymentStatus.SUCCESS))
            .count();
        assertEquals(1, successCount, "Only one concurrent payment should succeed");
        
        // Verify wallet debited only once
        Wallet final_wallet = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(4000.0, final_wallet.getBalance(), "Wallet should be debited exactly once");
    }
    
    @Test
    void testConcurrentDebitAndCreditRaceCondition() throws Exception {
        // This tests: Payer debits, but Payee credit fails - what happens?
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("payer@test.com","123"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("payee@test.com","234"),
            9_999_000.0);  // Near max balance to trigger overflow
        
        Payments payment = helper.createAndSavePayment(
            payer.getId(),
            payee.getId(),
            1000.0,
            PaymentStatus.AUTH_PENDING
        );
        
        // Simulate concurrent: Payment process + Someone else trying to credit payee
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        executor.submit(() -> {
            paymentService.verifyPayment(
                new VerifyPaymentRequest(payment.getId(), "pin"),
                "key1"
            );
        });
        
        executor.submit(() -> {
            // Simulate another payment trying to credit same payee
            try {
                Thread.sleep(50);  // Let first payment start
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Wallet toCredit = walletRepository.findById(payee.getId()).orElseThrow();
            toCredit.setBalance(10_000_000.0);  // Push past overflow
            walletRepository.save(toCredit);
        });
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // Assert - Check for money loss
        Wallet finalPayer = walletRepository.findById(payer.getId()).orElseThrow();
        Wallet finalPayee = walletRepository.findById(payee.getId()).orElseThrow();
        
        // Money should not disappear!
        assertTrue(finalPayer.getBalance() >= 4000.0, "Payer should not lose extra money");
    }
}