package org.banksolution.repository;

import org.banksolution.entity.TransactionSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionSnapshotRepository extends JpaRepository<TransactionSnapshotEntity, UUID> {
    List<TransactionSnapshotEntity> findByCustomerId(UUID customerId);
    Optional<TransactionSnapshotEntity> findByPaymentId(String paymentId);
    boolean existsByPaymentId(String paymentId);
}