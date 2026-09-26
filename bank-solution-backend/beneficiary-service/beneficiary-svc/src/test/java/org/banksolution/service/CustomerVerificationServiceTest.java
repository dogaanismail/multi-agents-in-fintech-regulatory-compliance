package org.banksolution.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.BeneficiaryFixtures.createCustomerResponse;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.exception.CustomerServiceUnavailableException;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerVerificationServiceTest {

    @Mock
    private CustomerServiceClient customerServiceClient;

    @InjectMocks
    private CustomerVerificationService customerVerificationService;

    @Test
    void shouldPassWhenCustomerServiceReturnsTheCustomer() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenReturn(createCustomerResponse(customerId));

        assertThatCode(() -> customerVerificationService.verifyCustomerExists(customerId)).doesNotThrowAnyException();
    }

    @Test
    void shouldReportAMissingCustomerWhenCustomerServiceAnswers404() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenThrow(createFeignException(404));

        assertThatThrownBy(() -> customerVerificationService.verifyCustomerExists(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    @Test
    void shouldReportAMissingCustomerWhenCustomerServiceAnswersWithAnEmptyBody() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenReturn(null);

        assertThatThrownBy(() -> customerVerificationService.verifyCustomerExists(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    }

    @Test
    void shouldReportCustomerServiceUnavailableRatherThanAMissingCustomerOnAServerError() {
        UUID customerId = UUID.randomUUID();
        when(customerServiceClient.getCustomerById(customerId)).thenThrow(createFeignException(503));

        assertThatThrownBy(() -> customerVerificationService.verifyCustomerExists(customerId))
                .isInstanceOf(CustomerServiceUnavailableException.class)
                .hasCauseInstanceOf(FeignException.class);
    }

    private static FeignException createFeignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/customers", Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("getCustomerById", feign.Response.builder()
                .status(status)
                .reason("status " + status)
                .request(request)
                .headers(Map.of())
                .build());
    }
}
