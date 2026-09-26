package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "Customer address details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @Schema(description = "City of residence")
    @NotBlank(message = "City can't be blank.")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters.")
    private String city;

    @Schema(description = "ISO 3166-1 alpha-2 country code", example = "GB")
    @NotBlank(message = "Country code can't be blank.")
    @Size(min = 2, max = 2, message = "Country code must be a 2-letter code.")
    private String countryCode;
}
