package org.banksolution.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.security.KeycloakRealmRolesAuthoritiesMapper.REALM_ROLES_CLAIM;

class KeycloakRealmRolesAuthoritiesMapperTest {

    private final KeycloakRealmRolesAuthoritiesMapper keycloakRealmRolesAuthoritiesMapper =
            new KeycloakRealmRolesAuthoritiesMapper();

    @Test
    void shouldTurnEveryRealmRoleIntoASpringRole() {
        OidcUserAuthority oidcUserAuthority = createOidcUserAuthority(Map.of(
                "sub", "officer",
                REALM_ROLES_CLAIM, List.of("compliance-officer", "viewer")));

        Set<GrantedAuthority> grantedAuthorities =
                keycloakRealmRolesAuthoritiesMapper.mapAuthorities(List.of(oidcUserAuthority));

        assertThat(grantedAuthorities).containsExactlyInAnyOrder(
                new SimpleGrantedAuthority("ROLE_compliance-officer"),
                new SimpleGrantedAuthority("ROLE_viewer"));
    }

    @Test
    void shouldGrantNothingWhenTheIdTokenCarriesNoRealmRoles() {
        OidcUserAuthority oidcUserAuthority = createOidcUserAuthority(Map.of("sub", "nobody"));

        assertThat(keycloakRealmRolesAuthoritiesMapper.mapAuthorities(List.of(oidcUserAuthority))).isEmpty();
    }

    @Test
    void shouldIgnoreAuthoritiesThatAreNotOidcUserAuthorities() {
        GrantedAuthority scopeAuthority = new SimpleGrantedAuthority("SCOPE_openid");

        assertThat(keycloakRealmRolesAuthoritiesMapper.mapAuthorities(List.of(scopeAuthority))).isEmpty();
    }

    private static OidcUserAuthority createOidcUserAuthority(Map<String, Object> idTokenClaims) {
        OidcIdToken oidcIdToken = new OidcIdToken(
                "id-token",
                Instant.parse("2026-10-05T10:00:00Z"),
                Instant.parse("2026-10-05T10:05:00Z"),
                idTokenClaims);
        return new OidcUserAuthority(oidcIdToken);
    }
}
