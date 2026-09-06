package org.banksolution.config;

import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.TrackingToken;
import org.axonframework.eventhandling.pooled.PooledStreamingEventProcessor;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.banksolution.enums.PaymentEventTrigger;
import org.banksolution.infrastructure.deadletter.DeadLetterRetryScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The unit test pins the processor semantics against an in-memory store; this one checks
 * the same wiring survives the real Spring context: JPA token store, JPA dead-letter table
 * (Hibernate must know the entity), the scheduler bean, and a payment flowing through the
 * streaming processors out to Kafka.
 */
class AxonEventProcessingIntegrationTest extends PaymentFlowSupport {

    @Autowired
    private EventProcessingConfiguration eventProcessingConfiguration;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DeadLetterRetryScheduler deadLetterRetryScheduler;

    @Test
    void shouldRunEveryKafkaPublishingGroupAsARunningPooledStreamingProcessor() {
        for (String processingGroup : PaymentEventProcessingGroups.allGroups()) {
            assertThat(eventProcessingConfiguration.eventProcessorByProcessingGroup(processingGroup, PooledStreamingEventProcessor.class))
                    .as(processingGroup)
                    .hasValueSatisfying(processor -> {
                        assertThat(processor.isRunning()).isTrue();
                        assertThat(processor.isError()).isFalse();
                    });
        }
    }

    @Test
    void shouldBackEveryDeadLetteringGroupWithAnEmptyJpaQueueTheSchedulerCanDrain() {
        for (String processingGroup : PaymentEventProcessingGroups.deadLetteringGroups()) {
            assertThat(eventProcessingConfiguration.deadLetterQueue(processingGroup))
                    .as(processingGroup)
                    .hasValueSatisfying(deadLetterQueue -> {
                        Long parkedLetters = transactionTemplate.execute(status -> deadLetterQueue.size());
                        assertThat(parkedLetters).isZero();
                    });
            assertThat(deadLetterRetryScheduler.retryDeadLettersOf(processingGroup)).isZero();
        }
    }

    @Test
    void shouldAdvanceThePersistedTokenOfEachPublisherAsAPaymentFlowsOutToKafka() throws Exception {
        UUID paymentId = givenPaymentCreated();

        awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_INITIATED);
        awaitLedgerPostingRequested(paymentId, com.aml.ledger.PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);

        for (String processingGroup : PaymentEventProcessingGroups.allGroups()) {
            await().atMost(FLOW_TIMEOUT).untilAsserted(() -> {
                TrackingToken trackingToken = transactionTemplate.execute(status -> tokenStore.fetchToken(processingGroup, 0));
                assertThat(trackingToken).as(processingGroup).isNotNull();
            });
        }
    }
}
