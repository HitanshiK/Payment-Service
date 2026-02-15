package com.paymentSystem.project.dto.request;

import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.PaymentType;
import lombok.Data;

@Data
public class ExternalPaymentRequest {

    private Currency currency;

    private double amount ;

    private Long walletId;

    private PaymentType type ;
}
