package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;
import org.banksolution.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerCreateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerUpdateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createUniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest extends BaseIntegrationTest {

    private static final String CUSTOMERS_URL = "/api/v1/customers";

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldCreateACustomerAndReturnItWithGeneratedIds() throws Exception {
        String email = createUniqueEmail();
        CustomerCreateRequest customerCreateRequest = createCustomerCreateRequest(email);

        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value(customerCreateRequest.getFirstName()))
                .andExpect(jsonPath("$.customerType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.customerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.address.id").isNotEmpty())
                .andExpect(jsonPath("$.address.city").value("Tallinn"));

        assertThat(customerRepository.existsCustomerEntityByEmail(email)).isTrue();
    }

    @Test
    void shouldRejectACreateForAnEmailThatAlreadyExists() throws Exception {
        String email = createUniqueEmail();
        givenPersistedCustomer(email);

        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCreateRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer already exists with email: " + email));
    }

    @Test
    void shouldRejectACreateThatFailsBeanValidation() throws Exception {
        CustomerCreateRequest customerCreateRequest = createCustomerCreateRequest("not-an-email");
        customerCreateRequest.setFirstName(" ");

        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.subErrors").isNotEmpty());
    }

    @Test
    void shouldReturnTheCustomerById() throws Exception {
        String email = createUniqueEmail();
        CustomerResponse createdCustomerResponse = givenPersistedCustomer(email);

        mockMvc.perform(get(CUSTOMERS_URL + "/" + createdCustomerResponse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCustomerResponse.getId().toString()))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownCustomerId() throws Exception {
        UUID unknownCustomerId = UUID.randomUUID();

        mockMvc.perform(get(CUSTOMERS_URL + "/" + unknownCustomerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: " + unknownCustomerId));
    }

    @Test
    void shouldPageThroughCustomersWithMetadata() throws Exception {
        givenPersistedCustomer(createUniqueEmail());

        mockMvc.perform(get(CUSTOMERS_URL).param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.first").value(true))
                .andExpect(jsonPath("$.page.empty").value(false));
    }

    @Test
    void shouldUpdateTheCustomerIncludingItsAddress() throws Exception {
        CustomerResponse createdCustomerResponse = givenPersistedCustomer(createUniqueEmail());
        String newEmail = createUniqueEmail();
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(newEmail);

        mockMvc.perform(put(CUSTOMERS_URL + "/" + createdCustomerResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.firstName").value(customerUpdateRequest.getFirstName()))
                .andExpect(jsonPath("$.customerType").value("BUSINESS"))
                .andExpect(jsonPath("$.customerStatus").value("PASSIVE"))
                .andExpect(jsonPath("$.address.city").value("Helsinki"))
                .andExpect(jsonPath("$.address.countryCode").value("FI"));
    }

    @Test
    void shouldRejectAnUpdateToAnEmailOwnedByAnotherCustomer() throws Exception {
        String takenEmail = createUniqueEmail();
        givenPersistedCustomer(takenEmail);
        CustomerResponse createdCustomerResponse = givenPersistedCustomer(createUniqueEmail());

        mockMvc.perform(put(CUSTOMERS_URL + "/" + createdCustomerResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerUpdateRequest(takenEmail))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer already exists with email: " + takenEmail));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingAnUnknownCustomer() throws Exception {
        UUID unknownCustomerId = UUID.randomUUID();

        mockMvc.perform(put(CUSTOMERS_URL + "/" + unknownCustomerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerUpdateRequest(createUniqueEmail()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSoftDeleteTheCustomerAndPersistTheDeletionTimestamp() throws Exception {
        CustomerResponse createdCustomerResponse = givenPersistedCustomer(createUniqueEmail());

        mockMvc.perform(delete(CUSTOMERS_URL + "/" + createdCustomerResponse.getId()))
                .andExpect(status().isNoContent());

        CustomerEntity softDeletedCustomerEntity =
                customerRepository.findById(createdCustomerResponse.getId()).orElseThrow();
        assertThat(softDeletedCustomerEntity.getDeletedAt()).isNotNull();
        assertThat(softDeletedCustomerEntity.getDeletedReason()).isEqualTo("Soft deleted by user");
    }

    @Test
    void shouldReturnNotFoundWhenDeletingAnUnknownCustomer() throws Exception {
        mockMvc.perform(delete(CUSTOMERS_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private CustomerResponse givenPersistedCustomer(String email) throws Exception {
        MvcResult createCustomerResult = mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCreateRequest(email))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                createCustomerResult.getResponse().getContentAsString(), CustomerResponse.class);
    }
}
