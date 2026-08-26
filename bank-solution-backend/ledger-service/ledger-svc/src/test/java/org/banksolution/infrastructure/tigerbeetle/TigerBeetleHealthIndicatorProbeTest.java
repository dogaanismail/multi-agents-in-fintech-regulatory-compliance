package org.banksolution.infrastructure.tigerbeetle;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.Client;
import com.tigerbeetle.IdBatch;
import org.banksolution.config.TigerBeetleProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The client blocks forever when the cluster is gone, so the probe's own timeout is what
 * keeps /actuator/health answering; these failure modes need a stubbed client.
 */
class TigerBeetleHealthIndicatorProbeTest {

    private final Client tigerBeetleClient = mock(Client.class);
    private final TigerBeetleHealthIndicator tigerBeetleHealthIndicator = new TigerBeetleHealthIndicator(
            tigerBeetleClient, new TigerBeetleProperties(0, List.of("127.0.0.1:3000")));

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void shouldReportDownWhenTheClusterNeverReplies() {
        when(tigerBeetleClient.lookupAccountsAsync(any(IdBatch.class))).thenReturn(new CompletableFuture<>());

        Health health = tigerBeetleHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("clusterId", 0L).containsKey("addresses");
        assertThat(health.getDetails().get("error").toString()).contains("TimeoutException");
    }

    @Test
    void shouldReportDownWhenTheProbeFails() {
        when(tigerBeetleClient.lookupAccountsAsync(any(IdBatch.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("client closed")));

        Health health = tigerBeetleHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error").toString()).contains("client closed");
    }

    @Test
    void shouldReportDownAndKeepTheInterruptWhenTheProbeIsInterrupted() {
        when(tigerBeetleClient.lookupAccountsAsync(any(IdBatch.class))).thenReturn(new CompletableFuture<>());
        Thread.currentThread().interrupt();

        Health health = tigerBeetleHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void shouldReportUpWhenTheClusterAnswers() {
        when(tigerBeetleClient.lookupAccountsAsync(any(IdBatch.class)))
                .thenReturn(CompletableFuture.completedFuture(new AccountBatch(0)));

        assertThat(tigerBeetleHealthIndicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
