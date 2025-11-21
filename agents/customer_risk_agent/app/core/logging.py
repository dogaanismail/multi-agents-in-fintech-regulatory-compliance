"""
Logging configuration
"""

import logging
import sys
from pathlib import Path

# Create logs directory
logs_dir = Path(__file__).parent.parent.parent / "logs"
logs_dir.mkdir(exist_ok=True)

# Configure logger
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(logs_dir / "customer_risk_agent.log")
    ]
)

logger = logging.getLogger("customer_risk_agent")
