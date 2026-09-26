package org.banksolution.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import org.banksolution.entity.BeneficiaryEntity;
import org.banksolution.enums.BeneficiaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficiaryRepository extends JpaRepository<@NonNull BeneficiaryEntity, @NonNull UUID> {

    Optional<BeneficiaryEntity> findByIdAndBeneficiaryStatusNot(
            UUID beneficiaryId,
            BeneficiaryStatus excludedBeneficiaryStatus);

    List<BeneficiaryEntity> findAllByCustomerIdAndBeneficiaryStatusNotOrderByCreatedAtDesc(
            UUID customerId,
            BeneficiaryStatus excludedBeneficiaryStatus);
}
