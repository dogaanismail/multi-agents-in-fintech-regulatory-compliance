"""
Logging configuration
"""

import logging
import sys
from pathlib import Path

from opentelemetry import trace as _otel_trace

# Create logs directory
logs_dir = Path(__file__).parent.parent.parent / "logs"
logs_dir.mkdir(exist_ok=True)


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


# Build handlers with the trace-context filter attached
_trace_filter = TraceContextFilter()
_console_handler = logging.StreamHandler(sys.stdout)
_console_handler.addFilter(_trace_filter)
_file_handler = logging.FileHandler(logs_dir / "customer_risk_agent.log")
_file_handler.addFilter(_trace_filter)

# Configure logger (format includes trace correlation fields)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - [trace=%(trace_id)s span=%(span_id)s] - %(message)s",
    handlers=[_console_handler, _file_handler]
)

logger = logging.getLogger("customer_risk_agent")
