package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;

@UtilityClass
public class CustomerMapper {

    public static CustomerEntity toCustomerEntity(CustomerCreateRequest customerCreateRequest) {
        return CustomerEntity.builder()
                .firstName(customerCreateRequest.getFirstName())
                .lastName(customerCreateRequest.getLastName())
                .middleName(customerCreateRequest.getMiddleName())
                .email(customerCreateRequest.getEmail())
                .phoneNumber(customerCreateRequest.getPhoneNumber())
                .dateOfBirth(customerCreateRequest.getDateOfBirth())
                .nationality(customerCreateRequest.getNationality())
                .customerType(customerCreateRequest.getCustomerType())
                .address(AddressMapper.toCustomerAddress(customerCreateRequest.getAddress()))
                .build();
    }

    public static void updateCustomerEntity(CustomerEntity customerEntity, CustomerUpdateRequest customerUpdateRequest) {
        customerEntity.setFirstName(customerUpdateRequest.getFirstName());
        customerEntity.setLastName(customerUpdateRequest.getLastName());
        customerEntity.setMiddleName(customerUpdateRequest.getMiddleName());
        customerEntity.setEmail(customerUpdateRequest.getEmail());
        customerEntity.setPhoneNumber(customerUpdateRequest.getPhoneNumber());
        customerEntity.setDateOfBirth(customerUpdateRequest.getDateOfBirth());
        customerEntity.setNationality(customerUpdateRequest.getNationality());
        customerEntity.setCustomerType(customerUpdateRequest.getCustomerType());
        customerEntity.setCustomerStatus(customerUpdateRequest.getCustomerStatus());

        AddressMapper.updateCustomerAddress(customerEntity.getAddress(), customerUpdateRequest.getAddress());
    }

    public static CustomerResponse toCustomerResponse(CustomerEntity customerEntity) {
        return CustomerResponse.builder()
                .id(customerEntity.getId())
                .firstName(customerEntity.getFirstName())
                .lastName(customerEntity.getLastName())
                .middleName(customerEntity.getMiddleName())
                .email(customerEntity.getEmail())
                .phoneNumber(customerEntity.getPhoneNumber())
                .dateOfBirth(customerEntity.getDateOfBirth())
                .nationality(customerEntity.getNationality())
                .customerType(customerEntity.getCustomerType())
                .customerStatus(customerEntity.getCustomerStatus())
                .address(AddressMapper.toAddressResponse(customerEntity.getAddress()))
                .createdAt(customerEntity.getCreatedAt())
                .updatedAt(customerEntity.getUpdatedAt())
                .build();
    }
}
