package ru.practicum.onlineStore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .pathMatchers("/cart/**", "/orders/**").hasAnyRole("ADMIN", "CUSTOMER")
                        .pathMatchers(HttpMethod.GET,"/", "main/items", "/items/**", "/**").permitAll()
                        .pathMatchers(HttpMethod.POST,"/items/{id}").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }


    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        Logger log = LoggerFactory.getLogger("OidcUserServiceLogger");
        OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

        return userRequest -> delegate.loadUser(userRequest).map(oidcUser -> {
            log.info("Loaded OIDC user: {}", oidcUser.getFullName());

            Map<String, Object> realmAccess = Optional.ofNullable(oidcUser.getClaims().get("realm_access"))
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .orElse(Map.of());
            log.info("realmAccess claims: {}", realmAccess);

            List<String> roles = Optional.ofNullable(realmAccess.get("roles"))
                    .filter(List.class::isInstance)
                    .map(list -> (List<String>) list)
                    .orElse(List.of());
            log.info("Roles list: {}", roles);

            Set<GrantedAuthority> mappedAuthorities = roles.stream()
                    .map(String::toUpperCase)
                    .map(role -> "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            log.info("Mapped authorities: {}", mappedAuthorities);

            mappedAuthorities.addAll(oidcUser.getAuthorities());
            log.info("Final authorities: {}", mappedAuthorities);

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        });
    }


}
