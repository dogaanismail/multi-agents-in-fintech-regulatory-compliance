ALTER TABLE payment_history
    DROP COLUMN IF EXISTS account_charge_initiated_at;
ALTER TABLE payment_history
    DROP COLUMN IF EXISTS account_charged_at;
ALTER TABLE payment_history
    DROP COLUMN IF EXISTS account_charge_failed_at;

ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS ledger_authorisation_initiated_at TIMESTAMP;
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS ledger_authorised_at TIMESTAMP;
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS ledger_settlement_initiated_at TIMESTAMP;
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS ledger_settled_at TIMESTAMP;
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS ledger_release_initiated_at TIMESTAMP;
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS ledger_released_at TIMESTAMP;

COMMENT ON COLUMN payment_history.ledger_authorisation_initiated_at IS 'Timestamp when ledger authorisation was initiated';
COMMENT ON COLUMN payment_history.ledger_authorised_at IS 'Timestamp when funds were authorised on the ledger';
COMMENT ON COLUMN payment_history.ledger_settlement_initiated_at IS 'Timestamp when ledger settlement was initiated';
COMMENT ON COLUMN payment_history.ledger_settled_at IS 'Timestamp when funds were settled on the ledger';
COMMENT ON COLUMN payment_history.ledger_release_initiated_at IS 'Timestamp when ledger release was initiated';
COMMENT ON COLUMN payment_history.ledger_released_at IS 'Timestamp when authorised funds were released';
