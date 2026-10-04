package org.banksolution.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.banksolution.outbox.enums.OutboxEventFailType;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "outbox_event")
@Table(name = "outbox_event")
public class OutboxEventEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private String referenceId;

    @Column(name = "destination", nullable = false, updatable = false, length = 511)
    private String destination;

    @Column(name = "payload_type", nullable = false, updatable = false, length = 511)
    private String payloadType;

    @Column(name = "payload", nullable = false, updatable = false)
    private byte[] payload;

    @Column(name = "idempotence_key", updatable = false, length = 511)
    private String idempotenceKey;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", updatable = false)
    private Map<String, String> headers = new HashMap<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Builder.Default
    @Column(name = "attempts")
    private int attempts = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_type", length = 50)
    private OutboxEventFailType failType;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Transient
    @Builder.Default
    private boolean newOutboxEvent = true;

    @Override
    public boolean isNew() {
        return newOutboxEvent;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        this.newOutboxEvent = false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof OutboxEventEntity outboxEventEntity && id != null && id.equals(outboxEventEntity.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
