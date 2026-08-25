package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.ledger.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.WalletBalanceChangedEventException;
import org.banksolution.service.AccountWalletBalanceService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletBalanceChangedEventConsumer {

    private final AccountWalletBalanceService accountWalletBalanceService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.wallet-balance-changed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload WalletBalanceChangedEvent walletBalanceChangedEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed WalletBalanceChangedEvent: ledgerAccountId:{}, currency:{}, partition:{}, offset:{}",
                walletBalanceChangedEvent.getLedgerAccountId(),
                walletBalanceChangedEvent.getCurrency(),
                partition,
                offset);

        try {
            accountWalletBalanceService.applyWalletBalanceChange(walletBalanceChangedEvent);
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            throw new WalletBalanceChangedEventException(
                    "Failed to process WalletBalanceChangedEvent for ledgerAccountId: %s",
                    exception,
                    walletBalanceChangedEvent.getLedgerAccountId());
        }
    }
}
