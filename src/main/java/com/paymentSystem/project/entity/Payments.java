package com.paymentSystem.project.entity;

import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@NoArgsConstructor
@Table(name = "payments")
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey; //headers

    @Column(name = "ref_id", unique = true, nullable = false)
    private String refId;          //unique txn id found in response

    private Long originalPaymentId;   // in case of refunds

    @Enumerated(EnumType.STRING)
    private Currency currency = Currency.INR;

    @Enumerated(EnumType.STRING)
    private Currency convertedCurrency ;

    private long payeeWalletId; // the one who receives

    private long payerWalletId; // the one who pays

    private Double amount = 0D;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.INITIATED;

    private String failureReason ;

    @CreationTimestamp
    private Timestamp createdAt ;

    @UpdateTimestamp
    private Timestamp updatedAt ;

    private boolean deleted = false;


}
