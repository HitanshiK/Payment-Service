package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Component
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUser_Id (Long user);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Wallet findByIdForUpdate (Long id);
}
