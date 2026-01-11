package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.AgentObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentObservationRepository extends JpaRepository<@NonNull AgentObservationEntity, @NonNull UUID> {

    List<AgentObservationEntity> findByMarlAssessmentId(UUID marlAssessmentId);
}
