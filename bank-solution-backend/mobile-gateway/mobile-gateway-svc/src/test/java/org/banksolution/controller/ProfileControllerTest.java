package org.banksolution.controller;

import org.banksolution.common.BaseMobileGatewayTest;
import org.junit.jupiter.api.Test;

import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_EMAIL;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCustomerJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerTest extends BaseMobileGatewayTest {

    private static final String PROFILE = "/api/v1/me/profile";

    @Test
    void shouldRejectARequestWithoutACustomerToken() throws Exception {
        mockMvc.perform(get(PROFILE)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAskALoginWithoutACustomerRecordToOnboardFirst() throws Exception {
        stubCallerNotOnboarded();

        mockMvc.perform(get(PROFILE).with(createCustomerJwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Complete onboarding before using banking features"));
    }

    @Test
    void shouldDescribeTheCustomerLinkedToTheLogin() throws Exception {
        stubOnboardedCaller();

        mockMvc.perform(get(PROFILE).with(createCustomerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CALLER_CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.email").value(CALLER_EMAIL));
    }
}
