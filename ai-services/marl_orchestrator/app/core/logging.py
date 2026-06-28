"""
Logging configuration for MARL Orchestrator

Author: Ismail Dogan
"""

import logging
import sys
from pathlib import Path
from pythonjsonlogger import jsonlogger

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
    
    # JSON formatter (includes trace correlation fields)
    formatter = jsonlogger.JsonFormatter(
        '%(asctime)s %(name)s %(levelname)s %(trace_id)s %(span_id)s %(message)s',
        timestamp=True
    )
    console_handler.setFormatter(formatter)

    # File handler
    file_handler = logging.FileHandler(log_dir / "marl_orchestrator.log")
    file_handler.setLevel(logging.INFO)
    file_handler.setFormatter(formatter)

    # Inject trace_id / span_id into every record
    trace_filter = TraceContextFilter()
    console_handler.addFilter(trace_filter)
    file_handler.addFilter(trace_filter)

    # Add handlers
    logger.addHandler(console_handler)
    logger.addHandler(file_handler)
    
    return logger


# Global logger instance
logger = setup_logger()
