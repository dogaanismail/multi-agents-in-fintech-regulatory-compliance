package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.banksolution.mapper.CustomerMapper;
import org.banksolution.model.response.MobileProfileResponse;
import org.banksolution.service.CustomerIdentityService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/profile")
@Tag(name = "Profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CustomerIdentityService customerIdentityService;

    @GetMapping
    @Operation(summary = "Get my profile")
    public ResponseEntity<@NonNull MobileProfileResponse> getProfile(@AuthenticationPrincipal Jwt caller) {
        return ResponseEntity.ok(CustomerMapper.toMobileProfileResponse(customerIdentityService.getOnboardedCustomer(caller)));
    }
}
