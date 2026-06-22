package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Long> {

    @Query("""
    SELECT p
    FROM Payments p
    WHERE p.payerWalletId = :payerWalletId
      AND p.createdAt >= :startOfDay
""")    List<Payments> currentDayTransactions(
            @Param("payerWalletId") Long payerWalletId,
            @Param("startOfDay") Timestamp startOfDay
    );
}
