"""
Main FastAPI application
Network Analysis Agent for AML Compliance

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from fastapi import FastAPI, APIRouter
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.core.config import settings
from app.core.logging import logger
from app.services.model_loader import model_loader
from app.api import health, model, predictions

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager
    Handles startup and shutdown events
    """
    # Startup
    logger.info("🚀 Starting Network Analysis Agent...")
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
    logger.info("👋 Shutting down Network Analysis Agent...")


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
        "description": "AI-powered network topology analysis for detecting suspicious accounts in AML compliance",
        "approach": "Uses network centrality metrics, clustering, and community detection",
        "endpoints": {
            "health": "/health",
            "docs": "/docs",
            "redoc": "/redoc",
            "api": settings.api_v1_prefix
        }
    }

# Create API router and include sub-routers
api_router = APIRouter()
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(model.router, tags=["Model"])
api_router.include_router(predictions.router, tags=["Prediction"])

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
