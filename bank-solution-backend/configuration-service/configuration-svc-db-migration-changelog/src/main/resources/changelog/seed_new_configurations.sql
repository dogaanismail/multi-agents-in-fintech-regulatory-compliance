INSERT INTO system_config (id, config_key, config_value, config_type, category, description, default_value)
VALUES
    -- Agent trust weights: compliance officer can skew the MADDPG state composition toward
    -- a specific detection agent if one proves more reliable for a given fraud typology.
    (gen_random_uuid(), 'AGENT_WEIGHT_TRANSACTION',       '0.333', 'FLOAT',   'AGENT_BEHAVIOR',    'Trust weight applied to the Transaction Pattern Agent observation when composing the MADDPG state vector (0–1, all three weights should sum to ~1.0)', '0.333'),
    (gen_random_uuid(), 'AGENT_WEIGHT_CUSTOMER',          '0.333', 'FLOAT',   'AGENT_BEHAVIOR',    'Trust weight applied to the Customer Risk Agent observation when composing the MADDPG state vector',                                                   '0.333'),
    (gen_random_uuid(), 'AGENT_WEIGHT_NETWORK',           '0.334', 'FLOAT',   'AGENT_BEHAVIOR',    'Trust weight applied to the Network Analysis Agent observation when composing the MADDPG state vector',                                               '0.334'),

    -- Compliance freeze: pause MADDPG learning during regulatory audits without a container restart.
    (gen_random_uuid(), 'FREEZE_TRAINING',                'false', 'BOOLEAN', 'OFFLINE_RETRAINING','When true the offline batch retraining cycle is skipped entirely — use during compliance audits to preserve current model weights',                   'false'),

    -- Fallback risk: score used when a detection agent is unreachable so the orchestrator never
    -- defaults to "no risk" silently.
    (gen_random_uuid(), 'FALLBACK_AGENT_RISK_SCORE',      '50.0',  'FLOAT',   'AGENT_BEHAVIOR',    'Risk score (0–100) substituted when a detection agent is unreachable; 50 = neutral/uncertain, 0 = treat as safe, 100 = treat as high-risk',          '50.0'),

    -- Replay buffer retention: old experiences dilute current fraud patterns; evict periodically.
    (gen_random_uuid(), 'EXPERIENCE_RETENTION_DAYS',      '90',    'INTEGER', 'OFFLINE_RETRAINING','Days to retain replay buffer experiences before automated eviction; set to 0 to keep experiences indefinitely',                                       '90'),

    -- Config cache TTL: how often marl_orchestrator polls configuration-service for updates.
    (gen_random_uuid(), 'CONFIG_REFRESH_INTERVAL_SECONDS','300',   'INTEGER', 'OFFLINE_RETRAINING','Seconds between marl_orchestrator dynamic-config cache refreshes from configuration-service; lower values increase responsiveness but add HTTP load', '300');
