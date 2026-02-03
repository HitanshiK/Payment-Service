package com.paymentSystem.project.exceptions;

public class WalletFrozenException extends BusinessException {
    public WalletFrozenException() {
        super("Wallet is frozen", "WALLET_FROZEN", 403);
    }
}
