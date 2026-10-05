package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.banksolution.model.response.CurrentUserResponse;
import org.banksolution.security.KeycloakAuthoritiesMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Current User")
public class CurrentUserController {

    @GetMapping
    @Operation(summary = "Get the signed-in user", description = "Returns the signed-in user, their roles and their effective permissions")
    public ResponseEntity<@NonNull CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal OidcUser oidcUser) {
        return ResponseEntity.ok(new CurrentUserResponse(
                oidcUser.getPreferredUsername(),
                oidcUser.getFullName(),
                oidcUser.getEmail(),
                readClaim(oidcUser, KeycloakAuthoritiesMapper.REALM_ROLES_CLAIM),
                readClaim(oidcUser, KeycloakAuthoritiesMapper.PERMISSIONS_CLAIM)));
    }

    private static List<String> readClaim(OidcUser oidcUser, String claimName) {
        return Optional.ofNullable(oidcUser.getClaimAsStringList(claimName)).orElse(List.of());
    }
}
