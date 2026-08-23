ALTER TABLE agent_observation
    ADD COLUMN feature_contributions JSONB,
    ADD COLUMN shap_base_value       DECIMAL(12, 6);

COMMENT ON COLUMN agent_observation.feature_contributions IS 'Top SHAP feature contributions for this agent''s score; null when explainability was unavailable';
COMMENT ON COLUMN agent_observation.shap_base_value IS 'SHAP expected value the contributions are relative to';
