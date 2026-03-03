package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
@Component
public interface LedgersRepository extends JpaRepository<Ledger, Long> {

}
