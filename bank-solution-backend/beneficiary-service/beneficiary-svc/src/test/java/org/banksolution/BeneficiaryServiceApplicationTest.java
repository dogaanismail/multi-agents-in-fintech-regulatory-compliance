package org.banksolution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.banksolution.common.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

class BeneficiaryServiceApplicationTest extends BaseIntegrationTest {

    @Test
    void shouldReportHealthUpWithTheMigratedDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
