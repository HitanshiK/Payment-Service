package com.paymentSystem.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GatewayPayoutResponse {

    private String gatewayPayoutId;    // Unique ID from gateway

    private String referenceId;        // Your referenceId

    private String status;             // PENDING / PROCESSING / SUCCESS / FAILED

    private Double amount;

    private String currency;
}