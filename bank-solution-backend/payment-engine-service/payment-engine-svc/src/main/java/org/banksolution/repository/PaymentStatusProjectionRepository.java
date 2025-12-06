package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.PaymentStatusProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentStatusProjectionRepository extends JpaRepository<@NonNull PaymentStatusProjection, @NonNull UUID> {

    Optional<PaymentStatusProjection> findByReferenceNumber(String referenceNumber);

    List<PaymentStatusProjection> findByCustomerId(UUID customerId);

    Optional<PaymentStatusProjection> findByExternalPaymentId(UUID externalPaymentId);
}
