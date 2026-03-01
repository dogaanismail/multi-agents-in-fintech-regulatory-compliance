package org.banksolution.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.CurrencyRateSyncService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionScheduledService {

    private final CurrencyRateSyncService currencyRateSyncService;

    public void fetchAndUpdateRates() {
        log.info("Scheduled currency rate sync triggered");
        try {
            currencyRateSyncService.syncRates();
        } catch (Exception e) {
            log.error("Failed to sync exchange rates during scheduled run", e);
        }
    }
}
