package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.banksolution.exception.WalletBalanceChangedEventException;
import org.banksolution.service.AccountWalletBalanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AccountFixtures.createWalletBalanceChangedEvent;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletBalanceChangedEventConsumerTest {

    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private AccountWalletBalanceService accountWalletBalanceService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private WalletBalanceChangedEventConsumer walletBalanceChangedEventConsumer;

    @Test
    void shouldAcknowledgeAfterApplyingTheBalanceChange() {
        WalletBalanceChangedEvent walletBalanceChangedEvent =
                createWalletBalanceChangedEvent(UUID.randomUUID().toString(), "750.00", "650.00");

        walletBalanceChangedEventConsumer.consume(walletBalanceChangedEvent, PARTITION, OFFSET, acknowledgment);

        verify(accountWalletBalanceService).applyWalletBalanceChange(walletBalanceChangedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenApplyingTheBalanceChangeFails() {
        String ledgerAccountId = UUID.randomUUID().toString();
        WalletBalanceChangedEvent walletBalanceChangedEvent =
                createWalletBalanceChangedEvent(ledgerAccountId, "750.00", "650.00");
        IllegalStateException databaseFailure = new IllegalStateException("database down");
        doThrow(databaseFailure).when(accountWalletBalanceService).applyWalletBalanceChange(walletBalanceChangedEvent);

        assertThatThrownBy(() ->
                walletBalanceChangedEventConsumer.consume(walletBalanceChangedEvent, PARTITION, OFFSET, acknowledgment))
                .isInstanceOf(WalletBalanceChangedEventException.class)
                .hasMessageContaining(ledgerAccountId)
                .hasCause(databaseFailure);

        verify(acknowledgment, never()).acknowledge();
    }
}
