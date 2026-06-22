package com.paymentSystem.project.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class VerifyPaymentRequest {
    Long paymentId;
    String pin;

    String paymentDetails; //dummy

    public VerifyPaymentRequest(long id, String pin) {
        this.paymentId = id;
        this.pin = pin;
    }
}
