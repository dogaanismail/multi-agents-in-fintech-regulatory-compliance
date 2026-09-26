package org.banksolution.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.BeneficiaryCoordinateEntity;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.exception.BeneficiaryCoordinateNotFoundException;
import org.banksolution.exception.BeneficiaryNotFoundException;
import org.banksolution.exception.BeneficiaryValidationException;
import org.banksolution.mapper.BeneficiaryCoordinateMapper;
import org.banksolution.mapper.BeneficiaryMapper;
import org.banksolution.model.request.BeneficiaryCoordinateRequest;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.request.BeneficiaryUpdateRequest;
import org.banksolution.model.response.BeneficiaryResponse;
import org.banksolution.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    public static final String DEFAULT_DELETED_REASON = "Deleted by customer";

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerVerificationService customerVerificationService;
    private final Clock clock;

    @Transactional
    public BeneficiaryResponse createBeneficiary(BeneficiaryCreateRequest beneficiaryCreateRequest) {
        log.info("Creating {} beneficiary for customer: {}",
                beneficiaryCreateRequest.getType(),
                beneficiaryCreateRequest.getCustomerId());

        customerVerificationService.verifyCustomerExists(beneficiaryCreateRequest.getCustomerId());

        BeneficiaryEntity beneficiaryEntity = BeneficiaryMapper.toBeneficiaryEntity(
                beneficiaryCreateRequest,
                clock.instant());
        BeneficiaryEntity savedBeneficiaryEntity = beneficiaryRepository.save(beneficiaryEntity);

        log.info("Beneficiary created successfully with id: {}", savedBeneficiaryEntity.getId());
        return BeneficiaryMapper.toBeneficiaryResponse(savedBeneficiaryEntity);
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getBeneficiaryById(UUID beneficiaryId) {
        log.info("Fetching beneficiary with id: {}", beneficiaryId);
        return BeneficiaryMapper.toBeneficiaryResponse(findNotDeletedBeneficiary(beneficiaryId));
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiariesByCustomerId(UUID customerId) {
        log.info("Fetching beneficiaries for customer: {}", customerId);
        return beneficiaryRepository
                .findAllByCustomerIdAndBeneficiaryStatusNotOrderByCreatedAtDesc(customerId, BeneficiaryStatus.DELETED)
                .stream()
                .map(BeneficiaryMapper::toBeneficiaryResponse)
                .toList();
    }

    @Transactional
    public BeneficiaryResponse updateBeneficiary(
            UUID beneficiaryId,
            BeneficiaryUpdateRequest beneficiaryUpdateRequest) {
        log.info("Updating beneficiary with id: {}", beneficiaryId);

        BeneficiaryEntity beneficiaryEntity = findNotDeletedBeneficiary(beneficiaryId);
        if (beneficiaryUpdateRequest.getBeneficiaryStatus() == BeneficiaryStatus.DELETED) {
            throw new BeneficiaryValidationException(List.of("use DELETE to delete a beneficiary"));
        }

        BeneficiaryMapper.updateBeneficiaryEntity(beneficiaryEntity, beneficiaryUpdateRequest, clock.instant());
        BeneficiaryEntity updatedBeneficiaryEntity = beneficiaryRepository.save(beneficiaryEntity);

        log.info("Beneficiary updated successfully with id: {}", beneficiaryId);
        return BeneficiaryMapper.toBeneficiaryResponse(updatedBeneficiaryEntity);
    }

    @Transactional
    public void deleteBeneficiary(
            UUID beneficiaryId,
            String deletedReason) {
        log.info("Soft deleting beneficiary with id: {}", beneficiaryId);

        BeneficiaryEntity beneficiaryEntity = findNotDeletedBeneficiary(beneficiaryId);
        Instant deletedAt = clock.instant();

        beneficiaryEntity.setBeneficiaryStatus(BeneficiaryStatus.DELETED);
        beneficiaryEntity.setDeletedAt(deletedAt);
        beneficiaryEntity.setDeletedReason(deletedReason);
        beneficiaryEntity.setUpdatedAt(deletedAt);
        findNotDeletedCoordinates(beneficiaryEntity).forEach(beneficiaryCoordinateEntity ->
                softDeleteBeneficiaryCoordinate(beneficiaryCoordinateEntity, deletedReason, deletedAt));
        beneficiaryRepository.save(beneficiaryEntity);

        log.info("Beneficiary soft deleted successfully with id: {}", beneficiaryId);
    }

    @Transactional
    public BeneficiaryResponse addBeneficiaryCoordinate(
            UUID beneficiaryId,
            BeneficiaryCoordinateRequest beneficiaryCoordinateRequest) {

        log.info("Adding {}/{} coordinate to beneficiary: {}",
                beneficiaryCoordinateRequest.getPayoutMethod(),
                beneficiaryCoordinateRequest.getCurrency(),
                beneficiaryId);

        BeneficiaryEntity beneficiaryEntity = findNotDeletedBeneficiary(beneficiaryId);
        Instant createdAt = clock.instant();
        beneficiaryEntity.addBeneficiaryCoordinate(
                BeneficiaryCoordinateMapper.toBeneficiaryCoordinateEntity(beneficiaryCoordinateRequest, createdAt));
        beneficiaryEntity.setUpdatedAt(createdAt);
        BeneficiaryEntity updatedBeneficiaryEntity = beneficiaryRepository.save(beneficiaryEntity);

        return BeneficiaryMapper.toBeneficiaryResponse(updatedBeneficiaryEntity);
    }

    @Transactional
    public void deleteBeneficiaryCoordinate(UUID beneficiaryId, UUID beneficiaryCoordinateId, String deletedReason) {
        log.info("Soft deleting coordinate {} of beneficiary: {}", beneficiaryCoordinateId, beneficiaryId);

        BeneficiaryEntity beneficiaryEntity = findNotDeletedBeneficiary(beneficiaryId);
        List<BeneficiaryCoordinateEntity> notDeletedBeneficiaryCoordinates = findNotDeletedCoordinates(beneficiaryEntity);
        BeneficiaryCoordinateEntity beneficiaryCoordinateEntity = notDeletedBeneficiaryCoordinates.stream()
                .filter(notDeletedBeneficiaryCoordinate -> notDeletedBeneficiaryCoordinate.getId().equals(beneficiaryCoordinateId))
                .findFirst()
                .orElseThrow(() -> new BeneficiaryCoordinateNotFoundException(beneficiaryId, beneficiaryCoordinateId));

        if (notDeletedBeneficiaryCoordinates.size() == 1) {
            throw new BeneficiaryValidationException(List.of(
                    "a beneficiary needs at least one coordinate: delete the beneficiary instead"));
        }

        Instant deletedAt = clock.instant();
        softDeleteBeneficiaryCoordinate(beneficiaryCoordinateEntity, deletedReason, deletedAt);

        beneficiaryEntity.setUpdatedAt(deletedAt);
        beneficiaryRepository.save(beneficiaryEntity);
    }

    private BeneficiaryEntity findNotDeletedBeneficiary(UUID beneficiaryId) {
        return beneficiaryRepository.findByIdAndBeneficiaryStatusNot(beneficiaryId, BeneficiaryStatus.DELETED)
                .orElseThrow(() -> new BeneficiaryNotFoundException(beneficiaryId));
    }

    private static List<BeneficiaryCoordinateEntity> findNotDeletedCoordinates(BeneficiaryEntity beneficiaryEntity) {
        return beneficiaryEntity.getCoordinates().stream()
                .filter(beneficiaryCoordinateEntity -> beneficiaryCoordinateEntity.getDeletedAt() == null)
                .toList();
    }

    private static void softDeleteBeneficiaryCoordinate(
            BeneficiaryCoordinateEntity beneficiaryCoordinateEntity,
            String deletedReason,
            Instant deletedAt) {

        beneficiaryCoordinateEntity.setDeletedAt(deletedAt);
        beneficiaryCoordinateEntity.setDeletedReason(deletedReason);
        beneficiaryCoordinateEntity.setUpdatedAt(deletedAt);
    }
}
