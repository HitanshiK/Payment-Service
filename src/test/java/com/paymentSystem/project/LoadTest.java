package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.paymentSystem.project.entity.Payments;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
class LoadTest {
    @Autowired
    TestDataHelper helper;
    @Autowired
    PaymentService paymentService;
    @Test
    void testHighVolumeTransactions() throws Exception {
        // Create 100 users with 1000 each
        List<Wallet> wallets = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            wallets.add(helper.createAndSaveWallet(
                helper.createAndSaveUser("user" + i + "@test.com","123"),
                1000.0
            ));
        }
        
        // Create 500 payments
        List<Payments> payments = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            payments.add(helper.createAndSavePayment(
                wallets.get(i % 100).getId(),
                wallets.get((i + 1) % 100).getId(),
                10.0,
                PaymentStatus.AUTH_PENDING
            ));
        }
        
        // Execute all concurrently
        ExecutorService executor = Executors.newFixedThreadPool(50);
        long startTime = System.currentTimeMillis();
        
        payments.forEach(payment -> {
            executor.submit(() -> {
                paymentService.verifyPayment(
                    new VerifyPaymentRequest(payment.getId(), "pin"),
                    UUID.randomUUID().toString()
                );
            });
        });
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Processed 500 payments in " + duration + "ms");
        System.out.println("Throughput: " + (500000.0 / duration) + " payments/sec");
    }
}