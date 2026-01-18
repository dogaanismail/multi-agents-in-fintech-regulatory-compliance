package org.banksolution.service;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.CustomerProfileEntity;
import org.banksolution.entity.TransactionSnapshotEntity;
import org.banksolution.mapper.CustomerProfileMapper;
import org.banksolution.mapper.TransactionSnapshotMapper;
import org.banksolution.repository.CustomerProfileRepository;
import org.banksolution.repository.TransactionSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileAggregationService {

    private final TransactionSnapshotRepository transactionSnapshotRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final FeatureCalculationService featureCalculationService;

    @Transactional
    public void processPaymentEvent(PaymentCompletedEvent event) {
        TransactionSnapshotEntity transactionSnapshotEntity = TransactionSnapshotMapper.toEntity(event);
        transactionSnapshotRepository.save(transactionSnapshotEntity);
        log.debug("Transaction snapshot saved for paymentId: {}", event.getPaymentId());
    }

    @Transactional
    public void updateCustomerProfile(UUID customerId) {
        List<TransactionSnapshotEntity> transactions = transactionSnapshotRepository.findByCustomerId(customerId);
        if (transactions.isEmpty()) {
            log.warn("No transactions found for customerId: {}, skipping profile update", customerId);
            return;
        }

        UUID accountId = transactions.getFirst().getAccountId();
        CustomerProfileEntity customerProfile = customerProfileRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewProfile(customerId, accountId));

        featureCalculationService.calculateAndUpdateFeatures(customerProfile, transactions);

        customerProfile.setLastUpdatedAt(Instant.now());
        customerProfileRepository.save(customerProfile);

        log.info("Customer profile updated: customerId: {}, accountId: {}, txCount: {}",
                customerId,
                accountId,
                customerProfile.getTransactionCount());
    }

    private CustomerProfileEntity createNewProfile(UUID customerId, UUID accountId) {
        log.info("Creating new customer profile: customerId: {}, accountId: {}", customerId, accountId);
        return CustomerProfileMapper.createDefaultProfile(customerId, accountId);
    }
}
