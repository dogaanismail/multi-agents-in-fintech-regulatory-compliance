package org.banksolution.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ledger.posting")
public record LedgerPostingProperties(@Positive int authorisationTimeoutSeconds) {
}
