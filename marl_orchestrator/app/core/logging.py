"""
Logging configuration for MARL Orchestrator

Author: Ismail Dogan
"""

import logging
import sys
from pathlib import Path
from pythonjsonlogger import jsonlogger


def setup_logger(name: str = "marl_orchestrator") -> logging.Logger:
    """
    Setup structured JSON logger
    
    Args:
        name: Logger name
        
    Returns:
        Configured logger instance
    """
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    
    # Create logs directory if it doesn't exist
    log_dir = Path("./logs")
    log_dir.mkdir(exist_ok=True)
    
    # Console handler with JSON formatting
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    
    # JSON formatter
    formatter = jsonlogger.JsonFormatter(
        '%(asctime)s %(name)s %(levelname)s %(message)s',
        timestamp=True
    )
    console_handler.setFormatter(formatter)
    
    # File handler
    file_handler = logging.FileHandler(log_dir / "marl_orchestrator.log")
    file_handler.setLevel(logging.INFO)
    file_handler.setFormatter(formatter)
    
    # Add handlers
    logger.addHandler(console_handler)
    logger.addHandler(file_handler)
    
    return logger


# Global logger instance
logger = setup_logger()
