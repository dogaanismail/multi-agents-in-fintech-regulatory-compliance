INSERT INTO system_config (id, config_key, config_value, config_type, category, description, default_value)
VALUES
    (gen_random_uuid(), 'REWARD_OVERRIDE_BLOCK_TO_APPROVE', '-0.9',  'FLOAT', 'MANUAL_REWARD', 'Penalty applied when a compliance officer overrides a BLOCK decision and approves the payment (agent was over-blocking)', '-0.9'),
    (gen_random_uuid(), 'REWARD_OVERRIDE_ALLOW_TO_REJECT',  '-1.2',  'FLOAT', 'MANUAL_REWARD', 'Penalty applied when a compliance officer overrides an ALLOW/COMPLETED decision and rejects the payment (agent missed fraud)', '-1.2'),
    (gen_random_uuid(), 'REWARD_OVERRIDE_MULTIPLIER',       '3.0',   'FLOAT', 'MANUAL_REWARD', 'Multiplier applied to override penalties to emphasise the severity of a terminal decision being reversed by a compliance officer', '3.0');
