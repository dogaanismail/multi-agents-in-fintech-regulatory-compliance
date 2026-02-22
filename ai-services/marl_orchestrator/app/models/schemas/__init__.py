"""
Schemas package.

Organised by domain:
  inference_schemas  — agent observations, decisions, health, model info
  training_schemas   — offline training status, trigger, history, buffer stats

All symbols are re-exported here so existing code that does
`from app.models.schemas import SomeModel` continues to work.
"""

from .inference_schemas import (
    ActionType,
    AgentObservation,
    CoordinatedDecisionRequest,
    CoordinatedDecisionResponse,
    CustomerFeatures,
    EnrichedTransactionEvent,
    HealthResponse,
    ModelInfoResponse,
    NetworkFeatures,
    TransactionFeatures,
)
from .training_schemas import (
    BufferStatsResponse,
    TrainingRunResponse,
    TrainingStatusResponse,
    TriggerTrainingResponse,
)

__all__ = [
    # Inference
    "ActionType",
    "AgentObservation",
    "CoordinatedDecisionRequest",
    "CoordinatedDecisionResponse",
    "CustomerFeatures",
    "EnrichedTransactionEvent",
    "HealthResponse",
    "ModelInfoResponse",
    "NetworkFeatures",
    "TransactionFeatures",
    # Training
    "BufferStatsResponse",
    "TrainingRunResponse",
    "TrainingStatusResponse",
    "TriggerTrainingResponse",
]
