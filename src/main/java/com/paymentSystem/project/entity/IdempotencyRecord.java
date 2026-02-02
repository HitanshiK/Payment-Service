package com.paymentSystem.project.entity;

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
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.ONGOING;

    @OneToOne
    @JoinColumn(name = "paymentId")
    Payments payments;

    private String response; //response to the api

    @CreationTimestamp
    private Timestamp createdAt ;

    @UpdateTimestamp
    private Timestamp updatedAt ;

}
