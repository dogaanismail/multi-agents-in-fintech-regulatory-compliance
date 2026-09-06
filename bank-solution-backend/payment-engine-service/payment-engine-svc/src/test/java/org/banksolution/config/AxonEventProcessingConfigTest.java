package org.banksolution.config;

import org.axonframework.config.Configuration;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.axonframework.eventhandling.pooled.PooledStreamingEventProcessor;
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.messaging.deadletter.DeadLetter;
import org.axonframework.messaging.deadletter.InMemorySequencedDeadLetterQueue;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.banksolution.domain.payment.saga.LedgerPostingSaga;
import org.banksolution.domain.payment.saga.PaymentRiskSaga;
import org.banksolution.infrastructure.deadletter.DeadLetterRetryPolicy;
import org.banksolution.infrastructure.deadletter.DeadLetterRetryScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.domain.payment.PaymentEventProcessingGroups.PAYMENT_SNAPSHOT_PUBLISHER;

/**
 * Runs the real processor configuration against an in-memory event store to pin the
 * guarantees the Kafka publishers now rely on: publication happens off the command thread,
 * history is not replayed, a failed publication is parked rather than lost or blocking, and
 * a parked payment is retried in order once the failure clears.
 */
class AxonEventProcessingConfigTest {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(10);
    private static final String PAYMENT_A = "payment-a";
    private static final String PAYMENT_B = "payment-b";
    private static final int MAX_RETRIES = 2;

    private final Map<String, SequencedDeadLetterQueue<EventMessage<?>>> deadLetterQueues = new ConcurrentHashMap<>();
    private final RecordingPublisher recordingPublisher = new RecordingPublisher();
    private final AtomicLong sequenceNumbers = new AtomicLong();
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        configuration = DefaultConfigurer.defaultConfiguration()
                .configureEmbeddedEventStore(_ -> new InMemoryEventStorageEngine())
                .eventProcessing(eventProcessingConfigurer -> {
                    eventProcessingConfigurer.registerTokenStore(_ -> new InMemoryTokenStore());
                    eventProcessingConfigurer.registerEventHandler(_ -> recordingPublisher);
                    eventProcessingConfigurer.registerEventHandler(_ -> new RecordingFeedbackPublisher());
                    AxonEventProcessingConfig.configureEventProcessing(
                            eventProcessingConfigurer,
                            processingGroup -> deadLetterQueues.computeIfAbsent(
                                    processingGroup, ignored -> InMemorySequencedDeadLetterQueue.defaultQueue()));
                })
                .buildConfiguration();
    }

    @AfterEach
    void tearDown() {
        configuration.shutdown();
    }

    @Test
    void shouldRunEveryKafkaPublishingGroupOnItsOwnPooledStreamingProcessor() {
        configuration.start();
        EventProcessingConfiguration eventProcessingConfiguration = configuration.eventProcessingConfiguration();

        for (String processingGroup : PaymentEventProcessingGroups.allGroups()) {
            assertThat(eventProcessingConfiguration.eventProcessorByProcessingGroup(processingGroup, PooledStreamingEventProcessor.class))
                    .as(processingGroup)
                    .hasValueSatisfying(processor -> {
                        assertThat(processor.getName()).isEqualTo(processingGroup);
                        assertThat(processor.isRunning()).isTrue();
                    });
        }

        assertThat(eventProcessingConfiguration.sagaProcessingGroup(PaymentRiskSaga.class))
                .isEqualTo(PaymentEventProcessingGroups.PAYMENT_RISK_SAGA);
        assertThat(eventProcessingConfiguration.sagaProcessingGroup(LedgerPostingSaga.class))
                .isEqualTo(PaymentEventProcessingGroups.LEDGER_POSTING_SAGA);
    }

    @Test
    void shouldPropagateSagaFailuresSoTheProcessorRetriesThemInsteadOfLoggingThemAway() {
        configuration.start();
        EventProcessingConfiguration eventProcessingConfiguration = configuration.eventProcessingConfiguration();

        for (String sagaGroup : PaymentEventProcessingGroups.sagaGroups()) {
            assertThat(eventProcessingConfiguration.listenerInvocationErrorHandler(sagaGroup))
                    .as(sagaGroup)
                    .isInstanceOf(PropagatingErrorHandler.class);
            assertThat(eventProcessingConfiguration.deadLetterQueue(sagaGroup)).as(sagaGroup).isEmpty();
        }
    }

    @Test
    void shouldDeadLetterEveryKafkaPublisherWithTheRetryCountingPolicy() {
        configuration.start();
        EventProcessingConfiguration eventProcessingConfiguration = configuration.eventProcessingConfiguration();

        for (String publisherGroup : PaymentEventProcessingGroups.deadLetteringGroups()) {
            assertThat(eventProcessingConfiguration.deadLetterQueue(publisherGroup))
                    .as(publisherGroup)
                    .hasValueSatisfying(deadLetterQueue ->
                            assertThat(deadLetterQueue).isSameAs(deadLetterQueues.get(publisherGroup)));
            assertThat(eventProcessingConfiguration.sequencedDeadLetterProcessor(publisherGroup))
                    .as(publisherGroup)
                    .isPresent();
        }
    }

    @Test
    void shouldStartAtTheHeadOfTheEventStoreInsteadOfReplayingHistory() {
        publish(PAYMENT_A, "historic");
        configuration.start();

        publish(PAYMENT_B, "live");

        awaitPublished("live");
        assertThat(recordingPublisher.published).containsExactly("live");
    }

    @Test
    void shouldPublishOffTheCallingThreadOnceTheEventIsInTheStore() {
        configuration.start();

        publish(PAYMENT_A, "snapshot");

        awaitPublished("snapshot");
        assertThat(recordingPublisher.publishingThreads).doesNotContain(Thread.currentThread().getName());
    }

    @Test
    void shouldParkAFailedPublicationWithoutBlockingOtherPayments() {
        configuration.start();
        recordingPublisher.failing.add(PAYMENT_A);

        publish(PAYMENT_A, "a-1");
        publish(PAYMENT_B, "b-1");

        awaitPublished("b-1");
        assertThat(recordingPublisher.published).containsExactly("b-1");
        assertThat(deadLetterQueueOf().deadLetterSequence(PAYMENT_A))
                .singleElement()
                .satisfies(deadLetter -> {
                    assertThat(deadLetter.message().getPayload()).isEqualTo(new PublicationProbe(PAYMENT_A, "a-1"));
                    assertThat(deadLetter.cause()).hasValueSatisfying(cause ->
                            assertThat(cause.message()).contains("broker unavailable"));
                    assertThat(DeadLetterRetryPolicy.retriesOf(deadLetter)).isZero();
                });
    }

    @Test
    void shouldQueueLaterEventsOfAParkedPaymentBehindItToPreserveSnapshotOrder() {
        configuration.start();
        recordingPublisher.failing.add(PAYMENT_A);
        publish(PAYMENT_A, "a-1");
        awaitParked(1);

        recordingPublisher.failing.remove(PAYMENT_A);
        publish(PAYMENT_A, "a-2");

        awaitParked(2);
        assertThat(recordingPublisher.published).isEmpty();
        assertThat(deadLetterQueueOf().deadLetterSequence(PAYMENT_A))
                .extracting(deadLetter -> ((PublicationProbe) deadLetter.message().getPayload()).marker())
                .containsExactly("a-1", "a-2");
    }

    @Test
    void shouldRetryAParkedPaymentInOrderOnceTheFailureClears() {
        configuration.start();
        recordingPublisher.failing.add(PAYMENT_A);
        publish(PAYMENT_A, "a-1");
        publish(PAYMENT_A, "a-2");
        awaitParked(2);
        recordingPublisher.failing.remove(PAYMENT_A);

        int retriedSequences = createDeadLetterRetryScheduler().retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER);

        assertThat(retriedSequences).isEqualTo(1);
        assertThat(recordingPublisher.published).containsExactly("a-1", "a-2");
        assertThat(deadLetterQueueOf().size()).isZero();
    }

    @Test
    void shouldCountFailedRetriesAndKeepTheLetterParkedForAnOperatorAfterTheLastOne() {
        configuration.start();
        recordingPublisher.failing.add(PAYMENT_A);
        publish(PAYMENT_A, "a-1");
        awaitParked(1);
        DeadLetterRetryScheduler deadLetterRetryScheduler = createDeadLetterRetryScheduler();

        for (int retry = 1; retry <= MAX_RETRIES; retry++) {
            assertThat(deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER)).isZero();
            assertThat(parkedRetriesOf()).isEqualTo(retry);
            assertThat(recordingPublisher.attempts.get(PAYMENT_A)).isEqualTo(1 + retry);
        }

        assertThat(deadLetterRetryScheduler.retryDeadLettersOf(PAYMENT_SNAPSHOT_PUBLISHER)).isZero();
        assertThat(parkedRetriesOf()).isEqualTo(MAX_RETRIES);
        assertThat(recordingPublisher.attempts.get(PAYMENT_A)).isEqualTo(1 + MAX_RETRIES);
        assertThat(deadLetterQueueOf().sequenceSize(PAYMENT_A)).isEqualTo(1);
    }

    private DeadLetterRetryScheduler createDeadLetterRetryScheduler() {
        return new DeadLetterRetryScheduler(configuration.eventProcessingConfiguration(), MAX_RETRIES);
    }

    private void publish(String paymentId, String marker) {
        configuration.eventStore().publish(new GenericDomainEventMessage<>(
                PublicationProbe.class.getSimpleName(),
                paymentId,
                sequenceNumbers.getAndIncrement(),
                new PublicationProbe(paymentId, marker)));
    }

    private void awaitPublished(String marker) {
        await().atMost(PROCESSING_TIMEOUT).untilAsserted(() ->
                assertThat(recordingPublisher.published).contains(marker));
    }

    private void awaitParked(long parkedLetters) {
        await().atMost(PROCESSING_TIMEOUT).untilAsserted(() -> assertThat(deadLetterQueueOf()
                .sequenceSize(AxonEventProcessingConfigTest.PAYMENT_A))
                .isEqualTo(parkedLetters));
    }

    private int parkedRetriesOf() {
        List<DeadLetter<? extends EventMessage<?>>> deadLetters = new CopyOnWriteArrayList<>();
        deadLetterQueueOf()
                .deadLetterSequence(AxonEventProcessingConfigTest.PAYMENT_A)
                .forEach(deadLetters::add);
        assertThat(deadLetters).hasSize(1);

        return DeadLetterRetryPolicy.retriesOf(deadLetters.getFirst());
    }

    private SequencedDeadLetterQueue<EventMessage<?>> deadLetterQueueOf() {
        return deadLetterQueues.get(PaymentEventProcessingGroups.PAYMENT_SNAPSHOT_PUBLISHER);
    }

    record PublicationProbe(String paymentId, String marker) {
    }

    @ProcessingGroup(PaymentEventProcessingGroups.COMPLIANCE_FEEDBACK_PUBLISHER)
    static class RecordingFeedbackPublisher {

        @EventHandler
        public void on(PublicationProbe publicationProbe) {
            // Only here so the group has a handler and therefore a processor to assert on.
        }
    }

    @ProcessingGroup(PAYMENT_SNAPSHOT_PUBLISHER)
    static class RecordingPublisher {

        final List<String> published = new CopyOnWriteArrayList<>();
        final List<String> publishingThreads = new CopyOnWriteArrayList<>();
        final Set<String> failing = ConcurrentHashMap.newKeySet();
        final Map<String, Integer> attempts = new ConcurrentHashMap<>();

        @EventHandler
        public void on(PublicationProbe publicationProbe) {
            attempts.merge(publicationProbe.paymentId(), 1, Integer::sum);

            if (failing.contains(publicationProbe.paymentId())) {
                throw new IllegalStateException("broker unavailable for " + publicationProbe.paymentId());
            }

            publishingThreads.add(Thread.currentThread().getName());
            published.add(publicationProbe.marker());
        }
    }
}
