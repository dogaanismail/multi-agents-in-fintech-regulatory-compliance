"""
Main FastAPI application
Transaction Pattern Agent for AML Monitoring

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.core.config import settings
from app.core.logging import logger
from app.core.telemetry import setup_telemetry
from app.services.model_loader import model_loader
from app.api import api_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager
    Handles startup and shutdown events
    """
    # Startup
    logger.info("🚀 Starting Transaction Pattern Agent...")
    logger.info(f"Environment: {settings.environment}")
    logger.info(f"Version: {settings.app_version}")
    
    try:
        model_loader.load_models()
        logger.info("✅ Models loaded successfully")
    except Exception as e:
        logger.error(f"❌ Failed to load models: {str(e)}")
        raise
    
    yield
    
    # Shutdown
    logger.info("👋 Shutting down Transaction Pattern Agent...")


# Create FastAPI application
app = FastAPI(
    title=settings.app_name,
    description=settings.description,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Distributed tracing: instrument inbound FastAPI requests and export to Jaeger.
setup_telemetry("transaction-pattern-agent", app=app)

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
