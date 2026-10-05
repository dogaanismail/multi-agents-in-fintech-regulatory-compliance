package org.banksolution.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.banksolution.exception.OnboardingRequiredException;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.banksolution.integration.customer.model.CustomerResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerIdentityService {

    private final CustomerServiceClient customerServiceClient;

    public CustomerResponse getOnboardedCustomer(Jwt caller) {
        return findOnboardedCustomer(caller).orElseThrow(OnboardingRequiredException::new);
    }

    public Optional<CustomerResponse> findOnboardedCustomer(Jwt caller) {
        try {
            return Optional.of(customerServiceClient.getCustomerByIdentitySubject(caller.getSubject()));
        } catch (FeignException.NotFound _) {
            return Optional.empty();
        }
    }
}
