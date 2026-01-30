package org.banksolution.service;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.TransactionSnapshotEntity;
import org.banksolution.mapper.TransactionSnapshotMapper;
import org.banksolution.repository.TransactionSnapshotRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSnapshotService {

    private final TransactionSnapshotRepository transactionSnapshotRepository;

    @Transactional
    public Optional<TransactionSnapshotEntity> saveIfNotExists(PaymentCompletedEvent event) {
        String paymentId = event.getPaymentId();
        
        Optional<TransactionSnapshotEntity> existing = transactionSnapshotRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.info("Transaction snapshot already exists for paymentId: {}, skipping save", paymentId);
            return existing;
        }

        try {
            TransactionSnapshotEntity entity = TransactionSnapshotMapper.toEntity(event);
            TransactionSnapshotEntity saved = transactionSnapshotRepository.saveAndFlush(entity);
            log.debug("Transaction snapshot saved for paymentId: {}", paymentId);
            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            log.info("Transaction snapshot already exists for paymentId: {} (concurrent insert), retrieving existing", paymentId);
            return transactionSnapshotRepository.findByPaymentId(paymentId);
        }
    }

    @Transactional(readOnly = true)
    public List<TransactionSnapshotEntity> findByCustomerId(UUID customerId) {
        return transactionSnapshotRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public boolean existsByPaymentId(String paymentId) {
        return transactionSnapshotRepository.existsByPaymentId(paymentId);
    }
}
