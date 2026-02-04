package com.paymentSystem.project.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String phoneOrEmail;
    private String pin;
}
