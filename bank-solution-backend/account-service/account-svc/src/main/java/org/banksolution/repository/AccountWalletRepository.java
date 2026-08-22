package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountWalletRepository extends JpaRepository<@NonNull AccountWalletEntity, @NonNull UUID> {

    List<AccountWalletEntity> findByAccountId(UUID accountId);

    Optional<AccountWalletEntity> findByAccountIdAndCurrency(UUID accountId, Currency currency);
}
