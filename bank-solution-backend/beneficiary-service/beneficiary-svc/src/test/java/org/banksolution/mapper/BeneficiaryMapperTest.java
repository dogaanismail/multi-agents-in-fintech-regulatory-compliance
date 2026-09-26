package org.banksolution.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.BeneficiaryFixtures.CREATED_AT;
import static org.banksolution.fixtures.BeneficiaryFixtures.createCompanyBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryEntity;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryUpdateRequest;

import java.time.Instant;
import java.util.UUID;

import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.PayoutMethod;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.request.BeneficiaryUpdateRequest;
import org.banksolution.model.response.BeneficiaryCoordinateResponse;
import org.banksolution.model.response.BeneficiaryResponse;
import org.junit.jupiter.api.Test;

class BeneficiaryMapperTest {

    @Test
    void shouldMapTheCreateRequestToAnActiveBeneficiaryStampedWithTheGivenInstant() {
        BeneficiaryCreateRequest beneficiaryCreateRequest = createCompanyBeneficiaryCreateRequest(UUID.randomUUID());

        BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(beneficiaryCreateRequest, CREATED_AT);

        assertThat(beneficiaryEntity)
                .usingRecursiveComparison()
                .comparingOnlyFields("customerId", "alias", "companyName", "firstName", "lastName")
                .isEqualTo(beneficiaryCreateRequest);
        assertThat(beneficiaryEntity.getBeneficiaryType()).isEqualTo(beneficiaryCreateRequest.getType());
        assertThat(beneficiaryEntity.getBeneficiaryStatus()).isEqualTo(BeneficiaryStatus.ACTIVE);
        assertThat(beneficiaryEntity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(beneficiaryEntity.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldAttachEveryRequestedCoordinateBackToItsBeneficiary() {
        BeneficiaryCreateRequest beneficiaryCreateRequest = createCompanyBeneficiaryCreateRequest(UUID.randomUUID());

        BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(beneficiaryCreateRequest, CREATED_AT);

        assertThat(beneficiaryEntity.getCoordinates())
                .extracting(BeneficiaryCoordinateEntity::getPayoutMethod)
                .containsExactly(PayoutMethod.LOCAL, PayoutMethod.SWIFT);
        assertThat(beneficiaryEntity.getCoordinates())
                .allMatch(beneficiaryCoordinateEntity -> beneficiaryCoordinateEntity.getBeneficiary() == beneficiaryEntity);
    }

    @Test
    void shouldOverwriteNamesAliasStatusAndUpdatedAtButKeepTheType() {
        BeneficiaryEntity beneficiaryEntity = createIndividualBeneficiaryEntity(UUID.randomUUID());
        BeneficiaryUpdateRequest beneficiaryUpdateRequest = createIndividualBeneficiaryUpdateRequest(BeneficiaryStatus.INACTIVE);
        Instant updatedAt = CREATED_AT.plusSeconds(60);

        BeneficiaryMapper.updateBeneficiaryEntity(beneficiaryEntity, beneficiaryUpdateRequest, updatedAt);

        assertThat(beneficiaryEntity)
                .usingRecursiveComparison()
                .comparingOnlyFields("alias", "companyName", "firstName", "lastName", "beneficiaryStatus")
                .isEqualTo(beneficiaryUpdateRequest);
        assertThat(beneficiaryEntity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(beneficiaryEntity.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldMapTheEntityToAResponseWithItsCoordinates() {
        BeneficiaryEntity beneficiaryEntity = createIndividualBeneficiaryEntity(UUID.randomUUID());

        BeneficiaryResponse beneficiaryResponse = BeneficiaryMapper.toBeneficiaryResponse(beneficiaryEntity);

        assertThat(beneficiaryResponse)
                .usingRecursiveComparison()
                .ignoringFields("coordinates", "type")
                .isEqualTo(beneficiaryEntity);
        assertThat(beneficiaryResponse.getType()).isEqualTo(beneficiaryEntity.getBeneficiaryType());
        assertThat(beneficiaryResponse.getCoordinates())
                .extracting(BeneficiaryCoordinateResponse::getId)
                .containsExactly(beneficiaryEntity.getCoordinates().getFirst().getId());
    }

    @Test
    void shouldLeaveSoftDeletedCoordinatesOutOfTheResponse() {
        BeneficiaryEntity beneficiaryEntity = createIndividualBeneficiaryEntity(UUID.randomUUID());
        BeneficiaryCreateRequest beneficiaryCreateRequest = createIndividualBeneficiaryCreateRequest(beneficiaryEntity.getCustomerId());
        BeneficiaryEntity otherBeneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(beneficiaryCreateRequest, CREATED_AT);
        BeneficiaryCoordinateEntity deletedBeneficiaryCoordinateEntity = otherBeneficiaryEntity.getCoordinates().getFirst();
        deletedBeneficiaryCoordinateEntity.setDeletedAt(CREATED_AT);
        beneficiaryEntity.addBeneficiaryCoordinate(deletedBeneficiaryCoordinateEntity);

        BeneficiaryResponse beneficiaryResponse = BeneficiaryMapper.toBeneficiaryResponse(beneficiaryEntity);

        assertThat(beneficiaryEntity.getCoordinates()).hasSize(2);
        assertThat(beneficiaryResponse.getCoordinates()).hasSize(1);
    }
}
