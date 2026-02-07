package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
