package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.ExternalPayments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository
public interface ExternalPaymentsRepository extends JpaRepository<ExternalPayments, Long> {

    public ExternalPayments findByGatewayOrderId(String gatewayOrderId);
}
