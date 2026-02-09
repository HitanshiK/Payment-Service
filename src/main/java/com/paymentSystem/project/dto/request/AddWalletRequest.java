package com.paymentSystem.project.dto.request;

import com.paymentSystem.project.enums.Currency;
import lombok.Data;

@Data
public class AddWalletRequest {
    private Currency currency;
    private  boolean isDefault;
    private double perTransLimit;
}
