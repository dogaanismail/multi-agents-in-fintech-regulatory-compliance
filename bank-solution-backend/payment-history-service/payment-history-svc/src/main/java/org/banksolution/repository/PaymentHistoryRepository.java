package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<@NonNull PaymentHistory, @NonNull UUID> {

    Optional<PaymentHistory> findByReferenceNumber(String referenceNumber);

    List<PaymentHistory> findByCustomerId(UUID customerId);

    List<PaymentHistory> findByStatus(String status);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentHistory> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.customerId = :customerId AND ph.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentHistory> findByCustomerIdAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.fraudStatus = :fraudStatus")
    List<PaymentHistory> findByFraudStatus(@Param("fraudStatus") String fraudStatus);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.riskLevel = :riskLevel")
    List<PaymentHistory> findByRiskLevel(@Param("riskLevel") String riskLevel);
}
