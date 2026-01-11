package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.AccountEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<@NonNull AccountEntity, @NonNull UUID> {

    boolean existsByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = {"balances"})
    @Query("SELECT a FROM account a WHERE a.customerId = :customerId AND a.deletedAt IS NULL")
    List<AccountEntity> findByCustomerId(@Param("customerId") UUID customerId);

    @EntityGraph(attributePaths = {"balances"})
    List<AccountEntity> findAllByIdIn(List<UUID> ids);

    @Override
    @EntityGraph(attributePaths = {"balances"})
    @NonNull
    Optional<@NonNull AccountEntity> findById(@NonNull UUID id);

}


