"""
Logger - Centralized logging for MADDPG package

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import logging
import sys
from typing import Optional


def get_logger(name: str = "maddpg", level: Optional[int] = None) -> logging.Logger:
    """
    Get or create a logger for MADDPG package
    
    Args:
        name: Logger name (default: "maddpg")
        level: Logging level (default: INFO)
    
    Returns:
        Configured logger instance
    """
    logger = logging.getLogger(name)
    
    # Only configure if not already configured
    if not logger.handlers:
        if level is None:
            level = logging.INFO
            
        logger.setLevel(level)
        
        # Console handler
        handler = logging.StreamHandler(sys.stdout)
        handler.setLevel(level)
        
        # Format
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        handler.setFormatter(formatter)
        
        logger.addHandler(handler)
        
        # Prevent propagation to avoid duplicate logs
        logger.propagate = False
    
    return logger


# Default logger instance for the package
logger = get_logger("maddpg")
