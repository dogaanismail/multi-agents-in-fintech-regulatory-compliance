package org.banksolution.repository;

import org.banksolution.valueobject.PaymentId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PaymentIdMappingRepository {

    private final Map<String, PaymentId> referenceToPaymentIdMap = new ConcurrentHashMap<>();

    public void save(String referenceNumber, PaymentId paymentId) {
        referenceToPaymentIdMap.put(referenceNumber, paymentId);
    }

    public Optional<PaymentId> findPaymentIdByReferenceNumber(String referenceNumber) {
        return Optional.ofNullable(referenceToPaymentIdMap.get(referenceNumber));
    }
}
