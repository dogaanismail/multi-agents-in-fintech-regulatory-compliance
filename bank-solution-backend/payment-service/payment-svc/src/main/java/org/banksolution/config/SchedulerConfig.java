package org.banksolution.config;

import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.banksolution.service.CurrencyRateSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SchedulerConfig {

    public static final String CURRENCY_RATES_REFRESH_TASK_NAME = "currency-rates-refresh";

    @Bean
    public RecurringTask<Void> currencyRatesRefreshTask(
            CurrencyRateSyncService currencyRateSyncService,
            @Value("${app.exchange-rate.scheduler.interval-ms}") long currencyRatesRefreshIntervalMs,
            @Value("${app.exchange-rate.scheduler.retry-delay}") Duration currencyRatesRefreshRetryDelay) {

        return Tasks.recurring(CURRENCY_RATES_REFRESH_TASK_NAME, FixedDelay.ofMillis(currencyRatesRefreshIntervalMs))
                .onFailure(new FailureHandler.OnFailureRetryLater<>(currencyRatesRefreshRetryDelay))
                .execute((_, _) -> currencyRateSyncService.syncRates());
    }
}
