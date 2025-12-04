package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<@NonNull CustomerEntity, @NonNull UUID> {

    boolean existsCustomerEntityByEmail(String email);
}
