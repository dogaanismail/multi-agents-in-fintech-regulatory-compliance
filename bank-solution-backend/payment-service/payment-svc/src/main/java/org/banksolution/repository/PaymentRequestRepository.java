package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.PaymentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRequestRepository extends JpaRepository<@NonNull PaymentRequestEntity, @NonNull UUID> {

    Optional<PaymentRequestEntity> findByReferenceNumber(String referenceNumber);

    List<PaymentRequestEntity> findByCustomerId(UUID customerId);

    boolean existsByReferenceNumber(String referenceNumber);
}

