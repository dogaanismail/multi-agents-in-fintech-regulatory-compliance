package org.banksolution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.BeneficiaryFixtures.createBeneficiaryCoordinateEntity;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryEntity;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryUpdateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createLocalGbpCoordinateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createWalletEurCoordinateRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PayoutMethod;
import org.banksolution.exception.BeneficiaryCoordinateNotFoundException;
import org.banksolution.exception.BeneficiaryNotFoundException;
import org.banksolution.exception.BeneficiaryValidationException;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.request.BeneficiaryUpdateRequest;
import org.banksolution.model.response.BeneficiaryCoordinateResponse;
import org.banksolution.model.response.BeneficiaryResponse;
import org.banksolution.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String DELETED_REASON = "Closed the account";

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private CustomerVerificationService customerVerificationService;

    private BeneficiaryService beneficiaryService;

    @BeforeEach
    void createServiceWithFixedClock() {
        beneficiaryService = new BeneficiaryService(
                beneficiaryRepository,
                customerVerificationService,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCreateAnActiveBeneficiaryForAnExistingCustomer() {
        UUID customerId = UUID.randomUUID();
        BeneficiaryCreateRequest beneficiaryCreateRequest = createIndividualBeneficiaryCreateRequest(customerId);
        when(beneficiaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse beneficiaryResponse = beneficiaryService.createBeneficiary(beneficiaryCreateRequest);

        verify(customerVerificationService).verifyCustomerExists(customerId);
        ArgumentCaptor<BeneficiaryEntity> beneficiaryEntityCaptor = ArgumentCaptor.forClass(BeneficiaryEntity.class);
        verify(beneficiaryRepository).save(beneficiaryEntityCaptor.capture());
        assertThat(beneficiaryEntityCaptor.getValue().getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(beneficiaryResponse.getBeneficiaryStatus()).isEqualTo(BeneficiaryStatus.ACTIVE);
        assertThat(beneficiaryResponse.getCoordinates()).hasSize(1);
    }

    @Test
    void shouldNotSaveWhenTheCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        BeneficiaryCreateRequest beneficiaryCreateRequest = createIndividualBeneficiaryCreateRequest(customerId);
        doThrow(new CustomerNotFoundException(customerId)).when(customerVerificationService).verifyCustomerExists(customerId);

        assertThatThrownBy(() -> beneficiaryService.createBeneficiary(beneficiaryCreateRequest))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(beneficiaryRepository, never()).save(any());
    }


    @Test
    void shouldTreatADeletedBeneficiaryAsNotFound() {
        UUID beneficiaryId = UUID.randomUUID();
        when(beneficiaryRepository.findByIdAndBeneficiaryStatusNot(beneficiaryId, BeneficiaryStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiaryService.getBeneficiaryById(beneficiaryId))
                .isInstanceOf(BeneficiaryNotFoundException.class)
                .hasMessageContaining(beneficiaryId.toString());
    }

    @Test
    void shouldListOnlyTheCustomersNotDeletedBeneficiaries() {
        UUID customerId = UUID.randomUUID();
        BeneficiaryEntity beneficiaryEntity = createIndividualBeneficiaryEntity(customerId);
        when(beneficiaryRepository.findAllByCustomerIdAndBeneficiaryStatusNotOrderByCreatedAtDesc(customerId, BeneficiaryStatus.DELETED))
                .thenReturn(List.of(beneficiaryEntity));

        List<BeneficiaryResponse> beneficiaryResponses = beneficiaryService.getBeneficiariesByCustomerId(customerId);

        assertThat(beneficiaryResponses).extracting(BeneficiaryResponse::getId).containsExactly(beneficiaryEntity.getId());
    }

    @Test
    void shouldUpdateNamesAndStatusAndStampUpdatedAt() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        BeneficiaryUpdateRequest beneficiaryUpdateRequest = createIndividualBeneficiaryUpdateRequest(BeneficiaryStatus.INACTIVE);
        when(beneficiaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse beneficiaryResponse = beneficiaryService.updateBeneficiary(beneficiaryEntity.getId(), beneficiaryUpdateRequest);

        assertThat(beneficiaryResponse.getFirstName()).isEqualTo(beneficiaryUpdateRequest.getFirstName());
        assertThat(beneficiaryResponse.getBeneficiaryStatus()).isEqualTo(BeneficiaryStatus.INACTIVE);
        assertThat(beneficiaryResponse.getUpdatedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void shouldRefuseToDeleteThroughAnUpdate() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        BeneficiaryUpdateRequest beneficiaryUpdateRequest = createIndividualBeneficiaryUpdateRequest(BeneficiaryStatus.DELETED);
        UUID beneficiaryId = beneficiaryEntity.getId();

        assertThatThrownBy(() -> beneficiaryService.updateBeneficiary(beneficiaryId, beneficiaryUpdateRequest))
                .isInstanceOf(BeneficiaryValidationException.class)
                .hasMessageContaining("use DELETE");

        verify(beneficiaryRepository, never()).save(any());
    }


    @Test
    void shouldSoftDeleteTheBeneficiaryAndEveryCoordinateWithTheGivenReason() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        beneficiaryEntity.addBeneficiaryCoordinate(createBeneficiaryCoordinateEntity(PayoutMethod.WALLET, Currency.EUR));

        beneficiaryService.deleteBeneficiary(beneficiaryEntity.getId(), DELETED_REASON);

        verify(beneficiaryRepository).save(beneficiaryEntity);
        assertThat(beneficiaryEntity.getBeneficiaryStatus()).isEqualTo(BeneficiaryStatus.DELETED);
        assertThat(beneficiaryEntity.getDeletedAt()).isEqualTo(FIXED_NOW);
        assertThat(beneficiaryEntity.getDeletedReason()).isEqualTo(DELETED_REASON);
        assertThat(beneficiaryEntity.getCoordinates())
                .allMatch(beneficiaryCoordinateEntity -> FIXED_NOW.equals(beneficiaryCoordinateEntity.getDeletedAt())
                        && DELETED_REASON.equals(beneficiaryCoordinateEntity.getDeletedReason()));
    }

    @Test
    void shouldAddACoordinateWithAFreshPayoutMethodAndCurrency() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        when(beneficiaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse beneficiaryResponse = beneficiaryService.addBeneficiaryCoordinate(
                beneficiaryEntity.getId(), createWalletEurCoordinateRequest());

        assertThat(beneficiaryResponse.getCoordinates())
                .extracting(BeneficiaryCoordinateResponse::getPayoutMethod)
                .containsExactly(PayoutMethod.LOCAL, PayoutMethod.WALLET);
        assertThat(beneficiaryEntity.getUpdatedAt()).isEqualTo(FIXED_NOW);
    }


    @Test
    void shouldAllowReAddingAPayoutMethodAndCurrencyWhoseCoordinateWasDeleted() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        beneficiaryEntity.getCoordinates().getFirst().setDeletedAt(FIXED_NOW);
        beneficiaryEntity.addBeneficiaryCoordinate(createBeneficiaryCoordinateEntity(PayoutMethod.WALLET, Currency.EUR));
        when(beneficiaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse beneficiaryResponse = beneficiaryService.addBeneficiaryCoordinate(
                beneficiaryEntity.getId(), createLocalGbpCoordinateRequest());

        assertThat(beneficiaryResponse.getCoordinates())
                .extracting(BeneficiaryCoordinateResponse::getPayoutMethod)
                .containsExactly(PayoutMethod.WALLET, PayoutMethod.LOCAL);
    }

    @Test
    void shouldSoftDeleteOneCoordinateWhenAnotherRemains() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        BeneficiaryCoordinateEntity walletCoordinateEntity = createBeneficiaryCoordinateEntity(PayoutMethod.WALLET, Currency.EUR);
        beneficiaryEntity.addBeneficiaryCoordinate(walletCoordinateEntity);

        beneficiaryService.deleteBeneficiaryCoordinate(beneficiaryEntity.getId(), walletCoordinateEntity.getId(), DELETED_REASON);

        assertThat(walletCoordinateEntity.getDeletedAt()).isEqualTo(FIXED_NOW);
        assertThat(walletCoordinateEntity.getDeletedReason()).isEqualTo(DELETED_REASON);
        assertThat(beneficiaryEntity.getCoordinates().getFirst().getDeletedAt()).isNull();
        verify(beneficiaryRepository).save(beneficiaryEntity);
    }

    @Test
    void shouldRefuseToDeleteTheLastActiveCoordinate() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        UUID beneficiaryId = beneficiaryEntity.getId();
        UUID lastBeneficiaryCoordinateId = beneficiaryEntity.getCoordinates().getFirst().getId();

        assertThatThrownBy(() -> beneficiaryService.deleteBeneficiaryCoordinate(beneficiaryId, lastBeneficiaryCoordinateId, DELETED_REASON))
                .isInstanceOf(BeneficiaryValidationException.class)
                .hasMessageContaining("at least one coordinate");

        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    void shouldReportAnUnknownOrAlreadyDeletedCoordinateAsNotFound() {
        BeneficiaryEntity beneficiaryEntity = givenStoredBeneficiary();
        UUID beneficiaryId = beneficiaryEntity.getId();
        UUID unknownBeneficiaryCoordinateId = UUID.randomUUID();

        assertThatThrownBy(() -> beneficiaryService.deleteBeneficiaryCoordinate(beneficiaryId, unknownBeneficiaryCoordinateId, DELETED_REASON))
                .isInstanceOf(BeneficiaryCoordinateNotFoundException.class)
                .hasMessageContaining(unknownBeneficiaryCoordinateId.toString());
    }

    private BeneficiaryEntity givenStoredBeneficiary() {
        BeneficiaryEntity beneficiaryEntity = createIndividualBeneficiaryEntity(UUID.randomUUID());
        when(beneficiaryRepository.findByIdAndBeneficiaryStatusNot(beneficiaryEntity.getId(), BeneficiaryStatus.DELETED))
                .thenReturn(Optional.of(beneficiaryEntity));

        return beneficiaryEntity;
    }
}
