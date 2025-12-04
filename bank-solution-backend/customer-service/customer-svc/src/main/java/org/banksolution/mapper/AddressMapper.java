package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.CustomerAddress;
import org.banksolution.model.request.AddressRequest;
import org.banksolution.model.response.AddressResponse;

@UtilityClass
public class AddressMapper {

    public static CustomerAddress toEntity(AddressRequest request) {
        return CustomerAddress.builder()
                .city(request.getCity())
                .countryCode(request.getCountryCode())
                .build();
    }

    public static void updateEntity(CustomerAddress entity, AddressRequest request) {
        entity.setCity(request.getCity());
        entity.setCountryCode(request.getCountryCode());
    }

    public static AddressResponse toResponse(CustomerAddress entity) {
        return AddressResponse.builder()
                .id(entity.getId())
                .city(entity.getCity())
                .countryCode(entity.getCountryCode())
                .build();
    }
}

