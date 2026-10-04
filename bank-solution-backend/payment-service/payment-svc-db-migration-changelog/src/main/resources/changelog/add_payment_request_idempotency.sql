ALTER TABLE payment_request
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE payment_request
    ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_request_customer_idempotency_key
    ON payment_request (customer_id, idempotency_key);

COMMENT ON COLUMN payment_request.idempotency_key IS 'Client Idempotency-Key the payment id was derived from; null for payments created before idempotency keys';
COMMENT ON COLUMN payment_request.request_fingerprint IS 'SHA-256 of the canonical request, so a reused key with a different body is rejected instead of replayed';
