package com.paymentSystem.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.dto.response.GatewayWebhookData;
import com.paymentSystem.project.entity.ExternalPayments;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.enums.Currency;
import com.paymentSystem.project.enums.PaymentStatus;
import com.paymentSystem.project.repos.ExternalPaymentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalPaymentService {

    private final ExternalPaymentsRepository repository;
    private final ObjectMapper mapper;

    /**

     * 10. Handle overflow (refund extra if needed)
     * 11. Update payment status
     * 12. Update external payment
     * 13. Create wallet ledger entry
     * 14. Commit transaction
     */

    public ExternalPayments createExternalPayment (Payments payments){
        if(payments != null){
            ExternalPayments externalPayments = new ExternalPayments();
            externalPayments.setPayment(payments);
            externalPayments.setGatewayAmount(payments.getAmount());
            externalPayments.setReferenceId(UUID.randomUUID().toString());
            externalPayments.setStatus(PaymentStatus.ONGOING);
            repository.save(externalPayments);
            return externalPayments;
        }else{
            throw new RuntimeException("Payment not found");
        }
    }

    public ExternalPayments parseWebhookData(GatewayWebhookData data){
        try{
            String orderId = data.getGatewayOrderId();

            ExternalPayments payments = repository.findByGatewayOrderId(orderId);
            if (payments == null) {
                throw new RuntimeException("Payment record not found with order id " + orderId);
            }

            if(payments.getStatus().equals(PaymentStatus.SUCCESS)){
                return payments;
            }

            if (data.getStatus().equalsIgnoreCase("SUCCESS")) {
                payments.setGatewayReferenceId(data.getGatewayPaymentId());
                payments.setStatus(PaymentStatus.GATEWAY_SUCCESS);
                payments.setGatewayCurrency(Currency.valueOf(data.getCurrency()));
            } else {
                payments.setGatewayReferenceId(data.getGatewayPaymentId());
                payments.setStatus(PaymentStatus.FAILED);
            }
            String resposne = mapper.writeValueAsString(data);
            payments.setResponse(resposne);
            return payments;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
