"""
Configuration management for Network Analysis Agent
"""

from pydantic_settings import BaseSettings
from pathlib import Path
from typing import Optional


class Settings(BaseSettings):
    """Application settings"""
    
    # Application
    app_name: str = "Network Analysis Agent API"
    app_version: str = "1.0.0"
    description: str = "AI-powered agent for detecting suspicious accounts using network topology analysis"
    
    # Server
    host: str = "0.0.0.0"
    port: int = 8003
    workers: int = 1
    log_level: str = "info"
    
    # CORS
    cors_origins: list = ["*"]
    cors_credentials: bool = True
    cors_methods: list = ["*"]
    cors_headers: list = ["*"]
    
    # Model
    model_dir: Path = Path(__file__).parent.parent.parent / "trained_models"
    model_filename: str = "network_analysis_catboost_model.pkl"
    scaler_filename: str = "network_analysis_catboost_scaler.pkl"
    metadata_filename: str = "network_analysis_catboost_metadata.json"
    
    # API
    api_v1_prefix: str = "/api/v1"
    
    # Batch processing
    max_batch_size: int = 500
    
    # Network analysis
    min_centrality: float = 0.0
    max_risk_accounts: int = 1000
    
    # Environment
    environment: str = "development"  # development, staging, production
    debug: bool = False
    
    class Config:
        env_file = ".env"
        case_sensitive = False


# Global settings instance
settings = Settings()
