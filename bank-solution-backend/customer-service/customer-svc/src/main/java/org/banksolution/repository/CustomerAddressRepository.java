package org.banksolution.repository;

import lombok.NonNull;
import org.banksolution.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<@NonNull CustomerAddress, @NonNull UUID> {
}
