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

    public Double currencyAmount (Currency currency , Double amount, Wallet wallet){
        if(currencyCheck(currency, wallet)){
            return 1D;
        }else{
            ExchangeRate rate = exchangeRateRepo.
                    findByToCurrencyAndFromCurrency(
                            wallet.getCurrency().toString(),currency.toString());

            return amount * rate.getFxRate();
        }

    }
}
