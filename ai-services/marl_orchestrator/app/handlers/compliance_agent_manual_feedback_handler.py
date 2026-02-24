"""
Compliance Agent Manual Feedback Handler

Processes ComplianceAgentManualFeedbackEvent messages consumed from Kafka.
Determines whether the feedback represents a manual review or a decision override,
calculates the appropriate reward via RewardCalculatorService, and applies it
to the experience buffer via ExperienceBufferService.
"""

from typing import Dict, Any

from app.core.logging import logger
from app.services.experience_buffer_service import experience_buffer_service
from app.services.reward_calculator_service import reward_calculator_service


class ComplianceAgentManualFeedbackHandler:

    def __init__(self):
        self.experience_buffer_service = experience_buffer_service
        self.reward_calculator_service = reward_calculator_service
        logger.info("ComplianceAgentManualFeedbackHandler initialized")

    async def handle(self, event: Dict[str, Any]) -> bool:
        try:
            payment_id = event.get("paymentId")
            feedback_type = event.get("feedbackType")
            original_marl_action = event.get("originalMarlAction", "UNKNOWN")
            officer_decision = event.get("officerDecision")
            reviewed_by = event.get("reviewedBy")
            notes = event.get("notes")

            logger.info(
                f"Handling compliance feedback: payment={payment_id}, "
                f"type={feedback_type}, marl={original_marl_action}, decision={officer_decision}"
            )

            if feedback_type == "MANUAL_REVIEW":
                reward = self.reward_calculator_service.calculate_manual_reward(
                    marl_action=original_marl_action,
                    officer_decision=officer_decision,
                )
            elif feedback_type == "DECISION_OVERRIDE":
                reward = self.reward_calculator_service.calculate_override_reward(
                    original_marl_action=original_marl_action,
                    officer_decision=officer_decision,
                )
            else:
                logger.warning(f"Unknown feedbackType={feedback_type} for payment={payment_id}, skipping")
                return False

            success = await self.experience_buffer_service.apply_manual_reward(
                payment_id=payment_id,
                manual_reward=reward,
                reviewed_by=reviewed_by,
            )

            if success:
                logger.info(
                    f"Applied {feedback_type} reward={reward:.4f} for payment={payment_id} by {reviewed_by}"
                )
            else:
                logger.warning(
                    f"No experience entry found for payment={payment_id} — reward not applied"
                )

            return success

        except Exception as e:
            logger.error(f"Error handling compliance feedback: {str(e)}")
            raise


compliance_agent_manual_feedback_handler = ComplianceAgentManualFeedbackHandler()
