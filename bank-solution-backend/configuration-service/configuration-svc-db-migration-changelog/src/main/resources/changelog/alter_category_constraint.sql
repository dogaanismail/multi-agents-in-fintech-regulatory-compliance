-- Drop the old CHECK constraint and replace it with one that includes AGENT_BEHAVIOR
ALTER TABLE system_config DROP CONSTRAINT IF EXISTS chk_category;

ALTER TABLE system_config
    ADD CONSTRAINT chk_category
        CHECK (category IN (
            'OFFLINE_RETRAINING',
            'AUTO_REWARD',
            'MANUAL_REWARD',
            'ESCALATION',
            'AGENT_BEHAVIOR'
        ));
