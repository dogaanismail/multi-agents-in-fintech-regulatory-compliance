package org.banksolution.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "ledger.tigerbeetle")
public record TigerBeetleProperties(
        long clusterId,
        @NotEmpty List<String> addresses) {
}
