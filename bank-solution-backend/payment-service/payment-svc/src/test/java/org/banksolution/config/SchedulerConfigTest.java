package org.banksolution.config;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.banksolution.service.CurrencyRateSyncService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchedulerConfigTest {

    private static final long CURRENCY_RATES_REFRESH_INTERVAL_MS = 3_600_000L;
    private static final Duration CURRENCY_RATES_REFRESH_RETRY_DELAY = Duration.ofMinutes(5);

    private final SchedulerConfig schedulerConfig = new SchedulerConfig();
    private final CurrencyRateSyncService currencyRateSyncService = mock(CurrencyRateSyncService.class);

    @Test
    void shouldRegisterTheCurrencyRatesRefreshAsASingleRecurringInstance() {
        RecurringTask<Void> currencyRatesRefreshTask = createCurrencyRatesRefreshTask();

        assertThat(currencyRatesRefreshTask.getDefaultTaskInstance())
                .isEqualTo(TaskInstanceId.of(SchedulerConfig.CURRENCY_RATES_REFRESH_TASK_NAME, RecurringTask.INSTANCE));
        assertThat(currencyRatesRefreshTask.getFailureHandler()).isInstanceOf(FailureHandler.OnFailureRetryLater.class);
    }

    @Test
    void shouldSyncTheCurrencyRatesOnEveryExecution() {
        createCurrencyRatesRefreshTask().executeRecurringly(null, null);

        verify(currencyRateSyncService).syncRates();
    }

    @Test
    void shouldLetASyncFailureReachTheSchedulerSoItIsRetried() {
        RecurringTask<Void> currencyRatesRefreshTask = createCurrencyRatesRefreshTask();
        IllegalStateException providerUnavailable = new IllegalStateException("exchange rate provider unavailable");
        doThrow(providerUnavailable).when(currencyRateSyncService).syncRates();

        assertThatThrownBy(() -> currencyRatesRefreshTask.executeRecurringly(null, null)).isSameAs(providerUnavailable);
    }

    private RecurringTask<Void> createCurrencyRatesRefreshTask() {
        return schedulerConfig.currencyRatesRefreshTask(
                currencyRateSyncService,
                CURRENCY_RATES_REFRESH_INTERVAL_MS,
                CURRENCY_RATES_REFRESH_RETRY_DELAY);
    }
}
