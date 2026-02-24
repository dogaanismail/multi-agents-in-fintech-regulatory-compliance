-- Add decision override metadata columns to payment_history table
ALTER TABLE payment_history
    ADD COLUMN IF NOT EXISTS decision_overridden_by   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS decision_override_reason VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS decision_overridden_at   TIMESTAMP WITH TIME ZONE;
