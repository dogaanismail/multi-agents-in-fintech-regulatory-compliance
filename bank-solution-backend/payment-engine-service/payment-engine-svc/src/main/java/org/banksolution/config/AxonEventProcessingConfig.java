package org.banksolution.config;

import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configuration;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.axonframework.eventhandling.deadletter.jpa.JpaSequencedDeadLetterQueue;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.axonframework.serialization.Serializer;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.banksolution.domain.payment.saga.LedgerPostingSaga;
import org.banksolution.domain.payment.saga.PaymentRiskSaga;
import org.banksolution.infrastructure.deadletter.DeadLetterRetryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.function.Function;

/**
 * Every handler that talks to Kafka runs on a pooled streaming processor instead of in the
 * command's own unit of work, so the event store — not the thread that happened to be
 * running — is what guarantees the publication eventually happens.
 * <p>
 * Publishers keep a dead-letter queue: a failed publication is parked and retried by
 * {@link org.banksolution.infrastructure.deadletter.DeadLetterRetryScheduler} without
 * blocking the stream. Sagas cannot be dead-lettered, so their failures propagate and the
 * processor itself retries them with back-off.
 * <p>
 * New processors start at the head of the store: replaying history would re-request risk
 * assessments and ledger postings for payments that finished long ago.
 */
@org.springframework.context.annotation.Configuration
public class AxonEventProcessingConfig {

    static final int BATCH_SIZE = 1;

    @Autowired
    public void configureEventProcessing(
            EventProcessingConfigurer configurer,
            EntityManagerProvider entityManagerProvider,
            TransactionManager transactionManager,
            @Qualifier("eventSerializer") Serializer eventSerializer,
            Serializer serializer) {

        configureEventProcessing(configurer, processingGroup -> JpaSequencedDeadLetterQueue.<EventMessage<?>>builder()
                .processingGroup(processingGroup)
                .entityManagerProvider(entityManagerProvider)
                .transactionManager(transactionManager)
                .eventSerializer(eventSerializer)
                .genericSerializer(serializer)
                .build());
    }

    static void configureEventProcessing(
            EventProcessingConfigurer configurer,
            Function<String, SequencedDeadLetterQueue<EventMessage<?>>> deadLetterQueueFactory) {

        configurer.registerSaga(PaymentRiskSaga.class);
        configurer.assignHandlerTypesMatching(
                PaymentEventProcessingGroups.PAYMENT_RISK_SAGA, PaymentRiskSaga.class::equals);

        configurer.registerSaga(LedgerPostingSaga.class);
        configurer.assignHandlerTypesMatching(
                PaymentEventProcessingGroups.LEDGER_POSTING_SAGA, LedgerPostingSaga.class::equals);

        for (String processingGroup : PaymentEventProcessingGroups.allGroups()) {
            configurer.registerPooledStreamingEventProcessor(
                    processingGroup,
                    Configuration::eventStore,
                    (_, builder) -> builder
                            .initialToken(StreamableMessageSource::createHeadToken)
                            .batchSize(BATCH_SIZE));
            configurer.registerListenerInvocationErrorHandler(
                    processingGroup, _ -> PropagatingErrorHandler.instance());
        }

        for (String processingGroup : PaymentEventProcessingGroups.deadLetteringGroups()) {
            configurer.registerDeadLetterQueue(
                    processingGroup, _ -> deadLetterQueueFactory.apply(processingGroup));
            configurer.registerDeadLetterPolicy(
                    processingGroup, _ -> new DeadLetterRetryPolicy());
        }
    }
}
