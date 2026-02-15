package com.paymentSystem.project.dto.request;

import lombok.Data;

@Data
public class GatewayOrderRequest {

    private Long amount;
    private String currency;
    private String receipt;
    private String email;
    private String mobile;

}

