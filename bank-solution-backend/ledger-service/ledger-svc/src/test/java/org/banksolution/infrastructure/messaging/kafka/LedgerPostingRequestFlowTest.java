package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.ledger.WalletBalanceChangedEvent;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.service.LedgerAccountService;
import org.banksolution.service.LedgerPostingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AvroEventFixtures.*;

class LedgerPostingRequestFlowTest extends BaseIntegrationTest {

    private static final Duration FLOW_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";
    private static final Currency CURRENCY = Currency.GBP;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AUTHORISED_AMOUNT = new BigDecimal("250.00");

    @Value("${spring.kafka.topics.incoming.ledger-posting-requested}")
    private String ledgerPostingRequestedTopic;

    @Value("${spring.kafka.topics.outgoing.ledger-posting-completed}")
    private String ledgerPostingCompletedTopic;

    @Value("${spring.kafka.topics.outgoing.wallet-balance-changed}")
    private String walletBalanceChangedTopic;

    @Autowired
    private LedgerAccountService ledgerAccountService;

    @Autowired
    private LedgerPostingService ledgerPostingService;

    @Test
    void shouldHoldTheFundsAndReportTheAuthorisationWithTheNewWalletBalance() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        publish(createOutboundAuthorisationRequestedEvent(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY));

        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = awaitOutcome(clientTransactionId);
        assertThat(ledgerPostingCompletedEvent.getSuccess()).isTrue();
        assertThat(ledgerPostingCompletedEvent.getPostingInstructionType()).isEqualTo(PostingInstructionType.OUTBOUND_AUTHORISATION);
        assertThat(ledgerPostingCompletedEvent.getTransferId()).isEqualTo(
                LedgerTransferIds.deriveTransferId(clientTransactionId,
                        org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION).toString());
        assertThat(ledgerPostingCompletedEvent.getAmount()).isEqualTo("250.00");
        WalletBalanceChangedEvent walletBalanceChangedEvent = awaitWalletBalance(customerAccountId, "1000.00", "750.00");
        assertThat(walletBalanceChangedEvent.getPostedBalance()).isEqualTo("1000.00");
        assertThat(walletBalanceChangedEvent.getPendingDebits()).isEqualTo("250.00");
        assertThat(walletBalanceChangedEvent.getCustomerAccountId()).isEqualTo(customerAccountId.toString());
    }

    @Test
    void shouldSettleAnAuthorisationRequestedOverKafka() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();
        publish(createOutboundAuthorisationRequestedEvent(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT, CURRENCY));
        awaitOutcome(clientTransactionId);

        publish(createSettlementRequestedEvent(clientTransactionId));

        LedgerPostingCompletedEvent settlementOutcome = KafkaTestClients.awaitMatchingEvent(
                ledgerPostingCompletedTopic, FLOW_TIMEOUT,
                (LedgerPostingCompletedEvent outcome) -> clientTransactionId.toString().equals(outcome.getClientTransactionId())
                        && outcome.getPostingInstructionType() == PostingInstructionType.SETTLEMENT);
        assertThat(settlementOutcome.getSuccess()).isTrue();
        assertThat(awaitWalletBalance(customerAccountId, "750.00", "750.00").getPendingDebits()).isEqualTo("0.00");
    }

    @Test
    void shouldReportInsufficientFundsAsARejectedPostingNotAFailedMessage() throws Exception {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        publish(createOutboundAuthorisationRequestedEvent(clientTransactionId, customerAccountId, new BigDecimal("100000.00"), CURRENCY));

        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = awaitOutcome(clientTransactionId);
        assertThat(ledgerPostingCompletedEvent.getSuccess()).isFalse();
        assertThat(ledgerPostingCompletedEvent.getFailureReason()).startsWith("Insufficient funds on ledger account: "
                + LedgerAccountIds.deriveWalletAccountId(customerAccountId, CURRENCY));
        assertThat(ledgerPostingCompletedEvent.getTransferId()).isNull();
    }

    @Test
    void shouldRejectASettlementThatHasNoAuthorisationBehindIt() throws Exception {
        UUID clientTransactionId = UUID.randomUUID();

        publish(createSettlementRequestedEvent(clientTransactionId));

        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = awaitOutcome(clientTransactionId);
        assertThat(ledgerPostingCompletedEvent.getSuccess()).isFalse();
        assertThat(ledgerPostingCompletedEvent.getFailureReason())
                .isEqualTo("No authorisation found for client transaction: " + clientTransactionId);
    }

    @Test
    void shouldParkAMalformedRequestOnTheDeadLetterTopicWithoutRetrying() throws Exception {
        String malformedClientTransactionId = "not-a-uuid-" + UUID.randomUUID();
        LedgerPostingRequestedEvent malformedLedgerPostingRequestedEvent =
                createMalformedLedgerPostingRequestedEvent(malformedClientTransactionId);

        try (KafkaProducer<String, LedgerPostingRequestedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(ledgerPostingRequestedTopic, malformedClientTransactionId,
                    malformedLedgerPostingRequestedEvent)).get();
        }

        LedgerPostingRequestedEvent parkedLedgerPostingRequestedEvent = KafkaTestClients.awaitMatchingEvent(
                ledgerPostingRequestedTopic + DEAD_LETTER_TOPIC_SUFFIX,
                DEAD_LETTER_TIMEOUT,
                (LedgerPostingRequestedEvent deadLetteredEvent) ->
                        malformedClientTransactionId.equals(deadLetteredEvent.getClientTransactionId()));
        assertThat(parkedLedgerPostingRequestedEvent.getEventId()).isEqualTo(malformedLedgerPostingRequestedEvent.getEventId());
    }

    private UUID givenFundedWallet() {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, CURRENCY);
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.inboundHardSettlement(
                UUID.randomUUID(), OPENING_BALANCE, CURRENCY, customerAccountId, null));
        return customerAccountId;
    }

    private <T extends SpecificRecord> void publish(LedgerPostingRequestedEvent ledgerPostingRequestedEvent)
            throws ExecutionException, InterruptedException {

        try (KafkaProducer<String, LedgerPostingRequestedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(ledgerPostingRequestedTopic,
                    ledgerPostingRequestedEvent.getClientTransactionId(), ledgerPostingRequestedEvent)).get();
        }
    }

    private LedgerPostingCompletedEvent awaitOutcome(UUID clientTransactionId) {
        return KafkaTestClients.awaitMatchingEvent(ledgerPostingCompletedTopic, FLOW_TIMEOUT,
                (LedgerPostingCompletedEvent ledgerPostingCompletedEvent) ->
                        clientTransactionId.toString().equals(ledgerPostingCompletedEvent.getClientTransactionId()));
    }

    private WalletBalanceChangedEvent awaitWalletBalance(
            UUID customerAccountId,
            String expectedPostedBalance,
            String expectedAvailableBalance) {

        String walletLedgerAccountId = LedgerAccountIds.deriveWalletAccountId(customerAccountId, CURRENCY).toString();
        return KafkaTestClients.awaitMatchingEvent(walletBalanceChangedTopic, FLOW_TIMEOUT,
                (WalletBalanceChangedEvent walletBalanceChangedEvent) ->
                        walletLedgerAccountId.equals(walletBalanceChangedEvent.getLedgerAccountId())
                                && expectedPostedBalance.equals(walletBalanceChangedEvent.getPostedBalance())
                                && expectedAvailableBalance.equals(walletBalanceChangedEvent.getAvailableBalance()));
    }
}
