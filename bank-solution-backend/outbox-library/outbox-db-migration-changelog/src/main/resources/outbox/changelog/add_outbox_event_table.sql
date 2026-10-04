CREATE TABLE outbox_event
(
    id              UUID PRIMARY KEY,
    reference_id    VARCHAR(255) NOT NULL,
    destination     VARCHAR(511) NOT NULL,
    payload_type    VARCHAR(511) NOT NULL,
    payload         BYTEA        NOT NULL,
    idempotence_key VARCHAR(511),
    headers         JSONB,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER               DEFAULT 0,
    next_attempt_at TIMESTAMP             DEFAULT NOW(),
    fail_type       VARCHAR(50),
    fail_reason     TEXT,
    created_at      TIMESTAMP             DEFAULT NULL,
    processed_at    TIMESTAMP,
    CONSTRAINT chk_outbox_event_status CHECK (status IN ('PENDING', 'RETRY', 'PROCESSED', 'FAILED')),
    CONSTRAINT chk_outbox_event_fail_type CHECK (fail_type IN ('TRANSIENT', 'PERMANENT'))
);

CREATE INDEX idx_outbox_event_status ON outbox_event (status);
CREATE INDEX idx_outbox_event_next_attempt ON outbox_event (status, next_attempt_at) WHERE status IN ('PENDING', 'RETRY');

COMMENT ON TABLE outbox_event IS 'Transactional outbox holding events that are published to downstream consumers.';
COMMENT ON COLUMN outbox_event.id IS 'Unique identifier of the outbox event, also used as the event identifier by consumers.';
COMMENT ON COLUMN outbox_event.reference_id IS 'Identifier of the aggregate the event was raised for.';
COMMENT ON COLUMN outbox_event.destination IS 'The logical destination the event is published to.';
COMMENT ON COLUMN outbox_event.payload_type IS 'The type of the serialised payload, used by the relay to deserialise it.';
COMMENT ON COLUMN outbox_event.payload IS 'The serialised event delivered to consumers.';
COMMENT ON COLUMN outbox_event.idempotence_key IS 'Business key consumers can use to discard duplicates.';
COMMENT ON COLUMN outbox_event.headers IS 'Metadata describing the event without deserialising the payload.';
COMMENT ON COLUMN outbox_event.status IS 'Whether the event is awaiting publication, awaiting a retry, processed or permanently failed.';
COMMENT ON COLUMN outbox_event.attempts IS 'The number of failed publication attempts for this event.';
COMMENT ON COLUMN outbox_event.next_attempt_at IS 'The earliest time the event may be published again.';
COMMENT ON COLUMN outbox_event.fail_type IS 'Whether the last failure was transient and retryable, or permanent.';
COMMENT ON COLUMN outbox_event.fail_reason IS 'The error reported by the most recent failed publication attempt.';
COMMENT ON COLUMN outbox_event.created_at IS 'Timestamp when the event was recorded alongside the aggregate change.';
COMMENT ON COLUMN outbox_event.processed_at IS 'Timestamp when the event was successfully published.';
