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

    @Transactional
    public void processPaymentSnapshotEvent(PaymentSnapshotEvent snapshot) {
        log.info("Processing payment snapshot: paymentId:{}, referenceNumber:{}, version:{}, trigger:{}",
                snapshot.getPaymentId(),
                snapshot.getReferenceNumber(),
                snapshot.getVersion(),
                snapshot.getEventTrigger());

        Optional<PaymentHistoryEntity> existingHistory =
                paymentHistoryRepository.findById(UUID.fromString(snapshot.getPaymentId()));

        PaymentHistoryEntity history;
        if (existingHistory.isPresent()) {
            history = existingHistory.get();
            log.info("Updating existing payment history: paymentId:{}, referenceNumber:{}, old version:{}, new version:{}",
                    snapshot.getPaymentId(),
                    snapshot.getReferenceNumber(),
                    history.getEntityVersion(),
                    snapshot.getVersion());
        } else {
            history = new PaymentHistoryEntity();
            log.info("Creating new payment history: paymentId:{}, referenceNumber:{}",
                    snapshot.getPaymentId(),
                    snapshot.getReferenceNumber());
        }

        mapSnapshotToHistory(snapshot, history);
        paymentHistoryRepository.save(history);

        log.info("Payment history saved: paymentId:{}, referenceNumber:{}, version:{}",
                snapshot.getPaymentId(),
                snapshot.getReferenceNumber(),
                history.getEntityVersion());
    }

}
