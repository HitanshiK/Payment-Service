package com.paymentSystem.project.entity;

import com.paymentSystem.project.enums.Currency;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@NoArgsConstructor
@Table(name = "foreign_exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Currency fromCurrency;

    private Currency toCurrency;

    private double fxRate;

    @CreationTimestamp
    private Timestamp createdAt;

    //private Timestamp effectiveAt;

}



//6️⃣ ExchangeRate
//
//Needed for multi-currency support
//
//Purpose: Stores FX rates used during conversions
//
//Key Fields
//
//id
//
//        from_currency
//
//to_currency
//
//        rate
//
//effective_at
//
//        created_at