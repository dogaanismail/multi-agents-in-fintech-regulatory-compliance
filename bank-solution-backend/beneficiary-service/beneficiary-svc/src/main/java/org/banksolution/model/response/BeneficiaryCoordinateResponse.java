package org.banksolution.model.response;

import java.time.Instant;
import java.util.UUID;

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
public class BeneficiaryCoordinateResponse {

    private UUID id;
    private PayoutMethod payoutMethod;
    private Currency currency;
    private String accountNumber;
    private String sortCode;
    private String iban;
    private String bic;
    private String bankName;
    private String bankCountry;
    private String address;
    private String city;
    private String countryCode;
    private String postcode;
    private String state;
    private String externalId;
    private String walletProvider;
    private String contactNumber;
    private Instant createdAt;
    private Instant updatedAt;

}
