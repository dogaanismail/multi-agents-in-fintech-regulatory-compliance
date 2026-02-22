package org.banksolution.repository;

import org.banksolution.entity.SystemConfigEntity;
import org.banksolution.enums.ConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfigurationRepository extends JpaRepository<SystemConfigEntity, UUID> {

    Optional<SystemConfigEntity> findByConfigKey(String configKey);

    List<SystemConfigEntity> findByCategory(ConfigCategory category);

    boolean existsByConfigKey(String configKey);
}
