package org.banksolution;

import org.banksolution.common.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentServiceApplicationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReportHealthUpWithTheMigratedDatabaseAndRunningBroker() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldScheduleTheRecurringRateRefreshOnStartup() {
        Integer scheduledRefreshTasks = jdbcTemplate.queryForObject(
                "select count(*) from scheduled_tasks where task_name = 'currency-rates-refresh' and task_instance = 'main'",
                Integer.class);

        assertThat(scheduledRefreshTasks).isEqualTo(1);
    }
}
