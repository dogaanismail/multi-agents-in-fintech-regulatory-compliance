"""
Logging configuration for Network Analysis Agent
"""

import logging
import sys
from pathlib import Path
from datetime import datetime

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


# Create logs directory if it doesn't exist
log_dir = Path(__file__).parent.parent.parent / "logs"
log_dir.mkdir(exist_ok=True)

# Create logger
logger = logging.getLogger("network_analysis_agent")
logger.setLevel(logging.INFO)

# Create formatters (include trace correlation fields)
detailed_formatter = logging.Formatter(
    '%(asctime)s - %(name)s - %(levelname)s - %(funcName)s:%(lineno)d - [trace=%(trace_id)s span=%(span_id)s] - %(message)s'
)
simple_formatter = logging.Formatter(
    '%(asctime)s - %(levelname)s - [trace=%(trace_id)s span=%(span_id)s] - %(message)s'
)

_trace_filter = TraceContextFilter()

# Console handler
console_handler = logging.StreamHandler(sys.stdout)
console_handler.setLevel(logging.INFO)
console_handler.setFormatter(simple_formatter)
console_handler.addFilter(_trace_filter)

# File handler (with date in filename)
log_file = log_dir / f"network_analysis_agent_{datetime.now().strftime('%Y%m%d')}.log"
file_handler = logging.FileHandler(log_file)
file_handler.setLevel(logging.DEBUG)
file_handler.setFormatter(detailed_formatter)
file_handler.addFilter(_trace_filter)

# Add handlers
logger.addHandler(console_handler)
logger.addHandler(file_handler)

# Prevent duplicate logs
logger.propagate = False
