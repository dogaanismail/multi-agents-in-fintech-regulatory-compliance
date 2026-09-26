package org.banksolution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PayoutMethod;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "beneficiary_coordinate")
@Table(name = "beneficiary_coordinate")
public class BeneficiaryCoordinateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false, updatable = false)
    private BeneficiaryEntity beneficiary;

    @Column(name = "payout_method", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PayoutMethod payoutMethod;

    @Column(name = "currency", nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "account_number", length = 8)
    private String accountNumber;

    @Column(name = "sort_code", length = 6)
    private String sortCode;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "bic", length = 11)
    private String bic;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_country", length = 2)
    private String bankCountry;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "postcode", length = 12)
    private String postcode;

    @Column(name = "state")
    private String state;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "wallet_provider", length = 20)
    private String walletProvider;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;
}
