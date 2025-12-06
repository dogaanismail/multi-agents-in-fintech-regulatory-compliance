package org.banksolution.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.persistence.entity.PaymentIdMapping;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentIdMappingRepository {

    private final PaymentIdMappingJpaRepository jpaRepository;

    @Transactional
    public void save(String referenceNumber, PaymentId paymentId) {
        PaymentIdMapping mapping = PaymentIdMapping.builder()
                .referenceNumber(referenceNumber)
                .paymentId(paymentId.toString())
                .build();
        jpaRepository.save(mapping);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentId> findPaymentIdByReferenceNumber(String referenceNumber) {
        return jpaRepository.findByReferenceNumber(referenceNumber)
                .map(mapping -> new PaymentId(mapping.getPaymentId()));
    }
}
