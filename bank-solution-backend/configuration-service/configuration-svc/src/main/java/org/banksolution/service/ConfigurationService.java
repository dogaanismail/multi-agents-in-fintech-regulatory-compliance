package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.CacheConfig;
import org.banksolution.entity.SystemConfigEntity;
import org.banksolution.enums.ConfigCategory;
import org.banksolution.exception.ConfigAlreadyExistsException;
import org.banksolution.exception.ConfigNotFoundException;
import org.banksolution.mapper.ConfigurationMapper;
import org.banksolution.model.request.CreateConfigRequest;
import org.banksolution.model.request.UpdateConfigRequest;
import org.banksolution.model.response.ConfigurationResponse;
import org.banksolution.repository.ConfigurationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CONFIGURATIONS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CONFIGURATIONS_BY_CATEGORY_CACHE, allEntries = true)
    })
    public ConfigurationResponse createConfiguration(CreateConfigRequest request) {
        log.info("Creating configuration with key: {}", request.getConfigKey());

        String normalizedKey = request.getConfigKey().toUpperCase();

        if (configurationRepository.existsByConfigKey(normalizedKey)) {
            throw new ConfigAlreadyExistsException(normalizedKey);
        }

        SystemConfigEntity entity = ConfigurationMapper.toEntity(request);
        SystemConfigEntity saved = configurationRepository.save(entity);

        log.info("Configuration created with id: {}", saved.getId());
        return ConfigurationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CONFIGURATIONS_CACHE)
    public List<ConfigurationResponse> getAllConfigurations() {
        log.info("Fetching all configurations");
        return configurationRepository.findAll()
                .stream()
                .map(ConfigurationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfigurationResponse getConfigurationById(UUID id) {
        log.info("Fetching configuration with id: {}", id);
        SystemConfigEntity entity = configurationRepository.findById(id)
                .orElseThrow(() -> new ConfigNotFoundException(id));
        return ConfigurationMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CONFIGURATION_BY_KEY_CACHE, key = "#key")
    public ConfigurationResponse getConfigurationByKey(String key) {
        log.info("Fetching configuration with key: {}", key);
        SystemConfigEntity entity = configurationRepository.findByConfigKey(key.toUpperCase())
                .orElseThrow(() -> new ConfigNotFoundException(key));
        return ConfigurationMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CONFIGURATIONS_BY_CATEGORY_CACHE, key = "#category")
    public List<ConfigurationResponse> getConfigurationsByCategory(ConfigCategory category) {
        log.info("Fetching configurations for category: {}", category);
        return configurationRepository.findByCategory(category)
                .stream()
                .map(ConfigurationMapper::toResponse)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CONFIGURATIONS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CONFIGURATION_BY_KEY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CONFIGURATIONS_BY_CATEGORY_CACHE, allEntries = true)
    })
    public ConfigurationResponse updateConfiguration(UUID id, UpdateConfigRequest request) {
        log.info("Updating configuration with id: {}", id);

        SystemConfigEntity entity = configurationRepository.findById(id)
                .orElseThrow(() -> new ConfigNotFoundException(id));

        entity.setConfigValue(request.getConfigValue());
        entity.setConfigType(request.getConfigType());
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        SystemConfigEntity saved = configurationRepository.save(entity);

        log.info("Configuration updated with id: {}", saved.getId());
        return ConfigurationMapper.toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CONFIGURATIONS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CONFIGURATION_BY_KEY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.CONFIGURATIONS_BY_CATEGORY_CACHE, allEntries = true)
    })
    public void deleteConfiguration(UUID id) {
        log.info("Deleting configuration with id: {}", id);

        if (!configurationRepository.existsById(id)) {
            throw new ConfigNotFoundException(id);
        }

        configurationRepository.deleteById(id);
        log.info("Configuration deleted with id: {}", id);
    }
}
