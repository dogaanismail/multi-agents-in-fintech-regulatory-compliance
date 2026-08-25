package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.repository.AccountRepository;
import org.banksolution.repository.AccountWalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.fixtures.AccountFixtures.createAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createAccountWalletEntity;
import static org.banksolution.fixtures.AccountFixtures.createWalletBalanceChangedEvent;

class WalletBalanceChangedEventFlowTest extends BaseIntegrationTest {

    private static final Duration PROJECTION_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.kafka.topics.incoming.wallet-balance-changed}")
    private String walletBalanceChangedTopic;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountWalletRepository accountWalletRepository;

    @Test
    void shouldProjectTheLedgerBalancesOntoTheWalletAndStayIdempotentOnRedelivery()
            throws ExecutionException, InterruptedException {

        AccountEntity accountEntity = accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID()));
        AccountWalletEntity accountWalletEntity =
                accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.GBP, true));
        String ledgerAccountId = accountWalletEntity.getLedgerAccountId().toString();
        WalletBalanceChangedEvent walletBalanceChangedEvent =
                createWalletBalanceChangedEvent(ledgerAccountId, "750.00", "650.00");

        try (KafkaProducer<String, WalletBalanceChangedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(walletBalanceChangedTopic, ledgerAccountId, walletBalanceChangedEvent)).get();
            producer.send(new ProducerRecord<>(walletBalanceChangedTopic, ledgerAccountId, walletBalanceChangedEvent)).get();
        }

        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() -> {
            AccountWalletEntity projectedAccountWalletEntity =
                    accountWalletRepository.findById(accountWalletEntity.getId()).orElseThrow();
            assertThat(projectedAccountWalletEntity.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(projectedAccountWalletEntity.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("650.00"));
        });
    }
}
