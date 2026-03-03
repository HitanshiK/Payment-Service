package com.paymentSystem.project.utils;

import com.paymentSystem.project.entity.ExchangeRate;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.repos.ExchangeRateRepository;
import com.paymentSystem.project.repos.WalletRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrencyUtils {

    private final WalletRepository walletRepository;
    private final ExchangeRateRepository exchangeRateRepo;

    public Boolean currencyCheck (Currency currency, Wallet wallet){
        return currency.equals(wallet.getCurrency());
    }

    public Double currencyAmount (Payments payments, Wallet wallet){
        if(currencyCheck(payments.getCurrency(), wallet)){
            return 1D;
        }else{
            payments.setConvertedCurrency(wallet.getCurrency());

            ExchangeRate rate = exchangeRateRepo.
                    findByToCurrencyAndFromCurrency(
                            wallet.getCurrency().toString(),payments.getCurrency().toString());

            return payments.getAmount() * rate.getFxRate();
        }

    }
}
