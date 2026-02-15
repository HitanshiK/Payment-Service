package com.paymentSystem.project.controllers;

import com.paymentSystem.project.dto.request.CreatePaymentRequest;
import com.paymentSystem.project.dto.request.ExternalPaymentRequest;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    PaymentService paymentService;

    @PostMapping("/topUp")
    public PaymentResponse walletTopUp(@RequestBody ExternalPaymentRequest request,HttpServletRequest httpRequest ){
        String key = httpRequest.getAttribute("IDEMPOTENCY_KEY").toString();
        return paymentService.createExternalPaymentIntent(request, key);
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
