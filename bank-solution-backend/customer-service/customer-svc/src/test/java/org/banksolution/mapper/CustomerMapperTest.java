package org.banksolution.mapper;

import org.banksolution.entity.CustomerAddress;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.entity.enums.CustomerStatus;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerCreateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerEntity;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerUpdateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createUniqueEmail;

class CustomerMapperTest {

    @Test
    void shouldCopyEveryFieldOfTheCreateRequestIntoANewEntity() {
        CustomerCreateRequest customerCreateRequest = createCustomerCreateRequest(createUniqueEmail());

        CustomerEntity customerEntity = CustomerMapper.toCustomerEntity(customerCreateRequest);

        assertThat(customerEntity.getId()).isNull();
        assertThat(customerEntity.getFirstName()).isEqualTo(customerCreateRequest.getFirstName());
        assertThat(customerEntity.getLastName()).isEqualTo(customerCreateRequest.getLastName());
        assertThat(customerEntity.getMiddleName()).isEqualTo(customerCreateRequest.getMiddleName());
        assertThat(customerEntity.getEmail()).isEqualTo(customerCreateRequest.getEmail());
        assertThat(customerEntity.getPhoneNumber()).isEqualTo(customerCreateRequest.getPhoneNumber());
        assertThat(customerEntity.getDateOfBirth()).isEqualTo(customerCreateRequest.getDateOfBirth());
        assertThat(customerEntity.getNationality()).isEqualTo(customerCreateRequest.getNationality());
        assertThat(customerEntity.getCustomerType()).isEqualTo(customerCreateRequest.getCustomerType());
        assertThat(customerEntity.getAddress().getCity()).isEqualTo(customerCreateRequest.getAddress().getCity());
        assertThat(customerEntity.getAddress().getCountryCode())
                .isEqualTo(customerCreateRequest.getAddress().getCountryCode());
    }

    @Test
    void shouldStartEveryNewCustomerActive() {
        CustomerCreateRequest customerCreateRequest = createCustomerCreateRequest(createUniqueEmail());

        CustomerEntity customerEntity = CustomerMapper.toCustomerEntity(customerCreateRequest);

        assertThat(customerEntity.getCustomerStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void shouldOverwriteEveryFieldOfTheExistingEntityOnUpdate() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(createUniqueEmail());

        CustomerMapper.updateCustomerEntity(customerEntity, customerUpdateRequest);

        assertThat(customerEntity.getFirstName()).isEqualTo(customerUpdateRequest.getFirstName());
        assertThat(customerEntity.getLastName()).isEqualTo(customerUpdateRequest.getLastName());
        assertThat(customerEntity.getMiddleName()).isEqualTo(customerUpdateRequest.getMiddleName());
        assertThat(customerEntity.getEmail()).isEqualTo(customerUpdateRequest.getEmail());
        assertThat(customerEntity.getPhoneNumber()).isEqualTo(customerUpdateRequest.getPhoneNumber());
        assertThat(customerEntity.getDateOfBirth()).isEqualTo(customerUpdateRequest.getDateOfBirth());
        assertThat(customerEntity.getNationality()).isEqualTo(customerUpdateRequest.getNationality());
        assertThat(customerEntity.getCustomerType()).isEqualTo(customerUpdateRequest.getCustomerType());
        assertThat(customerEntity.getCustomerStatus()).isEqualTo(customerUpdateRequest.getCustomerStatus());
    }

    @Test
    void shouldUpdateTheAddressInPlaceInsteadOfReplacingIt() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        CustomerAddress customerAddressBeforeUpdate = customerEntity.getAddress();
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(createUniqueEmail());

        CustomerMapper.updateCustomerEntity(customerEntity, customerUpdateRequest);

        assertThat(customerEntity.getAddress()).isSameAs(customerAddressBeforeUpdate);
        assertThat(customerEntity.getAddress().getCity()).isEqualTo(customerUpdateRequest.getAddress().getCity());
        assertThat(customerEntity.getAddress().getCountryCode())
                .isEqualTo(customerUpdateRequest.getAddress().getCountryCode());
    }

    @Test
    void shouldExposeEveryEntityFieldInTheResponse() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        customerEntity.setId(UUID.randomUUID());
        customerEntity.getAddress().setId(UUID.randomUUID());
        customerEntity.setCreatedAt(Instant.parse("2026-08-26T10:00:00Z"));
        customerEntity.setUpdatedAt(Instant.parse("2026-08-26T11:00:00Z"));

        CustomerResponse customerResponse = CustomerMapper.toCustomerResponse(customerEntity);

        assertThat(customerResponse.getId()).isEqualTo(customerEntity.getId());
        assertThat(customerResponse.getFirstName()).isEqualTo(customerEntity.getFirstName());
        assertThat(customerResponse.getLastName()).isEqualTo(customerEntity.getLastName());
        assertThat(customerResponse.getMiddleName()).isEqualTo(customerEntity.getMiddleName());
        assertThat(customerResponse.getEmail()).isEqualTo(customerEntity.getEmail());
        assertThat(customerResponse.getPhoneNumber()).isEqualTo(customerEntity.getPhoneNumber());
        assertThat(customerResponse.getDateOfBirth()).isEqualTo(customerEntity.getDateOfBirth());
        assertThat(customerResponse.getNationality()).isEqualTo(customerEntity.getNationality());
        assertThat(customerResponse.getCustomerType()).isEqualTo(customerEntity.getCustomerType());
        assertThat(customerResponse.getCustomerStatus()).isEqualTo(customerEntity.getCustomerStatus());
        assertThat(customerResponse.getAddress().getId()).isEqualTo(customerEntity.getAddress().getId());
        assertThat(customerResponse.getCreatedAt()).isEqualTo(customerEntity.getCreatedAt());
        assertThat(customerResponse.getUpdatedAt()).isEqualTo(customerEntity.getUpdatedAt());
    }
}
