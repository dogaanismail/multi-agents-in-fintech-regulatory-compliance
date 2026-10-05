"""
API package initialization
"""

from fastapi import APIRouter, Depends

from app.core.security import MARL_READ, require_permission
from . import health, inference, learning, review_queue, training

# Create main API router
api_router = APIRouter()

# Include sub-routers
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(inference.router, tags=["Inference"])
api_router.include_router(training.router, tags=["Training"])
api_router.include_router(
    learning.router, tags=["Learning Evidence"], dependencies=[Depends(require_permission(MARL_READ))])
api_router.include_router(
    review_queue.router, tags=["Review Queue"], dependencies=[Depends(require_permission(MARL_READ))])

__all__ = ["api_router"]
