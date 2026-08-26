package org.banksolution.infrastructure.tigerbeetle;

import com.tigerbeetle.Client;
import com.tigerbeetle.IdBatch;
import com.tigerbeetle.UInt128;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.TigerBeetleProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component("tigerBeetle")
@RequiredArgsConstructor
@Slf4j
public class TigerBeetleHealthIndicator implements HealthIndicator {

    private static final long PROBE_ACCOUNT_ID = 1L;
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final Client tigerBeetleClient;
    private final TigerBeetleProperties tigerBeetleProperties;

    @Override
    public Health health() {
        try {
            tigerBeetleClient
                    .lookupAccountsAsync(new IdBatch(UInt128.asBytes(PROBE_ACCOUNT_ID)))
                    .get(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            return up();
        } catch (TimeoutException probeTimeout) {
            return down("no reply within " + PROBE_TIMEOUT.toMillis() + "ms", probeTimeout);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            return down("probe interrupted", interruption);
        } catch (Exception probeFailure) {
            return down(probeFailure.getMessage(), probeFailure);
        }
    }

    private Health up() {
        return Health.up()
                .withDetail("clusterId", tigerBeetleProperties.clusterId())
                .withDetail("addresses", tigerBeetleProperties.addresses())
                .build();
    }

    private Health down(
            String reason,
            Throwable cause) {

        log.warn("TigerBeetle health probe failed: {}", reason);
        return Health.down(cause)
                .withDetail("clusterId", tigerBeetleProperties.clusterId())
                .withDetail("addresses", tigerBeetleProperties.addresses())
                .build();
    }
}
