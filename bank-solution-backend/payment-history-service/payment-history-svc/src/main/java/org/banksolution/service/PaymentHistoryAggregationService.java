package org.banksolution.service;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.repository.PaymentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.banksolution.mapper.PaymentSnapshotMapper.mapSnapshotToHistory;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryAggregationService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    /**
     * Snapshots for one payment share a Kafka key, so live delivery is ordered; a replayed or
     * redelivered snapshot may still carry an older aggregate version than the row already holds.
     */
    private static boolean isStale(PaymentSnapshotEvent paymentSnapshotEvent, PaymentHistoryEntity paymentHistoryEntity) {
        Integer storedAggregateVersion = paymentHistoryEntity.getAggregateVersion();
        return storedAggregateVersion != null && paymentSnapshotEvent.getVersion() < storedAggregateVersion;
    }

    @Transactional
    public void processPaymentSnapshotEvent(PaymentSnapshotEvent paymentSnapshotEvent) {
        log.info("Processing payment snapshot: paymentId:{}, referenceNumber:{}, version:{}, trigger:{}",
                paymentSnapshotEvent.getPaymentId(),
                paymentSnapshotEvent.getReferenceNumber(),
                paymentSnapshotEvent.getVersion(),
                paymentSnapshotEvent.getEventTrigger());

        Optional<PaymentHistoryEntity> existingPaymentHistoryEntity =
                paymentHistoryRepository.findById(UUID.fromString(paymentSnapshotEvent.getPaymentId()));

        PaymentHistoryEntity paymentHistoryEntity;
        if (existingPaymentHistoryEntity.isPresent()) {
            paymentHistoryEntity = existingPaymentHistoryEntity.get();
            if (isStale(paymentSnapshotEvent, paymentHistoryEntity)) {
                log.warn("Ignoring stale payment snapshot: paymentId:{}, snapshot version:{}, stored version:{}",
                        paymentSnapshotEvent.getPaymentId(),
                        paymentSnapshotEvent.getVersion(),
                        paymentHistoryEntity.getAggregateVersion());
                return;
            }
            log.info("Updating existing payment history: paymentId:{}, referenceNumber:{}, old version:{}, new version:{}",
                    paymentSnapshotEvent.getPaymentId(),
                    paymentSnapshotEvent.getReferenceNumber(),
                    paymentHistoryEntity.getEntityVersion(),
                    paymentSnapshotEvent.getVersion());
        } else {
            paymentHistoryEntity = new PaymentHistoryEntity();
            log.info("Creating new payment history: paymentId:{}, referenceNumber:{}",
                    paymentSnapshotEvent.getPaymentId(),
                    paymentSnapshotEvent.getReferenceNumber());
        }

        mapSnapshotToHistory(paymentSnapshotEvent, paymentHistoryEntity);
        paymentHistoryRepository.save(paymentHistoryEntity);

        log.info("Payment history saved: paymentId:{}, referenceNumber:{}, version:{}",
                paymentSnapshotEvent.getPaymentId(),
                paymentSnapshotEvent.getReferenceNumber(),
                paymentHistoryEntity.getEntityVersion());
    }

}
