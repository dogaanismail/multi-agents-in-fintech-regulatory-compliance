-- Lifecycle Timestamps
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS fraud_check_approved_at TIMESTAMP;
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS manual_review_requested_at TIMESTAMP;
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS manual_review_approved_at TIMESTAMP;
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS manual_review_rejected_at TIMESTAMP;
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS account_charge_initiated_at TIMESTAMP;
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS account_charged_at TIMESTAMP;
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS account_charge_failed_at TIMESTAMP;

-- Decision Metadata
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS manual_reviewed_by VARCHAR(255);
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS manual_review_notes VARCHAR(2000);
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS block_reason VARCHAR(1000);
ALTER TABLE payment_history ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000);
