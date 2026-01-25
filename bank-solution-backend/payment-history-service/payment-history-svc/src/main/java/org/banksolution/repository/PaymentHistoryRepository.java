package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.PaymentHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<@NonNull PaymentHistoryEntity, @NonNull UUID> {

    Page<@NonNull PaymentHistoryEntity> findByCustomerId(UUID customerId, Pageable pageable);

    Page<@NonNull PaymentHistoryEntity> findByStatus(String status, Pageable pageable);

    @Query("SELECT ph FROM PaymentHistoryEntity ph WHERE ph.createdAt BETWEEN :startDate AND :endDate")
    Page<@NonNull PaymentHistoryEntity> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT ph FROM PaymentHistoryEntity ph WHERE ph.customerId = :customerId AND ph.createdAt BETWEEN :startDate AND :endDate")
    Page<@NonNull PaymentHistoryEntity> findByCustomerIdAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT ph FROM PaymentHistoryEntity ph WHERE ph.fraudStatus = :fraudStatus")
    Page<@NonNull PaymentHistoryEntity> findByFraudStatus(@Param("fraudStatus") String fraudStatus, Pageable pageable);

    @Query("SELECT ph FROM PaymentHistoryEntity ph WHERE ph.riskLevel = :riskLevel")
    Page<@NonNull PaymentHistoryEntity> findByRiskLevel(@Param("riskLevel") String riskLevel, Pageable pageable);
}
