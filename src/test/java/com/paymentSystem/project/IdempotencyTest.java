package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.CachedResponse;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.IdempotencyService;
import com.paymentSystem.project.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
@ActiveProfiles("test")  // ← Add this
@SpringBootTest
class IdempotencyTest {
    
    @Autowired
    private PaymentService paymentService;
    @MockBean
    private IdempotencyService idempotencyService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    TestDataHelper helper;

    // In-memory stand-in for the Redis cache so we can verify real idempotent replay
    // without a live Redis: a miss on first call, a hit on retry with the same key.
    private final Map<String, CachedResponse> cacheStore = new HashMap<>();

    @BeforeEach
    void stubIdempotencyCache() {
        cacheStore.clear();
        when(idempotencyService.getCachedResponse(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(cacheStore.get(inv.<String>getArgument(0))));
        doAnswer(inv -> {
            cacheStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(idempotencyService).cacheResponse(anyString(), any(CachedResponse.class));
    }

    @Test
    void testDuplicateRequestWithSameKeyIsIdempotent() {
        // Arrange
        Wallet payer = helper.createAndSaveWallet(helper.createAndSaveUser("user1@test.com","1234"), 5000.0);
        Wallet payee = helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","2345"), 0d);
        Payments payment = helper.createAndSavePayment(
            payer.getId(),
            payee.getId(),
            1000.0,
            PaymentStatus.AUTH_PENDING
        );
        
        String idempotencyKey = "request-12345";
        // PIN must match the payer's stored hash (user created with "123"), otherwise the
        // first call fails at the PIN check and never debits — making the replay assertion moot.
        VerifyPaymentRequest request = new VerifyPaymentRequest(payment.getId(), "1234");
        
        // Act - First request
        PaymentResponse response1 = paymentService.verifyPayment(request, idempotencyKey);
        double balanceAfterFirst = walletRepository.findById(payer.getId())
            .orElseThrow().getBalance();
        
        // Act - Retry with same key
        PaymentResponse response2 = paymentService.verifyPayment(request, idempotencyKey);
        double balanceAfterSecond = walletRepository.findById(payer.getId())
            .orElseThrow().getBalance();
        
        // Assert
        assertEquals(response1.getId(), response2.getId());
        assertEquals(balanceAfterFirst, balanceAfterSecond, 
            "Balance should NOT change on retry - should be idempotent");
        assertEquals(4000.0, balanceAfterFirst, "Should only be debited once");
    }
}