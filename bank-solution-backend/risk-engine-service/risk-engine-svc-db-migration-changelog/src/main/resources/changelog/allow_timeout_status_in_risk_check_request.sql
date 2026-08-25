-- RiskCheckStatus has a TIMEOUT value the original check constraint did not allow,
-- so writing it would have been rejected by Postgres.
ALTER TABLE risk_check_request
    DROP CONSTRAINT chk_risk_check_request_status;

ALTER TABLE risk_check_request
    ADD CONSTRAINT chk_risk_check_request_status
        CHECK (status IN ('AWAITING_MARL', 'COMPLETED', 'FAILED', 'TIMEOUT'));
