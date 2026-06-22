package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.entity.Ledger;

import java.util.List;

import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.LedgerType;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.LedgersRepository;
import com.paymentSystem.project.service.PaymentService;
import com.paymentSystem.project.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.paymentSystem.project.entity.Payments;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    void testEveryPaymentCreatesLedgerEntries() {
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","123"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0d);
        Payments payment = helper.createAndSavePayment(
                payer.getId(),
                payee.getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
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
    void testFailedPaymentAlsoCreatesLedger() {
        // Even failed payments should have ledger entries for audit
        Wallet payer= helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","123"), 100.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0d);

        Payments payment = helper.createAndSavePayment(
                payer.getId(),
                payee.getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );
        paymentService.verifyPayment(
            new VerifyPaymentRequest(payment.getId(), "wrong_pin"),
            "key1"
        );
        
        List<Ledger> ledgers = ledgersRepository.findByPaymentId(payment.getId());
        
        assertFalse(ledgers.isEmpty(), "Failed payment should still have ledger entry");
    }
}