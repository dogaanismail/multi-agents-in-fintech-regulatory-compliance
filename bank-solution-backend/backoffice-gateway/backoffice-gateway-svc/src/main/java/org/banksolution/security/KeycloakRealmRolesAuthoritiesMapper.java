package org.banksolution.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakRealmRolesAuthoritiesMapper implements GrantedAuthoritiesMapper {

    public static final String REALM_ROLES_CLAIM = "roles";
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    @Override
    public Set<GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .flatMap(KeycloakRealmRolesAuthoritiesMapper::readRealmRoles)
                .map(realmRole -> new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + realmRole))
                .collect(Collectors.toSet());
    }

    private static Stream<String> readRealmRoles(OidcUserAuthority oidcUserAuthority) {
        List<String> realmRoles = oidcUserAuthority.getIdToken().getClaimAsStringList(REALM_ROLES_CLAIM);
        return realmRoles == null ? Stream.empty() : realmRoles.stream();
    }
}
