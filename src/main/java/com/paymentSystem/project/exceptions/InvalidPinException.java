package com.paymentSystem.project.exceptions;

public class InvalidPinException extends BusinessException {
    public InvalidPinException() {
        super("Invalid transaction PIN", "INVALID_PIN", 401);
    }
}
