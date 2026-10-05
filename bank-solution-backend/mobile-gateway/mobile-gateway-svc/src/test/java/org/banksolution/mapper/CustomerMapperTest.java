package org.banksolution.mapper;

import org.banksolution.integration.customer.model.CustomerCreateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_EMAIL;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCallerCustomerResponse;
import static org.banksolution.fixtures.MobileGatewayFixtures.createOnboardingRequest;

class CustomerMapperTest {

    @Test
    void shouldCreateAnIndividualCustomerWithTheEmailOfTheLogin() {
        CustomerCreateRequest customerCreateRequest = CustomerMapper.toCustomerCreateRequest(CALLER_EMAIL, createOnboardingRequest());

        assertThat(customerCreateRequest.email()).isEqualTo(CALLER_EMAIL);
        assertThat(customerCreateRequest.customerType()).isEqualTo("INDIVIDUAL");
        assertThat(customerCreateRequest.address().city()).isEqualTo("London");
        assertThat(customerCreateRequest.address().countryCode()).isEqualTo("GB");
    }

    @Test
    void shouldExposeOnlyTheProfileFieldsOfACustomer() {
        assertThat(CustomerMapper.toMobileProfileResponse(createCallerCustomerResponse()).customerId())
                .isEqualTo(CALLER_CUSTOMER_ID);
    }
}
