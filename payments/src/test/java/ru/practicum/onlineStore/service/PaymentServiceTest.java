package ru.practicum.onlineStore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.BalanceResponse;
import org.openapitools.model.PaymentRequest;
import org.openapitools.model.PaymentResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    void testGetBalance() {
        String accountId = "defaultAccount";

        Mono<BalanceResponse> balanceMono = paymentService.getBalance(accountId);

        StepVerifier.create(balanceMono)
                .expectNextMatches(response ->
                        response.getAccountId().equals(accountId) &&
                                response.getBalance() == 3000.0
                )
                .verifyComplete();
    }

    @Test
    void testMakePayment_success() {
        PaymentRequest request = new PaymentRequest();
        request.setAccountId("defaultAccount");
        request.setOrderId("order-1");
        request.setAmount(2000.0); // меньше чем баланс

        Mono<PaymentResponse> paymentMono = paymentService.makePayment(request);

        StepVerifier.create(paymentMono)
                .expectNextMatches(response ->
                        response.getSuccess() &&
                                response.getTransactionId() != null &&
                                response.getError() == null
                )
                .verifyComplete();
    }

    @Test
    void testMakePayment_insufficientFunds() {
        PaymentRequest request = new PaymentRequest();
        request.setAccountId("defaultAccount");
        request.setOrderId("order-2");
        request.setAmount(5000.0); // больше чем баланс

        Mono<PaymentResponse> paymentMono = paymentService.makePayment(request);

        StepVerifier.create(paymentMono)
                .expectNextMatches(response ->
                        !response.getSuccess() &&
                                response.getTransactionId() != null &&
                                "Недостаточно средств".equals(response.getError())
                )
                .verifyComplete();
    }

    @Test
    void testMakePayment_exactBalance() {
        PaymentRequest request = new PaymentRequest();
        request.setAccountId("defaultAccount");
        request.setOrderId("order-3");
        request.setAmount(3000.0); // точно весь баланс

        Mono<PaymentResponse> paymentMono = paymentService.makePayment(request);

        StepVerifier.create(paymentMono)
                .expectNextMatches(response ->
                        response.getSuccess() &&
                                response.getTransactionId() != null &&
                                response.getError() == null
                )
                .verifyComplete();
    }
}
