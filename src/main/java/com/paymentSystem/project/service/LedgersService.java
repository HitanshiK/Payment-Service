package com.paymentSystem.project.service;

import com.paymentSystem.project.entity.Ledger;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.LedgerType;
import com.paymentSystem.project.enums.Owner;
import com.paymentSystem.project.repos.LedgersRepository;
import com.paymentSystem.project.repos.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LedgersService {
    private final LedgersRepository repository;

    public void createCreditLedger (Payments payments, double allowedAmount, Wallet wallet){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setWallet(wallet);
        ledger.setLedgerType(LedgerType.CREDIT);
        ledger.setAmount(allowedAmount);
        ledger.setCurrency(wallet.getCurrency());
        ledger.setOwner(Owner.USER);
        //fx rate not handled for external payment
        repository.save(ledger);
    }

    public void createSystemCreditLedger (Payments payments, Double amount){
        Ledger ledger1 = new Ledger();
        ledger1.setPayments(payments);
        ledger1.setLedgerType(LedgerType.CREDIT);
        ledger1.setAmount(amount);
        ledger1.setCurrency(payments.getCurrency());
        ledger1.setOwner(Owner.SYSTEM);
        ledger1.setForeignCurrency(payments.getCurrency());

        repository.save(ledger1);
    }

    public void createDebitLedger (Payments payments, Wallet wallet){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setWallet(wallet);
        ledger.setLedgerType(LedgerType.DEBIT);
        ledger.setAmount(payments.getAmount());
        ledger.setCurrency(payments.getCurrency());
        ledger.setOwner(Owner.USER);
        ledger.setForeignCurrency(payments.getCurrency());
        //fx rate not handled for external payment
        repository.save(ledger);
    }

    public void createSystemDebitLedger (Payments payments,Double amount){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setLedgerType(LedgerType.DEBIT);
        ledger.setAmount(amount);
        ledger.setCurrency(payments.getCurrency());
        ledger.setOwner(Owner.SYSTEM);
        ledger.setForeignCurrency(payments.getCurrency());
        //fx rate not handled for external payment
        repository.save(ledger);
    }
}
