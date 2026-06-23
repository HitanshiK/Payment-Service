package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Ledger;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.LedgersRepository;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.PaymentService;
import com.paymentSystem.project.service.IdempotencyService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")  // ← Add this
@Transactional
class PaymentIntegrationTest {
    
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentsRepository paymentsRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private LedgersRepository ledgersRepository;
    @Autowired
    TestDataHelper helper;
    @MockBean
    private IdempotencyService idempotencyService;

    @BeforeEach
    void stubIdempotencyCache() {
        when(idempotencyService.getCachedResponse(anyString())).thenReturn(Optional.empty());
    }
    
    @Test
    void testSuccessfulPaymentUpdatesAllEntities() {
        // Arrange - Create test data
        User payer = helper.createAndSaveUser("payer@test.com","123");
        User payee = helper.createAndSaveUser("payee@test.com","234");
        
        Wallet payerWallet = helper.createAndSaveWallet(payer, 5000.0);
        Wallet payeeWallet = helper.createAndSaveWallet(payee, 1000.0);
        
        Payments payment = helper.createAndSavePayment(
            payerWallet.getId(),
            payeeWallet.getId(),
            1000.0,
            PaymentStatus.AUTH_PENDING
        );
        
        // Act
        VerifyPaymentRequest request = new VerifyPaymentRequest(
            payment.getId(), "correct_pin"
        );
        PaymentResponse response = paymentService.verifyPayment(request, "key123");
        
        // Assert
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        
        // Verify wallet balances
        Wallet updatedPayerWallet = walletRepository.findById(payerWallet.getId()).orElseThrow();
        Wallet updatedPayeeWallet = walletRepository.findById(payeeWallet.getId()).orElseThrow();
        
        assertEquals(4000.0, updatedPayerWallet.getBalance());  // 5000 - 1000
        assertEquals(2000.0, updatedPayeeWallet.getBalance());  // 1000 + 1000
        
        // Verify payment status
        Payments updatedPayment = paymentsRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());
        
        // Verify ledger entries created
        List<Ledger> ledgers = ledgersRepository.findByPaymentId(payment.getId());
        assertEquals(2, ledgers.size());  // One debit, one credit
    }
    
    @Test
    void testFailureRollsBackAllChanges() {
        // Arrange
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","123"), 100.0);
        Payments payment = helper.createAndSavePayment(
            wallet.getId(),
            helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0d).getId(),
            1000.0,  // More than available
            PaymentStatus.AUTH_PENDING
        );
        
        // Act
        try {
            paymentService.verifyPayment(
                new VerifyPaymentRequest(payment.getId(), "correct_pin"),
                "key123"
            );
        } catch (RuntimeException e) {
            // Expected
        }
        
        // Assert - Wallet should remain unchanged
        Wallet unchanged = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(100.0, unchanged.getBalance());
    }
}