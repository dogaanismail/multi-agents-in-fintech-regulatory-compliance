-- Create a risk_assessment table
CREATE TABLE risk_assessment
(
    id                     UUID PRIMARY KEY,
    risk_check_request_id  UUID            NOT NULL UNIQUE,
    risk_score             DECIMAL(5, 4),
    risk_level             VARCHAR(20)     NOT NULL,
    risk_action            VARCHAR(20)     NOT NULL,
    fraud_indicators       TEXT[],
    ml_model_version       VARCHAR(50),
    processing_time_ms     BIGINT,
    assessed_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_assessment_request FOREIGN KEY (risk_check_request_id) REFERENCES risk_check_request (id),
    CONSTRAINT chk_risk_assessment_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_risk_assessment_action CHECK (risk_action IN ('PROCEED', 'ESCALATE', 'BLOCK'))
);

-- Create indexes
CREATE INDEX idx_risk_assessment_request_id ON risk_assessment (risk_check_request_id);
CREATE INDEX idx_risk_assessment_risk_level ON risk_assessment (risk_level);
CREATE INDEX idx_risk_assessment_risk_action ON risk_assessment (risk_action);
CREATE INDEX idx_risk_assessment_assessed_at ON risk_assessment (assessed_at);

-- Add comments
COMMENT ON TABLE risk_assessment IS 'Stores risk assessment results derived from MARL analysis';
COMMENT ON COLUMN risk_assessment.id IS 'Unique identifier for risk assessment';
COMMENT ON COLUMN risk_assessment.risk_check_request_id IS 'Reference to original risk check request';
COMMENT ON COLUMN risk_assessment.risk_score IS 'Overall risk score (0.0 to 1.0)';
COMMENT ON COLUMN risk_assessment.risk_level IS 'Categorized risk level';
COMMENT ON COLUMN risk_assessment.risk_action IS 'Recommended action based on assessment';
COMMENT ON COLUMN risk_assessment.fraud_indicators IS 'List of detected fraud indicators';
COMMENT ON COLUMN risk_assessment.ml_model_version IS 'Version of ML model used';
COMMENT ON COLUMN risk_assessment.processing_time_ms IS 'Total processing time in milliseconds';
COMMENT ON COLUMN risk_assessment.assessed_at IS 'Timestamp when assessment was completed';