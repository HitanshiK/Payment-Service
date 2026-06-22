package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Component
public interface LedgersRepository extends JpaRepository<Ledger, Long> {

    @Query("SELECT l FROM Ledger l WHERE l.payments.id = :paymentId")
    List<Ledger> findByPaymentId(long paymentId);
}
