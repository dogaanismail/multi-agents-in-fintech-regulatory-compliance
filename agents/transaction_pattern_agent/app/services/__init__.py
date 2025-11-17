"""Services module initialization"""

from .model_loader import model_loader, ModelLoader
from .prediction_service import prediction_service, PredictionService

__all__ = [
    "model_loader",
    "ModelLoader",
    "prediction_service",
    "PredictionService"
]
