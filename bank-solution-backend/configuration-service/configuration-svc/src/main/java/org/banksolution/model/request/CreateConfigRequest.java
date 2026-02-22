package org.banksolution.model.request;

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
public class CreateConfigRequest {

    @NotBlank(message = "Config key can't be blank.")
    private String configKey;

    @NotBlank(message = "Config value can't be blank.")
    private String configValue;

    @NotNull(message = "Config type can't be null.")
    private ConfigType configType;

    @NotNull(message = "Category can't be null.")
    private ConfigCategory category;

    private String description;

    @NotBlank(message = "Default value can't be blank.")
    private String defaultValue;
}
