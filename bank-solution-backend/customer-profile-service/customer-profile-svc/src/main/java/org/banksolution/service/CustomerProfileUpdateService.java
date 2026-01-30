package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.CustomerProfileEntity;
import org.banksolution.entity.TransactionSnapshotEntity;
import org.banksolution.mapper.CustomerProfileMapper;
import org.banksolution.repository.CustomerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerProfileUpdateService {

    private final CustomerProfileRepository customerProfileRepository;
    private final TransactionSnapshotService transactionSnapshotService;
    private final FeatureCalculationService featureCalculationService;

    @Transactional
    public void updateProfile(UUID customerId) {
        List<TransactionSnapshotEntity> transactions = transactionSnapshotService.findByCustomerId(customerId);

        if (transactions.isEmpty()) {
            log.warn("No transactions found for customerId: {}, skipping profile update", customerId);
            return;
        }

        UUID accountId = transactions.getFirst().getAccountId();
        CustomerProfileEntity customerProfile = findOrCreateProfile(customerId, accountId);

        featureCalculationService.calculateAndUpdateFeatures(customerProfile, transactions);
        customerProfile.setLastUpdatedAt(Instant.now());

        customerProfileRepository.save(customerProfile);

        log.info("Customer profile updated: customerId: {}, accountId: {}, txCount: {}",
                customerId,
                accountId,
                customerProfile.getTransactionCount());
    }

    private CustomerProfileEntity findOrCreateProfile(UUID customerId, UUID accountId) {
        return customerProfileRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Creating new customer profile: customerId: {}, accountId: {}", customerId, accountId);
                    return CustomerProfileMapper.createDefaultProfile(customerId, accountId);
                });
    }
}
