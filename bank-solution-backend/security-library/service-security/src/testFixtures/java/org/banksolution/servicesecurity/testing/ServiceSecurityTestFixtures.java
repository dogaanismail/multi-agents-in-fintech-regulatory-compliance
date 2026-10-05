package org.banksolution.servicesecurity.testing;

import org.banksolution.servicesecurity.AuthenticatedCaller;
import org.banksolution.servicesecurity.Permissions;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class ServiceSecurityTestFixtures {

    public static final String TEST_CALLER_USERNAME = "integration-test-caller";

    private ServiceSecurityTestFixtures() {
    }

    public static JwtRequestPostProcessor createCallerJwt(String username, String... permissions) {
        return jwt()
                .jwt(token -> token.claim(AuthenticatedCaller.USERNAME_CLAIM, username))
                .authorities(Arrays.stream(permissions).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }

    public static JwtRequestPostProcessor createCallerJwtWithEveryPermission() {
        return createCallerJwt(TEST_CALLER_USERNAME, readEveryPermission());
    }

    public static String[] readEveryPermission() {
        return Arrays.stream(Permissions.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .map(ServiceSecurityTestFixtures::readConstant)
                .toArray(String[]::new);
    }

    private static String readConstant(Field permissionField) {
        try {
            return (String) permissionField.get(null);
        } catch (IllegalAccessException inaccessible) {
            throw new IllegalStateException(inaccessible);
        }
    }
}
