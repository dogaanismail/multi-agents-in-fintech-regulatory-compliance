package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Schema(description = "Customer address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    @Schema(description = "Address id", example = "5a9c2e7f-4d1b-4c8e-9f3a-6b0d2e8c1f47")
    private UUID id;

    @Schema(description = "City of residence")
    private String city;

    @Schema(description = "ISO 3166-1 alpha-2 country code", example = "GB")
    private String countryCode;
}

