"""
API package initialization
"""

from fastapi import APIRouter
from . import health, inference, learning, review_queue, training

# Create main API router
api_router = APIRouter()

# Include sub-routers
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(inference.router, tags=["Inference"])
api_router.include_router(training.router, tags=["Training"])
api_router.include_router(learning.router, tags=["Learning Evidence"])
api_router.include_router(review_queue.router, tags=["Review Queue"])

__all__ = ["api_router"]
