package org.banksolution.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.BeneficiaryFixtures.CREATED_AT;
import static org.banksolution.fixtures.BeneficiaryFixtures.createCompanyBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createWalletEurCoordinateRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.PayoutMethod;
import org.banksolution.mapper.BeneficiaryCoordinateMapper;
import org.banksolution.mapper.BeneficiaryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

class BeneficiaryRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void shouldRoundTripABeneficiaryWithACoordinateOfEveryPayoutMethod() {
        BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(
                createCompanyBeneficiaryCreateRequest(UUID.randomUUID()), CREATED_AT);
        beneficiaryEntity.addBeneficiaryCoordinate(
                BeneficiaryCoordinateMapper.toBeneficiaryCoordinateEntity(createWalletEurCoordinateRequest(), CREATED_AT));

        UUID beneficiaryId = beneficiaryRepository.save(beneficiaryEntity).getId();

        List<PayoutMethod> storedPayoutMethods = transactionTemplate.execute(transactionStatus ->
                beneficiaryRepository.findById(beneficiaryId).orElseThrow().getCoordinates().stream()
                        .map(BeneficiaryCoordinateEntity::getPayoutMethod)
                        .toList());
        assertThat(storedPayoutMethods).containsExactlyInAnyOrder(PayoutMethod.values());
    }

    @Test
    void shouldPersistEveryBeneficiaryStatusAllowedByTheSchema() {
        UUID customerId = UUID.randomUUID();

        List<BeneficiaryStatus> storedBeneficiaryStatuses = Arrays.stream(BeneficiaryStatus.values())
                .map(beneficiaryStatus -> {
                    BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(
                            createIndividualBeneficiaryCreateRequest(customerId), CREATED_AT);
                    beneficiaryEntity.setBeneficiaryStatus(beneficiaryStatus);
                    return beneficiaryRepository.save(beneficiaryEntity).getBeneficiaryStatus();
                })
                .toList();

        assertThat(storedBeneficiaryStatuses).containsExactly(BeneficiaryStatus.values());
    }

    @Test
    void shouldListACustomersNotDeletedBeneficiariesNewestFirst() {
        UUID customerId = UUID.randomUUID();
        UUID olderBeneficiaryId = saveBeneficiary(customerId, BeneficiaryStatus.ACTIVE, 0);
        UUID newerBeneficiaryId = saveBeneficiary(customerId, BeneficiaryStatus.INACTIVE, 60);
        saveBeneficiary(customerId, BeneficiaryStatus.DELETED, 120);
        saveBeneficiary(UUID.randomUUID(), BeneficiaryStatus.ACTIVE, 180);

        List<BeneficiaryEntity> beneficiaryEntities = beneficiaryRepository
                .findAllByCustomerIdAndBeneficiaryStatusNotOrderByCreatedAtDesc(customerId, BeneficiaryStatus.DELETED);

        assertThat(beneficiaryEntities).extracting(BeneficiaryEntity::getId).containsExactly(newerBeneficiaryId, olderBeneficiaryId);
    }

    @Test
    void shouldNotFindADeletedBeneficiaryById() {
        UUID deletedBeneficiaryId = saveBeneficiary(UUID.randomUUID(), BeneficiaryStatus.DELETED, 0);

        assertThat(beneficiaryRepository.findByIdAndBeneficiaryStatusNot(deletedBeneficiaryId, BeneficiaryStatus.DELETED)).isEmpty();
    }

    @Test
    void shouldBumpTheVersionOnEveryUpdate() {
        BeneficiaryEntity savedBeneficiaryEntity = beneficiaryRepository.save(BeneficiaryMapper.toBeneficiaryEntity(
                createIndividualBeneficiaryCreateRequest(UUID.randomUUID()), CREATED_AT));
        short versionAfterInsert = savedBeneficiaryEntity.getVersion();

        savedBeneficiaryEntity.setAlias("Renamed");
        BeneficiaryEntity updatedBeneficiaryEntity = beneficiaryRepository.save(savedBeneficiaryEntity);

        assertThat(updatedBeneficiaryEntity.getVersion()).isEqualTo((short) (versionAfterInsert + 1));
    }

    @Test
    void shouldRejectTwoCoordinatesWithTheSameExternalId() {
        String externalId = "nium-" + UUID.randomUUID();
        BeneficiaryEntity firstBeneficiaryEntity = createBeneficiaryWithExternalId(externalId);
        BeneficiaryEntity secondBeneficiaryEntity = createBeneficiaryWithExternalId(externalId);
        beneficiaryRepository.saveAndFlush(firstBeneficiaryEntity);

        assertThatThrownBy(() -> beneficiaryRepository.saveAndFlush(secondBeneficiaryEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID saveBeneficiary(UUID customerId, BeneficiaryStatus beneficiaryStatus, long secondsAfterCreatedAt) {
        BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(
                createIndividualBeneficiaryCreateRequest(customerId), CREATED_AT.plusSeconds(secondsAfterCreatedAt));
        beneficiaryEntity.setBeneficiaryStatus(beneficiaryStatus);
        return beneficiaryRepository.save(beneficiaryEntity).getId();
    }

    private static BeneficiaryEntity createBeneficiaryWithExternalId(String externalId) {
        BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(
                createIndividualBeneficiaryCreateRequest(UUID.randomUUID()), CREATED_AT);
        beneficiaryEntity.getCoordinates().getFirst().setExternalId(externalId);
        return beneficiaryEntity;
    }
}
