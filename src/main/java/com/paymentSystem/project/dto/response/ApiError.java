package com.paymentSystem.project.dto.response;

import lombok.Data;

@Data
public class ApiError {
    private String code;
    private String message;
    private int status;
    private long timestamp;

    public ApiError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
}
