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
@Table(name = "beneficiary")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String name;

    private String type;   // BANK / UPI / WALLET

    private String accountNumber;
    private String ifscCode;

    private String upiId;

    private Boolean verified;

    private Status status = Status.ACTIVE;          // ACTIVE / BLOCKED

    @CreationTimestamp
    private Timestamp createdAt;
}