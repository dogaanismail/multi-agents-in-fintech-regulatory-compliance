package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Personal details a new customer provides; the email comes from the login")
public record OnboardingRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phoneNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Size(min = 2, max = 2) String nationality,
        @NotBlank String city,
        @NotBlank @Size(min = 2, max = 2) String countryCode) {
}
