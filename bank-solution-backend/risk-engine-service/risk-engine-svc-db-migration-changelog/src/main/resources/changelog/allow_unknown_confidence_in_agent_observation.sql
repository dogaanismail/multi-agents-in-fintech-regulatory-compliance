-- The orchestrator reports confidence UNKNOWN when it substitutes a fallback observation for
-- an unreachable or failing agent; the original check constraint rejected it, which rolled
-- back the whole fraud-analysis-completed transaction after the verdict had been published.
ALTER TABLE agent_observation
    DROP CONSTRAINT chk_agent_observation_confidence;

ALTER TABLE agent_observation
    ADD CONSTRAINT chk_agent_observation_confidence
        CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL', 'UNKNOWN'));
