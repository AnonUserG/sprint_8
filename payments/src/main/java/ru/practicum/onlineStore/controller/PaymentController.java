package ru.practicum.onlineStore.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ApiApi;
import org.openapitools.model.BalanceResponse;
import org.openapitools.model.PaymentRequest;
import org.openapitools.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.practicum.onlineStore.service.PaymentService;

@Controller
@RequestMapping("${openapi.paymentService.base-path:}")
@RequiredArgsConstructor
public class PaymentController implements ApiApi {

    @Autowired
    private PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> apiPaymentsAccountIdBalanceGet(
            @Parameter(name = "accountId", description = "", required = true, in = ParameterIn.PATH) @PathVariable("accountId") String accountId,
            @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        return paymentService.getBalance(accountId)
                .map(ResponseEntity::ok)
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> apiPaymentsPayPost(
            @Parameter(name = "PaymentRequest", description = "", required = true) @Valid @RequestBody Mono<PaymentRequest> paymentRequest,
            @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        return paymentRequest
                .flatMap(paymentService::makePayment)
                .flatMap(response -> {
                    if (Boolean.TRUE.equals(response.getSuccess())) {
                        return Mono.just(ResponseEntity.ok(response));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response));
                    }
                })
                .onErrorResume(e -> {
                    PaymentResponse error = new PaymentResponse();
                    error.setSuccess(false);
                    error.setError(e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
                });
    }
}
