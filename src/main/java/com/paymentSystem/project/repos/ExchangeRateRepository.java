package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.ExchangeRate;
import com.paymentSystem.project.entity.IdempotencyRecord;
import com.paymentSystem.project.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;


@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    public ExchangeRate findByToCurrencyAndFromCurrency(Currency toCurrency , Currency fromCurrency);
}
