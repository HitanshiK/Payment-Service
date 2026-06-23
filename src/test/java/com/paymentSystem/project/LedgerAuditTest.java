package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Ledger;

import java.util.List;

import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.LedgerType;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.LedgersRepository;
import com.paymentSystem.project.service.PaymentService;
import com.paymentSystem.project.service.WalletService;
import com.paymentSystem.project.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.paymentSystem.project.entity.Payments;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")  // ← Add this
class LedgerAuditTest {

    @Autowired
    TestDataHelper helper;
    @Autowired
    PaymentService paymentService;
    @Autowired
    WalletService walletService;
    @Autowired
    LedgersRepository ledgersRepository;
    @MockBean
    IdempotencyService idempotencyService;

    @BeforeEach
    void stubIdempotencyCache() {
        when(idempotencyService.getCachedResponse(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void testEveryPaymentCreatesLedgerEntries() {
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","1234"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","2345"), 0d);
        Payments payment = helper.createAndSavePayment(
                payer.getId(),
                payee.getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );

        // Actually process the payment so the debit + credit ledgers get written.
        paymentService.verifyPayment(
                new VerifyPaymentRequest(payment.getId(), "1234"),
                "ledger-audit-key"
        );

        List<Ledger> ledgers = ledgersRepository.findByPaymentId(payment.getId());

        // Should have debit AND credit
        assertEquals(2, ledgers.size());
        assertEquals(LedgerType.DEBIT, ledgers.get(0).getLedgerType());
        assertEquals(LedgerType.CREDIT, ledgers.get(1).getLedgerType());
        
        // Amounts should match
        assertEquals(1000.0, Math.abs(ledgers.get(0).getAmount()));
        assertEquals(1000.0, Math.abs(ledgers.get(1).getAmount()));
    }
    
    @Test
    void testFailedPaymentDoesNotCreateLedger() {
        // A failed payment moves no money, so it writes NO ledger entry.
        // The failure is recorded at the payment level (status + failure reason), not in the ledger.
        Wallet payer= helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","1234"), 100.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","2345"), 0d);

        Payments payment = helper.createAndSavePayment(
                payer.getId(),
                payee.getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );

        // Wrong PIN -> payment fails before any money movement.
        PaymentResponse response = paymentService.verifyPayment(
            new VerifyPaymentRequest(payment.getId(), "1235"),
            "key1"
        );

        // Failure is captured on the payment itself...
        assertEquals(PaymentStatus.FAILED.toString(), response.getStatus());

        // ...and no ledger entries are written.
        List<Ledger> ledgers = ledgersRepository.findByPaymentId(payment.getId());
        assertTrue(ledgers.isEmpty(), "Failed payment should not create ledger entries");
    }
}