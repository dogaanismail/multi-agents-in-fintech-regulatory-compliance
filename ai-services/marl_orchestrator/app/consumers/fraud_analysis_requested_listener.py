"""
Fraud Analysis Requested Listener - Consumes FraudAnalysisRequested from Kafka

Thin Kafka consumer that listens to fraud analysis requests and delegates
processing to FraudAnalysisRequestedHandler. Retry, dead-letter routing and
offset-commit handling live in AvroConsumerThread.
"""

from app.consumers.base import AvroConsumerThread
from app.core.config import settings
from app.handlers.fraud_analysis_requested_handler import fraud_analysis_requested_handler


class FraudAnalysisRequestedListener(AvroConsumerThread):

    def __init__(self):
        super().__init__(
            name="FraudAnalysisRequestedListener",
            topic=settings.fraud_analysis_requested_topic,
            handler=fraud_analysis_requested_handler,
        )


fraud_analysis_requested_listener = FraudAnalysisRequestedListener()
