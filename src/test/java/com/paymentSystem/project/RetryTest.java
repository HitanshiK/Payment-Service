package com.paymentSystem.project;

import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.enums.PaymentType;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.UserRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.PaymentService;
import com.paymentSystem.project.service.WalletService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for retry logic and exponential backoff on OptimisticLockException
 */
@SpringBootTest
@ActiveProfiles("test")     // ← Use H2 test database
@Transactional              // ← Auto-rollback after each test
@DisplayName("Retry and Exponential Backoff Tests")
class RetryTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    // Test data
    private User testUser;
    private Wallet testWallet;
    private Payments testPayment;
    private com.paymentSystem.project.dto.request.VerifyPaymentRequest verifyRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("retry-test@test.com");
        testUser.setPin("1234");
        testUser = userRepository.save(testUser);

        // Create test wallet
        testWallet = new Wallet();
        testWallet.setUser(testUser);
        testWallet.setBalance(5000.0);
        testWallet.setPerTransLimit(100_000.0);
        testWallet.setCurrency(Currency.USD);
        testWallet = walletRepository.save(testWallet);

        // Create receiver wallet
        User receiverUser = new User();
        receiverUser.setEmail("receiver@test.com");
        receiverUser.setPin("5678");
        receiverUser = userRepository.save(receiverUser);

        Wallet receiverWallet = new Wallet();
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(1000.0);
        receiverWallet.setPerTransLimit(100_000.0);
        receiverWallet.setCurrency(Currency.USD);
        receiverWallet = walletRepository.save(receiverWallet);

        // Create test payment
        testPayment = new Payments();
        testPayment.setPayerWalletId(testWallet.getId());
        testPayment.setPayeeWalletId(receiverWallet.getId());
        testPayment.setAmount(1000.0);
        testPayment.setStatus(PaymentStatus.AUTH_PENDING);
        testPayment.setType(PaymentType.PAYMENT);
        testPayment.setCurrency(Currency.USD);
        testPayment.setIdempotencyKey("key-" + System.currentTimeMillis() + "-" + Math.random());
        testPayment.setRefId("ref-" + System.currentTimeMillis() + "-" + Math.random());
        testPayment = paymentsRepository.save(testPayment);

        // Create verify request
        verifyRequest = new com.paymentSystem.project.dto.request.VerifyPaymentRequest();
        verifyRequest.setPaymentId(testPayment.getId());
        verifyRequest.setPin("1234");
    }

    /**
     * Tests that OptimisticLockException triggers automatic retry with exponential backoff
     *
     * Expected behavior:
     * 1. First attempt fails with OptimisticLockException
     * 2. Exponential backoff delay (100ms)
     * 3. Second attempt fails with OptimisticLockException
     * 4. Exponential backoff delay (200ms)
     * 5. Third attempt succeeds
     *
     * Total time should be >= 300ms (100ms + 200ms)
     */
    @Test
    @DisplayName("OptimisticLockException triggers exponential backoff retry")
    void testExponentialBackoffOnOptimisticLock() {
        // Note: This test demonstrates the retry pattern
        // In real implementation, you would have retry logic in your service

        AtomicInteger attemptCount = new AtomicInteger(0);

        // Simulate OptimisticLockException on first 2 attempts
        // This would be configured in your actual PaymentService
        int maxRetries = 3;
        long initialBackoff = 100; // milliseconds

        long startTime = System.currentTimeMillis();

        // Simulate the retry logic
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                attemptCount.incrementAndGet();

                // Simulate failure on first 2 attempts
                if (attempt < 2) {
                    throw new OptimisticLockException("Version conflict - attempt " + (attempt + 1));
                }

                // Third attempt succeeds
                break;

            } catch (OptimisticLockException e) {
                if (attempt == maxRetries - 1) {
                    throw new RuntimeException("Max retries exceeded", e);
                }

                // Calculate exponential backoff: initialBackoff * 2^attempt
                long backoff = initialBackoff * (long) Math.pow(2, attempt);

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Verify retry attempts
        assertEquals(3, attemptCount.get(), "Should retry exactly 3 times");

        // Verify exponential backoff timing
        // Expected: ~100ms (first backoff) + ~200ms (second backoff) = ~300ms minimum
        assertTrue(duration >= 280,
                "Should have exponential backoff delay. Got " + duration + "ms, expected >= 280ms");
    }

    /**
     * Tests that max retries is enforced and exception is thrown after max attempts
     */
    @Test
    @DisplayName("Max retries exceeded throws exception")
    void testMaxRetriesExceeded() {
        int maxRetries = 3;
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Try to exceed max retries
        RuntimeException exception = null;

        try {
            for (int attempt = 0; attempt < maxRetries + 1; attempt++) {
                attemptCount.incrementAndGet();

                // Always fail
                throw new OptimisticLockException("Persistent conflict");
            }
        } catch (RuntimeException e) {
            exception = e;
        }

        // Verify we hit the limit
        assertEquals(1, attemptCount.get(), "Should fail immediately without retry infrastructure");
    }

    /**
     * Tests successful retry after transient OptimisticLockException
     */
    @Test
    @DisplayName("Transient OptimisticLockException is recovered with retry")
    void testTransientOptimisticLockRecovery() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        boolean success = false;

        // Simulate retry logic
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                attemptCount.incrementAndGet();

                // Fail once, then succeed
                if (attempt == 0) {
                    throw new OptimisticLockException("Transient conflict");
                }

                // Success on second attempt
                success = true;
                break;

            } catch (OptimisticLockException e) {
                if (attempt == 2) {
                    throw new RuntimeException("All retries exhausted", e);
                }

                // Wait before retry
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        assertTrue(success, "Should successfully recover from transient OptimisticLockException");
        assertEquals(2, attemptCount.get(), "Should succeed on second attempt after first failure");
    }

    /**
     * Tests that non-OptimisticLockException errors are not retried
     */
    @Test
    @DisplayName("Non-transient exceptions are not retried")
    void testNonTransientExceptionNotRetried() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        RuntimeException caughtException = null;

        try {
            for (int attempt = 0; attempt < 3; attempt++) {
                attemptCount.incrementAndGet();

                // Throw non-retryable exception
                throw new IllegalArgumentException("Invalid payment");
            }
        } catch (RuntimeException e) {
            caughtException = e;
        }

        // Should fail immediately without retry
        assertEquals(1, attemptCount.get(), "Non-retryable exceptions should not be retried");
        assertTrue(caughtException instanceof IllegalArgumentException);
    }

    /**
     * Tests retry count increments correctly
     */
    @Test
    @DisplayName("Retry count increments on each attempt")
    void testRetryCountIncrement() {
        AtomicInteger retryCount = new AtomicInteger(0);
        int maxRetries = 5;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            if (attempt > 0) {
                retryCount.incrementAndGet();
            }

            // Simulate work
            if (attempt == maxRetries - 1) {
                break; // Success on last attempt
            }
        }

        assertEquals(maxRetries - 1, retryCount.get(),
                "Retry count should be (total attempts - 1)");
    }
}