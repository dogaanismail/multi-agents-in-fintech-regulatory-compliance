package org.banksolution.config;

import jakarta.validation.constraints.NotEmpty;
import org.banksolution.enums.Currency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "ledger.chart-of-accounts")
public record ChartOfAccountsProperties(
        boolean seedEnabled,
        @NotEmpty List<Currency> currencies) {
}
