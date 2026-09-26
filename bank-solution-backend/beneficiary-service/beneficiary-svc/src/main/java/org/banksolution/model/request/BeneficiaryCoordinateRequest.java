package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Where the beneficiary is paid: one set of bank or wallet details for a payout method and currency")
public class BeneficiaryCoordinateRequest {

    @NotNull(message = "Payout method cannot be null")
    @Schema(description = "LOCAL, SWIFT or WALLET", example = "LOCAL")
    private PayoutMethod payoutMethod;

    @NotNull(message = "Currency cannot be null")
    @Schema(description = "ISO 4217 currency paid out on this coordinate", example = "GBP")
    private Currency currency;

    @Schema(description = "Bank account number", example = "31926819")
    private String accountNumber;

    @Schema(description = "UK sort code", example = "601613")
    private String sortCode;

    @Schema(description = "IBAN", example = "GB33BUKB20201555555555")
    private String iban;

    @Schema(description = "BIC / SWIFT code", example = "BUKBGB22")
    private String bic;

    @Schema(description = "Bank name", example = "Barclays")
    private String bankName;

    @Schema(description = "Bank country as a 2-letter ISO code", example = "GB")
    private String bankCountry;

    @Schema(description = "Beneficiary address", example = "1 Princes Street")
    private String address;

    @Schema(description = "Beneficiary city", example = "London")
    private String city;

    @Schema(description = "Beneficiary country as a 2-letter ISO code", example = "GB")
    private String countryCode;

    @Schema(description = "Beneficiary postcode", example = "EC2R 8BP")
    private String postcode;

    @Schema(description = "Beneficiary state or region", example = "Greater London")
    private String state;

    @Schema(description = "Wallet provider (WALLET only)", example = "REVOLUT")
    private String walletProvider;

    @Schema(description = "Contact number (WALLET only)", example = "447700900123")
    private String contactNumber;
}
