package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Component
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUser (Long user);
}
