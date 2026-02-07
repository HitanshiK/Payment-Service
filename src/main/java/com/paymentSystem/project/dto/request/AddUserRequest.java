package com.paymentSystem.project.dto.request;

import lombok.Data;

@Data
public class AddUserRequest {
    private String name;
    private String email;
    private String mobile;
    private String loginPassword;
    private String pin;
}
