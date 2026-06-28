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
    
    # Kafka
    kafka_bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    schema_registry_url: str = os.getenv("SCHEMA_REGISTRY_URL", "http://localhost:8081")
    kafka_consumer_group: str = "marl-orchestrator-group"
    fraud_analysis_requested_topic: str = "fraud.analysis.requested"
    fraud_analysis_completed_topic: str = "fraud.analysis.completed"
    agent_manual_feedback_topic: str = "agent.manual.feedback"

    # Dead-letter queue + bounded retry
    kafka_dlq_topic_suffix: str = os.getenv("KAFKA_DLQ_TOPIC_SUFFIX", ".DLT")
    kafka_max_delivery_attempts: int = int(os.getenv("KAFKA_MAX_DELIVERY_ATTEMPTS", "3"))
    kafka_retry_backoff_seconds: float = float(os.getenv("KAFKA_RETRY_BACKOFF_SECONDS", "1.0"))
    kafka_retry_backoff_max_seconds: float = float(os.getenv("KAFKA_RETRY_BACKOFF_MAX_SECONDS", "10.0"))
    kafka_statistics_interval_ms: int = int(os.getenv("KAFKA_STATISTICS_INTERVAL_MS", "10000"))

    # Database (Offline Replay Buffer Persistence)
    database_url: str = os.getenv(
        "DATABASE_URL",
        "postgresql+asyncpg://marl_user:marl_password@localhost:5438/marl_orchestrator_db"
    )

    # Offline Batch Retraining
    training_interval_seconds: int = int(os.getenv("TRAINING_INTERVAL_SECONDS", "300"))
    min_experiences_for_training: int = int(os.getenv("MIN_EXPERIENCES_FOR_TRAINING", "64"))
    training_batch_size: int = int(os.getenv("TRAINING_BATCH_SIZE", "64"))
    max_experiences_per_batch: int = int(os.getenv("MAX_EXPERIENCES_PER_BATCH", "1000"))
    save_model_after_training: bool = os.getenv("SAVE_MODEL_AFTER_TRAINING", "True").lower() == "true"

    # Agent trust weights — must sum to ~1.0; used to blend per-agent risk scores
    agent_weight_transaction: float = float(os.getenv("AGENT_WEIGHT_TRANSACTION", "0.333"))
    agent_weight_customer: float = float(os.getenv("AGENT_WEIGHT_CUSTOMER", "0.333"))
    agent_weight_network: float = float(os.getenv("AGENT_WEIGHT_NETWORK", "0.334"))

    # Freeze training — compliance officers can pause learning during audits
    freeze_training: bool = os.getenv("FREEZE_TRAINING", "False").lower() == "true"

    # Fallback risk score when an agent is unreachable (0-100); default 50 = neutral
    fallback_agent_risk_score: float = float(os.getenv("FALLBACK_AGENT_RISK_SCORE", "50.0"))

    # Experience buffer retention (days); 0 = keep forever
    experience_retention_days: int = int(os.getenv("EXPERIENCE_RETENTION_DAYS", "90"))

    # How often the in-memory config cache refreshes from configuration-service (seconds)
    config_refresh_interval_seconds: int = int(os.getenv("CONFIG_REFRESH_INTERVAL_SECONDS", "300"))

    # Reward Configuration (Configurable - per professor's recommendation)
    # Automated rewards (heuristic, before manual review)
    reward_auto_high_risk_block: float = float(os.getenv("REWARD_AUTO_HIGH_RISK_BLOCK", "0.3"))
    reward_auto_low_risk_allow: float = float(os.getenv("REWARD_AUTO_LOW_RISK_ALLOW", "0.3"))
    reward_auto_conflict: float = float(os.getenv("REWARD_AUTO_CONFLICT", "-0.3"))
    reward_auto_risk_threshold: float = float(os.getenv("REWARD_AUTO_RISK_THRESHOLD", "0.5"))
    # Manual review rewards (verified expert judgement - higher weight)
    reward_manual_correct_block: float = float(os.getenv("REWARD_MANUAL_CORRECT_BLOCK", "1.0"))
    reward_manual_correct_allow: float = float(os.getenv("REWARD_MANUAL_CORRECT_ALLOW", "0.5"))
    reward_manual_wrong_block: float = float(os.getenv("REWARD_MANUAL_WRONG_BLOCK", "-0.5"))
    reward_manual_wrong_allow: float = float(os.getenv("REWARD_MANUAL_WRONG_ALLOW", "-1.0"))
    reward_manual_weight_multiplier: float = float(os.getenv("REWARD_MANUAL_WEIGHT_MULTIPLIER", "2.0"))
    # Override rewards (compliance officer reverses a terminal decision)
    reward_override_block_to_approve: float = float(os.getenv("REWARD_OVERRIDE_BLOCK_TO_APPROVE", "-0.9"))
    reward_override_allow_to_reject: float = float(os.getenv("REWARD_OVERRIDE_ALLOW_TO_REJECT", "-1.2"))
    reward_override_multiplier: float = float(os.getenv("REWARD_OVERRIDE_MULTIPLIER", "3.0"))
    # Escalation rewards
    reward_escalation_mode: str = os.getenv("REWARD_ESCALATION_MODE", "final_decision")  # none | positive | final_decision
    reward_escalation_positive: float = float(os.getenv("REWARD_ESCALATION_POSITIVE", "0.3"))
    # Confidence weighting
    reward_use_confidence_weighting: bool = os.getenv("REWARD_USE_CONFIDENCE_WEIGHTING", "True").lower() == "true"
    # Confidence threshold for auto-escalation
    escalation_confidence_threshold: float = float(os.getenv("ESCALATION_CONFIDENCE_THRESHOLD", "0.6"))

    # Logging
    log_path: str = "./logs"
    
    class Config:
        env_file = ".env"
        case_sensitive = False
        protected_namespaces = ('settings_',)


settings = Settings()
