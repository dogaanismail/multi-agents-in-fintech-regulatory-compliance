package org.banksolution.repository;

import org.banksolution.entity.ConfigAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfigAuditLogRepository extends JpaRepository<ConfigAuditLogEntity, UUID> {

    List<ConfigAuditLogEntity> findByConfigKeyOrderByCreatedAtAsc(String configKey);

}
