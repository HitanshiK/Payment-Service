package com.paymentSystem.project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;


@Configuration
@EnableRetry
public class TestRetryConfig {
    private static final Logger log = LoggerFactory.getLogger(TestRetryConfig.class);

}