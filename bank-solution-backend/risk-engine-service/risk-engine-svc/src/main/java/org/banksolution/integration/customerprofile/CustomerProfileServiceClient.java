package org.banksolution.integration.customerprofile;

import feign.Headers;
import org.banksolution.integration.customerprofile.dto.CustomerFeaturesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customer-profile-svc",
        url = "${integration.customer-profile-service.url}"
)
@Headers({"Content-Type: application/json"})
public interface CustomerProfileServiceClient {

    @GetMapping("/customer/{customerId}/features")
    CustomerFeaturesResponse getCustomerFeatures(@PathVariable String customerId);

    @GetMapping("/account/{accountId}/features")
    CustomerFeaturesResponse getFeaturesByAccountId(@PathVariable String accountId);
}