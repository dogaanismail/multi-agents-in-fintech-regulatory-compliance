package org.banksolution.config;

import org.banksolution.security.KeycloakAuthoritiesMapper;
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

import static org.banksolution.security.Permission.ACCOUNT_OPEN;
import static org.banksolution.security.Permission.ACCOUNT_READ;
import static org.banksolution.security.Permission.BENEFICIARY_WRITE;
import static org.banksolution.security.Permission.CONFIGURATION_READ;
import static org.banksolution.security.Permission.CONFIGURATION_WRITE;
import static org.banksolution.security.Permission.CUSTOMER_CREATE;
import static org.banksolution.security.Permission.CUSTOMER_DELETE;
import static org.banksolution.security.Permission.CUSTOMER_READ;
import static org.banksolution.security.Permission.CUSTOMER_UPDATE;
import static org.banksolution.security.Permission.LEDGER_POST;
import static org.banksolution.security.Permission.LEDGER_READ;
import static org.banksolution.security.Permission.MARL_READ;
import static org.banksolution.security.Permission.MARL_TRAIN;
import static org.banksolution.security.Permission.PAYMENT_CREATE;
import static org.banksolution.security.Permission.PAYMENT_ENGINE_COMMAND;
import static org.banksolution.security.Permission.PAYMENT_OVERRIDE;
import static org.banksolution.security.Permission.PAYMENT_READ;
import static org.banksolution.security.Permission.PAYMENT_REVIEW;
import static org.banksolution.security.Permission.RISK_READ;

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
                        .pathMatchers(HttpMethod.GET, "/api/v1/me").authenticated()

                        .pathMatchers(HttpMethod.GET, "/api/v1/payments/**", "/api/v1/payment-history/**",
                                "/api/v1/payment-engine/**", "/api/v1/exchange-rates/**")
                        .hasAuthority(PAYMENT_READ.authority())
                        .pathMatchers(HttpMethod.GET, "/api/v1/customers/**", "/api/v1/customer-profiles/**",
                                "/api/v1/beneficiaries/**")
                        .hasAuthority(CUSTOMER_READ.authority())
                        .pathMatchers(HttpMethod.GET, "/api/v1/accounts/**").hasAuthority(ACCOUNT_READ.authority())
                        .pathMatchers(HttpMethod.GET, "/api/v1/ledger/**").hasAuthority(LEDGER_READ.authority())
                        .pathMatchers(HttpMethod.GET, "/api/v1/risk/**", "/api/v1/networks/**")
                        .hasAuthority(RISK_READ.authority())
                        .pathMatchers(HttpMethod.GET, "/api/v1/marl/**").hasAuthority(MARL_READ.authority())
                        .pathMatchers(HttpMethod.GET, "/api/v1/configurations/**")
                        .hasAuthority(CONFIGURATION_READ.authority())

                        .pathMatchers(HttpMethod.POST, "/api/v1/customers").hasAuthority(CUSTOMER_CREATE.authority())
                        .pathMatchers(HttpMethod.PUT, "/api/v1/customers/*").hasAuthority(CUSTOMER_UPDATE.authority())
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/customers/*").hasAuthority(CUSTOMER_DELETE.authority())
                        .pathMatchers(HttpMethod.POST, "/api/v1/accounts/open-account")
                        .hasAuthority(ACCOUNT_OPEN.authority())
                        .pathMatchers(HttpMethod.POST, "/api/v1/payments/request").hasAuthority(PAYMENT_CREATE.authority())
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/payment-engine/payments/*/manual-review/approve",
                                "/api/v1/payment-engine/payments/*/manual-review/reject")
                        .hasAuthority(PAYMENT_REVIEW.authority())
                        .pathMatchers(HttpMethod.POST, "/api/v1/payment-engine/payments/*/decision/override")
                        .hasAuthority(PAYMENT_OVERRIDE.authority())
                        .pathMatchers(HttpMethod.POST, "/api/v1/payment-engine/payments")
                        .hasAuthority(PAYMENT_ENGINE_COMMAND.authority())
                        .pathMatchers("/api/v1/configurations/**").hasAuthority(CONFIGURATION_WRITE.authority())
                        .pathMatchers(HttpMethod.POST, "/api/v1/marl/training/**").hasAuthority(MARL_TRAIN.authority())
                        .pathMatchers(HttpMethod.POST, "/api/v1/ledger/**").hasAuthority(LEDGER_POST.authority())
                        .pathMatchers("/api/v1/beneficiaries/**").hasAuthority(BENEFICIARY_WRITE.authority())

                        .pathMatchers("/api/v1/**").denyAll()
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
    public GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {
        return new KeycloakAuthoritiesMapper();
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
