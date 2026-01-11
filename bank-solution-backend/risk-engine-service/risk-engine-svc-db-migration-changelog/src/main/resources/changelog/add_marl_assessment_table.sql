-- Create a marl_assessment table
CREATE TABLE marl_assessment
(
    id                     UUID PRIMARY KEY,
    risk_check_request_id  UUID            NOT NULL UNIQUE,
    action                 VARCHAR(20)     NOT NULL,
    confidence             DECIMAL(5, 4)   NOT NULL,
    maddpg_q_value         DECIMAL(10, 6)  NOT NULL,
    processing_time_ms     DECIMAL(10, 2)  NOT NULL,
    mode                   VARCHAR(20)     NOT NULL,
    response_timestamp     BIGINT          NOT NULL,
    received_at            TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marl_assessment_request FOREIGN KEY (risk_check_request_id) REFERENCES risk_check_request (id),
    CONSTRAINT chk_marl_assessment_action CHECK (action IN ('ALLOW', 'BLOCK', 'REVIEW')),
    CONSTRAINT chk_marl_assessment_mode CHECK (mode IN ('inference', 'training'))
);

-- Create indexes
CREATE INDEX idx_marl_assessment_request_id ON marl_assessment (risk_check_request_id);
CREATE INDEX idx_marl_assessment_action ON marl_assessment (action);
CREATE INDEX idx_marl_assessment_received_at ON marl_assessment (received_at);

-- Add comments
COMMENT ON TABLE marl_assessment IS 'Stores MARL orchestrator assessment details';
COMMENT ON COLUMN marl_assessment.id IS 'Unique identifier for MARL assessment';
COMMENT ON COLUMN marl_assessment.risk_check_request_id IS 'Reference to original risk check request';
COMMENT ON COLUMN marl_assessment.action IS 'MARL coordinated decision (ALLOW/BLOCK/REVIEW)';
COMMENT ON COLUMN marl_assessment.confidence IS 'Decision confidence level (0.0 to 1.0)';
COMMENT ON COLUMN marl_assessment.maddpg_q_value IS 'Q-value from MADDPG critic';
COMMENT ON COLUMN marl_assessment.processing_time_ms IS 'MARL processing time in milliseconds';
COMMENT ON COLUMN marl_assessment.mode IS 'Execution mode (inference/training)';
COMMENT ON COLUMN marl_assessment.response_timestamp IS 'MARL response timestamp (epoch millis)';
COMMENT ON COLUMN marl_assessment.received_at IS 'Timestamp when response was received';