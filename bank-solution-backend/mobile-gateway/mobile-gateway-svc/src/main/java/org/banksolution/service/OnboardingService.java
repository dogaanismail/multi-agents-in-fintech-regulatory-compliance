package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.banksolution.integration.customer.model.CustomerResponse;
import org.banksolution.mapper.CustomerMapper;
import org.banksolution.model.request.OnboardingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private static final String EMAIL_CLAIM = "email";

    private final CustomerIdentityService customerIdentityService;
    private final CustomerServiceClient customerServiceClient;

    public CustomerResponse onboardCustomer(
            Jwt caller,
            OnboardingRequest onboardingRequest) {

        Optional<CustomerResponse> onboardedCustomer = customerIdentityService.findOnboardedCustomer(caller);
        if (onboardedCustomer.isPresent()) {
            return onboardedCustomer.get();
        }

        String loginEmail = caller.getClaimAsString(EMAIL_CLAIM);
        if (loginEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The login has no email address");
        }

        log.info("Onboarding customer for identity {}", caller.getSubject());
        return customerServiceClient.onboardCustomer(
                caller.getSubject(),
                CustomerMapper.toCustomerCreateRequest(loginEmail, onboardingRequest));
    }
}
