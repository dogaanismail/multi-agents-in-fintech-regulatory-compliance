INSERT INTO system_config (id, config_key, config_value, config_type, category, description, default_value)
VALUES
    (gen_random_uuid(), 'TRAINING_INTERVAL_SECONDS',       '300',              'INTEGER', 'OFFLINE_RETRAINING', 'Interval in seconds between offline batch retraining cycles',                             '300'),
    (gen_random_uuid(), 'MIN_EXPERIENCES_FOR_TRAINING',    '1',               'INTEGER', 'OFFLINE_RETRAINING', 'Minimum number of experience samples required before a retraining batch can be triggered', '1'),
    (gen_random_uuid(), 'TRAINING_BATCH_SIZE',             '64',               'INTEGER', 'OFFLINE_RETRAINING', 'Number of experiences sampled per retraining batch',                                       '64'),
    (gen_random_uuid(), 'MAX_EXPERIENCES_PER_BATCH',       '1000',             'INTEGER', 'OFFLINE_RETRAINING', 'Maximum number of experiences that can be included in a single retraining batch',          '1000'),
    (gen_random_uuid(), 'SAVE_MODEL_AFTER_TRAINING',       'true',             'BOOLEAN', 'OFFLINE_RETRAINING', 'Whether to persist the trained MADDPG model weights after each retraining cycle',          'true'),

    (gen_random_uuid(), 'REWARD_AUTO_HIGH_RISK_BLOCK',     '0.3',              'FLOAT',   'AUTO_REWARD',        'Automated reward given when agents block a transaction assessed as high-risk',               '0.3'),
    (gen_random_uuid(), 'REWARD_AUTO_LOW_RISK_ALLOW',      '0.3',              'FLOAT',   'AUTO_REWARD',        'Automated reward given when agents allow a transaction assessed as low-risk',                '0.3'),
    (gen_random_uuid(), 'REWARD_AUTO_CONFLICT',            '-0.3',             'FLOAT',   'AUTO_REWARD',        'Automated penalty applied when agent decisions are in conflict with each other',             '-0.3'),
    (gen_random_uuid(), 'REWARD_AUTO_RISK_THRESHOLD',      '0.5',              'FLOAT',   'AUTO_REWARD',        'Risk score boundary separating high-risk from low-risk for automated reward calculation',    '0.5'),

    (gen_random_uuid(), 'REWARD_MANUAL_CORRECT_BLOCK',     '1.0',              'FLOAT',   'MANUAL_REWARD',      'Reward assigned when a compliance officer confirms an agent block decision was correct',      '1.0'),
    (gen_random_uuid(), 'REWARD_MANUAL_CORRECT_ALLOW',     '0.5',              'FLOAT',   'MANUAL_REWARD',      'Reward assigned when a compliance officer confirms an agent allow decision was correct',      '0.5'),
    (gen_random_uuid(), 'REWARD_MANUAL_WRONG_BLOCK',       '-0.5',             'FLOAT',   'MANUAL_REWARD',      'Penalty assigned when a compliance officer overrides an incorrect agent block decision',      '-0.5'),
    (gen_random_uuid(), 'REWARD_MANUAL_WRONG_ALLOW',       '-1.0',             'FLOAT',   'MANUAL_REWARD',      'Penalty assigned when a compliance officer overrides an incorrect agent allow decision',      '-1.0'),
    (gen_random_uuid(), 'REWARD_MANUAL_WEIGHT_MULTIPLIER', '2.0',              'FLOAT',   'MANUAL_REWARD',      'Multiplier applied to manual review rewards to emphasise verified expert judgement over automated rewards', '2.0'),

    (gen_random_uuid(), 'REWARD_ESCALATION_MODE',          'final_decision',   'STRING',  'ESCALATION',         'Determines how agents are rewarded for escalation: none | positive | final_decision',       'final_decision'),
    (gen_random_uuid(), 'REWARD_ESCALATION_POSITIVE',      '0.3',              'FLOAT',   'ESCALATION',         'Small positive reward given to agents for correctly identifying edge cases needing escalation', '0.3'),
    (gen_random_uuid(), 'REWARD_USE_CONFIDENCE_WEIGHTING', 'true',             'BOOLEAN', 'ESCALATION',         'Whether agent rewards are weighted by individual confidence scores',                          'true'),
    (gen_random_uuid(), 'ESCALATION_CONFIDENCE_THRESHOLD', '0.6',              'FLOAT',   'ESCALATION',         'Confidence score below which agents automatically escalate a decision to manual review',      '0.6');
