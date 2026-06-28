"""
Logging configuration for Transaction Pattern Agent
"""

import logging
import sys
from pathlib import Path

from opentelemetry import trace as _otel_trace


class TraceContextFilter(logging.Filter):
    """Stamp the active OpenTelemetry trace_id / span_id onto each log record
    so log lines can be correlated with the trace in Jaeger."""

    def filter(self, record: logging.LogRecord) -> bool:
        record.trace_id = "-"
        record.span_id = "-"
        try:
            ctx = _otel_trace.get_current_span().get_span_context()
            if ctx.is_valid:
                record.trace_id = format(ctx.trace_id, "032x")
                record.span_id = format(ctx.span_id, "016x")
        except Exception:
            pass
        return True


def setup_logging(log_level: str = "INFO") -> logging.Logger:
    """
    Configure application logging
    
    Args:
        log_level: Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
    
    Returns:
        Configured logger instance
    """
    # Create logger
    logger = logging.getLogger("transaction_pattern_agent")
    logger.setLevel(getattr(logging, log_level.upper()))
    
    # Remove existing handlers
    logger.handlers.clear()
    
    # Console handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(getattr(logging, log_level.upper()))
    
    # Formatter (includes trace correlation fields)
    formatter = logging.Formatter(
        fmt="%(asctime)s - %(name)s - %(levelname)s - [trace=%(trace_id)s span=%(span_id)s] - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    console_handler.setFormatter(formatter)
    console_handler.addFilter(TraceContextFilter())

    # Add handler
    logger.addHandler(console_handler)
    
    return logger


# Global logger instance
logger = setup_logging()
