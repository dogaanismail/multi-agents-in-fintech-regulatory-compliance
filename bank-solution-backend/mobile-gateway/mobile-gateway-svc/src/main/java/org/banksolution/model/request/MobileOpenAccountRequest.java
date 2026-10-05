package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Account the signed-in customer opens for themselves")
public record MobileOpenAccountRequest(
        @NotBlank String accountType,
        @NotBlank String bankLocation,
        @NotEmpty List<String> currencies) {
}
