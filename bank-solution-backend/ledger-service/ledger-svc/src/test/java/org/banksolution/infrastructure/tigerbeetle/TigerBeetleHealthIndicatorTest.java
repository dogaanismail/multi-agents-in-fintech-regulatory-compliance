package org.banksolution.infrastructure.tigerbeetle;

import org.banksolution.common.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TigerBeetleHealthIndicatorTest extends BaseIntegrationTest {

    @Autowired
    private TigerBeetleHealthIndicator tigerBeetleHealthIndicator;

    @Test
    void shouldReportUpWhenTheClusterAnswers() {
        assertThat(Objects.requireNonNull(tigerBeetleHealthIndicator.health())
                .getStatus())
                .isEqualTo(Status.UP);
    }

    @Test
    void shouldReportTheClusterItIsProbing() {
        assertThat(Objects.requireNonNull(tigerBeetleHealthIndicator.health()).getDetails())
                .containsKey("clusterId")
                .containsKey("addresses");
    }

    @Test
    void shouldExposeTigerBeetleThroughTheActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.tigerBeetle.status").value("UP"));
    }
}
