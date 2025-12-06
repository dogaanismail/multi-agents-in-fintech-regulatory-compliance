package org.banksolution.service;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.mapper.PaymentSnapshotMapper;
import org.banksolution.repository.PaymentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryAggregationService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional
    public void processPaymentSnapshot(PaymentSnapshotEvent snapshot) {
        log.info("Processing payment snapshot: referenceNumber:{}, version:{}, trigger:{}",
                snapshot.getReferenceNumber(),
                snapshot.getVersion(),
                snapshot.getEventTrigger());

        Optional<PaymentHistoryEntity> existingHistory =
                paymentHistoryRepository.findByReferenceNumber(snapshot.getReferenceNumber());

        PaymentHistoryEntity history;
        if (existingHistory.isPresent()) {
            history = existingHistory.get();
            log.info("Updating existing payment history: referenceNumber:{}, old version:{}, new version:{}",
                    snapshot.getReferenceNumber(),
                    history.getEntityVersion(),
                    snapshot.getVersion());
        } else {
            history = new PaymentHistoryEntity();
            log.info("Creating new payment history: referenceNumber={}", snapshot.getReferenceNumber());
        }

        PaymentSnapshotMapper.mapSnapshotToHistory(snapshot, history);

        paymentHistoryRepository.save(history);
        log.info("Payment history saved: referenceNumber:{}, version:{}",
                snapshot.getReferenceNumber(),
                history.getEntityVersion());
    }

}
