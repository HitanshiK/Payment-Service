package com.paymentSystem.project.dto.response;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class AddWalletResponse {
    private Long userId;
    private Long WalletId;
    private String currency;
    private Timestamp createdAt;
}
