-- Create a payment_request table
CREATE TABLE payment_request
(
    id                     UUID PRIMARY KEY,
    customer_id            UUID           NOT NULL,
    source_account_id      UUID,
    destination_account_id UUID,
    amount                 DECIMAL(19, 2) NOT NULL,
    currency               VARCHAR(3)     NOT NULL,
    payment_type           VARCHAR(50)    NOT NULL,
    description            VARCHAR(500),
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_currency CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP', 'JPY', 'CHF')),
    CONSTRAINT chk_payment_type CHECK (payment_type IN ('TRANSFER_IN', 'TRANSFER_OUT', 'DEPOSIT', 'WITHDRAWAL'))
);

-- Create indexes
CREATE INDEX idx_payment_request_customer_id ON payment_request (customer_id);
CREATE INDEX idx_payment_request_source_account ON payment_request (source_account_id);
CREATE INDEX idx_payment_request_destination_account ON payment_request (destination_account_id);

-- Add comments
COMMENT ON TABLE payment_request IS 'Stores immutable payment requests (event sourcing pattern - no status field)';
COMMENT ON COLUMN payment_request.id IS 'Unique identifier for payment request';
COMMENT ON COLUMN payment_request.customer_id IS 'Customer who initiated the payment';
COMMENT ON COLUMN payment_request.source_account_id IS 'Source account for TRANSFER_OUT/WITHDRAWAL';
COMMENT ON COLUMN payment_request.destination_account_id IS 'Destination account for TRANSFER_IN/DEPOSIT';
COMMENT ON COLUMN payment_request.amount IS 'Payment amount (must be positive)';
COMMENT ON COLUMN payment_request.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN payment_request.payment_type IS 'Type of payment operation';
COMMENT ON COLUMN payment_request.description IS 'Optional payment description';
COMMENT ON COLUMN payment_request.created_at IS 'Timestamp when request was created';
COMMENT ON COLUMN payment_request.updated_at IS 'Timestamp when record was last updated';
COMMENT ON COLUMN payment_request.version IS 'Optimistic locking version';

