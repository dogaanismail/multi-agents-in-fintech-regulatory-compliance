package org.banksolution.mapper;

import java.time.Instant;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.request.BeneficiaryUpdateRequest;
import org.banksolution.model.response.BeneficiaryResponse;

@UtilityClass
public class BeneficiaryMapper {

    public static BeneficiaryEntity toBeneficiaryEntity(BeneficiaryCreateRequest beneficiaryCreateRequest, Instant createdAt) {
        BeneficiaryEntity beneficiaryEntity = BeneficiaryEntity.builder()
                .customerId(beneficiaryCreateRequest.getCustomerId())
                .beneficiaryType(beneficiaryCreateRequest.getType())
                .alias(beneficiaryCreateRequest.getAlias())
                .companyName(beneficiaryCreateRequest.getCompanyName())
                .firstName(beneficiaryCreateRequest.getFirstName())
                .lastName(beneficiaryCreateRequest.getLastName())
                .beneficiaryStatus(BeneficiaryStatus.ACTIVE)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        beneficiaryCreateRequest.getCoordinates().forEach(beneficiaryCoordinateRequest ->
                beneficiaryEntity.addBeneficiaryCoordinate(
                        BeneficiaryCoordinateMapper.toBeneficiaryCoordinateEntity(beneficiaryCoordinateRequest, createdAt)));

        return beneficiaryEntity;
    }

    public static void updateBeneficiaryEntity(
            BeneficiaryEntity beneficiaryEntity,
            BeneficiaryUpdateRequest beneficiaryUpdateRequest,
            Instant updatedAt) {

        beneficiaryEntity.setAlias(beneficiaryUpdateRequest.getAlias());
        beneficiaryEntity.setCompanyName(beneficiaryUpdateRequest.getCompanyName());
        beneficiaryEntity.setFirstName(beneficiaryUpdateRequest.getFirstName());
        beneficiaryEntity.setLastName(beneficiaryUpdateRequest.getLastName());
        beneficiaryEntity.setBeneficiaryStatus(beneficiaryUpdateRequest.getBeneficiaryStatus());
        beneficiaryEntity.setUpdatedAt(updatedAt);
    }

    public static BeneficiaryResponse toBeneficiaryResponse(BeneficiaryEntity beneficiaryEntity) {
        return BeneficiaryResponse.builder()
                .id(beneficiaryEntity.getId())
                .customerId(beneficiaryEntity.getCustomerId())
                .type(beneficiaryEntity.getBeneficiaryType())
                .alias(beneficiaryEntity.getAlias())
                .companyName(beneficiaryEntity.getCompanyName())
                .firstName(beneficiaryEntity.getFirstName())
                .lastName(beneficiaryEntity.getLastName())
                .beneficiaryStatus(beneficiaryEntity.getBeneficiaryStatus())
                .coordinates(beneficiaryEntity.getCoordinates().stream()
                        .filter(beneficiaryCoordinateEntity -> beneficiaryCoordinateEntity.getDeletedAt() == null)
                        .map(BeneficiaryCoordinateMapper::toBeneficiaryCoordinateResponse)
                        .toList())
                .createdAt(beneficiaryEntity.getCreatedAt())
                .updatedAt(beneficiaryEntity.getUpdatedAt())
                .build();
    }
}
