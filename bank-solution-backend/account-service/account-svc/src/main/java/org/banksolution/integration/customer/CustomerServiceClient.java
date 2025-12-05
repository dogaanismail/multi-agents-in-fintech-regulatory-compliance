package org.banksolution.integration.customer;

import feign.Headers;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "customer-svc",
        url = "${integration.customer-service.url}"
)
@Headers({"Content-Type: application/json"})
public interface CustomerServiceClient {

    @GetMapping("/{id}")
    CustomerResponse getCustomerById(@PathVariable UUID id);

}
