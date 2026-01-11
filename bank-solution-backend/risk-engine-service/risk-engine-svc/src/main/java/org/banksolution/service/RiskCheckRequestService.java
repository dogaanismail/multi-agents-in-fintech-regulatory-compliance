package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.exception.RiskCheckRequestNotFoundException;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCheckRequestService {

    private final RiskCheckRequestRepository riskCheckRequestRepository;

    @Transactional
    public void save(RiskCheckRequestEntity entity) {
        riskCheckRequestRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public RiskCheckRequestEntity findById(UUID id) {
        return riskCheckRequestRepository.findById(id)
                .orElseThrow(() -> new RiskCheckRequestNotFoundException(id));
    }
}
