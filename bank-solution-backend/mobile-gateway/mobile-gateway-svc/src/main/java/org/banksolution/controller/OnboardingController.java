package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.banksolution.mapper.CustomerMapper;
import org.banksolution.model.request.OnboardingRequest;
import org.banksolution.model.response.MobileProfileResponse;
import org.banksolution.service.OnboardingService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/onboarding")
@Tag(name = "Onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    @Operation(summary = "Become a customer", description = "Creates my customer record; repeating it returns the same customer")
    public ResponseEntity<@NonNull MobileProfileResponse> onboard(
            @AuthenticationPrincipal Jwt caller,
            @Valid @RequestBody OnboardingRequest onboardingRequest) {

        return ResponseEntity.ok(CustomerMapper.toMobileProfileResponse(
                onboardingService.onboardCustomer(caller, onboardingRequest)));
    }
}
