package org.banksolution.outbox.relay;

import java.time.Clock;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class OutboxEventCleaner {

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final Clock clock;
    private final Duration retention;

    public OutboxEventCleaner(
            OutboxEventRepository outboxEventRepository,
            TransactionTemplate requiresNewTransactionTemplate,
            Clock clock,
            Duration retention) {

        this.outboxEventRepository = outboxEventRepository;
        this.requiresNewTransactionTemplate = requiresNewTransactionTemplate;
        this.clock = clock;
        this.retention = retention;
    }

    public int deleteExpiredProcessedOutboxEvents() {

        Integer deletedOutboxEvents = requiresNewTransactionTemplate.execute(transactionStatus ->
                outboxEventRepository.deleteProcessedOutboxEventsBefore(clock.instant().minus(retention)));
        int deletedCount = deletedOutboxEvents == null ? 0 : deletedOutboxEvents;
        if (deletedCount > 0) {
            log.info("Deleted {} processed outbox event(s) older than {}", deletedCount, retention);
        }

        return deletedCount;
    }
}
