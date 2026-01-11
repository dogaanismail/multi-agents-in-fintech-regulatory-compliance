package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.RiskAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<@NonNull RiskAssessmentEntity, @NonNull UUID> {
}