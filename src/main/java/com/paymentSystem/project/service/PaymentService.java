package com.paymentSystem.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.dto.request.CreatePaymentRequest;
import com.paymentSystem.project.dto.request.ExternalPaymentRequest;
import com.paymentSystem.project.dto.response.CachedResponse;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.IdempotencyRecord;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.IdempotencyRepository;
import com.paymentSystem.project.repos.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


/// db fallback remaining for idempotency
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Autowired
    PaymentsRepository paymentsRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper mapper;
    private final IdempotencyRepository idempotencyRepository;
    private final PinService pinService;
    private final WalletService walletService;


    @Transactional
    public PaymentResponse createPaymentIntent(CreatePaymentRequest request, String idempotencyKey) {
        try {
            Optional<CachedResponse> cached = idempotencyService.getCachedResponse(idempotencyKey);

            if (cached.isPresent()) return mapper.readValue(
                    cached.get().getBody(),
                    PaymentResponse.class);

            Payments payments = new Payments();
            payments.setCurrency(request.getCurrency());
            payments.setAmount(request.getAmount());
            payments.setPayeeWalletId(request.getPayeeWalletId());
            payments.setPayerWalletId(request.getPayerWalletId());
            payments.setIdempotencyKey(idempotencyKey);
            paymentsRepository.save(payments);

            PaymentResponse response = new PaymentResponse(payments);

            //for db fallback
            idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, payments, mapper.writeValueAsString(payments)));

            idempotencyService.cacheResponse(idempotencyKey, new CachedResponse(200, mapper.writeValueAsString(payments)));

            return response;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse createExternalPaymentIntent(ExternalPaymentRequest request, String idempotencyKey) {
        try {
            Optional<CachedResponse> cached = idempotencyService.getCachedResponse(idempotencyKey);

            if (cached.isPresent()) return mapper.readValue(
                    cached.get().getBody(),
                    PaymentResponse.class);

            Payments payments = new Payments();
            payments.setCurrency(request.getCurrency());
            payments.setAmount(request.getAmount());
            payments.setPayeeWalletId(request.getWalletId());
            payments.setIdempotencyKey(idempotencyKey);
            paymentsRepository.save(payments);

            PaymentResponse response = new PaymentResponse(payments);

            //for db fallback
            idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, payments, mapper.writeValueAsString(payments)));

            idempotencyService.cacheResponse(idempotencyKey, new CachedResponse(200, mapper.writeValueAsString(payments)));

            //currency check

            return response;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse verifyPayment (Long paymentId, String key){
        try{
            Optional<CachedResponse> cached = idempotencyService.getCachedResponse(key);

            if (cached.isPresent()) return mapper.readValue(
                    cached.get().getBody(),
                    PaymentResponse.class);

            Payments payments = paymentsRepository.findById(paymentId).orElseThrow(()-> new RuntimeException("Payment Intent not found"));

            if(!payments.getStatus().equals(PaymentStatus.AUTH_PENDING)){
                throw new RuntimeException("INVALID STATUS");
            }

            if(walletService.isWalletLocked(payments.getPayerWalletId())){
                payments.setStatus(PaymentStatus.AUTH_FAILED);
                payments.setFailureReason("Wallet is locked due to multiple incorrect pin attempts");

                PaymentResponse response = new PaymentResponse(payments);
                response.setStatus(payments.getStatus().toString());
                response.setFailureReason(payments.getFailureReason());

                idempotencyRepository.save(new IdempotencyRecord(key, payments, mapper.writeValueAsString(payments)));
                idempotencyService.cacheResponse(key, new CachedResponse(200, mapper.writeValueAsString(payments)));

                return response;
            }

//            pinService.



        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new PaymentResponse();
    }
}
