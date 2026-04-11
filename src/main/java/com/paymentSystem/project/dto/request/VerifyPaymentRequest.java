package com.paymentSystem.project.dto.request;

import lombok.Data;

@Data
public class VerifyPaymentRequest {
    Long paymentId;
    String pin;

    String paymentDetails; //dummy
}
