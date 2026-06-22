package com.paymentSystem.project;

import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.repos.PaymentsRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")  // ← Add this
class PessimisticLockTest {
    
    @Autowired
    private PaymentsRepository paymentsRepository;
    @Autowired
    private TestDataHelper helper;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Test
    void testFindByIdForUpdateLocksRecord() throws Exception {
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","123"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"),
                1000d);  // Near max (10M)

        Payments payment = helper.createAndSavePayment(
                payer.getId(),
                payee.getId(),
                10d,
                PaymentStatus.AUTH_PENDING
        );
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        
        // Thread 1: Lock the record
        executor.submit(() -> {
            transactionTemplate.execute(status -> {
                Payments p = paymentsRepository.findById(payment.getId())
                    .orElseThrow();
                locked.countDown();
                try {
                    Thread.sleep(3000);  // Hold lock for 3 seconds
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return p;
            });
            done.countDown();
        });
        
        locked.await();  // Wait until lock acquired
        
        // Thread 2: Try to access (should wait)
        long startTime = System.currentTimeMillis();
        executor.submit(() -> {
            transactionTemplate.execute(status -> {
                Payments p = paymentsRepository.findById(payment.getId())
                    .orElseThrow();
                return p;
            });
        });
        
        done.await();
        long duration = System.currentTimeMillis() - startTime;
        
        // Thread 2 should have waited ~3 seconds
        assertTrue(duration >= 3000, "Lock should have caused wait");
        executor.shutdown();
    }
}