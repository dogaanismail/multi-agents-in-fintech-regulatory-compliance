package org.banksolution.config;

import org.banksolution.security.KeycloakRealmRolesAuthoritiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import static org.banksolution.security.BackofficeRole.ADMIN;
import static org.banksolution.security.BackofficeRole.COMPLIANCE_OFFICER;
import static org.banksolution.security.BackofficeRole.OPERATOR;
import static org.banksolution.security.BackofficeRole.VIEWER;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String POST_LOGOUT_REDIRECT_URI = "{baseUrl}/";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/payment-engine/payments/*/manual-review/approve",
                                "/api/v1/payment-engine/payments/*/manual-review/reject",
                                "/api/v1/payment-engine/payments/*/decision/override")
                        .hasRole(COMPLIANCE_OFFICER.keycloakRoleName())
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/customers",
                                "/api/v1/accounts/open-account",
                                "/api/v1/payments/request")
                        .hasRole(OPERATOR.keycloakRoleName())
                        .pathMatchers(HttpMethod.PUT, "/api/v1/customers/*")
                        .hasRole(OPERATOR.keycloakRoleName())
                        .pathMatchers(HttpMethod.GET, "/api/v1/**")
                        .hasRole(VIEWER.keycloakRoleName())
                        .pathMatchers("/api/v1/**")
                        .hasRole(ADMIN.keycloakRoleName())
                        .anyExchange().authenticated())
                .oauth2Login(login -> login
                        .authorizationRequestResolver(pkceAuthorizationRequestResolver(clientRegistrationRepository)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler()))
                .logout(logout -> logout
                        .logoutSuccessHandler(keycloakLogoutSuccessHandler(clientRegistrationRepository)))
                .build();
    }

    @Bean
    public GrantedAuthoritiesMapper keycloakRealmRolesAuthoritiesMapper() {
        return new KeycloakRealmRolesAuthoritiesMapper();
    }

    @Bean
    public WebFilter csrfCookieWebFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> csrfToken = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
            return csrfToken.then(chain.filter(exchange));
        };
    }

    private static ServerOAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        DefaultServerOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
        authorizationRequestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return authorizationRequestResolver;
    }

    private static ServerLogoutSuccessHandler keycloakLogoutSuccessHandler(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        OidcClientInitiatedServerLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri(POST_LOGOUT_REDIRECT_URI);
        return logoutSuccessHandler;
    }
}
