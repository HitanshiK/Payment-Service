package com.paymentSystem.project.dto.request;

import com.paymentSystem.project.enums.Currency;
import lombok.Data;

@Data
public class CreatePaymentRequest {

    private Currency currency;

    private Long payerWalletId;

    private Long payeeWalletId;

    private double amount;
}
