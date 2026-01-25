"""
MARL Orchestrator - Multi-Agent Deep Deterministic Policy Gradient (MADDPG)
Coordinates detection agents for AML compliance

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import asyncio
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.core.config import settings
from app.core.logging import logger
from app.api import api_router
from app.consumers.fraud_analysis_requested_listener import fraud_analysis_requested_listener

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager
    Handles startup and shutdown events
    """
    # Startup
    logger.info("🚀 Starting MARL Orchestrator...")
    logger.info(f"Environment: {settings.environment}")
    logger.info(f"Version: {settings.app_version}")
    logger.info(f"MADDPG Mode: {settings.maddpg_mode}")
    
    # Load MADDPG models if in inference mode
    if settings.maddpg_mode == "inference":
        try:
            from maddpg.core import maddpg_coordinator
            maddpg_coordinator.load_models()
            logger.info("✅ MADDPG models loaded successfully")
        except Exception as e:
            logger.warning(f"⚠️ MADDPG models not loaded: {str(e)}")
            logger.info("💡 Running in training mode - models will be trained first")
    
    # Start Kafka listener in background
    logger.info("🎧 Starting Kafka fraud analysis request listener...")
    listener_task = asyncio.create_task(fraud_analysis_requested_listener.start())
    
    yield
    
    # Shutdown
    logger.info("👋 Shutting down MARL Orchestrator...")
    fraud_analysis_requested_listener.stop()
    await listener_task


# Create FastAPI application
app = FastAPI(
    title=settings.app_name,
    description=settings.description,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=settings.cors_credentials,
    allow_methods=settings.cors_methods,
    allow_headers=settings.cors_headers,
)

# Root endpoint
@app.get("/", tags=["General"])
async def root():
    """
    Root endpoint - API information
    
    Returns:
        API information and available endpoints
    """
    return {
        "message": settings.app_name,
        "version": settings.app_version,
        "status": "operational",
        "environment": settings.environment,
        "description": "MADDPG-based multi-agent coordinator for AML compliance",
        "approach": "Coordinates 3 detection agents using Multi-Agent Deep Deterministic Policy Gradient",
        "detection_agents": {
            "transaction_pattern": f"http://localhost:{settings.transaction_agent_port}",
            "customer_risk": f"http://localhost:{settings.customer_agent_port}",
            "network_analysis": f"http://localhost:{settings.network_agent_port}"
        },
        "endpoints": {
            "health": "/health",
            "docs": "/docs",
            "redoc": "/redoc",
            "api": settings.api_v1_prefix
        }
    }

# Include API routes
app.include_router(api_router, prefix=settings.api_v1_prefix)

if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        workers=settings.workers,
        log_level=settings.log_level,
        reload=settings.debug
    )
