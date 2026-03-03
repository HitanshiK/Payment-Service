package com.paymentSystem.project.dto.response;

import com.paymentSystem.project.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GatewayOrderResponse {

    private String gatewayOrderId;
    private Double amount;
    private Currency currency;
    private String status;
    private String message;
}

