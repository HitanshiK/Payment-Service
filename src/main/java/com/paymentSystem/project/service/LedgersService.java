package com.paymentSystem.project.service;

import com.paymentSystem.project.entity.Ledger;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.LedgerType;
import com.paymentSystem.project.enums.Owner;
import com.paymentSystem.project.enums.PaymentType;
import com.paymentSystem.project.repos.ExchangeRateRepository;
import com.paymentSystem.project.repos.LedgersRepository;
import com.paymentSystem.project.repos.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class LedgersService {
    private final LedgersRepository repository;
    private final ExchangeRateRepository fxRateRepo;

    public Ledger createCreditLedger (Payments payments, Double allowedAmount, Wallet wallet){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setWallet(wallet);
        ledger.setLedgerType(LedgerType.CREDIT);
        ledger.setOwner(Owner.USER);
        if(payments.getType().equals(PaymentType.TOP_UP)){
            ledger.setAmount(allowedAmount);
            ledger.setCurrency(payments.getCurrency());
        }else {
            double fxRate = fxRateRepo.findByToCurrencyAndFromCurrency(payments.getCurrency(),wallet.getCurrency()).getFxRate();
            BigDecimal amount = BigDecimal.valueOf(allowedAmount);
            amount =  amount.divide(BigDecimal.valueOf(fxRate), 2, RoundingMode.HALF_UP);
            ledger.setAmount(amount.doubleValue());
            ledger.setCurrency(wallet.getCurrency());
            ledger.setCurrencyAmount(allowedAmount);
            ledger.setForeignCurrency(payments.getCurrency());
            ledger.setFxRate(fxRate);
        }
        //fx rate not handled for external payment
        return ledger;
    }

    public Ledger createSystemCreditLedger (Payments payments, Double amount){
        Ledger ledger1 = new Ledger();
        ledger1.setPayments(payments);
        ledger1.setLedgerType(LedgerType.CREDIT);
        ledger1.setAmount(amount);
        ledger1.setCurrency(payments.getCurrency());
        ledger1.setOwner(Owner.SYSTEM);
        ledger1.setForeignCurrency(payments.getCurrency());
        return ledger1;
    }

    public Ledger createDebitLedger (Payments payments, Wallet wallet){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setWallet(wallet);
        ledger.setLedgerType(LedgerType.DEBIT);
        ledger.setOwner(Owner.USER);
        double fxRate = fxRateRepo.findByToCurrencyAndFromCurrency(payments.getCurrency(),wallet.getCurrency()).getFxRate();
        ledger.setAmount(payments.getAmount());
        ledger.setCurrency(wallet.getCurrency());
        ledger.setCurrencyAmount(payments.getCurrencyAmount());
        ledger.setForeignCurrency(payments.getCurrency());
        ledger.setFxRate(fxRate);
        //fx rate not handled for external payment
        return  ledger;
    }

    public Ledger createSystemDebitLedger (Payments payments,Double amount){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setLedgerType(LedgerType.DEBIT);
        ledger.setAmount(amount);
        ledger.setCurrency(payments.getCurrency());
        ledger.setOwner(Owner.SYSTEM);
        ledger.setForeignCurrency(payments.getCurrency());
        //fx rate not handled for external payment
        return ledger;
    }
}
