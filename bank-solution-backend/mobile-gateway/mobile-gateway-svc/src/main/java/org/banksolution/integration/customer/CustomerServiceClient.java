package org.banksolution.integration.customer;

import org.banksolution.integration.customer.model.CustomerCreateRequest;
import org.banksolution.integration.customer.model.CustomerResponse;
import org.banksolution.servicesecurity.feign.ServiceTokenFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        configuration = ServiceTokenFeignConfiguration.class,
        name = "customer-svc",
        url = "${integration.customer-service.url}")
public interface CustomerServiceClient {

    @GetMapping("/identities/{identitySubject}")
    CustomerResponse getCustomerByIdentitySubject(@PathVariable String identitySubject);

    @PostMapping("/identities/{identitySubject}")
    CustomerResponse onboardCustomer(
            @PathVariable String identitySubject,
            @RequestBody CustomerCreateRequest customerCreateRequest);
}
