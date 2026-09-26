package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Hot-reloadable system configuration entry")
public class ConfigurationResponse {

    @Schema(description = "Configuration identifier", example = "3c9a7b2e-1d4f-4a6b-8e5c-7f0d1a2b3c4d")
    private UUID id;

    @Schema(description = "Unique configuration key", example = "TRAINING_INTERVAL_SECONDS")
    private String configKey;

    @Schema(description = "Current value, encoded as text", example = "300")
    private String configValue;

    @Schema(description = "Value type used to parse the value", example = "INTEGER")
    private ConfigType configType;

    @Schema(description = "Functional area the setting belongs to", example = "OFFLINE_RETRAINING")
    private ConfigCategory category;

    @Schema(description = "Human-readable purpose of the setting")
    private String description;

    @Schema(description = "Fallback value, encoded as text", example = "300")
    private String defaultValue;

    @Schema(description = "When the entry was created", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "When the entry was last updated", example = "2026-09-27T10:15:30Z")
    private Instant updatedAt;

}
