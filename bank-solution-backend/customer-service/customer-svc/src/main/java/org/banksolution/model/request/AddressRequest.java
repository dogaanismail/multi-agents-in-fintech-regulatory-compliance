package org.banksolution.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "City can't be blank.")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters.")
    private String city;

    @NotBlank(message = "Country code can't be blank.")
    @Size(min = 2, max = 2, message = "Country code must be a 2-letter code.")
    private String countryCode;
}
