package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.CustomerFeaturesResponse;
import org.banksolution.entity.CustomerProfileEntity;
import org.banksolution.mapper.CustomerProfileMapper;
import org.banksolution.repository.CustomerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;

    @Transactional(readOnly = true)
    public CustomerFeaturesResponse getCustomerFeatures(String customerId) {
        log.debug("Fetching customer features for customerId: {}", customerId);

        UUID customerUuid = UUID.fromString(customerId);
        CustomerProfileEntity profile = customerProfileRepository.findByCustomerId(customerUuid)
                .orElseGet(() -> {
                    log.warn("No profile found for customerId: {}, returning default features", customerId);
                    return CustomerProfileMapper.createDefaultProfile(customerUuid, null);
                });

        return CustomerProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public CustomerFeaturesResponse getFeaturesByAccountId(String accountId) {
        log.debug("Fetching customer features for accountId: {}", accountId);

        UUID accountUuid = UUID.fromString(accountId);
        CustomerProfileEntity profile = customerProfileRepository.findByAccountId(accountUuid)
                .orElseGet(() -> {
                    log.warn("No profile found for accountId: {}, returning default features", accountId);
                    return CustomerProfileMapper.createDefaultProfile(null, accountUuid);
                });

        return CustomerProfileMapper.toResponse(profile);
    }
}