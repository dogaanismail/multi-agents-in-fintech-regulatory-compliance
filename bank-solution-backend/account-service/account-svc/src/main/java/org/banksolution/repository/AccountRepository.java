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

    @EntityGraph(attributePaths = {"wallets"})
    @Query("SELECT a FROM account a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<AccountEntity> findActiveById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"wallets"})
    @Query("SELECT a FROM account a WHERE a.id IN :ids AND a.deletedAt IS NULL")
    List<AccountEntity> findActiveByIdIn(@Param("ids") List<UUID> ids);

    @EntityGraph(attributePaths = {"wallets"})
    @Query("SELECT a FROM account a WHERE a.customerId = :customerId AND a.deletedAt IS NULL")
    List<AccountEntity> findActiveByCustomerId(@Param("customerId") UUID customerId);
}
