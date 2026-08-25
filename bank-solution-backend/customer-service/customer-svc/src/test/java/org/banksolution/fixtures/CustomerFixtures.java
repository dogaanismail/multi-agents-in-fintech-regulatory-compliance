package org.banksolution.fixtures;

import org.banksolution.entity.CustomerAddress;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.entity.enums.CustomerStatus;
import org.banksolution.entity.enums.CustomerType;
import org.banksolution.model.request.AddressRequest;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;

import java.time.LocalDate;
import java.util.UUID;

public final class CustomerFixtures {

    private CustomerFixtures() {
    }

    public static String createUniqueEmail() {
        return "alice." + UUID.randomUUID() + "@example.com";
    }

    public static AddressRequest createAddressRequest() {
        return AddressRequest.builder()
                .city("Tallinn")
                .countryCode("EE")
                .build();
    }

    public static CustomerCreateRequest createCustomerCreateRequest(String email) {
        return CustomerCreateRequest.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .middleName("Marie")
                .email(email)
                .phoneNumber("+37255512345")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .nationality("EE")
                .customerType(CustomerType.INDIVIDUAL)
                .address(createAddressRequest())
                .build();
    }

    public static CustomerUpdateRequest createCustomerUpdateRequest(String email) {
        return CustomerUpdateRequest.builder()
                .firstName("Alicia")
                .lastName("Smith")
                .middleName("Anne")
                .email(email)
                .phoneNumber("+37255598765")
                .dateOfBirth(LocalDate.of(1991, 6, 16))
                .nationality("FI")
                .customerType(CustomerType.BUSINESS)
                .customerStatus(CustomerStatus.PASSIVE)
                .address(AddressRequest.builder()
                        .city("Helsinki")
                        .countryCode("FI")
                        .build())
                .build();
    }

    public static CustomerAddress createCustomerAddress() {
        return CustomerAddress.builder()
                .city("Tallinn")
                .countryCode("EE")
                .build();
    }

    public static CustomerEntity createCustomerEntity(String email) {
        return CustomerEntity.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .middleName("Marie")
                .email(email)
                .phoneNumber("+37255512345")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .nationality("EE")
                .customerType(CustomerType.INDIVIDUAL)
                .customerStatus(CustomerStatus.ACTIVE)
                .address(createCustomerAddress())
                .build();
    }
}
