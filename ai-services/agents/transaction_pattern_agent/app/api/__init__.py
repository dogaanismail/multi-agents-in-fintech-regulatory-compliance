"""API routes initialization"""

from fastapi import APIRouter, Depends
from app.core.security import require_service_caller
from . import health, model, predictions

# Create main API router
api_router = APIRouter()

# Include sub-routers
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(model.router, tags=["Model"], dependencies=[Depends(require_service_caller)])
api_router.include_router(predictions.router, tags=["Prediction"], dependencies=[Depends(require_service_caller)])

__all__ = ["api_router"]
