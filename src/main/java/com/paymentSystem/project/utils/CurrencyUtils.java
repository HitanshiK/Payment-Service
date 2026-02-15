package com.paymentSystem.project.utils;

import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.repos.WalletRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrencyUtils {

    private final WalletRepository walletRepository;

    public Boolean currencyCheck (Currency currency, long walletId){
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(()
                -> new RuntimeException("Wallet Not found"));
        return currency.equals(wallet.getCurrency());
    }

    public Double currencyAmount (){return 1D;}
}
