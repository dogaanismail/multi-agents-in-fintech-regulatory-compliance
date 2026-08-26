package org.banksolution.service;

import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.infrastructure.messaging.kafka.producer.WalletBalanceChangedEventProducer;
import org.banksolution.repository.TigerBeetleAccountRepository;
import org.banksolution.repository.TigerBeetleTransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletBalancePublicationServiceFailureTest {

    @Mock
    private TigerBeetleAccountRepository tigerBeetleAccountRepository;

    @Mock
    private TigerBeetleTransferRepository tigerBeetleTransferRepository;

    @Mock
    private WalletBalanceChangedEventProducer walletBalanceChangedEventProducer;

    @InjectMocks
    private WalletBalancePublicationService walletBalancePublicationService;

    @Test
    void shouldNeverFailTheCommittedPostingWhenTheBalancesCannotBeRead() {
        when(tigerBeetleAccountRepository.findLedgerAccountsByIds(anyList()))
                .thenThrow(new LedgerUnavailableException(new InterruptedException("cluster down")));
        LedgerTransfer appliedLedgerTransfer = LedgerTransfer.builder()
                .id(UUID.randomUUID())
                .clientTransactionId(UUID.randomUUID())
                .postingInstructionType(PostingInstructionType.OUTBOUND_HARD_SETTLEMENT)
                .debitAccountId(UUID.randomUUID())
                .creditAccountId(UUID.randomUUID())
                .amount(new BigDecimal("1.00"))
                .currency(Currency.GBP)
                .build();

        assertThatCode(() -> walletBalancePublicationService.publishWalletBalanceChanges(List.of(appliedLedgerTransfer)))
                .doesNotThrowAnyException();

        verifyNoInteractions(walletBalanceChangedEventProducer);
    }

    @Test
    void shouldTolerateAnEmptyPostingEvenWhenTheLookupFails() {
        when(tigerBeetleAccountRepository.findLedgerAccountsByIds(anyList()))
                .thenThrow(new IllegalStateException("client closed"));

        assertThatCode(() -> walletBalancePublicationService.publishWalletBalanceChanges(List.of()))
                .doesNotThrowAnyException();

        verifyNoInteractions(walletBalanceChangedEventProducer, tigerBeetleTransferRepository);
    }
}
