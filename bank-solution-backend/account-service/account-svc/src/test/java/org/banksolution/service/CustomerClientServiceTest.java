package org.banksolution.service;

import feign.FeignException;
import feign.Request;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AccountFixtures.createCustomerResponse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerClientServiceTest {

    @Mock
    private CustomerServiceClient customerServiceClient;

    @InjectMocks
    private CustomerClientService customerClientService;

    @Test
    void shouldConfirmACustomerTheCustomerServiceReturns() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenReturn(createCustomerResponse(customerId));

        assertThat(customerClientService.customerExists(customerId)).isTrue();
    }

    @Test
    void shouldTreatAnEmptyBodyAsAMissingCustomer() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenReturn(null);

        assertThat(customerClientService.customerExists(customerId)).isFalse();
    }

    @Test
    void shouldTreatAFeignFailureAsAMissingCustomer() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenThrow(createNotFoundFeignException());

        assertThat(customerClientService.customerExists(customerId)).isFalse();
    }

    static FeignException createNotFoundFeignException() {
        Request request = Request.create(
                Request.HttpMethod.GET, "/api/v1/customers", Map.of(), null, StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("customer not found", request, null, Map.of());
    }
}
