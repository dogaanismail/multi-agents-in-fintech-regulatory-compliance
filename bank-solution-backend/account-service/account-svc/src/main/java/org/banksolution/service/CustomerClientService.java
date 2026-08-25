package org.banksolution.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerClientService {

    private final CustomerServiceClient customerServiceClient;

    public boolean customerExists(UUID customerId) {
        try {
            CustomerResponse customerResponse = customerServiceClient.getCustomerById(customerId);
            return customerResponse != null;
        } catch (FeignException feignException) {
            log.error("Could not verify customer: {}, status: {}", customerId, feignException.status(), feignException);
            return false;
        }
    }
}
