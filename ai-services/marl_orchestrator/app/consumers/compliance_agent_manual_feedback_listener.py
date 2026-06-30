"""
Compliance Agent Manual Feedback Listener - Consumes ComplianceAgentManualFeedbackEvent from Kafka

Thin Kafka consumer that listens to compliance officer feedback events and
delegates processing to ComplianceAgentManualFeedbackHandler. Retry, dead-letter
routing and offset-commit handling live in AvroConsumerThread.
"""

from app.consumers.base import AvroConsumerThread
from app.core.config import settings
from app.handlers.compliance_agent_manual_feedback_handler import compliance_agent_manual_feedback_handler


class ComplianceAgentManualFeedbackListener(AvroConsumerThread):

    def __init__(self):
        super().__init__(
            name="ComplianceAgentManualFeedbackListener",
            topic=settings.agent_manual_feedback_topic,
            handler=compliance_agent_manual_feedback_handler,
        )


compliance_agent_manual_feedback_listener = ComplianceAgentManualFeedbackListener()
