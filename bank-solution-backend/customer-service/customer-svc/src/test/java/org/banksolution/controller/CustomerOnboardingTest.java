package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.servicesecurity.Permissions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.banksolution.fixtures.CustomerFixtures.createCustomerCreateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createUniqueEmail;
import static org.banksolution.servicesecurity.testing.ServiceSecurityTestFixtures.createCallerJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerOnboardingTest extends BaseIntegrationTest {

    private static final String IDENTITIES_URL = "/api/v1/customers/identities/";

    @Test
    void shouldOnboardACustomerForAnIdentityAndFindThemByIt() throws Exception {
        String identitySubject = UUID.randomUUID().toString();
        String email = createUniqueEmail();

        mockMvc.perform(post(IDENTITIES_URL + identitySubject)
                        .with(createCallerJwt("service-account-mobile-gateway", Permissions.CUSTOMER_ONBOARD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCreateRequest(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get(IDENTITIES_URL + identitySubject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldRefuseASecondCustomerForTheSameIdentity() throws Exception {
        String identitySubject = UUID.randomUUID().toString();
        mockMvc.perform(post(IDENTITIES_URL + identitySubject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCreateRequest(createUniqueEmail()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(IDENTITIES_URL + identitySubject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCreateRequest(createUniqueEmail()))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReportAnIdentityWithoutACustomerAsNotFound() throws Exception {
        mockMvc.perform(get(IDENTITIES_URL + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotLetAStaffCallerWhoCanCreateCustomersLinkAnIdentity() throws Exception {
        mockMvc.perform(post(IDENTITIES_URL + UUID.randomUUID())
                        .with(createCallerJwt("operator", Permissions.CUSTOMER_CREATE, Permissions.CUSTOMER_READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCreateRequest(createUniqueEmail()))))
                .andExpect(status().isForbidden());
    }
}
