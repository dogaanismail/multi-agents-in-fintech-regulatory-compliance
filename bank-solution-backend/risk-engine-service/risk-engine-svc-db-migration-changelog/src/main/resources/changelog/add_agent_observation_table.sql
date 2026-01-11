-- Create agent_observation table
CREATE TABLE agent_observation
(
    id                  UUID PRIMARY KEY,
    marl_assessment_id  UUID            NOT NULL,
    agent_name          VARCHAR(50)     NOT NULL,
    agent_type          VARCHAR(30)     NOT NULL,
    is_suspicious       BOOLEAN         NOT NULL,
    probability         DECIMAL(5, 4)   NOT NULL,
    risk_score          DECIMAL(6, 2)   NOT NULL,
    confidence          VARCHAR(20)     NOT NULL,
    response_time_ms    DECIMAL(10, 2)  NOT NULL,
    contribution        DECIMAL(5, 4),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_observation_marl FOREIGN KEY (marl_assessment_id) REFERENCES marl_assessment (id),
    CONSTRAINT chk_agent_observation_type CHECK (agent_type IN ('TRANSACTION', 'CUSTOMER', 'NETWORK')),
    CONSTRAINT chk_agent_observation_confidence CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Create indexes
CREATE INDEX idx_agent_observation_marl_id ON agent_observation (marl_assessment_id);
CREATE INDEX idx_agent_observation_agent_type ON agent_observation (agent_type);
CREATE INDEX idx_agent_observation_is_suspicious ON agent_observation (is_suspicious);

-- Add comments
COMMENT ON TABLE agent_observation IS 'Stores individual agent observations from MARL system';
COMMENT ON COLUMN agent_observation.id IS 'Unique identifier for agent observation';
COMMENT ON COLUMN agent_observation.marl_assessment_id IS 'Reference to parent MARL assessment';
COMMENT ON COLUMN agent_observation.agent_name IS 'Name of the agent';
COMMENT ON COLUMN agent_observation.agent_type IS 'Type of agent (TRANSACTION/CUSTOMER/NETWORK)';
COMMENT ON COLUMN agent_observation.is_suspicious IS 'Binary fraud classification';
COMMENT ON COLUMN agent_observation.probability IS 'Fraud probability (0.0 to 1.0)';
COMMENT ON COLUMN agent_observation.risk_score IS 'Risk score (0-100 scale)';
COMMENT ON COLUMN agent_observation.confidence IS 'Confidence level of the agent';
COMMENT ON COLUMN agent_observation.response_time_ms IS 'Agent response time in milliseconds';
COMMENT ON COLUMN agent_observation.contribution IS 'Agent contribution to final decision';
COMMENT ON COLUMN agent_observation.created_at IS 'Timestamp when observation was recorded';