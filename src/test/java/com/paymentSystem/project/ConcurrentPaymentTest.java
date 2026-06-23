package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.IdempotencyService;
import com.paymentSystem.project.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentPaymentTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    TestDataHelper helper;
    @MockBean
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setup() {
        // Mock IdempotencyService to avoid Redis connection in tests
        when(idempotencyService.getCachedResponse(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void testConcurrentDuplicateRequests_OnlyOneSucceeds() throws Exception {
        // Arrange: one payment, healthy balance (balance is NOT the constraint here —
        // the payment's own status + version lock is what should allow only one debit)
        Wallet wallet = helper.createAndSaveWallet(
                helper.createAndSaveUser("user@test.com", "1234"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(
                helper.createAndSaveUser("user2@test.com", "5678"), 0.0);

        Payments payment = helper.createAndSavePayment(
                wallet.getId(), payee.getId(), 1000.0, PaymentStatus.AUTH_PENDING);

        VerifyPaymentRequest request = new VerifyPaymentRequest(payment.getId(), "1234");

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        List<PaymentResponse> responses = Collections.synchronizedList(new ArrayList<>());
        List<String> failures = Collections.synchronizedList(new ArrayList<>());

        // Act: each thread uses a DIFFERENT idempotency key (so Layer 1 cache can't
        // dedupe), all hitting the SAME payment at the SAME time.
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    System.out.println("🔵 Thread-" + index + " started");
                    ready.countDown();
                    start.await();   // release all threads together for a real race
                    System.out.println("⚡ Thread-" + index + " executing verifyPayment");
                    String idempotencyKey = UUID.randomUUID().toString();
                    PaymentResponse response = paymentService.verifyPayment(request, idempotencyKey);
                    System.out.println("✅ Thread-" + index + " got response: status=" + response.getStatus() +
                            ", message=" + response.getMessage());
                    responses.add(response);
                } catch (Exception e) {
                    System.out.println("❌ Thread-" + index + " failed: " + e.getClass().getSimpleName() +
                            " - " + e.getMessage());
                    e.printStackTrace();
                    failures.add("Thread-" + index + ": " + e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);   // wait until all threads are armed
        start.countDown();                  // fire simultaneously
        System.out.println("🚀 All threads released simultaneously at " + System.currentTimeMillis());
        boolean completed = done.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        System.out.println("\n📊 Test Results:");
        System.out.println("   Responses: " + responses.size());
        System.out.println("   Failures: " + failures.size());
        for (int i = 0; i < responses.size(); i++) {
            PaymentResponse r = responses.get(i);
            System.out.println("   Response[" + i + "]: status=" + r.getStatus() + ", message=" + r.getMessage());
        }
        failures.forEach(f -> System.out.println("   Failure: " + f));

        assertTrue(completed, "All threads should finish within 15s");
        assertEquals(threadCount, responses.size() + failures.size(),
                "Every thread should produce either a response or a failure");

        long successCount = responses.stream()
                .filter(r -> PaymentStatus.SUCCESS.toString().equals(r.getStatus()))
                .count();

        System.out.println("\n   Success count: " + successCount + " (expected: 1)");
        assertEquals(1, successCount, "Only one concurrent attempt should succeed");

        // The losers should fail for the RIGHT reason (status guard / lock), not e.g. NPE
        assertEquals(threadCount - 1,
                responses.stream()
                        .filter(r -> !PaymentStatus.SUCCESS.toString().equals(r.getStatus()))
                        .count()
                        + failures.size(),
                "All other attempts should be rejected");

        // Money safety: debited exactly once
        Wallet finalPayer = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(4000.0, finalPayer.getBalance(),
                "Wallet must be debited exactly once");

        Wallet finalPayee = walletRepository.findById(payee.getId()).orElseThrow();
        assertEquals(1000.0, finalPayee.getBalance(),
                "Payee must be credited exactly once");
    }
    
    @Test
    void testConcurrentDebitAndCreditRaceCondition() throws Exception {
        // This tests: Payer debits, but Payee credit fails - what happens?
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("payer@test.com","1234"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("payee@test.com","5678"),
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
                new VerifyPaymentRequest(payment.getId(), "1234"),
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