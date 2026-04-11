package com.paymentSystem.project.dto.request;

import lombok.Data;

@Data
public class GatewayPayoutRequest {

    private String referenceId;
    private Double amount;
    private String currency;
    private String paymentDetails;
}