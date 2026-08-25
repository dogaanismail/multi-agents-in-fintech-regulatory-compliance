package org.banksolution.mapper;

import org.banksolution.entity.CustomerAddress;
import org.banksolution.model.request.AddressRequest;
import org.banksolution.model.response.AddressResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.CustomerFixtures.createAddressRequest;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerAddress;

class AddressMapperTest {

    @Test
    void shouldCopyCityAndCountryCodeIntoANewAddress() {
        AddressRequest addressRequest = createAddressRequest();

        CustomerAddress customerAddress = AddressMapper.toCustomerAddress(addressRequest);

        assertThat(customerAddress.getId()).isNull();
        assertThat(customerAddress.getCity()).isEqualTo(addressRequest.getCity());
        assertThat(customerAddress.getCountryCode()).isEqualTo(addressRequest.getCountryCode());
    }

    @Test
    void shouldOverwriteCityAndCountryCodeOnTheExistingAddress() {
        CustomerAddress customerAddress = createCustomerAddress();
        AddressRequest addressRequest = AddressRequest.builder()
                .city("Helsinki")
                .countryCode("FI")
                .build();

        AddressMapper.updateCustomerAddress(customerAddress, addressRequest);

        assertThat(customerAddress.getCity()).isEqualTo("Helsinki");
        assertThat(customerAddress.getCountryCode()).isEqualTo("FI");
    }

    @Test
    void shouldExposeTheAddressWithItsIdInTheResponse() {
        CustomerAddress customerAddress = createCustomerAddress();
        customerAddress.setId(UUID.randomUUID());

        AddressResponse addressResponse = AddressMapper.toAddressResponse(customerAddress);

        assertThat(addressResponse.getId()).isEqualTo(customerAddress.getId());
        assertThat(addressResponse.getCity()).isEqualTo(customerAddress.getCity());
        assertThat(addressResponse.getCountryCode()).isEqualTo(customerAddress.getCountryCode());
    }
}
