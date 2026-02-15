package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Component
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

}
