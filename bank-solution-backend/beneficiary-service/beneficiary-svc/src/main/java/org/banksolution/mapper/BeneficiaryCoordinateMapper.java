package org.banksolution.mapper;

import java.time.Instant;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.model.request.BeneficiaryCoordinateRequest;
import org.banksolution.model.response.BeneficiaryCoordinateResponse;

@UtilityClass
public class BeneficiaryCoordinateMapper {

    public static BeneficiaryCoordinateEntity toBeneficiaryCoordinateEntity(
            BeneficiaryCoordinateRequest beneficiaryCoordinateRequest,
            Instant createdAt) {

        return BeneficiaryCoordinateEntity.builder()
                .payoutMethod(beneficiaryCoordinateRequest.getPayoutMethod())
                .currency(beneficiaryCoordinateRequest.getCurrency())
                .accountNumber(beneficiaryCoordinateRequest.getAccountNumber())
                .sortCode(beneficiaryCoordinateRequest.getSortCode())
                .iban(beneficiaryCoordinateRequest.getIban())
                .bic(beneficiaryCoordinateRequest.getBic())
                .bankName(beneficiaryCoordinateRequest.getBankName())
                .bankCountry(beneficiaryCoordinateRequest.getBankCountry())
                .address(beneficiaryCoordinateRequest.getAddress())
                .city(beneficiaryCoordinateRequest.getCity())
                .countryCode(beneficiaryCoordinateRequest.getCountryCode())
                .postcode(beneficiaryCoordinateRequest.getPostcode())
                .state(beneficiaryCoordinateRequest.getState())
                .walletProvider(beneficiaryCoordinateRequest.getWalletProvider())
                .contactNumber(beneficiaryCoordinateRequest.getContactNumber())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public static BeneficiaryCoordinateResponse toBeneficiaryCoordinateResponse(
            BeneficiaryCoordinateEntity beneficiaryCoordinateEntity) {

        return BeneficiaryCoordinateResponse.builder()
                .id(beneficiaryCoordinateEntity.getId())
                .payoutMethod(beneficiaryCoordinateEntity.getPayoutMethod())
                .currency(beneficiaryCoordinateEntity.getCurrency())
                .accountNumber(beneficiaryCoordinateEntity.getAccountNumber())
                .sortCode(beneficiaryCoordinateEntity.getSortCode())
                .iban(beneficiaryCoordinateEntity.getIban())
                .bic(beneficiaryCoordinateEntity.getBic())
                .bankName(beneficiaryCoordinateEntity.getBankName())
                .bankCountry(beneficiaryCoordinateEntity.getBankCountry())
                .address(beneficiaryCoordinateEntity.getAddress())
                .city(beneficiaryCoordinateEntity.getCity())
                .countryCode(beneficiaryCoordinateEntity.getCountryCode())
                .postcode(beneficiaryCoordinateEntity.getPostcode())
                .state(beneficiaryCoordinateEntity.getState())
                .externalId(beneficiaryCoordinateEntity.getExternalId())
                .walletProvider(beneficiaryCoordinateEntity.getWalletProvider())
                .contactNumber(beneficiaryCoordinateEntity.getContactNumber())
                .createdAt(beneficiaryCoordinateEntity.getCreatedAt())
                .updatedAt(beneficiaryCoordinateEntity.getUpdatedAt())
                .build();
    }
}
