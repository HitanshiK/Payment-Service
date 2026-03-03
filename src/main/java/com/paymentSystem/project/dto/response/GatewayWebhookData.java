package com.paymentSystem.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GatewayWebhookData {

    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String status;
    private Long amount;
    private String currency;
    private String reason ;

}

