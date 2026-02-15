package com.paymentSystem.project.ExternalPayment;

import com.paymentSystem.project.dto.request.GatewayOrderRequest;
import com.paymentSystem.project.dto.response.GatewayOrderResponse;
import com.paymentSystem.project.dto.response.GatewayRefundResponse;
import com.paymentSystem.project.dto.response.GatewayWebhookData;

public interface ExternalPaymentGateway {

    /**
     * Creates an order at payment gateway.
     * Returns gateway-specific order response.
     */
    GatewayOrderResponse createOrder(GatewayOrderRequest request);

    /**
     * Verifies webhook signature to ensure authenticity.
     */
    boolean verifySignature(String payload, String signature);

    /**
     * Extracts structured webhook data from raw payload.
     */
    GatewayWebhookData parseWebhook(String payload);

    /**
     * Initiates refund for a successful payment.
     */
    GatewayRefundResponse refund(String gatewayPaymentId, Long amount);
}

