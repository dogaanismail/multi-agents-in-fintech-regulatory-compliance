package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.infrastructure.messaging.kafka.producer.WalletBalanceChangedEventProducer;
import org.banksolution.mapper.WalletBalanceChangedEventMapper;
import org.banksolution.repository.TigerBeetleAccountRepository;
import org.banksolution.repository.TigerBeetleTransferRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletBalancePublicationService {

    private final TigerBeetleAccountRepository tigerBeetleAccountRepository;
    private final TigerBeetleTransferRepository tigerBeetleTransferRepository;
    private final WalletBalanceChangedEventProducer walletBalanceChangedEventProducer;

    /**
     * The posting is already committed in TigerBeetle when this runs, so a publication
     * failure must not fail the posting. The events carry absolute balances, so a missed
     * publication self-heals on the wallet's next movement.
     */
    public void publishWalletBalanceChanges(List<LedgerTransfer> appliedLedgerTransfers) {
        try {
            List<LedgerAccount> touchedWalletAccounts =
                    findTouchedWalletAccounts(appliedLedgerTransfers);

            touchedWalletAccounts.stream()
                    .map(WalletBalanceChangedEventMapper::toWalletBalanceChangedEvent)
                    .forEach(walletBalanceChangedEventProducer::publish);
        } catch (Exception e) {
            log.error("Failed to publish wallet balance changes for client transaction {}",
                    appliedLedgerTransfers.isEmpty() ? null : appliedLedgerTransfers.getFirst().clientTransactionId(),
                    e);
        }
    }

    private List<LedgerAccount> findTouchedWalletAccounts(List<LedgerTransfer> appliedLedgerTransfers) {
        List<UUID> touchedAccountIds = resolveTouchedAccountIds(appliedLedgerTransfers);

        return tigerBeetleAccountRepository.findLedgerAccountsByIds(touchedAccountIds).stream()
                .filter(ledgerAccount -> ledgerAccount.accountType() == LedgerAccountType.WALLET)
                .toList();
    }

    /**
     * Settlement and release transfers reference their authorisation instead of naming
     * accounts, so the authorisation transfers are resolved to find the wallets they moved.
     */
    private List<UUID> resolveTouchedAccountIds(List<LedgerTransfer> appliedLedgerTransfers) {
        List<UUID> pendingTransferIds = appliedLedgerTransfers.stream()
                .map(LedgerTransfer::pendingTransferId)
                .filter(Objects::nonNull)
                .toList();

        Stream<LedgerTransfer> accountBearingTransfers = appliedLedgerTransfers.stream()
                .filter(ledgerTransfer -> ledgerTransfer.pendingTransferId() == null);

        Stream<LedgerTransfer> resolvedAuthorisationTransfers = pendingTransferIds.isEmpty()
                ? Stream.empty()
                : tigerBeetleTransferRepository.findLedgerTransfersByIds(pendingTransferIds).stream();

        return Stream.concat(accountBearingTransfers, resolvedAuthorisationTransfers)
                .flatMap(ledgerTransfer -> Stream.of(
                        ledgerTransfer.debitAccountId(),
                        ledgerTransfer.creditAccountId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
