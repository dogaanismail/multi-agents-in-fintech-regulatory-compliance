package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountBalanceRepository extends JpaRepository<@NonNull AccountBalanceEntity, @NonNull UUID> {

    @Query("SELECT ab FROM account_balance ab WHERE ab.account.id = :accountId AND ab.currency = :currency AND ab.deletedAt IS NULL")
    Optional<AccountBalanceEntity> findByAccountIdAndCurrency(
            @Param("accountId") UUID accountId,
            @Param("currency") Currency currency);

    @Query("SELECT ab FROM account_balance ab WHERE ab.account.id = :accountId AND ab.deletedAt IS NULL")
    List<AccountBalanceEntity> findByAccountId(@Param("accountId") UUID accountId);
}

