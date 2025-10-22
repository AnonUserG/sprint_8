package ru.practicum.onlineStore.controller;

import org.openapitools.model.BalanceResponse;
import org.openapitools.model.PaymentRequest;
import org.openapitools.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.practicum.onlineStore.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest(PaymentController.class)
@TestPropertySource(properties = "openapi.paymentService.base-path=")
public class PaymentControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void testGetBalance_success() {
        BalanceResponse mockResponse = new BalanceResponse();
        mockResponse.setBalance(150.0);

        Mockito.when(paymentService.getBalance(eq("defaultAccount")))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.get()
                .uri("/api/payments/{accountId}/balance", "defaultAccount")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(150.0);
    }

    @Test
    void testGetBalance_notFound() {
        Mockito.when(paymentService.getBalance(eq("missingAccount")))
                .thenReturn(Mono.error(new RuntimeException("Not found")));

        webTestClient.get()
                .uri("/api/payments/{accountId}/balance", "missingAccount")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testMakePayment_success() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(50.0);
        request.setAccountId("defaultAccount");
        request.setOrderId("orderId");

        PaymentResponse response = new PaymentResponse();
        response.setSuccess(true);

        Mockito.when(paymentService.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    void testMakePayment_insufficientFunds() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(500.0);
        request.setAccountId("defaultAccount");
        request.setOrderId("orderId");

        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setError("Insufficient funds");

        Mockito.when(paymentService.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(402)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error").isEqualTo("Insufficient funds");
    }

    @Test
    void testMakePayment_error() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(100.0);
        request.setAccountId("defaultAccount");
        request.setOrderId("orderId");

        Mockito.when(paymentService.makePayment(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Internal error")));

        webTestClient.post()
                .uri("/api/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error").isEqualTo("Internal error");
    }
}

