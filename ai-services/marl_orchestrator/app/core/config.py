"""
Configuration settings for MARL Orchestrator

Author: Ismail Dogan
"""

from pydantic_settings import BaseSettings
from typing import List
import os


class Settings(BaseSettings):
    """Application settings"""
    
    # Application
    app_name: str = "MARL Orchestrator - AML Compliance"
    app_version: str = "1.0.0"
    description: str = "Multi-Agent Deep Deterministic Policy Gradient (MADDPG) coordinator for AML detection"
    environment: str = os.getenv("ENVIRONMENT", "development")
    debug: bool = os.getenv("DEBUG", "True").lower() == "true"
    
    # Server
    host: str = "0.0.0.0"
    port: int = 1004
    workers: int = 1
    log_level: str = "info"
    
    # API
    api_v1_prefix: str = "/api/v1"
    
    # CORS
    cors_origins: List[str] = ["*"]
    cors_credentials: bool = True
    cors_methods: List[str] = ["*"]
    cors_headers: List[str] = ["*"]
    
    # Detection Agents
    transaction_agent_url: str = "http://localhost:1001"
    transaction_agent_port: int = 1001
    customer_agent_url: str = "http://localhost:1002"
    customer_agent_port: int = 1002
    network_agent_url: str = "http://localhost:1003"
    network_agent_port: int = 1003
    agent_timeout: int = 30  # seconds
    
    # MADDPG Settings
    maddpg_mode: str = os.getenv("MADDPG_MODE", "inference")  # "training" or "inference"
    state_dim: int = 6  # [txn_prob, txn_score, cust_prob, cust_score, net_prob, net_score]
    action_dim: int = 2  # [block, allow]
    hidden_dim: int = 256  # Hidden layer size for neural networks
    learning_rate: float = 0.001
    gamma: float = 0.99  # Discount factor
    tau: float = 0.01  # Soft update parameter
    buffer_size: int = 100000  # Replay buffer size
    batch_size: int = 64
    
    # Model Paths
    model_path: str = os.getenv("MODEL_PATH", "./trained_models")
    actor_txn_path: str = os.path.join(model_path, "actor_transaction.pth")
    actor_cust_path: str = os.path.join(model_path, "actor_customer.pth")
    actor_net_path: str = os.path.join(model_path, "actor_network.pth")
    critic_path: str = os.path.join(model_path, "critic.pth")
    
    # Kafka
    kafka_bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    schema_registry_url: str = os.getenv("SCHEMA_REGISTRY_URL", "http://localhost:8081")
    kafka_consumer_group: str = "marl-orchestrator-group"
    fraud_analysis_requested_topic: str = "fraud.analysis.requested"
    fraud_analysis_completed_topic: str = "fraud.analysis.completed"
    
    # Logging
    log_path: str = "./logs"
    
    class Config:
        env_file = ".env"
        case_sensitive = False
        protected_namespaces = ('settings_',)  # Fix Pydantic warning


settings = Settings()
