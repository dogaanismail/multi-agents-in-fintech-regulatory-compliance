package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.MarlAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarlAssessmentRepository extends JpaRepository<@NonNull MarlAssessmentEntity, @NonNull UUID> {

    Optional<MarlAssessmentEntity> findByRiskCheckRequestId(UUID riskCheckRequestId);

    boolean existsByRiskCheckRequestId(UUID riskCheckRequestId);
}
