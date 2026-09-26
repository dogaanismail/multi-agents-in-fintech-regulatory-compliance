package org.banksolution.service;

import feign.FeignException;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.exception.CustomerServiceUnavailableException;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerVerificationService {

    private final CustomerServiceClient customerServiceClient;

    public void verifyCustomerExists(UUID customerId) {
        CustomerResponse customerResponse = fetchCustomer(customerId);

        if (customerResponse == null) {
            throw new CustomerNotFoundException(customerId);
        }
    }

    private CustomerResponse fetchCustomer(UUID customerId) {
        try {
            return customerServiceClient.getCustomerById(customerId);
        } catch (FeignException.NotFound _) {
            return null;
        } catch (FeignException feignException) {
            log.error("Could not verify customer: {}, status: {}",
                    customerId,
                    feignException.status(),
                    feignException);

            throw new CustomerServiceUnavailableException(customerId, feignException);
        }
    }
}
