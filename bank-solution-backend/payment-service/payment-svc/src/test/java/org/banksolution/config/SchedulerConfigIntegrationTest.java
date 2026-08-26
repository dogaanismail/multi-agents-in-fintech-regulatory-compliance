package org.banksolution.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule;
import org.banksolution.common.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerConfigIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RecurringTaskWithPersistentSchedule<SchedulerConfig.ScheduleAndRateData> currencyRatesRefreshTask;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * A restarted instance must adopt the schedule already persisted by the previous one
     * instead of scheduling a second refresh task. A fresh SchedulerConfig stands in for
     * the restart: calling the bean method on the proxied configuration would only return
     * the cached scheduler.
     */
    @Test
    void shouldReuseThePersistedRefreshScheduleOnASecondStartup() {
        SchedulerConfig restartedSchedulerConfig = new SchedulerConfig();

        Scheduler restartedScheduler = restartedSchedulerConfig.scheduler(dataSource, currencyRatesRefreshTask);
        try {
            Integer scheduledRefreshTasks = jdbcTemplate.queryForObject(
                    "select count(*) from scheduled_tasks where task_name = 'currency-rates-refresh'", Integer.class);

            assertThat(scheduledRefreshTasks).isEqualTo(1);
            assertThat(restartedScheduler.getSchedulerState().isStarted()).isTrue();
        } finally {
            restartedSchedulerConfig.stopScheduler();
        }

        assertThat(restartedScheduler.getSchedulerState().isShuttingDown()).isTrue();
    }
}
