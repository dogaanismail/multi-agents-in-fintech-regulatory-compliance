package org.banksolution.infrastructure.persistence.repository;

import org.banksolution.infrastructure.persistence.entity.PaymentIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentIdMappingJpaRepository extends JpaRepository<PaymentIdMapping, String> {
    
    Optional<PaymentIdMapping> findByReferenceNumber(String referenceNumber);
}
