package ru.practicum.onlineStore.config;

import lombok.RequiredArgsConstructor;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.DefaultApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class PaymentsApiClientConfig {

    private final ReactiveClientRegistrationRepository clientRegistrations;
    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient paymentsApiClient() {
        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrations,
                authorizedClientService
        );

        var oauth2Filter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2Filter.setDefaultClientRegistrationId("payments");

        WebClient webClient = WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .filter(oauth2Filter)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public DefaultApi defaultApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}
