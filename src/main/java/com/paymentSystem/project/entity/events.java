package com.paymentSystem.project.entity;

import com.paymentSystem.project.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@NoArgsConstructor
@Table(name = "outbox_events")
public class events {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID of the related entity (paymentId, walletId...)
    private String referenceId;

    // PAYMENT_SUCCESS, PAYMENT_FAILED, REFUND_COMPLETED...
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    // JSON payload containing event data
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @CreationTimestamp
    private Timestamp createdAt;

    private Timestamp executeAt;

    private boolean executed = false;

}
