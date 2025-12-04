package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<@NonNull AccountEntity, @NonNull UUID> {

    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a FROM account a WHERE a.customerId = :customerId AND a.deletedAt IS NULL")
    List<AccountEntity> findByCustomerId(@Param("customerId") UUID customerId);

}


