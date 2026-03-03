package com.paymentSystem.project.dto.response;

import com.paymentSystem.project.entity.Payments;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentResponse {
    private long id;
    private String currency;
    private double amount;
    private long payerWalletId;
    private long payeeWalletId;
    private String status;
    private String message;

    public PaymentResponse(Payments payment){
        this.id = payment.getId();
        this.currency = payment.getCurrency().toString();
        this.amount = payment.getAmount();
        this.payeeWalletId = payment.getPayeeWalletId();
        this.payerWalletId = payment.getPayerWalletId();
    }

}
