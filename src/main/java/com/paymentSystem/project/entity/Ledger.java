package com.paymentSystem.project.entity;

import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.LedgerType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@NoArgsConstructor
@Table(name = "ledgers")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    Payments payments;

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    Wallet wallet;

    @Enumerated(EnumType.STRING)
    private LedgerType ledgerType ;

    @Enumerated(EnumType.STRING)
    private Currency currency = Currency.INR;   //walletCurrency

    @Enumerated(EnumType.STRING)
    private Currency originalCurrency = Currency.INR;   //sender's currency

    private Double amount = 0D;

    private String description ;

    @CreationTimestamp
    private Timestamp createdAt ;

    private Double fxRate = 1D;
}


