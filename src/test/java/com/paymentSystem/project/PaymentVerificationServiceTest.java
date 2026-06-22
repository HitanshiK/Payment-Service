package com.paymentSystem.project;

import com.paymentSystem.project.dto.request.VerifyPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.PaymentService;
import com.paymentSystem.project.service.PinService;
import com.paymentSystem.project.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")  // ← Add this
class PaymentVerificationServiceTest<PaymentVerificationService> {
    
    @Mock
    private PaymentsRepository paymentsRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PinService pinService;
    @Mock
    private WalletService walletService;
    @InjectMocks
    private PaymentService paymentService;
    @Autowired
    TestDataHelper helper;
    
    @Test
    void testInvalidPinReturnsFailure() {
        // Arrange

        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com","123"), 5000.0);
        Payments payment = helper.createAndSavePayment(
                wallet.getId(),
                helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0.0).getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );
        VerifyPaymentRequest request = new VerifyPaymentRequest(payment.getId(), "1234");

        
        when(paymentsRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(walletRepository.findById(payment.getPayerWalletId())).thenReturn(Optional.of(wallet));
        when(pinService.verifyPin("1234", wallet.getUser())).thenReturn(false);
        
        // Act
        PaymentResponse response = paymentService.verifyPayment(request, "key123");
        
        // Assert
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("INVALID PIN", response.getMessage());
    }
    
    @Test
    void testInsufficientBalanceReturnsFailure() {
        // Arrange
        Wallet wallet = helper.createAndSaveWallet(helper.createAndSaveUser("user@test.com","123"), 100.0);
        Payments payment = helper.createAndSavePayment(
                wallet.getId(),
                helper.createAndSaveWallet(helper.createAndSaveUser("user2@test.com","234"), 0.0).getId(),
                1000.0,
                PaymentStatus.AUTH_PENDING
        );
        VerifyPaymentRequest request = new VerifyPaymentRequest(payment.getId(), "1234");
        
        when(paymentsRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(walletRepository.findById(payment.getPayerWalletId())).thenReturn(Optional.of(wallet));
        when(pinService.verifyPin("1234", wallet.getUser())).thenReturn(true);
        when(walletService.checkWalletUnderFlow(wallet, 1000.0)).thenReturn(true);
        
        // Act
        PaymentResponse response = paymentService.verifyPayment(request, "key123");
        
        // Assert
        assertEquals("INSUFFICIENT BALANCE", response.getMessage());
    }
}