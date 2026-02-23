CREATE TABLE system_config_audit_log
(
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id   UUID         NOT NULL,
    config_key  VARCHAR(100) NOT NULL,
    old_value   VARCHAR(500),
    new_value   VARCHAR(500),
    change_type VARCHAR(20)  NOT NULL,
    changed_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_change_type CHECK (change_type IN ('CREATED', 'UPDATED', 'DELETED'))
);

CREATE INDEX idx_audit_log_config_id  ON system_config_audit_log (config_id);
CREATE INDEX idx_audit_log_config_key ON system_config_audit_log (config_key);
CREATE INDEX idx_audit_log_created_at ON system_config_audit_log (created_at);

COMMENT ON TABLE system_config_audit_log IS 'Immutable audit trail for all changes to system_config — required for regulatory compliance';
COMMENT ON COLUMN system_config_audit_log.id          IS 'Unique audit record identifier';
COMMENT ON COLUMN system_config_audit_log.config_id   IS 'FK to system_config.id at time of change';
COMMENT ON COLUMN system_config_audit_log.config_key  IS 'Denormalised key for fast lookups without joining system_config';
COMMENT ON COLUMN system_config_audit_log.old_value   IS 'Value before the change; NULL for CREATED events';
COMMENT ON COLUMN system_config_audit_log.new_value   IS 'Value after the change; NULL for DELETED events';
COMMENT ON COLUMN system_config_audit_log.change_type IS 'CREATED | UPDATED | DELETED';
COMMENT ON COLUMN system_config_audit_log.changed_by  IS 'Actor who performed the change (hardcoded SYSTEM until auth is implemented)';
COMMENT ON COLUMN system_config_audit_log.created_at  IS 'UTC timestamp of the change';
