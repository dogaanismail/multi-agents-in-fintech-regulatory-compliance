package org.banksolution.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.ConfigType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConfigRequest {

    @NotBlank(message = "Config value can't be blank.")
    private String configValue;

    @NotNull(message = "Config type can't be null.")
    private ConfigType configType;

    private String description;
}
