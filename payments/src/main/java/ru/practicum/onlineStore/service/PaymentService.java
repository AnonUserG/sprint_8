package ru.practicum.onlineStore.service;

import org.openapitools.model.BalanceResponse;
import org.openapitools.model.PaymentRequest;
import org.openapitools.model.PaymentResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class PaymentService {

    public Mono<BalanceResponse> getBalance(String accountId) {
        BalanceResponse response = new BalanceResponse();
        response.setAccountId(accountId);
        response.setBalance(3000.0);
        return Mono.just(response);
    }

    public Mono<PaymentResponse> makePayment(PaymentRequest request) {
        var amount = 3000.0;
        amount = amount - request.getAmount();
        var transactionID = UUID.randomUUID().toString();
        PaymentResponse paymentResponse = new PaymentResponse();

        if (amount >= 0.0) {
            paymentResponse.setSuccess(true);
            paymentResponse.transactionId(transactionID);
            paymentResponse.setError(null);

            return Mono.just(paymentResponse);
        } else {
            paymentResponse.setSuccess(false);
            paymentResponse.setTransactionId(transactionID);
            paymentResponse.setError("Недостаточно средств");

            return Mono.just(paymentResponse);
        }

    }
}
