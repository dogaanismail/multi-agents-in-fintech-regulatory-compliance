"""
API package initialization
"""

from fastapi import APIRouter
from . import health, inference

# Create main API router
api_router = APIRouter()

# Include sub-routers
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(inference.router, tags=["Inference"])

__all__ = ["api_router"]
