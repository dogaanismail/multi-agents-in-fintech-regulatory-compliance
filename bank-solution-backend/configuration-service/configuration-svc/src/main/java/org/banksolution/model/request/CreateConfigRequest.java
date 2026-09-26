package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.ConfigCategory;
import org.banksolution.enums.ConfigType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "New hot-reloadable system configuration entry")
public class CreateConfigRequest {

    @Schema(description = "Unique configuration key", example = "TRAINING_INTERVAL_SECONDS")
    @NotBlank(message = "Config key can't be blank.")
    private String configKey;

    @Schema(description = "Current value, encoded as text", example = "300")
    @NotBlank(message = "Config value can't be blank.")
    private String configValue;

    @Schema(description = "Value type used to parse the value", example = "INTEGER")
    @NotNull(message = "Config type can't be null.")
    private ConfigType configType;

    @Schema(description = "Functional area the setting belongs to", example = "OFFLINE_RETRAINING")
    @NotNull(message = "Category can't be null.")
    private ConfigCategory category;

    @Schema(description = "Human-readable purpose of the setting")
    private String description;

    @Schema(description = "Fallback value, encoded as text", example = "300")
    @NotBlank(message = "Default value can't be blank.")
    private String defaultValue;
}
