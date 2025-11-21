"""
Configuration management for Customer Risk Agent
"""

from pydantic_settings import BaseSettings
from pathlib import Path
from typing import Optional


class Settings(BaseSettings):
    """Application settings"""
    
    # Application
    app_name: str = "Customer Risk Agent API"
    app_version: str = "1.0.0"
    description: str = "AI-powered agent for assessing customer-level risk in AML compliance"
    
    # Server
    host: str = "0.0.0.0"
    port: int = 8002
    workers: int = 1
    log_level: str = "info"
    
    # CORS
    cors_origins: list = ["*"]
    cors_credentials: bool = True
    cors_methods: list = ["*"]
    cors_headers: list = ["*"]
    
    # Model
    model_dir: Path = Path(__file__).parent.parent.parent / "trained_models"
    model_filename: str = "customer_risk_model.pkl"
    
    # API
    api_v1_prefix: str = "/api/v1"
    
    # Batch processing
    max_batch_size: int = 500
    
    # Customer analysis
    min_transactions: int = 3
    analysis_timeframe_days: int = 30
    
    # Environment
    environment: str = "development"  # development, staging, production
    debug: bool = False
    
    class Config:
        env_file = ".env"
        case_sensitive = False


# Global settings instance
settings = Settings()
