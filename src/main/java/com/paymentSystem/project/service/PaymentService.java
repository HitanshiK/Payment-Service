package com.paymentSystem.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.ExternalPayment.MockExternalPaymentGateway;
import com.paymentSystem.project.dto.request.*;
import com.paymentSystem.project.dto.response.*;
import com.paymentSystem.project.entity.*;
import com.paymentSystem.project.entity.Wallet;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.enums.PaymentType;
import com.paymentSystem.project.enums.Status;
import com.paymentSystem.project.repos.IdempotencyRepository;
import com.paymentSystem.project.repos.LedgersRepository;
import com.paymentSystem.project.repos.PaymentsRepository;
import com.paymentSystem.project.repos.WalletRepository;
import com.paymentSystem.project.utils.CurrencyUtils;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.aspectj.bridge.IMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
/**Use REFUND when reversing an incoming payment (money came into system)
 Use REVERSAL when undoing an outgoing/internal debit**/

/// db fallback remaining for idempotency
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Double MAX_WALLET_BALANCE = 5_00_000d;
    private static final Double DAILY_TRANSACTION_LIMIT = 3_00_000d;

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
    private final PinService pinservice;
    private final LedgersRepository ledgersRepository;


    @Transactional
    public PaymentResponse createPaymentIntent(CreatePaymentRequest request, String idempotencyKey) {
        try {
            Optional<CachedResponse> cached = idempotencyService.getCachedResponse(idempotencyKey);

            if (cached.isPresent()) return mapper.readValue(
                    cached.get().getBody(),
                    PaymentResponse.class);

            Payments payments = new Payments();
            String message = null;

            if(request.getType().equals(PaymentType.PAYMENT)){
                payments.setCurrency(request.getCurrency());
                payments.setAmount(request.getAmount());
                payments.setPayeeWalletId(request.getPayeeWalletId());
                payments.setPayerWalletId(request.getPayerWalletId());
                payments.setIdempotencyKey(idempotencyKey);
                payments.setType(PaymentType.PAYMENT);
                paymentsRepository.save(payments);

                if(walletService.isWalletLocked(request.getPayerWalletId()) || walletService.isWalletLocked(request.getPayeeWalletId())){
                    message = "WALLET LOCKED";
                }

            } else if (request.getType().equals(PaymentType.PAYOUT)) {
                payments.setCurrency(request.getCurrency());
                payments.setAmount(request.getAmount());
                payments.setPayerWalletId(request.getPayerWalletId());
                payments.setIdempotencyKey(idempotencyKey);
                payments.setType(PaymentType.PAYOUT);
                paymentsRepository.save(payments);

                if(walletService.isWalletLocked(request.getPayerWalletId())){
                    message = "WALLET LOCKED";
                }
            }
            payments.setStatus(PaymentStatus.AUTH_PENDING);
            if(request.getAmount() <= 0){
                message = "AMOUNT SHOULD BE GREATER THAN 0";
            }

            PaymentResponse response = new PaymentResponse(payments);
            response.setMessage(message);

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
            PaymentType type = request.getType();
            Payments payments = new Payments();
            if (type.equals(PaymentType.TOP_UP)) {
                payments.setCurrency(request.getCurrency());
                payments.setAmount(request.getAmount());
                payments.setPayeeWalletId(request.getWalletId());
                payments.setIdempotencyKey(idempotencyKey);
                payments.setType(type);
                paymentsRepository.save(payments);
            }

            //for db fallback
            idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, payments, mapper.writeValueAsString(payments)));
            idempotencyService.cacheResponse(idempotencyKey, new CachedResponse(200, mapper.writeValueAsString(payments)));

            //currency check
            String message = "";
            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));
            Double currencyAmount = currencyUtils.currencyAmount(payments.getCurrency(), payments.getAmount(), wallet);
            payments.setCurrencyAmount(currencyAmount);

            if(currencyAmount > wallet.getPerTransLimit()){
                message = ("Per Transaction Limit Breached");
            }
            //wallet balance -> soft check
            if(type.equals(PaymentType.TOP_UP) && walletService.checkWalletOverflow(wallet, payments.getAmount())){
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
             externalPayments.setGatewayId(orderResponse.getGatewayOrderId());

             return orderResponse;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Retryable(
            value = OptimisticLockException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    @Transactional
    public PaymentResponse verifyPayment (VerifyPaymentRequest request, String key){
        try{
            Optional<CachedResponse> cached = idempotencyService.getCachedResponse(key);

            if (cached.isPresent()) return mapper.readValue(
                    cached.get().getBody(),
                    PaymentResponse.class);

            Payments payments = paymentsRepository.findById(request.getPaymentId()).orElseThrow(()-> new RuntimeException("Payment Intent not found"));

            validatePaymentStatus(payments);

            Wallet userWallet = walletRepository.findById(payments.getPayerWalletId())
                    .orElseThrow(() -> new RuntimeException("User Wallet Not found"));

            if(!pinservice.verifyPin(request.getPin(),userWallet.getUser())){
                return handlePaymentFailure(payments ,"INVALID PIN",key);
            }

            if(!walletService.validateWallets(payments)){
                return handlePaymentFailure(payments, "Wallet is locked due to multiple incorrect pin attempts", key);
            }

            Double currencyAmount = currencyUtils.currencyAmount(payments.getCurrency(), payments.getAmount(), userWallet);
            payments.setCurrencyAmount(currencyAmount);

            if(currencyAmount > userWallet.getPerTransLimit()){
                return handlePaymentFailure(payments, "Per Transaction Limit Breached", key);
            }

            if(dailyTransactionLimitCheck(userWallet, currencyAmount)){
                return handlePaymentFailure(payments, "Daily Transaction Limit reached", key);
            }

            if (walletService.checkWalletUnderFlow(userWallet, currencyAmount)){
                return handlePaymentFailure(payments, "INSUFFICIENT BALANCE", key);
            }

            try {
               walletService.debit(userWallet,currencyAmount);
            } catch (OptimisticLockException e) {
                return handlePaymentFailure(payments, "CONCURRENT_MODIFICATION", key);
            }

            if(payments.getType().equals(PaymentType.PAYMENT)){
                Wallet payeeWallet = walletRepository.findById(payments.getPayeeWalletId())
                        .orElseThrow(() -> new RuntimeException("Receiver's wallet not found"));

                Double currAmount = currencyUtils.currencyAmount(payments.getCurrency(), payments.getAmount(), payeeWallet);

                if(walletService.checkWalletOverflow(payeeWallet,currAmount )){
                    double excessAmount = currAmount - MAX_WALLET_BALANCE;
                   currAmount = currAmount - excessAmount;

                   reversePayment(payments, excessAmount, true);
                   Ledger ledger =ledgersService.createSystemCreditLedger(payments,currAmount);
                   ledgersRepository.save(ledger);
                   payments.setCreditedAmount(currAmount);
                   payments.setStatus(PaymentStatus.PARTIAL_SUCCESS);
                }
                try {
                    walletService.credit(payeeWallet,currAmount);
                } catch (OptimisticLockException e) {
                    return handlePaymentFailure(payments, "CONCURRENT_MODIFICATION", key);
                }
                Ledger ledger = ledgersService.createCreditLedger(payments,currAmount,payeeWallet);
                ledgersRepository.save(ledger);
                walletRepository.save(userWallet);
                walletRepository.save(payeeWallet);
                if(payments.getStatus().equals(PaymentStatus.AUTH_PENDING)){
                    payments.setStatus(PaymentStatus.SUCCESS);
                }
                paymentsRepository.save(payments);
            }else if(payments.getType().equals(PaymentType.PAYOUT)){
                Ledger ledger = ledgersService.createDebitLedger(payments,userWallet);
                ledgersRepository.save(ledger);
                walletRepository.save(userWallet);
                paymentsRepository.save(payments);

                ExternalPayments externalPayments = externalPaymentService.createExternalPayment(payments);

                GatewayPayoutRequest payoutRequest = new GatewayPayoutRequest();
                payoutRequest.setAmount(payments.getAmount());
                payoutRequest.setCurrency(String.valueOf(payments.getCurrency()));
                payoutRequest.setReferenceId(externalPayments.getReferenceId());
                payoutRequest.setPaymentDetails(request.getPaymentDetails());

                GatewayPayoutResponse payoutResponse = gateway.createPayout(payoutRequest);
                externalPayments.setGatewayId(payoutResponse.getGatewayPayoutId());
            }

            PaymentResponse response = new PaymentResponse(payments);
            response.setStatus(payments.getStatus().toString());
            response.setMessage(payments.getFailureReason());

            idempotencyRepository.save(new IdempotencyRecord(key, payments, mapper.writeValueAsString(payments)));
            idempotencyService.cacheResponse(key, new CachedResponse(200, mapper.writeValueAsString(payments)));
            return response;

        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void validatePaymentStatus (Payments payments){
        if(!(payments.getStatus() == PaymentStatus.AUTH_PENDING ||
                (payments.getStatus() == PaymentStatus.FAILED &&
                        "INVALID_PIN".equals(payments.getFailureReason())))){
            throw new RuntimeException("INVALID STATUS");
        }
    }

    @Transactional
    public void completePayment (GatewayWebhookData data){
        ExternalPayments externalPayments = externalPaymentService.parseWebhookData(data);
        Payments payments = externalPayments.getPayment();

        if(externalPayments.getStatus().equals(PaymentStatus.SUCCESS)){
            return;
        }

        if(externalPayments.getStatus().equals(PaymentStatus.GATEWAY_SUCCESS)){
            if(payments.getType().equals(PaymentType.TOP_UP)) {
                Wallet wallet = walletRepository.findById(payments.getPayeeWalletId())
                        .orElseThrow(() -> new RuntimeException("Wallet not found"));

                if (!wallet.getStatus().equals(Status.ACTIVE)) {
                    payments.setStatus(PaymentStatus.FAILED);
                    externalPayments.setStatus(PaymentStatus.FAILED);
                    externalPaymentService.handleRefund(externalPayments, externalPayments.getGatewayAmount());
                    return;
                }

                Double currencyAmount = currencyUtils.currencyAmount(payments.getCurrency(), payments.getAmount(),wallet);

                if(walletService.checkWalletOverflow(wallet,currencyAmount)){
                    double excessAmount = currencyAmount - MAX_WALLET_BALANCE;
                    currencyAmount = currencyAmount - excessAmount;

                    externalPaymentService.handleRefund(externalPayments, excessAmount);
                    Ledger ledger =ledgersService.createSystemDebitLedger(payments,currencyAmount);
                    ledgersRepository.save(ledger);
                    payments.setCreditedAmount(currencyAmount);
                    payments.setStatus(PaymentStatus.PARTIAL_SUCCESS);
                }
                try {
                    walletService.credit(wallet,currencyAmount);
                } catch (OptimisticLockException e) {
                    throw  new RuntimeException("Try again");
                }
                Ledger ledger = ledgersService.createCreditLedger(payments,currencyAmount,wallet);
                ledgersRepository.save(ledger);
                walletRepository.save(wallet);
                if(payments.getStatus().equals(PaymentStatus.AUTH_PENDING)){
                    payments.setStatus(PaymentStatus.SUCCESS);
                }
                paymentsRepository.save(payments);
            } else if (payments.getType().equals(PaymentType.PAYOUT)) {
                payments.setStatus(PaymentStatus.SUCCESS);
            }

        } else if (externalPayments.getStatus().equals(PaymentStatus.FAILED)) {
            payments.setStatus(PaymentStatus.FAILED);
            if(payments.getType().equals(PaymentType.PAYOUT)){
                reversePayment(payments, payments.getAmount(), true);
            }
        }
        externalPayments.setStatus(PaymentStatus.SUCCESS);
    }

    @Transactional
    public void reversePayment (Payments payments,Double amount,boolean skip){
        try{
            Wallet wallet = walletRepository.findById(payments.getPayerWalletId()).orElseThrow();
            try {
                Double currAmount = currencyUtils.currencyAmount(payments.getCurrency(), amount ,wallet);
                walletService.credit(wallet, currAmount);
            } catch (OptimisticLockException e) {
            }
           Ledger ledger =ledgersService.createCreditLedger(payments,payments.getAmount(),wallet);
            ledgersRepository.save(ledger);
            walletRepository.save(wallet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Transactional
    public void completeRefund (GatewayWebhookData data){
            ExternalPayments externalPayments = externalPaymentService.parseRefundWebhookData(data);
            Payments payments = externalPayments.getPayment();

            if(externalPayments.getRefundStatus().equals(PaymentStatus.SUCCESS)){
                return;
            }

        if (externalPayments.getRefundStatus().equals(PaymentStatus.GATEWAY_SUCCESS)) {
            if (payments.getAmount().equals(externalPayments.getRefundAmount())) {
                payments.setStatus(PaymentStatus.REFUNDED);
            }
            Ledger ledger = ledgersService.createSystemDebitLedger(payments, externalPayments.getRefundAmount());
            ledgersRepository.save(ledger);
            externalPayments.setRefundStatus(PaymentStatus.SUCCESS);
        } else if (externalPayments.getRefundStatus().equals(PaymentStatus.FAILED)) {
            payments.setStatus(PaymentStatus.FAILED);
        }
    }

    public PaymentResponse handlePaymentFailure (Payments payments, String remarks, String key){
       try {
            payments.setStatus(PaymentStatus.FAILED);
            payments.setFailureReason(remarks);

            PaymentResponse response = new PaymentResponse(payments);
            response.setStatus(payments.getStatus().toString());
            response.setMessage(payments.getFailureReason());

            idempotencyRepository.save(new IdempotencyRecord(key, payments, mapper.writeValueAsString(payments)));
            idempotencyService.cacheResponse(key, new CachedResponse(200, mapper.writeValueAsString(payments)));

            return response;
        }catch (Exception e){
           throw new RuntimeException (e.getCause());
       }
    }

    public boolean dailyTransactionLimitCheck ( Wallet wallet , Double amount ){
        try{
            LocalDateTime startOfDayLdt = LocalDate.now().atStartOfDay();
            Timestamp startOfDay = Timestamp.valueOf(startOfDayLdt);

            Double totalAmount = 0d;

            List<Payments> payments = paymentsRepository.currentDayTransactions(wallet.getId(),startOfDay);

            if(!payments.isEmpty()){
              payments =   payments.stream().filter( p -> p.getStatus().equals(PaymentStatus.SUCCESS) ||
                       p.getStatus().equals(PaymentStatus.PARTIAL_SUCCESS) ||
                      p.getStatus().equals(PaymentStatus.INITIATED) ||
                      p.getStatus().equals(PaymentStatus.ONGOING)).toList();

              for( Payments p : payments){
                  if(p.getStatus().equals(PaymentStatus.PARTIAL_SUCCESS)){
                      totalAmount += p.getCreditedAmount();
                  }else {
                      totalAmount += p.getAmount();
                  }
              }
            }
            if(totalAmount + amount > DAILY_TRANSACTION_LIMIT) return true;
            return false;
        }catch (Exception e){
            throw new RuntimeException(e.getCause());
        }
    }
}
