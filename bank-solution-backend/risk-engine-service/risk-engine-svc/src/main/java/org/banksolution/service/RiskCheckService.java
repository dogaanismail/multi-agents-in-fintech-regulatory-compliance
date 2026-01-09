package org.banksolution.service;

import com.aml.risk.RiskCheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.mapper.RiskCheckRequestEntityMapper;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.banksolution.enums.RiskCheckStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCheckService {

    private final RiskCheckRequestRepository riskCheckRequestRepository;

    @Transactional
    public void processRiskCheckRequest(RiskCheckRequest event) {
        log.info("Processing risk check request for paymentId: {}", event.getPaymentId());

        if (riskCheckRequestRepository.existsByPaymentId(event.getPaymentId())) {
            log.warn("Duplicate risk check request received for paymentId: {}, skipping", event.getPaymentId());
            return;
        }

        RiskCheckRequestEntity entity = RiskCheckRequestEntityMapper.toEntity(event);
        entity.setStatus(PROCESSING);

        RiskCheckRequestEntity savedEntity = riskCheckRequestRepository.save(entity);
        log.info("Saved risk check request to database: id:{}, paymentId:{}", savedEntity.getId(), savedEntity.getPaymentId());

        try {
            savedEntity.setStatus(AWAITING_MARL);
            riskCheckRequestRepository.save(savedEntity);
        } catch (Exception e) {
            log.error("Failed to publish FraudDetectionRequest for paymentId: {}", event.getPaymentId(), e);
            savedEntity.setStatus(FAILED);
            riskCheckRequestRepository.save(savedEntity);
            throw e;
        }
    }

}
