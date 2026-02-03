package com.paymentSystem.project.config;

import com.paymentSystem.project.middlewares.IdempotencyKeyFilter;
import com.paymentSystem.project.middlewares.RequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    //to define the order of custom filters
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilter(RequestLoggingFilter filter) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyKeyFilter> idempotencyFilter(IdempotencyKeyFilter filter) {
        FilterRegistrationBean<IdempotencyKeyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(2);
        return registration;
    }
}
