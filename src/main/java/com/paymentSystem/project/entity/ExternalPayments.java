package com.paymentSystem.project.entity;

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

    private long paymentId;

    private String referenceId;

    private PaymentStatus status = PaymentStatus.ONGOING;

    private String response;

    @CreationTimestamp
    private String createdAt;

    @UpdateTimestamp
    private String updatedAt;

}
