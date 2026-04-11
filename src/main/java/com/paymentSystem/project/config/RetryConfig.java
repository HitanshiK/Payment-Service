package com.paymentSystem.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;


@Configuration
@EnableRetry
public class RetryConfig {
    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    @Recover
    public void recover(Exception e) {
        log.error("All retry attempts failed: {}", e.getMessage());
    }
}