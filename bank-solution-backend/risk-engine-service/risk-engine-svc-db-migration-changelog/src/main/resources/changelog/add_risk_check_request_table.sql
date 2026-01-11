-- Create a risk_check_request table
CREATE TABLE risk_check_request
(
    id                     UUID PRIMARY KEY,
    payment_id             VARCHAR(255)    NOT NULL,
    customer_id            VARCHAR(255)    NOT NULL,
    source_account_id      VARCHAR(255),
    destination_account_id VARCHAR(255),
    amount                 DECIMAL(19, 4)  NOT NULL,
    currency               VARCHAR(3)      NOT NULL,
    payment_type           VARCHAR(50)     NOT NULL,
    description            TEXT,
    request_timestamp      BIGINT          NOT NULL,
    status                 VARCHAR(30)     NOT NULL DEFAULT 'AWAITING_MARL',
    created_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_risk_check_request_currency CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP', 'JPY', 'CHF')),
    CONSTRAINT chk_risk_check_request_payment_type CHECK (payment_type IN ('TRANSFER_IN', 'TRANSFER_OUT', 'DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT chk_risk_check_request_status CHECK (status IN ('AWAITING_MARL', 'COMPLETED', 'FAILED'))
);

-- Create indexes
CREATE INDEX idx_risk_check_request_payment_id ON risk_check_request (payment_id);
CREATE INDEX idx_risk_check_request_customer_id ON risk_check_request (customer_id);
CREATE INDEX idx_risk_check_request_status ON risk_check_request (status);
CREATE INDEX idx_risk_check_request_created_at ON risk_check_request (created_at);

-- Add comments
COMMENT ON TABLE risk_check_request IS 'Stores risk check requests received from payment engine';
COMMENT ON COLUMN risk_check_request.id IS 'Unique identifier for risk check request';
COMMENT ON COLUMN risk_check_request.payment_id IS 'Payment identifier from payment engine';
COMMENT ON COLUMN risk_check_request.customer_id IS 'Customer identifier';
COMMENT ON COLUMN risk_check_request.source_account_id IS 'Source account for transfers/withdrawals';
COMMENT ON COLUMN risk_check_request.destination_account_id IS 'Destination account for transfers/deposits';
COMMENT ON COLUMN risk_check_request.amount IS 'Transaction amount';
COMMENT ON COLUMN risk_check_request.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN risk_check_request.payment_type IS 'Type of payment operation';
COMMENT ON COLUMN risk_check_request.description IS 'Optional payment description';
COMMENT ON COLUMN risk_check_request.request_timestamp IS 'Original request timestamp (epoch millis)';
COMMENT ON COLUMN risk_check_request.status IS 'Current status of risk check';
COMMENT ON COLUMN risk_check_request.created_at IS 'Timestamp when record was created';
COMMENT ON COLUMN risk_check_request.updated_at IS 'Timestamp when record was last updated';
COMMENT ON COLUMN risk_check_request.version IS 'Optimistic locking version';