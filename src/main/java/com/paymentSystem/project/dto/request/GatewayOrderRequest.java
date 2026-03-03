package com.paymentSystem.project.dto.request;

import com.paymentSystem.project.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GatewayOrderRequest {

    private Double amount;
    private Currency currency;
    private String receipt;
    private String email;
    private String mobile;

}

