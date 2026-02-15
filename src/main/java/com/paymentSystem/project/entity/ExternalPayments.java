package com.paymentSystem.project.entity;

import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@NoArgsConstructor
@Table(name = "external_transactions")
public class ExternalPayments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "paymentId")
    private Payments payment;

    private String referenceId; //created internally for mapping;

    private PaymentStatus status = PaymentStatus.ONGOING;

    private String response;

    @Enumerated(EnumType.STRING)
    private Currency gatewayCurrency;

    private Double gatewayAmount;

    private String gatewayOrderId ;  //gateway unique id for intent

    private String gatewayReferenceId ; // gateway unique id for money movement

    @CreationTimestamp
    private String createdAt;

    @UpdateTimestamp
    private String updatedAt;

}
