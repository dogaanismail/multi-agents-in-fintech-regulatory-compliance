package org.banksolution.servicesecurity.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TrustedIssuerAuthenticationManagerResolver implements AuthenticationManagerResolver<String> {

    public static final String PERMISSIONS_CLAIM = "permissions";

    private final Set<String> trustedIssuers;
    private final Set<String> acceptedAudiences;
    private final Map<String, AuthenticationManager> authenticationManagersByIssuer = new ConcurrentHashMap<>();

    public TrustedIssuerAuthenticationManagerResolver(
            List<String> trustedIssuers,
            List<String> acceptedAudiences) {

        if (acceptedAudiences.isEmpty()) {
            throw new IllegalStateException("banksolution.security.audiences must name the audiences this service accepts");
        }

        this.trustedIssuers = Set.copyOf(trustedIssuers);
        this.acceptedAudiences = Set.copyOf(acceptedAudiences);
    }

    @Override
    public @Nullable AuthenticationManager resolve(@NonNull String issuer) {
        if (!trustedIssuers.contains(issuer)) {
            return null;
        }

        return authenticationManagersByIssuer.computeIfAbsent(issuer, this::createAuthenticationManager);
    }

    public static JwtAuthenticationConverter createPermissionsAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter permissionsConverter = new JwtGrantedAuthoritiesConverter();
        permissionsConverter.setAuthoritiesClaimName(PERMISSIONS_CLAIM);
        permissionsConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(permissionsConverter);
        authenticationConverter.setPrincipalClaimName(JwtClaimNames.SUB);

        return authenticationConverter;
    }

    OAuth2TokenValidator<Jwt> createTokenValidator(String issuer) {
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                audiences -> audiences.stream().anyMatch(acceptedAudiences::contains));

        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator);
    }

    private AuthenticationManager createAuthenticationManager(String issuer) {
        JwtDecoder discoveredOnFirstTokenJwtDecoder = new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            jwtDecoder.setJwtValidator(createTokenValidator(issuer));

            return jwtDecoder;
        });

        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(discoveredOnFirstTokenJwtDecoder);
        jwtAuthenticationProvider.setJwtAuthenticationConverter(createPermissionsAuthenticationConverter());

        return new ProviderManager(jwtAuthenticationProvider);
    }
}
