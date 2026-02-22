CREATE TABLE system_config
(
    id             UUID         PRIMARY KEY,
    config_key     VARCHAR(100) NOT NULL UNIQUE,
    config_value   VARCHAR(500) NOT NULL,
    config_type    VARCHAR(50)  NOT NULL DEFAULT 'STRING',
    category       VARCHAR(50)  NOT NULL,
    description    VARCHAR(500),
    default_value  VARCHAR(500) NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    deleted_reason VARCHAR(500),
    version        INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT chk_config_type CHECK (config_type IN ('STRING', 'FLOAT', 'INTEGER', 'BOOLEAN')),
    CONSTRAINT chk_category CHECK (category IN ('OFFLINE_RETRAINING', 'AUTO_REWARD', 'MANUAL_REWARD', 'ESCALATION'))
);

CREATE INDEX idx_system_config_config_key ON system_config (config_key);
CREATE INDEX idx_system_config_category ON system_config (category);
CREATE INDEX idx_system_config_created_at ON system_config (created_at);

COMMENT ON TABLE system_config IS 'Stores configurable parameters for MARL reward and offline retraining';
COMMENT ON COLUMN system_config.id IS 'Unique identifier for the configuration entry';
COMMENT ON COLUMN system_config.config_key IS 'Unique key identifier for the configuration';
COMMENT ON COLUMN system_config.config_value IS 'Current value of the configuration';
COMMENT ON COLUMN system_config.config_type IS 'Data type: STRING, FLOAT, INTEGER, BOOLEAN';
COMMENT ON COLUMN system_config.category IS 'Logical category of the configuration';
COMMENT ON COLUMN system_config.description IS 'Human-readable description of the configuration';
COMMENT ON COLUMN system_config.default_value IS 'Factory default value for the configuration';
COMMENT ON COLUMN system_config.created_at IS 'Timestamp when the record was created';
COMMENT ON COLUMN system_config.updated_at IS 'Timestamp when the record was last updated';
COMMENT ON COLUMN system_config.deleted_at IS 'Timestamp when the record was soft deleted';
COMMENT ON COLUMN system_config.deleted_reason IS 'Reason for soft deletion';
COMMENT ON COLUMN system_config.version IS 'Optimistic locking version';
