package org.banksolution.mapper;

import org.banksolution.integration.customer.model.CustomerAddressRequest;
import org.banksolution.integration.customer.model.CustomerCreateRequest;
import org.banksolution.integration.customer.model.CustomerResponse;
import org.banksolution.model.request.OnboardingRequest;
import org.banksolution.model.response.MobileProfileResponse;

public final class CustomerMapper {

    private static final String INDIVIDUAL_CUSTOMER = "INDIVIDUAL";

    private CustomerMapper() {
    }

    public static CustomerCreateRequest toCustomerCreateRequest(
            String loginEmail,
            OnboardingRequest onboardingRequest) {

        return new CustomerCreateRequest(
                onboardingRequest.firstName(),
                onboardingRequest.lastName(),
                loginEmail,
                onboardingRequest.phoneNumber(),
                onboardingRequest.dateOfBirth(),
                onboardingRequest.nationality(),
                INDIVIDUAL_CUSTOMER,
                new CustomerAddressRequest(onboardingRequest.city(), onboardingRequest.countryCode()));
    }

    public static MobileProfileResponse toMobileProfileResponse(CustomerResponse customerResponse) {
        return new MobileProfileResponse(
                customerResponse.id(),
                customerResponse.firstName(),
                customerResponse.lastName(),
                customerResponse.email());
    }
}
