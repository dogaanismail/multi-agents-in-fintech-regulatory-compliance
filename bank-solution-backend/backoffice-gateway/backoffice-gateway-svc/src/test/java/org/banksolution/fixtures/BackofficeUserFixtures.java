package org.banksolution.fixtures;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.OidcLoginMutator;

import java.util.List;
import java.util.stream.Stream;

import static org.banksolution.fixtures.StaffRealmFixtures.readRolePermissions;
import static org.banksolution.security.KeycloakAuthoritiesMapper.PERMISSIONS_CLAIM;
import static org.banksolution.security.KeycloakAuthoritiesMapper.REALM_ROLES_CLAIM;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

public final class BackofficeUserFixtures {

    public static final String KEYCLOAK_REGISTRATION_ID = "keycloak";
    private static final String STAFF_REALM_URL = "http://keycloak:8180/realms/bank-staff";

    private BackofficeUserFixtures() {
    }

    public static OidcLoginMutator createStaffLogin(String realmRole) {
        return createStaffLogin(realmRole, realmRole, null, null);
    }

    public static OidcLoginMutator createStaffLogin(
            String realmRole,
            String username,
            String fullName,
            String email) {

        List<String> permissions = readRolePermissions(realmRole).stream().sorted().toList();
        GrantedAuthority[] grantedAuthorities = Stream.concat(
                        permissions.stream().map(SimpleGrantedAuthority::new),
                        Stream.of(new SimpleGrantedAuthority("ROLE_" + realmRole)))
                .toArray(GrantedAuthority[]::new);

        return mockOidcLogin()
                .idToken(idToken -> {
                    idToken.claim("preferred_username", username)
                            .claim(REALM_ROLES_CLAIM, List.of(realmRole))
                            .claim(PERMISSIONS_CLAIM, permissions);
                    if (fullName != null) {
                        idToken.claim("name", fullName).claim("email", email);
                    }
                })
                .authorities(grantedAuthorities);
    }

    public static ClientRegistration createKeycloakClientRegistration() {
        return ClientRegistration.withRegistrationId(KEYCLOAK_REGISTRATION_ID)
                .clientId("backoffice")
                .clientSecret("gateway-tests-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(STAFF_REALM_URL + "/protocol/openid-connect/auth")
                .tokenUri(STAFF_REALM_URL + "/protocol/openid-connect/token")
                .jwkSetUri(STAFF_REALM_URL + "/protocol/openid-connect/certs")
                .userNameAttributeName("preferred_username")
                .build();
    }
}
