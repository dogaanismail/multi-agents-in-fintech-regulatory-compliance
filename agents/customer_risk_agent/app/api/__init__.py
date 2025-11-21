"""API routes initialization"""

from fastapi import APIRouter
from . import health, model, predictions

# Create main API router
api_router = APIRouter()

# Include sub-routers
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(model.router, tags=["Model"])
api_router.include_router(predictions.router, tags=["Prediction"])

__all__ = ["api_router"]
