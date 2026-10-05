package org.banksolution.servicesecurity.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrustedIssuerAuthenticationManagerResolverTest {

    private static final String STAFF_ISSUER = "http://keycloak:8180/realms/bank-staff";
    private static final String CUSTOMER_ISSUER = "http://keycloak:8180/realms/bank-customers";

    private final TrustedIssuerAuthenticationManagerResolver trustedIssuerAuthenticationManagerResolver =
            new TrustedIssuerAuthenticationManagerResolver(List.of(STAFF_ISSUER), List.of("ledger-service", "bank-platform"));

    @Test
    void shouldRefuseTokensFromAnIssuerThatIsNotTrusted() {
        assertThat(trustedIssuerAuthenticationManagerResolver.resolve(CUSTOMER_ISSUER)).isNull();
    }

    @Test
    void shouldResolveTheSameAuthenticationManagerForATrustedIssuerEveryTime() {
        assertThat(trustedIssuerAuthenticationManagerResolver.resolve(STAFF_ISSUER))
                .isNotNull()
                .isSameAs(trustedIssuerAuthenticationManagerResolver.resolve(STAFF_ISSUER));
    }

    @Test
    void shouldAcceptATokenAddressedToThisService() {
        OAuth2TokenValidator<Jwt> tokenValidator = trustedIssuerAuthenticationManagerResolver.createTokenValidator(STAFF_ISSUER);

        assertThat(tokenValidator.validate(createJwt(STAFF_ISSUER, List.of("ledger-service"))).hasErrors()).isFalse();
    }

    @Test
    void shouldRejectATokenAddressedToAnotherService() {
        OAuth2TokenValidator<Jwt> tokenValidator = trustedIssuerAuthenticationManagerResolver.createTokenValidator(STAFF_ISSUER);

        assertThat(tokenValidator.validate(createJwt(STAFF_ISSUER, List.of("account-service"))).hasErrors()).isTrue();
    }

    @Test
    void shouldRejectATokenWhoseIssuerClaimDoesNotMatchTheIssuerItWasResolvedFor() {
        OAuth2TokenValidator<Jwt> tokenValidator = trustedIssuerAuthenticationManagerResolver.createTokenValidator(STAFF_ISSUER);

        assertThat(tokenValidator.validate(createJwt(CUSTOMER_ISSUER, List.of("ledger-service"))).hasErrors()).isTrue();
    }

    @Test
    void shouldRefuseToStartWithoutAcceptedAudiences() {
        List<String> trustedIssuers = List.of(STAFF_ISSUER);
        List<String> noAudiences = List.of();

        assertThatThrownBy(() -> new TrustedIssuerAuthenticationManagerResolver(trustedIssuers, noAudiences))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldGrantThePermissionsClaimAsAuthoritiesWithoutAPrefix() {
        Jwt jwt = createJwt(STAFF_ISSUER, List.of("ledger-service"));

        assertThat(TrustedIssuerAuthenticationManagerResolver.createPermissionsAuthenticationConverter()
                .convert(jwt)
                .getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ledger.read", "ledger.post")
                .noneMatch(authority -> {
                    assert authority != null;
                    return authority.startsWith("SCOPE_") || authority.startsWith("ROLE_");
                });
    }

    private static Jwt createJwt(String issuer, List<String> audiences) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audiences)
                .subject("caller")
                .claim(TrustedIssuerAuthenticationManagerResolver.PERMISSIONS_CLAIM, List.of("ledger.read", "ledger.post"))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();
    }
}
