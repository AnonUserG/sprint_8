package ru.practicum.onlineStore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Arrays;

@Configuration
public class ClientRegistrationConfig {

    @Bean
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(Environment environment) {
        ClientRegistration keycloak = ClientRegistration.withRegistrationId("keycloak")
                .clientId(getRequiredProperty(environment, "spring.security.oauth2.client.registration.keycloak.client-id"))
                .clientName("Keycloak")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(getRequiredProperty(environment, "spring.security.oauth2.client.registration.keycloak.redirect-uri"))
                .scope(splitScopes(environment.getProperty("spring.security.oauth2.client.registration.keycloak.scope")))
                .authorizationUri(getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.authorization-uri"))
                .tokenUri(getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.token-uri"))
                .userInfoUri(getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.user-info-uri"))
                .userNameAttributeName("preferred_username")
                .jwkSetUri(getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.jwk-set-uri"))
                .build();

        ClientRegistration payments = ClientRegistration.withRegistrationId("payments")
                .clientId(getRequiredProperty(environment, "spring.security.oauth2.client.registration.payments.client-id"))
                .clientSecret(getRequiredProperty(environment, "spring.security.oauth2.client.registration.payments.client-secret"))
                .clientName("Payments API")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(splitScopes(environment.getProperty("spring.security.oauth2.client.registration.payments.scope")))
                .tokenUri(getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.token-uri"))
                .jwkSetUri(getRequiredProperty(environment, "spring.security.oauth2.client.provider.keycloak.jwk-set-uri"))
                .build();

        return new InMemoryReactiveClientRegistrationRepository(keycloak, payments);
    }

    private String[] splitScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private String getRequiredProperty(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required OAuth2 property: " + key);
        }
        return value;
    }
}
