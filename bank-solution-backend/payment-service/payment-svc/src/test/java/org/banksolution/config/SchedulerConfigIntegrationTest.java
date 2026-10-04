package org.banksolution.config;

import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.banksolution.common.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.common.initializers.WireMockInitializer.EXCHANGE_RATE_API_BASE_PATH;
import static org.banksolution.outbox.scheduling.OutboxSchedulerTasks.OUTBOX_RELAY_TASK_NAME;

class SchedulerConfigIntegrationTest extends BaseIntegrationTest {

    private static final TaskInstanceId CURRENCY_RATES_REFRESH_TASK_INSTANCE_ID =
            TaskInstanceId.of(SchedulerConfig.CURRENCY_RATES_REFRESH_TASK_NAME, RecurringTask.INSTANCE);

    @Autowired
    private SchedulerClient schedulerClient;

    @Value("${app.exchange-rate.scheduler.retry-delay}")
    private Duration currencyRatesRefreshRetryDelay;

    @Test
    void shouldScheduleTheCurrencyRatesRefreshNextToTheOutboxRelayOnOneScheduler() {
        assertThat(schedulerClient.getScheduledExecution(CURRENCY_RATES_REFRESH_TASK_INSTANCE_ID)).isPresent();
        assertThat(schedulerClient.getScheduledExecution(TaskInstanceId.of(OUTBOX_RELAY_TASK_NAME, RecurringTask.INSTANCE)))
                .isPresent();
    }

    @Test
    void shouldRetryAFailedRateSyncAfterTheRetryDelayInsteadOfTheFullInterval() {
        stubFor(get(urlPathMatching(EXCHANGE_RATE_API_BASE_PATH + "/.*")).willReturn(aResponse().withStatus(503)));
        Instant rescheduledAt = Instant.now();

        schedulerClient.reschedule(CURRENCY_RATES_REFRESH_TASK_INSTANCE_ID, rescheduledAt);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ScheduledExecution<Object> currencyRatesRefreshExecution = findCurrencyRatesRefreshExecution();
            assertThat(currencyRatesRefreshExecution.getLastFailure()).isAfterOrEqualTo(rescheduledAt);
            assertThat(currencyRatesRefreshExecution.getExecutionTime())
                    .isBetween(rescheduledAt.plus(currencyRatesRefreshRetryDelay), Instant.now().plus(currencyRatesRefreshRetryDelay));
        });
    }

    private ScheduledExecution<Object> findCurrencyRatesRefreshExecution() {
        return schedulerClient.getScheduledExecution(CURRENCY_RATES_REFRESH_TASK_INSTANCE_ID).orElseThrow();
    }
}
