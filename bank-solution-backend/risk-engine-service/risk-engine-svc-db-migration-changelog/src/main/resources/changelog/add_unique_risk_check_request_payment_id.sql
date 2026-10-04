CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_check_request_payment_id ON risk_check_request (payment_id);

DROP INDEX IF EXISTS idx_risk_check_request_payment_id;
