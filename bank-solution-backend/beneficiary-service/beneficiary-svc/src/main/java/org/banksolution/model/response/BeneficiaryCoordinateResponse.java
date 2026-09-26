package org.banksolution.model.response;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PayoutMethod;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payout details for one method and currency")
public class BeneficiaryCoordinateResponse {

    @Schema(description = "Coordinate identifier", example = "c7e2a9d4-1f6b-4c3e-8a5d-2b9f0e6c1a73")
    private UUID id;

    @Schema(description = "Payout method", example = "LOCAL")
    private PayoutMethod payoutMethod;

    @Schema(description = "Payout currency (ISO 4217)", example = "GBP")
    private Currency currency;

    @Schema(description = "Bank account number")
    private String accountNumber;

    @Schema(description = "UK sort code")
    private String sortCode;

    @Schema(description = "IBAN")
    private String iban;

    @Schema(description = "BIC / SWIFT code")
    private String bic;

    @Schema(description = "Bank name")
    private String bankName;

    @Schema(description = "Bank country (ISO 3166 alpha-2)", example = "GB")
    private String bankCountry;

    @Schema(description = "Beneficiary address")
    private String address;

    @Schema(description = "Beneficiary city")
    private String city;

    @Schema(description = "Beneficiary country (ISO 3166 alpha-2)", example = "GB")
    private String countryCode;

    @Schema(description = "Beneficiary postcode")
    private String postcode;

    @Schema(description = "Beneficiary state or region")
    private String state;

    @Schema(description = "Payout provider reference")
    private String externalId;

    @Schema(description = "Wallet provider, for wallet payouts")
    private String walletProvider;

    @Schema(description = "Contact number, for wallet payouts")
    private String contactNumber;

    @Schema(description = "When the coordinate was created", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "When the coordinate was last updated", example = "2026-09-27T10:15:30Z")
    private Instant updatedAt;

}
