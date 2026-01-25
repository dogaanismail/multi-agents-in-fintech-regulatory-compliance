-- Alter payment_history table constraints to match updated domain values

-- Drop existing constraints
ALTER TABLE payment_history DROP CONSTRAINT IF EXISTS chk_payment_history_currency;
ALTER TABLE payment_history DROP CONSTRAINT IF EXISTS chk_payment_history_status;
ALTER TABLE payment_history DROP CONSTRAINT IF EXISTS chk_payment_history_fraud_status;
ALTER TABLE payment_history DROP CONSTRAINT IF EXISTS chk_payment_history_risk_level;
ALTER TABLE payment_history DROP CONSTRAINT IF EXISTS chk_payment_history_risk_action;

-- Recreate constraints with updated values
ALTER TABLE payment_history 
    ADD CONSTRAINT chk_payment_history_currency CHECK (currency IN ('AED', 'ALL', 'CHF', 'EUR', 'GBP', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY', 'USD'));

ALTER TABLE payment_history 
    ADD CONSTRAINT chk_payment_history_status CHECK (status IN ('PENDING', 'INITIATED', 'RISK_CHECK_PENDING', 
                                                                 'FRAUD_CHECK_PENDING', 'APPROVED', 'COMPLETED', 
                                                                 'BLOCKED', 'REJECTED', 'FAILED'));

ALTER TABLE payment_history 
    ADD CONSTRAINT chk_payment_history_fraud_status CHECK (fraud_status IN ('PENDING', 'APPROVED', 'BLOCKED', 'REVIEW_REQUIRED', 'FAILED'));

ALTER TABLE payment_history 
    ADD CONSTRAINT chk_payment_history_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

ALTER TABLE payment_history 
    ADD CONSTRAINT chk_payment_history_risk_action CHECK (risk_action IN ('PROCEED', 'ESCALATE', 'BLOCK'));
