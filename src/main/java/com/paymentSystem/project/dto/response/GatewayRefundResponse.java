package com.paymentSystem.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GatewayRefundResponse {

    private String gatewayPaymentId;
    private String refundId;
    private Double refundedAmount;
    private String status;

}


