package com.paymentSystem.project.controllers;

import com.paymentSystem.project.ExternalPayment.MockExternalPaymentGateway;
import com.paymentSystem.project.dto.request.CreatePaymentRequest;
import com.paymentSystem.project.dto.request.ExternalPaymentRequest;
import com.paymentSystem.project.dto.response.GatewayOrderResponse;
import com.paymentSystem.project.dto.response.GatewayWebhookData;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    PaymentService paymentService;
    private final MockExternalPaymentGateway gateway;

    @PostMapping("/topUp")
    public GatewayOrderResponse walletTopUp(@RequestBody ExternalPaymentRequest request, HttpServletRequest httpRequest ){
        String key = httpRequest.getAttribute("IDEMPOTENCY_KEY").toString();
        return paymentService.createExternalPaymentIntent(request, key);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook (@RequestBody String payload,
                               @RequestHeader(value = "X-Signature", required = false) String signature){
        //verify signature
        if(!gateway.verifySignature(payload,signature)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        GatewayWebhookData webhookData = gateway.parseWebhook(payload);
        paymentService.completePayment(webhookData);
        return ResponseEntity.ok().build();
    }

    //for same payment request idempotency key should be same
    @PostMapping("/post")
    public PaymentResponse createPaymentEntity(@RequestBody CreatePaymentRequest request, HttpServletRequest httpRequest){
        String key = httpRequest.getAttribute("IDEMPOTENCY_KEY").toString();
        return paymentService.createPaymentIntent(request,key);
    }

    @PostMapping("/verify")
    public PaymentResponse verifyPayment (Long paymentId, HttpServletRequest httpServletRequest){
        String key = httpServletRequest.getAttribute("IDEMPOTENCY_KEY").toString();
       return paymentService.verifyPayment(paymentId,key);
    }

}
