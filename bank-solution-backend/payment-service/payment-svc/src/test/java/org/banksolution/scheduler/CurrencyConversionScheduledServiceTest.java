package org.banksolution.scheduler;

import org.banksolution.service.CurrencyRateSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionScheduledServiceTest {

    @Mock
    private CurrencyRateSyncService currencyRateSyncService;

    @InjectMocks
    private CurrencyConversionScheduledService currencyConversionScheduledService;

    @Test
    void shouldSyncRatesWhenTriggered() {
        currencyConversionScheduledService.fetchAndUpdateRates();

        verify(currencyRateSyncService).syncRates();
    }

    @Test
    void shouldSwallowASyncFailureSoTheRecurringTaskKeepsItsSchedule() {
        doThrow(new IllegalStateException("provider down")).when(currencyRateSyncService).syncRates();

        assertThatCode(currencyConversionScheduledService::fetchAndUpdateRates).doesNotThrowAnyException();
    }
}
