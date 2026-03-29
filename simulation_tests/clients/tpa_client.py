"""
Transaction Pattern Agent Client (TPA)
=======================================
Port 1001 — XGBoost model, threshold 0.1322
Endpoint: POST /api/v1/predict  (single transaction)
          POST /api/v1/batch-predict (up to 1,000 transactions)

What SHAP means for TPA:
  The TPA's recommendation field contains text like
  "REVIEW RECOMMENDED — fraud probability 0.43 (threshold 0.13)".
  The 'confidence' field (HIGH/MEDIUM/LOW) reflects how far
  the probability is from the threshold — a SHAP-derived concept
  where features like Amount, Receiver_GB, Cash_Withdrawal push the
  probability up.  The top contributing features to each prediction
  are implicitly captured in risk_score and fraud_probability.
"""

import datetime
from typing import Any, Dict, Optional

from simulation_tests.clients.base_client import BaseClient, ClientError
from simulation_tests.config import AgentURLs


class TPAClient(BaseClient):
    """Async client for Transaction Pattern Agent (port 1001)."""

    def __init__(self):
        super().__init__(base_url=AgentURLs.TPA)

    async def predict(
        self,
        amount: float,
        from_currency: str,
        to_currency: str,
        sender_country: str,
        receiver_country: str,
        payment_type: str,
    ) -> Optional[Dict[str, Any]]:
        """
        POST /api/v1/predict

        Constructs the TransactionInput schema expected by the TPA.

        Parameters match TransactionInput in tpa/app/models/schemas.py:
          date, time, amount, paymentCurrency, receivedCurrency,
          senderBankLocation, receiverBankLocation, paymentType

        paymentType accepted values (from SAML-D training data):
          "ACH", "Cash Deposit", "Cheque", "Credit card",
          "Debit card", "Cross-border", "Wire"
          → we map our internal types below.

        Returns the raw prediction dict or None on failure.
        """
        now = datetime.datetime.utcnow()

        # Map our internal payment type to TPA-trained values
        _type_map = {
            "TRANSFER_OUT": "Cross-border",
            "TRANSFER_IN":  "ACH",
            "WITHDRAWAL":   "Cash Deposit",
        }
        tpa_payment_type = _type_map.get(payment_type, "ACH")

        body = {
            "date": now.strftime("%Y-%m-%d"),
            "time": now.strftime("%H:%M:%S"),
            "amount": amount,
            "paymentCurrency": from_currency,
            "receivedCurrency": to_currency,
            "senderBankLocation": sender_country,
            "receiverBankLocation": receiver_country,
            "paymentType": tpa_payment_type,
        }
        try:
            return await self.post("/api/v1/predict", body)
        except ClientError:
            return None
        except Exception:
            return None
