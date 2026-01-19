package org.banksolution.service;

import com.aml.fraud.CustomerFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.integration.customerprofile.CustomerProfileServiceClient;
import org.banksolution.integration.customerprofile.dto.CustomerFeaturesResponse;
import org.banksolution.mapper.CustomerFeaturesMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerFeatureService {

    private final CustomerProfileServiceClient customerProfileServiceClient;

    public CustomerFeatures getCustomerFeatures(String customerId) {
        log.debug("Fetching customer features for customerId: {}", customerId);
        try {
            CustomerFeaturesResponse response = customerProfileServiceClient.getCustomerFeatures(customerId);
            return CustomerFeaturesMapper.toAvroCustomerFeatures(response);
        } catch (Exception e) {
            log.error("Failed to fetch customer features for customerId: {}", customerId, e);
            return CustomerFeaturesMapper.getDefaultCustomerFeatures(customerId, "");
        }
    }

    public CustomerFeatures getCustomerFeaturesByAccountId(String accountId) {
        log.debug("Fetching customer features for accountId: {}", accountId);
        try {
            CustomerFeaturesResponse response = customerProfileServiceClient.getFeaturesByAccountId(accountId);
            return CustomerFeaturesMapper.toAvroCustomerFeatures(response);
        } catch (Exception e) {
            log.error("Failed to fetch customer features for accountId: {}", accountId, e);
            return CustomerFeaturesMapper.getDefaultCustomerFeatures("", accountId);
        }
    }

}
