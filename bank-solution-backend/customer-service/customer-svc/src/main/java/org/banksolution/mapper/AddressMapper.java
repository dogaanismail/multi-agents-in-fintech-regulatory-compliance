package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.CustomerAddress;
import org.banksolution.model.request.AddressRequest;
import org.banksolution.model.response.AddressResponse;

@UtilityClass
public class AddressMapper {

    public static CustomerAddress toCustomerAddress(AddressRequest addressRequest) {
        return CustomerAddress.builder()
                .city(addressRequest.getCity())
                .countryCode(addressRequest.getCountryCode())
                .build();
    }

    public static void updateCustomerAddress(CustomerAddress customerAddress, AddressRequest addressRequest) {
        customerAddress.setCity(addressRequest.getCity());
        customerAddress.setCountryCode(addressRequest.getCountryCode());
    }

    public static AddressResponse toAddressResponse(CustomerAddress customerAddress) {
        return AddressResponse.builder()
                .id(customerAddress.getId())
                .city(customerAddress.getCity())
                .countryCode(customerAddress.getCountryCode())
                .build();
    }
}
