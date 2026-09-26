package org.banksolution.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.BeneficiaryFixtures.CREATED_AT;
import static org.banksolution.fixtures.BeneficiaryFixtures.createLocalGbpCoordinateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createWalletEurCoordinateRequest;

import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.model.request.BeneficiaryCoordinateRequest;
import org.banksolution.model.response.BeneficiaryCoordinateResponse;
import org.junit.jupiter.api.Test;

class BeneficiaryCoordinateMapperTest {

    @Test
    void shouldCopyEveryBankDetailFromTheRequestIntoTheEntity() {
        BeneficiaryCoordinateRequest beneficiaryCoordinateRequest = createLocalGbpCoordinateRequest();

        BeneficiaryCoordinateEntity beneficiaryCoordinateEntity =
                BeneficiaryCoordinateMapper.toBeneficiaryCoordinateEntity(beneficiaryCoordinateRequest, CREATED_AT);

        assertThat(beneficiaryCoordinateEntity)
                .usingRecursiveComparison()
                .comparingOnlyFields("payoutMethod", "currency", "accountNumber", "sortCode", "iban", "bic", "bankName",
                        "bankCountry", "address", "city", "countryCode", "postcode", "state", "walletProvider", "contactNumber")
                .isEqualTo(beneficiaryCoordinateRequest);
    }

    @Test
    void shouldStampBothTimestampsWithTheGivenInstantAndLeaveIdAndExternalIdUnset() {
        BeneficiaryCoordinateEntity beneficiaryCoordinateEntity =
                BeneficiaryCoordinateMapper.toBeneficiaryCoordinateEntity(createWalletEurCoordinateRequest(), CREATED_AT);

        assertThat(beneficiaryCoordinateEntity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(beneficiaryCoordinateEntity.getUpdatedAt()).isEqualTo(CREATED_AT);
        assertThat(beneficiaryCoordinateEntity.getId()).isNull();
        assertThat(beneficiaryCoordinateEntity.getExternalId()).isNull();
        assertThat(beneficiaryCoordinateEntity.getDeletedAt()).isNull();
    }

    @Test
    void shouldExposeTheExternalIdAndTimestampsInTheResponse() {
        BeneficiaryCoordinateEntity beneficiaryCoordinateEntity =
                BeneficiaryCoordinateMapper.toBeneficiaryCoordinateEntity(createLocalGbpCoordinateRequest(), CREATED_AT);
        beneficiaryCoordinateEntity.setExternalId("nium-beneficiary-hash-id");

        BeneficiaryCoordinateResponse beneficiaryCoordinateResponse =
                BeneficiaryCoordinateMapper.toBeneficiaryCoordinateResponse(beneficiaryCoordinateEntity);

        assertThat(beneficiaryCoordinateResponse)
                .usingRecursiveComparison()
                .isEqualTo(beneficiaryCoordinateEntity);
    }
}
