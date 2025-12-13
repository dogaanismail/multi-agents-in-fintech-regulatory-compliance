-- Create payment_history table for comprehensive payment audit trail
-- Stores complete payment lifecycle including risk assessments and MARL agent observations
CREATE TABLE payment_history
(
    payment_id               UUID PRIMARY KEY,
    reference_number         VARCHAR(255) NOT NULL UNIQUE,
    
    -- Payment Details
    customer_id              UUID         NOT NULL,
    source_account_id        UUID         NOT NULL,
    destination_account_id   UUID         NOT NULL,
    amount                   DECIMAL(19, 2) NOT NULL,
    currency                 VARCHAR(3)   NOT NULL,
    payment_type             VARCHAR(50),
    description              VARCHAR(1000),
    
    -- Status Tracking
    status                   VARCHAR(50)  NOT NULL,
    fraud_status             VARCHAR(50),
    
    -- Risk Assessment
    risk_score               DOUBLE PRECISION,
    risk_level               VARCHAR(20),
    risk_action              VARCHAR(20),
    fraud_indicators         JSONB,
    
    -- MARL Assessment (stored as JSONB for complex nested structure)
    marl_assessment          JSONB,
    
    -- Lifecycle Timestamps
    initiated_at             TIMESTAMP,
    risk_check_requested_at  TIMESTAMP,
    risk_check_completed_at  TIMESTAMP,
    completed_at             TIMESTAMP,
    blocked_at               TIMESTAMP,
    
    -- Processing Metadata
    risk_processing_time_ms  BIGINT,
    marl_processing_time_ms  BIGINT,
    ml_model_version         VARCHAR(50),
    aggregate_version        INTEGER,
    entity_version           SMALLINT     NOT NULL DEFAULT 0,
    
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_payment_history_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_history_currency CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP', 'JPY', 'CHF')),
    CONSTRAINT chk_payment_history_status CHECK (status IN ('PENDING', 'INITIATED', 'RISK_CHECK_PENDING', 
                                                              'FRAUD_CHECK_PENDING', 'APPROVED', 'COMPLETED', 
                                                              'BLOCKED', 'REJECTED', 'FAILED')),
    CONSTRAINT chk_payment_history_fraud_status CHECK (fraud_status IN ('PENDING', 'APPROVED', 'REJECTED', 'UNDER_REVIEW')),
    CONSTRAINT chk_payment_history_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_payment_history_risk_action CHECK (risk_action IN ('APPROVE', 'REVIEW', 'REJECT'))
);

-- Create indexes for frequent query patterns
CREATE INDEX idx_payment_history_customer_id ON payment_history (customer_id);
CREATE INDEX idx_payment_history_reference_number ON payment_history (reference_number);
CREATE INDEX idx_payment_history_status ON payment_history (status);
CREATE INDEX idx_payment_history_fraud_status ON payment_history (fraud_status);
CREATE INDEX idx_payment_history_risk_level ON payment_history (risk_level);
CREATE INDEX idx_payment_history_created_at ON payment_history (created_at DESC);
CREATE INDEX idx_payment_history_initiated_at ON payment_history (initiated_at DESC);

-- Composite indexes for common query combinations
CREATE INDEX idx_payment_history_customer_status ON payment_history (customer_id, status);
CREATE INDEX idx_payment_history_customer_created ON payment_history (customer_id, created_at DESC);
CREATE INDEX idx_payment_history_status_created ON payment_history (status, created_at DESC);

-- GIN index for JSONB columns (enables fast searching within JSON data)
CREATE INDEX idx_payment_history_fraud_indicators_gin ON payment_history USING GIN (fraud_indicators);
CREATE INDEX idx_payment_history_marl_assessment_gin ON payment_history USING GIN (marl_assessment);

-- Add comments for the payment_history table
COMMENT ON TABLE payment_history IS 'Complete payment audit trail with risk assessments and MARL agent observations';
COMMENT ON COLUMN payment_history.payment_id IS 'Primary key - aggregate identifier from event sourcing';
COMMENT ON COLUMN payment_history.reference_number IS 'Unique human-readable payment reference';
COMMENT ON COLUMN payment_history.customer_id IS 'Customer who initiated the payment';
COMMENT ON COLUMN payment_history.source_account_id IS 'Source account for the payment';
COMMENT ON COLUMN payment_history.destination_account_id IS 'Destination account for the payment';
COMMENT ON COLUMN payment_history.amount IS 'Payment amount';
COMMENT ON COLUMN payment_history.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN payment_history.payment_type IS 'Type of payment operation';
COMMENT ON COLUMN payment_history.status IS 'Current payment status';
COMMENT ON COLUMN payment_history.fraud_status IS 'Fraud check status';
COMMENT ON COLUMN payment_history.risk_score IS 'Risk assessment score (0-1)';
COMMENT ON COLUMN payment_history.risk_level IS 'Risk level classification';
COMMENT ON COLUMN payment_history.risk_action IS 'Recommended action based on risk';
COMMENT ON COLUMN payment_history.fraud_indicators IS 'JSON array of detected fraud indicators';
COMMENT ON COLUMN payment_history.marl_assessment IS 'Complete MARL orchestrator assessment with agent observations (JSONB)';
COMMENT ON COLUMN payment_history.initiated_at IS 'Timestamp when payment was initiated';
COMMENT ON COLUMN payment_history.risk_check_requested_at IS 'Timestamp when risk check was requested';
COMMENT ON COLUMN payment_history.risk_check_completed_at IS 'Timestamp when risk check completed';
COMMENT ON COLUMN payment_history.completed_at IS 'Timestamp when payment was completed';
COMMENT ON COLUMN payment_history.blocked_at IS 'Timestamp when payment was blocked';
COMMENT ON COLUMN payment_history.risk_processing_time_ms IS 'Risk engine processing time in milliseconds';
COMMENT ON COLUMN payment_history.marl_processing_time_ms IS 'MARL orchestrator processing time in milliseconds';
COMMENT ON COLUMN payment_history.ml_model_version IS 'ML model version used for risk assessment';
COMMENT ON COLUMN payment_history.aggregate_version IS 'Event version from payment aggregate (event sourcing)';
COMMENT ON COLUMN payment_history.entity_version IS 'JPA optimistic locking version (auto-incremented on update)';
COMMENT ON COLUMN payment_history.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN payment_history.updated_at IS 'Record last update timestamp';
