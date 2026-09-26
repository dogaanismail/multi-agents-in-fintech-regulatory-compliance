package org.banksolution.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.banksolution.common.initializers.WireMockInitializer.CUSTOMER_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.BeneficiaryFixtures.createCompanyBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createCustomerResponse;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryCreateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createIndividualBeneficiaryUpdateRequest;
import static org.banksolution.fixtures.BeneficiaryFixtures.createWalletEurCoordinateRequest;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.response.BeneficiaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class BeneficiaryControllerTest extends BaseIntegrationTest {

    private static final String BENEFICIARIES_PATH = "/api/v1/beneficiaries";
    private static final String BENEFICIARY_PATH = "/api/v1/beneficiaries/{beneficiaryId}";
    private static final String COORDINATES_PATH = "/api/v1/beneficiaries/{beneficiaryId}/coordinates";

    @Test
    void shouldCreateAndReadBackABeneficiaryForAnExistingCustomer() throws Exception {
        UUID customerId = givenExistingCustomer();

        BeneficiaryResponse beneficiaryResponse = createBeneficiary(createCompanyBeneficiaryCreateRequest(customerId));

        mockMvc.perform(MockMvcRequestBuilders.get(BENEFICIARY_PATH, beneficiaryResponse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.type").value("COMPANY"))
                .andExpect(jsonPath("$.beneficiaryStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.coordinates", hasSize(2)));
    }

    @Test
    void shouldAnswer422WhenTheCustomerDoesNotExist() throws Exception {
        UUID customerId = UUID.randomUUID();
        stubFor(get(urlEqualTo(CUSTOMER_SERVICE_BASE_PATH + "/" + customerId)).willReturn(aResponse().withStatus(404)));

        mockMvc.perform(post(BENEFICIARIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createIndividualBeneficiaryCreateRequest(customerId))))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldAnswer503WhenCustomerServiceFails() throws Exception {
        UUID customerId = UUID.randomUUID();
        stubFor(get(urlEqualTo(CUSTOMER_SERVICE_BASE_PATH + "/" + customerId)).willReturn(aResponse().withStatus(500)));

        mockMvc.perform(post(BENEFICIARIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createIndividualBeneficiaryCreateRequest(customerId))))
                .andExpect(status().isServiceUnavailable());
    }


    @Test
    void shouldAnswer400WhenNoCoordinateIsGiven() throws Exception {
        BeneficiaryCreateRequest beneficiaryCreateRequest = createIndividualBeneficiaryCreateRequest(UUID.randomUUID());
        beneficiaryCreateRequest.setCoordinates(List.of());

        mockMvc.perform(post(BENEFICIARIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beneficiaryCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors[0].field").value("coordinates"))
                .andExpect(jsonPath("$.subErrors[0].message").value("At least one coordinate must be provided"));
    }

    @Test
    void shouldAnswer400ForAnUnknownEnumValueInsteadOf404() throws Exception {
        mockMvc.perform(post(BENEFICIARIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ALIEN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListOnlyTheCustomersBeneficiaries() throws Exception {
        UUID customerId = givenExistingCustomer();
        createBeneficiary(createIndividualBeneficiaryCreateRequest(customerId));
        createBeneficiary(createCompanyBeneficiaryCreateRequest(customerId));

        mockMvc.perform(MockMvcRequestBuilders.get(BENEFICIARIES_PATH).param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldUpdateABeneficiaryAndRefuseDeletingItThroughAnUpdate() throws Exception {
        UUID customerId = givenExistingCustomer();
        BeneficiaryResponse beneficiaryResponse = createBeneficiary(createIndividualBeneficiaryCreateRequest(customerId));

        mockMvc.perform(put(BENEFICIARY_PATH, beneficiaryResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createIndividualBeneficiaryUpdateRequest(BeneficiaryStatus.INACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Janet"))
                .andExpect(jsonPath("$.beneficiaryStatus").value("INACTIVE"));

        mockMvc.perform(put(BENEFICIARY_PATH, beneficiaryResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createIndividualBeneficiaryUpdateRequest(BeneficiaryStatus.DELETED))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSoftDeleteABeneficiarySoItIsNoLongerFoundOrListed() throws Exception {
        UUID customerId = givenExistingCustomer();
        BeneficiaryResponse beneficiaryResponse = createBeneficiary(createIndividualBeneficiaryCreateRequest(customerId));

        mockMvc.perform(delete(BENEFICIARY_PATH, beneficiaryResponse.getId()).param("reason", "Customer request"))
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get(BENEFICIARY_PATH, beneficiaryResponse.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(MockMvcRequestBuilders.get(BENEFICIARIES_PATH).param("customerId", customerId.toString()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldAnswer400WithTheParameterForADeleteReasonLongerThan500Characters() throws Exception {
        UUID customerId = givenExistingCustomer();
        BeneficiaryResponse beneficiaryResponse = createBeneficiary(createIndividualBeneficiaryCreateRequest(customerId));

        mockMvc.perform(delete(BENEFICIARY_PATH, beneficiaryResponse.getId()).param("reason", "x".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors[0].field").value("reason"))
                .andExpect(jsonPath("$.subErrors[0].message").value("Reason must not exceed 500 characters."));

        mockMvc.perform(MockMvcRequestBuilders.get(BENEFICIARY_PATH, beneficiaryResponse.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAddACoordinateAndProtectTheLastOne() throws Exception {
        UUID customerId = givenExistingCustomer();
        BeneficiaryResponse beneficiaryResponse = createBeneficiary(createIndividualBeneficiaryCreateRequest(customerId));
        UUID localGbpCoordinateId = beneficiaryResponse.getCoordinates().getFirst().getId();

        mockMvc.perform(post(COORDINATES_PATH, beneficiaryResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletEurCoordinateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinates", hasSize(2)));

        mockMvc.perform(delete(COORDINATES_PATH + "/{beneficiaryCoordinateId}", beneficiaryResponse.getId(), localGbpCoordinateId))
                .andExpect(status().isNoContent());

        UUID walletCoordinateId = readBeneficiary(beneficiaryResponse.getId()).getCoordinates().getFirst().getId();
        mockMvc.perform(delete(COORDINATES_PATH + "/{beneficiaryCoordinateId}", beneficiaryResponse.getId(), walletCoordinateId))
                .andExpect(status().isBadRequest());
    }

    private UUID givenExistingCustomer() {
        UUID customerId = UUID.randomUUID();
        stubFor(get(urlEqualTo(CUSTOMER_SERVICE_BASE_PATH + "/" + customerId))
                .willReturn(okJson(objectMapper.writeValueAsString(createCustomerResponse(customerId)))));

        return customerId;
    }

    private BeneficiaryResponse createBeneficiary(BeneficiaryCreateRequest beneficiaryCreateRequest) throws Exception {
        String responseBody = mockMvc.perform(post(BENEFICIARIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beneficiaryCreateRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(responseBody, BeneficiaryResponse.class);
    }

    private BeneficiaryResponse readBeneficiary(UUID beneficiaryId) throws Exception {
        String responseBody = mockMvc.perform(MockMvcRequestBuilders.get(BENEFICIARY_PATH, beneficiaryId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(responseBody, BeneficiaryResponse.class);
    }
}
