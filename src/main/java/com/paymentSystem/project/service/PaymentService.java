package com.paymentSystem.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.ExternalPayment.MockExternalPaymentGateway;
import com.paymentSystem.project.dto.request.CreatePaymentRequest;
import com.paymentSystem.project.dto.request.ExternalPaymentRequest;
import com.paymentSystem.project.dto.request.GatewayOrderRequest;
import com.paymentSystem.project.dto.response.CachedResponse;
import com.paymentSystem.project.dto.response.GatewayOrderResponse;
import com.paymentSystem.project.dto.response.GatewayWebhookData;
import com.paymentSystem.project.dto.response.PaymentResponse;
import com.paymentSystem.project.entity.ExternalPayments;
import com.paymentSystem.project.entity.IdempotencyRecord;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.enums.Status;
import com.paymentSystem.project.repos.IdempotencyRepository;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.utils.CurrencyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PublicKey;
import java.util.Optional;


/// db fallback remaining for idempotency
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Double MAX_WALLET_BALANCE = 5_00_000d;

    @Autowired
    PaymentsRepository paymentsRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper mapper;
    private final IdempotencyRepository idempotencyRepository;
    private final WalletService walletService;
    private final CurrencyUtils currencyUtils;
    private final WalletRepository walletRepository;
    private final ExternalPaymentService externalPaymentService;
    private final MockExternalPaymentGateway gateway;
    private final LedgersService ledgersService;


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
    public GatewayOrderResponse createExternalPaymentIntent(ExternalPaymentRequest request, String idempotencyKey) {
        try {
            Optional<CachedResponse> cached = idempotencyService.getCachedResponse(idempotencyKey);

            if (cached.isPresent()) return mapper.readValue(
                    cached.get().getBody(),
                    GatewayOrderResponse.class);

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
            Wallet wallet = walletRepository.findByIdForUpdate(request.getWalletId());
            Double currencyAmount = currencyUtils.currencyAmount(payments, wallet);

            //wallet balance -> soft check
            String message = "";
            if(walletService.checkWalletOverflow(wallet,currencyAmount)){
                message = (String.format("Wallet limit reached. Excess amount will be refunded."));
            }

            //create  external payment
            ExternalPayments externalPayments = externalPaymentService.createExternalPayment(payments);

            //create gateway order
            GatewayOrderRequest orderRequest = new GatewayOrderRequest(
                    payments.getAmount(),payments.getCurrency(),
                    externalPayments.getReferenceId(),wallet.getUser().getEmail(),wallet.getUser().getMobile()
            );
             GatewayOrderResponse orderResponse =  gateway.createOrder(orderRequest);
             orderResponse.setMessage(message);
             externalPayments.setGatewayOrderId(orderResponse.getGatewayOrderId());

             return orderResponse;
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
                response.setMessage(payments.getFailureReason());

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

    @Transactional
    public void completePayment (GatewayWebhookData data){
        ExternalPayments externalPayments = externalPaymentService.parseWebhookData(data);
        Payments payments = externalPayments.getPayment();

        if(externalPayments.getStatus().equals(PaymentStatus.SUCCESS)){
            return;
        }

        if(externalPayments.getStatus().equals(PaymentStatus.GATEWAY_SUCCESS)){
            Wallet wallet = walletRepository.findByIdForUpdate(payments.getPayeeWalletId());

            if(!wallet.getStatus().equals(Status.ACTIVE)){
                //refund amount
            }

            Double currentBalance = wallet.getBalance();
            Double totalBalance = currentBalance + payments.getAmount();
            Double allowedBalance = 0d;
            Double excessAmount = 0d;

            if(totalBalance > MAX_WALLET_BALANCE){
                excessAmount = totalBalance - MAX_WALLET_BALANCE;
                allowedBalance = payments.getAmount() - excessAmount;
                //excess amount to be refunded ;
            }else{
                allowedBalance = payments.getAmount();
            }

            wallet.setBalance(allowedBalance);

            //create credit ledger ...if exceeded amount is greater than 0 then create ledger entry for System
            ledgersService.createCreditLedger(payments,allowedBalance,excessAmount, wallet);

            payments.setStatus(PaymentStatus.SUCCESS);

        } else if (externalPayments.getStatus().equals(PaymentStatus.FAILED)) {
            payments.setStatus(PaymentStatus.FAILED);
        }
    }
}
