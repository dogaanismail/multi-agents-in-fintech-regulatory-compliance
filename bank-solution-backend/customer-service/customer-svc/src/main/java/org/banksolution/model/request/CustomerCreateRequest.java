package org.banksolution.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.banksolution.entity.enums.CustomerType;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreateRequest {

    @NotBlank(message = "First name can't be blank.")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters.")
    private String firstName;

    @NotBlank(message = "Last name can't be blank.")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters.")
    private String lastName;

    @Size(max = 100, message = "Middle name must not exceed 100 characters.")
    private String middleName;

    @NotBlank(message = "Email can't be blank.")
    @Email(message = "Please enter valid e-mail address")
    @Size(min = 7, max = 255, message = "Email must be between 7 and 255 characters.")
    private String email;

    @NotBlank(message = "Phone number can't be blank.")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please enter a valid phone number.")
    private String phoneNumber;

    @NotNull(message = "Date of birth can't be null.")
    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Nationality can't be blank.")
    @Size(min = 2, max = 2, message = "Nationality must be a 2-letter country code.")
    private String nationality;

    @NotNull(message = "Customer type can't be null.")
    private CustomerType customerType;

    @Valid
    @NotNull(message = "Address can't be null.")
    private AddressRequest address;
}
