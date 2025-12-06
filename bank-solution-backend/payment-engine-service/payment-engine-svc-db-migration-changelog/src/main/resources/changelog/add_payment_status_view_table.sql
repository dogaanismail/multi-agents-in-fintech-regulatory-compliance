-- Create payment_status_view table for CQRS read model
-- This is the read-optimized projection table updated by event handlers
CREATE TABLE payment_status_view
(
    payment_id              UUID PRIMARY KEY,
    external_payment_id     UUID,
    reference_number        VARCHAR(255) NOT NULL UNIQUE,
    customer_id             UUID         NOT NULL,
    source_account_id       UUID         NOT NULL,
    destination_account_id  UUID         NOT NULL,
    amount                  DECIMAL(19, 2) NOT NULL,
    currency                VARCHAR(3)   NOT NULL,
    payment_type            VARCHAR(50)  NOT NULL,
    description             VARCHAR(500),
    status                  VARCHAR(50)  NOT NULL,
    fraud_status            VARCHAR(50),
    risk_score              DOUBLE PRECISION,
    risk_level              VARCHAR(20),
    risk_action             VARCHAR(20),
    initiated_at            TIMESTAMP,
    risk_check_completed_at TIMESTAMP,
    completed_at            TIMESTAMP,
    blocked_at              TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_currency CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP', 'JPY', 'CHF')),
    CONSTRAINT chk_payment_type CHECK (payment_type IN ('TRANSFER', 'PAYMENT', 'WITHDRAWAL')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'INITIATED', 'RISK_CHECK_PENDING', 
                                             'FRAUD_CHECK_PENDING', 'APPROVED', 'COMPLETED', 
                                             'BLOCKED', 'REJECTED', 'FAILED')),
    CONSTRAINT chk_fraud_status CHECK (fraud_status IN ('PENDING', 'APPROVED', 'REJECTED', 'UNDER_REVIEW')),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_risk_action CHECK (risk_action IN ('APPROVE', 'REVIEW', 'REJECT'))
);

-- Create indexes for frequent query patterns
CREATE INDEX idx_payment_status_view_customer_id ON payment_status_view (customer_id);
CREATE INDEX idx_payment_status_view_reference_number ON payment_status_view (reference_number);
CREATE INDEX idx_payment_status_view_external_payment_id ON payment_status_view (external_payment_id);
CREATE INDEX idx_payment_status_view_status ON payment_status_view (status);
CREATE INDEX idx_payment_status_view_fraud_status ON payment_status_view (fraud_status);
CREATE INDEX idx_payment_status_view_created_at ON payment_status_view (created_at);
CREATE INDEX idx_payment_status_view_initiated_at ON payment_status_view (initiated_at);

-- Create composite indexes for common query combinations
CREATE INDEX idx_payment_status_customer_status ON payment_status_view (customer_id, status);
CREATE INDEX idx_payment_status_customer_date ON payment_status_view (customer_id, initiated_at DESC);

-- Add comments for payment_status_view table
COMMENT ON TABLE payment_status_view IS 'CQRS read model for payment status queries - updated by PaymentProjectionHandler';
COMMENT ON COLUMN payment_status_view.payment_id IS 'Primary key - aggregate identifier from event sourcing';
COMMENT ON COLUMN payment_status_view.external_payment_id IS 'External system payment reference';
COMMENT ON COLUMN payment_status_view.reference_number IS 'Unique human-readable payment reference';
COMMENT ON COLUMN payment_status_view.customer_id IS 'Customer who initiated the payment';
COMMENT ON COLUMN payment_status_view.source_account_id IS 'Source account for the payment';
COMMENT ON COLUMN payment_status_view.destination_account_id IS 'Destination account for the payment';
COMMENT ON COLUMN payment_status_view.amount IS 'Payment amount';
COMMENT ON COLUMN payment_status_view.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN payment_status_view.payment_type IS 'Type of payment operation';
COMMENT ON COLUMN payment_status_view.status IS 'Current payment status from aggregate state';
COMMENT ON COLUMN payment_status_view.fraud_status IS 'Fraud check status';
COMMENT ON COLUMN payment_status_view.risk_score IS 'Risk assessment score (0-1)';
COMMENT ON COLUMN payment_status_view.risk_level IS 'Risk level classification';
COMMENT ON COLUMN payment_status_view.risk_action IS 'Recommended action based on risk';
COMMENT ON COLUMN payment_status_view.initiated_at IS 'Timestamp when payment was initiated';
COMMENT ON COLUMN payment_status_view.risk_check_completed_at IS 'Timestamp when risk check completed';
COMMENT ON COLUMN payment_status_view.completed_at IS 'Timestamp when payment was completed';
COMMENT ON COLUMN payment_status_view.blocked_at IS 'Timestamp when payment was blocked';
COMMENT ON COLUMN payment_status_view.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN payment_status_view.updated_at IS 'Record last update timestamp';
