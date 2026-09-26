package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.banksolution.entity.enums.CustomerType;

import java.time.LocalDate;

@Schema(description = "Details for registering a new customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreateRequest {

    @Schema(description = "Customer's first name")
    @NotBlank(message = "First name can't be blank.")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters.")
    private String firstName;

    @Schema(description = "Customer's last name")
    @NotBlank(message = "Last name can't be blank.")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters.")
    private String lastName;

    @Schema(description = "Customer's middle name")
    @Size(max = 100, message = "Middle name must not exceed 100 characters.")
    private String middleName;

    @Schema(description = "Customer's email address", example = "alice@example.com")
    @NotBlank(message = "Email can't be blank.")
    @Email(message = "Please enter valid e-mail address")
    @Size(min = 7, max = 255, message = "Email must be between 7 and 255 characters.")
    private String email;

    @Schema(description = "Phone number in E.164 format", example = "+447911123456")
    @NotBlank(message = "Phone number can't be blank.")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please enter a valid phone number.")
    private String phoneNumber;

    @Schema(description = "Customer's date of birth", example = "1990-04-15")
    @NotNull(message = "Date of birth can't be null.")
    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    @Schema(description = "ISO 3166-1 alpha-2 nationality code", example = "GB")
    @NotBlank(message = "Nationality can't be blank.")
    @Size(min = 2, max = 2, message = "Nationality must be a 2-letter country code.")
    private String nationality;

    @Schema(description = "Kind of customer", example = "INDIVIDUAL")
    @NotNull(message = "Customer type can't be null.")
    private CustomerType customerType;

    @Schema(description = "Customer's address")
    @Valid
    @NotNull(message = "Address can't be null.")
    private AddressRequest address;
}
