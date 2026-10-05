package org.banksolution.fixtures;

import org.banksolution.security.BackofficeRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.OidcLoginMutator;

import java.util.List;

import static org.banksolution.security.BackofficeRole.ADMIN;
import static org.banksolution.security.BackofficeRole.COMPLIANCE_OFFICER;
import static org.banksolution.security.BackofficeRole.OPERATOR;
import static org.banksolution.security.BackofficeRole.VIEWER;
import static org.banksolution.security.KeycloakRealmRolesAuthoritiesMapper.REALM_ROLES_CLAIM;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

public final class BackofficeUserFixtures {

    public static final String KEYCLOAK_REGISTRATION_ID = "keycloak";

    private BackofficeUserFixtures() {
    }

    public static List<BackofficeRole> createEffectiveRoles(BackofficeRole assignedRole) {
        return switch (assignedRole) {
            case VIEWER -> List.of(VIEWER);
            case OPERATOR -> List.of(OPERATOR, VIEWER);
            case COMPLIANCE_OFFICER -> List.of(COMPLIANCE_OFFICER, VIEWER);
            case ADMIN -> List.of(ADMIN, OPERATOR, COMPLIANCE_OFFICER, VIEWER);
        };
    }

    public static OidcLoginMutator createBackofficeLogin(BackofficeRole assignedRole) {
        List<String> realmRoles = createEffectiveRoles(assignedRole).stream()
                .map(BackofficeRole::keycloakRoleName)
                .toList();

        return mockOidcLogin()
                .idToken(idToken -> idToken
                        .claim("preferred_username", assignedRole.keycloakRoleName())
                        .claim(REALM_ROLES_CLAIM, realmRoles))
                .authorities(toRoleAuthorities(realmRoles));
    }

    public static OidcLoginMutator createOfficerLogin(
            String username,
            String fullName,
            String email) {

        List<String> realmRoles = List.of(COMPLIANCE_OFFICER.keycloakRoleName(), VIEWER.keycloakRoleName());

        return mockOidcLogin()
                .idToken(idToken -> idToken
                        .claim("preferred_username", username)
                        .claim("name", fullName)
                        .claim("email", email)
                        .claim(REALM_ROLES_CLAIM, realmRoles))
                .authorities(toRoleAuthorities(realmRoles));
    }

    private static SimpleGrantedAuthority[] toRoleAuthorities(List<String> realmRoles) {
        return realmRoles.stream()
                .map(realmRole -> new SimpleGrantedAuthority("ROLE_" + realmRole))
                .toArray(SimpleGrantedAuthority[]::new);
    }

    public static ClientRegistration createKeycloakClientRegistration() {
        return ClientRegistration.withRegistrationId(KEYCLOAK_REGISTRATION_ID)
                .clientId("backoffice")
                .clientSecret("gateway-tests-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("http://keycloak:8180/realms/bank-solution/protocol/openid-connect/auth")
                .tokenUri("http://keycloak:8180/realms/bank-solution/protocol/openid-connect/token")
                .jwkSetUri("http://keycloak:8180/realms/bank-solution/protocol/openid-connect/certs")
                .userNameAttributeName("preferred_username")
                .build();
    }
}
