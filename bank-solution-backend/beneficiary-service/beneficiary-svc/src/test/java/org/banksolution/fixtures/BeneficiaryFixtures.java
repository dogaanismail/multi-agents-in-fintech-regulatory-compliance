package org.banksolution.fixtures;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.BeneficiaryType;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PayoutMethod;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.banksolution.model.request.BeneficiaryCoordinateRequest;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.request.BeneficiaryUpdateRequest;

public final class BeneficiaryFixtures {

    public static final Instant CREATED_AT = Instant.parse("2026-09-27T09:00:00Z");

    private BeneficiaryFixtures() {
    }

    public static BeneficiaryCoordinateRequest createLocalGbpCoordinateRequest() {
        return BeneficiaryCoordinateRequest.builder()
                .payoutMethod(PayoutMethod.LOCAL)
                .currency(Currency.GBP)
                .accountNumber("31926819")
                .sortCode("601613")
                .bankName("NatWest")
                .bankCountry("GB")
                .address("1 Princes Street")
                .city("London")
                .countryCode("GB")
                .postcode("EC2R 8BP")
                .build();
    }

    public static BeneficiaryCoordinateRequest createLocalEurCoordinateRequest() {
        return BeneficiaryCoordinateRequest.builder()
                .payoutMethod(PayoutMethod.LOCAL)
                .currency(Currency.EUR)
                .iban("DE89370400440532013000")
                .bankName("Commerzbank")
                .bankCountry("DE")
                .countryCode("DE")
                .build();
    }

    public static BeneficiaryCoordinateRequest createSwiftUsdCoordinateRequest() {
        return BeneficiaryCoordinateRequest.builder()
                .payoutMethod(PayoutMethod.SWIFT)
                .currency(Currency.USD)
                .accountNumber("12345678")
                .bic("CHASUS33XXX")
                .bankName("JPMorgan Chase")
                .bankCountry("US")
                .state("NY")
                .build();
    }

    public static BeneficiaryCoordinateRequest createWalletEurCoordinateRequest() {
        return BeneficiaryCoordinateRequest.builder()
                .payoutMethod(PayoutMethod.WALLET)
                .currency(Currency.EUR)
                .walletProvider("REVOLUT")
                .contactNumber("+37255512345")
                .build();
    }

    public static BeneficiaryCreateRequest createIndividualBeneficiaryCreateRequest(UUID customerId) {
        return BeneficiaryCreateRequest.builder()
                .customerId(customerId)
                .type(BeneficiaryType.INDIVIDUAL)
                .alias("Mum")
                .firstName("Jane")
                .lastName("Doe")
                .coordinates(List.of(createLocalGbpCoordinateRequest()))
                .build();
    }

    public static BeneficiaryCreateRequest createCompanyBeneficiaryCreateRequest(UUID customerId) {
        return BeneficiaryCreateRequest.builder()
                .customerId(customerId)
                .type(BeneficiaryType.COMPANY)
                .alias("Landlord")
                .companyName("Acme Property Ltd")
                .coordinates(List.of(createLocalEurCoordinateRequest(), createSwiftUsdCoordinateRequest()))
                .build();
    }

    public static BeneficiaryUpdateRequest createIndividualBeneficiaryUpdateRequest(BeneficiaryStatus beneficiaryStatus) {
        return BeneficiaryUpdateRequest.builder()
                .alias("Mother")
                .firstName("Janet")
                .lastName("Doe-Smith")
                .beneficiaryStatus(beneficiaryStatus)
                .build();
    }

    public static BeneficiaryEntity createIndividualBeneficiaryEntity(UUID customerId) {
        BeneficiaryEntity beneficiaryEntity = BeneficiaryEntity.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .beneficiaryType(BeneficiaryType.INDIVIDUAL)
                .alias("Mum")
                .firstName("Jane")
                .lastName("Doe")
                .beneficiaryStatus(BeneficiaryStatus.ACTIVE)
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
        beneficiaryEntity.addBeneficiaryCoordinate(createBeneficiaryCoordinateEntity(PayoutMethod.LOCAL, Currency.GBP));
        return beneficiaryEntity;
    }

    public static BeneficiaryCoordinateEntity createBeneficiaryCoordinateEntity(PayoutMethod payoutMethod, Currency currency) {
        return BeneficiaryCoordinateEntity.builder()
                .id(UUID.randomUUID())
                .payoutMethod(payoutMethod)
                .currency(currency)
                .accountNumber("31926819")
                .sortCode("601613")
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }

    public static CustomerResponse createCustomerResponse(UUID customerId) {
        return CustomerResponse.builder()
                .id(customerId)
                .customerStatus("ACTIVE")
                .build();
    }
}
