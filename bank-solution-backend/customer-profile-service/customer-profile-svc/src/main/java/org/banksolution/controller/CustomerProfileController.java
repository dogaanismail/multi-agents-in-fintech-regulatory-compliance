package org.banksolution.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.CustomerFeaturesResponse;
import org.banksolution.service.CustomerProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-profiles")
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/customer/{customerId}/features")
    public ResponseEntity<CustomerFeaturesResponse> getCustomerFeatures(@PathVariable String customerId) {
        log.info("REST request to get customer features for customerId: {}", customerId);
        CustomerFeaturesResponse features = customerProfileService.getCustomerFeatures(customerId);
        return ResponseEntity.ok(features);
    }

    @GetMapping("/account/{accountId}/features")
    public ResponseEntity<CustomerFeaturesResponse> getFeaturesByAccountId(@PathVariable String accountId) {
        log.info("REST request to get customer features for accountId: {}", accountId);
        CustomerFeaturesResponse features = customerProfileService.getFeaturesByAccountId(accountId);
        return ResponseEntity.ok(features);
    }
}
