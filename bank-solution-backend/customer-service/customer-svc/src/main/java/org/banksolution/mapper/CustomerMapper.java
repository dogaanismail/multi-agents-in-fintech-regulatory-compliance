package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;

@UtilityClass
public class CustomerMapper {

    public static CustomerEntity toEntity(CustomerCreateRequest request) {
        return CustomerEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .customerType(request.getCustomerType())
                .address(AddressMapper.toEntity(request.getAddress()))
                .build();
    }

    public static void updateEntity(CustomerEntity entity, CustomerUpdateRequest request) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setMiddleName(request.getMiddleName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setNationality(request.getNationality());
        entity.setCustomerType(request.getCustomerType());
        entity.setCustomerStatus(request.getCustomerStatus());

        AddressMapper.updateEntity(entity.getAddress(), request.getAddress());
    }

    public static CustomerResponse toResponse(CustomerEntity entity) {
        return CustomerResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .middleName(entity.getMiddleName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .customerType(entity.getCustomerType())
                .customerStatus(entity.getCustomerStatus())
                .address(AddressMapper.toResponse(entity.getAddress()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

