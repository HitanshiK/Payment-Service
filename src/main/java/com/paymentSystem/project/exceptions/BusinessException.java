package com.paymentSystem.project.exceptions;

public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    protected BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
