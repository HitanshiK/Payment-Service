package com.paymentSystem.project.exceptions;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException() {
        super("Insufficient wallet balance", "INSUFFICIENT_BALANCE", 400);
    }
}
