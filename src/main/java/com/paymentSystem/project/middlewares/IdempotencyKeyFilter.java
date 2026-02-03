package com.paymentSystem.project.middlewares;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) &&
            request.getRequestURI().startsWith("/api/payments")) {

            String key = request.getHeader(IDEMPOTENCY_HEADER);

            if (key == null || key.isBlank()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing Idempotency-Key header");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
