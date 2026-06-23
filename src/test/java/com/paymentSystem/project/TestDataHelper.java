package com.paymentSystem.project;

import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.enums.PaymentType;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.UserRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.service.PinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

@Component
public class TestDataHelper {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private PaymentsRepository paymentsRepository;
    @Autowired
    private PinService pinService;

    public User createAndSaveUser(String email, String pin) {
        User user = new User();
        user.setEmail(email);
        // hashPassword = BCrypt encode WITHOUT the 4-6 digit validation that hashPin enforces,
        // so existing tests using short/non-numeric PINs still set up without IllegalArgumentException.
        user.setPin(pinService.hashPassword(pin));
        return userRepository.save(user);
    }
    
    public Wallet createAndSaveWallet(User user, Double balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(balance);
        wallet.setCurrency(com.paymentSystem.project.enums.Currency.INR);
        wallet.setPerTransLimit(100_000.0);
        return walletRepository.save(wallet);
    }
    
    public Payments createAndSavePayment(
            Long payerWalletId,
            Long payeeWalletId,
            Double amount,
            PaymentStatus status) {
        Payments payment = new Payments();
        payment.setPayerWalletId(payerWalletId);
        payment.setPayeeWalletId(payeeWalletId);
        payment.setAmount(amount);
        payment.setCurrency(com.paymentSystem.project.enums.Currency.INR);
        payment.setStatus(status);
        payment.setType(PaymentType.PAYMENT);
        payment.setIdempotencyKey("key-" + System.currentTimeMillis() + "-" + Math.random());
        payment.setRefId("ref-" + System.currentTimeMillis() + "-" + Math.random());
        return paymentsRepository.save(payment);
    }
}