package org.banksolution.servicesecurity;

import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthenticatedCaller {

    public static final String USERNAME_CLAIM = "preferred_username";

    private AuthenticatedCaller() {
    }

    public static String username(Jwt jwt) {
        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        return username != null ? username : jwt.getSubject();
    }
}
