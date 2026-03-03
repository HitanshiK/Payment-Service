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

    public void createCreditLedger (Payments payments, double allowedAmount, double excessAmount, Wallet wallet){
        Ledger ledger = new Ledger();
        ledger.setPayments(payments);
        ledger.setWallet(wallet);
        ledger.setLedgerType(LedgerType.CREDIT);
        ledger.setAmount(allowedAmount);
        ledger.setCurrency(payments.getCurrency());
        ledger.setOwner(Owner.USER);
        ledger.setOriginalCurrency(payments.getConvertedCurrency() != null
                ? payments.getConvertedCurrency() : payments.getCurrency());
        //fx rate not handled for external payment
        repository.save(ledger);

        if(excessAmount > 0){
            Ledger ledger1 = new Ledger();
            ledger1.setPayments(payments);
            ledger1.setWallet(wallet);
            ledger1.setLedgerType(LedgerType.CREDIT);
            ledger1.setAmount(allowedAmount);
            ledger1.setCurrency(payments.getCurrency());
            ledger1.setOwner(Owner.SYSTEM);
            ledger1.setOriginalCurrency(payments.getConvertedCurrency() != null
                    ? payments.getConvertedCurrency() : payments.getCurrency());

            repository.save(ledger1);
        }

    }
}
