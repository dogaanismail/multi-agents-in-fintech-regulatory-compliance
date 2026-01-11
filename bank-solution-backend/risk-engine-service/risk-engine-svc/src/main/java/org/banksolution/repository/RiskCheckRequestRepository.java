package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RiskCheckRequestRepository extends JpaRepository<@NonNull RiskCheckRequestEntity, @NonNull UUID> {
    boolean existsByPaymentId(String paymentId);
}
