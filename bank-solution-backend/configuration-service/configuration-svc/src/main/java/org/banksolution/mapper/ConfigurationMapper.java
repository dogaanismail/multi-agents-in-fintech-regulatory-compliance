package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.SystemConfigEntity;
import org.banksolution.model.request.CreateConfigRequest;
import org.banksolution.model.response.ConfigurationResponse;

@UtilityClass
public class ConfigurationMapper {

    public static SystemConfigEntity toEntity(CreateConfigRequest request) {
        return SystemConfigEntity.builder()
                .configKey(request.getConfigKey().toUpperCase())
                .configValue(request.getConfigValue())
                .configType(request.getConfigType())
                .category(request.getCategory())
                .description(request.getDescription())
                .defaultValue(request.getDefaultValue())
                .build();
    }

    public static ConfigurationResponse toResponse(SystemConfigEntity entity) {
        return ConfigurationResponse.builder()
                .id(entity.getId())
                .configKey(entity.getConfigKey())
                .configValue(entity.getConfigValue())
                .configType(entity.getConfigType())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .defaultValue(entity.getDefaultValue())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
