package org.banksolution.infrastructure.deadletter;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.deadletter.DeadLetter;
import org.axonframework.messaging.deadletter.SequencedDeadLetterProcessor;
import org.axonframework.messaging.deadletter.SequencedDeadLetterQueue;
import org.banksolution.domain.payment.PaymentEventProcessingGroups;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Slf4j
@Component
public class DeadLetterRetryScheduler {

    private final EventProcessingConfiguration eventProcessingConfiguration;
    private final int maxRetries;

    public DeadLetterRetryScheduler(
            EventProcessingConfiguration eventProcessingConfiguration,
            @Value("${payment-engine.dead-letter.max-retries}") int maxRetries) {

        this.eventProcessingConfiguration = eventProcessingConfiguration;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${payment-engine.dead-letter.retry-interval}")
    public void retryDeadLetters() {
        PaymentEventProcessingGroups.deadLetteringGroups().forEach(this::retryDeadLettersOf);
    }

    public int retryDeadLettersOf(String processingGroup) {
        SequencedDeadLetterQueue<EventMessage<?>> deadLetterQueue = eventProcessingConfiguration
                .deadLetterQueue(processingGroup)
                .orElseThrow(() -> new IllegalStateException("No dead-letter queue registered for " + processingGroup));

        SequencedDeadLetterProcessor<EventMessage<?>> deadLetterProcessor = eventProcessingConfiguration
                .sequencedDeadLetterProcessor(processingGroup)
                .orElseThrow(() -> new IllegalStateException("No dead-letter processor registered for " + processingGroup));

        long parkedSequences = deadLetterQueue.amountOfSequences();
        int drainedSequences = 0;
        for (long attempt = 0; attempt < parkedSequences; attempt++) {
            if (deadLetterProcessor.process(retriesRemaining())) {
                drainedSequences++;
            }
        }

        if (parkedSequences > 0) {
            log.info("Retried {} dead-lettered sequence(s) of processing group {}, {} drained",
                    parkedSequences,
                    processingGroup,
                    drainedSequences);
        }

        return drainedSequences;
    }

    public Predicate<DeadLetter<? extends EventMessage<?>>> retriesRemaining() {
        return deadLetter -> DeadLetterRetryPolicy.retriesOf(deadLetter) < maxRetries;
    }
}
