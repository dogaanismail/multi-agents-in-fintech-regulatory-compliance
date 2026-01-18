package org.banksolution.repository;

import org.banksolution.entity.CustomerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, UUID> {

    Optional<CustomerProfileEntity> findByCustomerId(UUID customerId);

    Optional<CustomerProfileEntity> findByAccountId(UUID accountId);
}