package com.paymentSystem.project.exceptions;

public class DuplicateRequestException extends BusinessException {
    public DuplicateRequestException() {
        super("Duplicate payment request", "DUPLICATE_REQUEST", 409);
    }
}
