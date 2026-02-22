package org.banksolution.model.response;

import lombok.*;
import org.banksolution.enums.ConfigCategory;
import org.banksolution.enums.ConfigType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationResponse {

    private UUID id;
    private String configKey;
    private String configValue;
    private ConfigType configType;
    private ConfigCategory category;
    private String description;
    private String defaultValue;
    private Instant createdAt;
    private Instant updatedAt;
}
