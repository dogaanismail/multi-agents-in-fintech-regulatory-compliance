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

public class KeycloakAuthoritiesMapper implements GrantedAuthoritiesMapper {

    public static final String REALM_ROLES_CLAIM = "roles";
    public static final String PERMISSIONS_CLAIM = "permissions";
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    @Override
    public Set<GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .flatMap(KeycloakAuthoritiesMapper::toGrantedAuthorities)
                .collect(Collectors.toSet());
    }

    private static Stream<GrantedAuthority> toGrantedAuthorities(OidcUserAuthority oidcUserAuthority) {
        Stream<GrantedAuthority> permissionAuthorities = readClaim(oidcUserAuthority, PERMISSIONS_CLAIM)
                .map(SimpleGrantedAuthority::new);
        Stream<GrantedAuthority> roleAuthorities = readClaim(oidcUserAuthority, REALM_ROLES_CLAIM)
                .map(realmRole -> new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + realmRole));
        return Stream.concat(permissionAuthorities, roleAuthorities);
    }

    private static Stream<String> readClaim(OidcUserAuthority oidcUserAuthority, String claimName) {
        List<String> claimValues = oidcUserAuthority.getIdToken().getClaimAsStringList(claimName);
        return claimValues == null ? Stream.empty() : claimValues.stream();
    }
}
