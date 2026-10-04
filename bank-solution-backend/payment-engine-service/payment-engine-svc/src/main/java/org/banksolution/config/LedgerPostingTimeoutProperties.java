package org.banksolution.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "payment-engine.ledger-posting")
public class LedgerPostingTimeoutProperties {

    private Duration initialTimeout = Duration.ofMinutes(2);
    private Duration maxTimeout = Duration.ofMinutes(30);
    private int maxResends = 10;
}
