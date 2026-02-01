package com.paymentSystem.project.entity;

import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@NoArgsConstructor
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @Enumerated(EnumType.STRING)
    private Currency currency = Currency.INR;

    private Double balance = 0D;

    private Status status = Status.ACTIVE;

    @CreationTimestamp
    private Timestamp createdAt ;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private boolean deleted = false;

    //helps to support multiple wallet against single user to handle multi currency
    //one of which will be default
    private boolean isDefault = false;

    private double perTransLimit = 1_00_000D;
}
