package org.banksolution.outbox.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.banksolution.outbox.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status IN ('PENDING', 'RETRY') AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> claimDueOutboxEvents(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Query(value = """
            SELECT * FROM outbox_event
            WHERE id = :outboxEventId AND status = 'PENDING'
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxEventEntity> claimPendingOutboxEvent(@Param("outboxEventId") UUID outboxEventId);

    @Modifying
    @Query(value = "DELETE FROM outbox_event WHERE status = 'PROCESSED' AND processed_at < :processedBefore", nativeQuery = true)
    int deleteProcessedOutboxEventsBefore(@Param("processedBefore") Instant processedBefore);
}
