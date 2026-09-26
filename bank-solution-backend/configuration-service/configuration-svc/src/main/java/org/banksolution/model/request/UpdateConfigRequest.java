package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.ConfigType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "New value for an existing configuration")
public class UpdateConfigRequest {

    @Schema(description = "New value, encoded as text", example = "300")
    @NotBlank(message = "Config value can't be blank.")
    private String configValue;

    @Schema(description = "Value type used to parse the value", example = "INTEGER")
    @NotNull(message = "Config type can't be null.")
    private ConfigType configType;

    @Schema(description = "Human-readable purpose of the setting")
    private String description;
}
