package org.banksolution.servicesecurity.config;

import org.banksolution.servicesecurity.Permissions;
import org.banksolution.servicesecurity.probe.ServiceSecurityTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.banksolution.servicesecurity.testing.ServiceSecurityTestFixtures.createCallerJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ServiceSecurityTestApplication.class)
@AutoConfigureMockMvc
class ServiceSecurityAutoConfigurationTest {

    private static final String PROBE = "/api/v1/probe";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectARequestWithoutABearerToken() throws Exception {
        mockMvc.perform(get(PROBE)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectACallerWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(post(PROBE).with(createCallerJwt("viewer", Permissions.PAYMENT_READ)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldServeACallerHoldingTheRequiredPermissionAndExposeTheirUsername() throws Exception {
        mockMvc.perform(get(PROBE).with(createCallerJwt("officer", Permissions.PAYMENT_READ)))
                .andExpect(status().isOk())
                .andExpect(content().string("officer"));
    }

    @Test
    void shouldAcceptStateChangingCallsWithoutACsrfTokenBecauseTheApiIsStateless() throws Exception {
        mockMvc.perform(post(PROBE).with(createCallerJwt("officer", Permissions.PAYMENT_REVIEW)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLeaveTheErrorPageReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/error")).andExpect(status().is5xxServerError());
    }
}
