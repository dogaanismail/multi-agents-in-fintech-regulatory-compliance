package org.banksolution.integration.customer;

import org.banksolution.servicesecurity.feign.ServiceTokenFeignConfiguration;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        configuration = ServiceTokenFeignConfiguration.class,
        name = "customer-svc",
        url = "${integration.customer-service.url}"
)
public interface CustomerServiceClient {

    @GetMapping("/{id}")
    CustomerResponse getCustomerById(@PathVariable("id") UUID customerId);

}
