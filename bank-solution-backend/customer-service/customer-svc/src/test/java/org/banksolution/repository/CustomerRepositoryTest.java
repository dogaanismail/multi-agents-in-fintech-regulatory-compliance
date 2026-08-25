package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.entity.enums.CustomerStatus;
import org.banksolution.entity.enums.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerEntity;
import static org.banksolution.fixtures.CustomerFixtures.createUniqueEmail;

class CustomerRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());

        UUID savedCustomerId = customerRepository.saveAndFlush(customerEntity).getId();
        CustomerEntity reloadedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();

        assertThat(reloadedCustomerEntity.getFirstName()).isEqualTo(customerEntity.getFirstName());
        assertThat(reloadedCustomerEntity.getLastName()).isEqualTo(customerEntity.getLastName());
        assertThat(reloadedCustomerEntity.getMiddleName()).isEqualTo(customerEntity.getMiddleName());
        assertThat(reloadedCustomerEntity.getEmail()).isEqualTo(customerEntity.getEmail());
        assertThat(reloadedCustomerEntity.getPhoneNumber()).isEqualTo(customerEntity.getPhoneNumber());
        assertThat(reloadedCustomerEntity.getDateOfBirth()).isEqualTo(customerEntity.getDateOfBirth());
        assertThat(reloadedCustomerEntity.getNationality()).isEqualTo(customerEntity.getNationality());
        assertThat(reloadedCustomerEntity.getCustomerType()).isEqualTo(customerEntity.getCustomerType());
        assertThat(reloadedCustomerEntity.getCustomerStatus()).isEqualTo(customerEntity.getCustomerStatus());
        assertThat(reloadedCustomerEntity.getCreatedAt()).isNotNull();
        assertThat(reloadedCustomerEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldCascadeTheAddressWhenTheCustomerIsSaved() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());

        UUID savedCustomerId = customerRepository.saveAndFlush(customerEntity).getId();
        CustomerEntity reloadedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();

        assertThat(reloadedCustomerEntity.getAddress().getId()).isNotNull();
        assertThat(reloadedCustomerEntity.getAddress().getCity()).isEqualTo("Tallinn");
        assertThat(reloadedCustomerEntity.getAddress().getCountryCode()).isEqualTo("EE");
    }

    @Test
    void shouldRejectASecondCustomerWithTheSameEmail() {
        String email = createUniqueEmail();
        customerRepository.saveAndFlush(createCustomerEntity(email));

        CustomerEntity duplicateCustomerEntity = createCustomerEntity(email);

        assertThatThrownBy(() -> customerRepository.saveAndFlush(duplicateCustomerEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAcceptEveryCustomerTypeAndStatusAgainstTheCheckConstraints() {
        for (CustomerType customerType : CustomerType.values()) {
            for (CustomerStatus customerStatus : CustomerStatus.values()) {
                CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
                customerEntity.setCustomerType(customerType);
                customerEntity.setCustomerStatus(customerStatus);

                UUID savedCustomerId = customerRepository.saveAndFlush(customerEntity).getId();
                CustomerEntity reloadedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();

                assertThat(reloadedCustomerEntity.getCustomerType()).isEqualTo(customerType);
                assertThat(reloadedCustomerEntity.getCustomerStatus()).isEqualTo(customerStatus);
            }
        }
    }

    @Test
    void shouldBumpTheOptimisticLockVersionWhenTheCustomerIsUpdated() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        UUID savedCustomerId = customerRepository.saveAndFlush(customerEntity).getId();

        CustomerEntity persistedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();
        short versionBeforeUpdate = persistedCustomerEntity.getVersion();
        persistedCustomerEntity.setPhoneNumber("+37255500000");
        customerRepository.saveAndFlush(persistedCustomerEntity);

        CustomerEntity updatedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();
        assertThat(updatedCustomerEntity.getVersion()).isEqualTo((short) (versionBeforeUpdate + 1));
        assertThat(updatedCustomerEntity.getUpdatedAt()).isAfterOrEqualTo(updatedCustomerEntity.getCreatedAt());
    }

    @Test
    void shouldPersistTheSoftDeletionTimestampAndReason() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        UUID savedCustomerId = customerRepository.saveAndFlush(customerEntity).getId();

        CustomerEntity persistedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();
        persistedCustomerEntity.setDeletedAt(Instant.now());
        persistedCustomerEntity.setDeletedReason("Soft deleted by user");
        customerRepository.saveAndFlush(persistedCustomerEntity);

        CustomerEntity softDeletedCustomerEntity = customerRepository.findById(savedCustomerId).orElseThrow();
        assertThat(softDeletedCustomerEntity.getDeletedAt()).isNotNull();
        assertThat(softDeletedCustomerEntity.getDeletedReason()).isEqualTo("Soft deleted by user");
    }

    @Test
    void shouldOnlyReportExistingEmails() {
        String email = createUniqueEmail();
        customerRepository.saveAndFlush(createCustomerEntity(email));

        assertThat(customerRepository.existsCustomerEntityByEmail(email)).isTrue();
        assertThat(customerRepository.existsCustomerEntityByEmail(createUniqueEmail())).isFalse();
    }
}
