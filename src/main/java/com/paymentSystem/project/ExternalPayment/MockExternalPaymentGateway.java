package com.paymentSystem.project.ExternalPayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.dto.request.GatewayOrderRequest;
import com.paymentSystem.project.dto.response.GatewayOrderResponse;
import com.paymentSystem.project.dto.response.GatewayRefundResponse;
import com.paymentSystem.project.dto.response.GatewayWebhookData;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class MockExternalPaymentGateway implements ExternalPaymentGateway {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Simulates gateway order creation.
     */
    @Override
    public GatewayOrderResponse createOrder(GatewayOrderRequest request) {

        String gatewayOrderId = "MOCK_ORDER_" + UUID.randomUUID();

        return new GatewayOrderResponse(
                gatewayOrderId,
                request.getAmount(),
                request.getCurrency(),
                "CREATED",""
        );
    }

    /**
     * Mock signature verification.
     * Always returns true for local testing.
     */
    @Override
    public boolean verifySignature(String payload, String signature) {
        return true;
    }

    /**
     * Simulates webhook parsing.
     * In real gateway, payload would be JSON.
     */
    @Override
    public GatewayWebhookData parseWebhook(String payload) {

        try {
            // Parse JSON payload into Map
            Map<String, Object> data =
                    objectMapper.readValue(payload, Map.class);

            String orderId = (String) data.get("gatewayOrderId");
            String paymentId = (String) data.get("gatewayPaymentId");
            String status = (String) data.get("status");
            Long amount = Long.valueOf(data.get("amount").toString());
            String currency = (String) data.get("currency");
            String reason = (String) data.get("reason");

            return new GatewayWebhookData(
                    orderId,
                    paymentId,
                    status,
                    amount,
                    currency,
                    reason
            );

        } catch (Exception e) {
            throw new RuntimeException("Invalid mock webhook payload", e);
        }
    }

    /**
     * Simulates refund processing.
     */
    @Override
    public GatewayRefundResponse refund(String gatewayPaymentId, Long amount) {

        String refundId = "MOCK_REFUND_" + UUID.randomUUID();

        return new GatewayRefundResponse(
                gatewayPaymentId,
                refundId,
                amount,
                "PROCESSED"
        );
    }
}