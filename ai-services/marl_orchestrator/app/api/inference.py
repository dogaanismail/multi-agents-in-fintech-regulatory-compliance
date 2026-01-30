"""
Inference endpoint - Coordinated decision making

Thin controller - delegates business logic to FraudDecisionService
"""

from fastapi import APIRouter, HTTPException, status

from ..models.schemas import (
    CoordinatedDecisionRequest,
    CoordinatedDecisionResponse
)
from ..services.fraud_decision_service import fraud_decision_service
from ..core.logging import logger

router = APIRouter()


@router.post("/predict", response_model=CoordinatedDecisionResponse, tags=["Inference"])
async def coordinated_predict(request: CoordinatedDecisionRequest):
    """
    Make coordinated AML decision using MADDPG.
    
    This endpoint delegates to FraudDecisionService which:
    1. Queries all 3 detection agents in parallel
    2. Converts observations to state vector
    3. MADDPG decides coordinated action
    4. Returns decision with confidence and contributions
    
    Args:
        request: Transaction, customer, and network features
    
    Returns:
        Coordinated decision with action, confidence, and agent contributions
    """
    try:
        # Delegate to service layer
        response = await fraud_decision_service.make_decision(
            payment_id=request.payment_id,
            transaction_features=request.transaction.model_dump(),
            customer_features=request.customer.model_dump(),
            network_features=request.network.model_dump()
        )
        
        return response
        
    except Exception as e:
        logger.error(f"REST prediction error for payment {request.payment_id}: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction failed: {str(e)}"
        )


@router.get("/predict/example", tags=["Inference"])
async def example_prediction():
    """
    Example coordinated prediction
    
    Returns:
        Example request and response
    """
    return {
        "message": "Example coordinated prediction endpoint",
        "example_request": {
            "transaction": {
                "Date": "2024-01-15",
                "Time": "14:30:00",
                "From_Bank": "HSBC Bank",
                "Account": "ACC123456",
                "To_Bank": "Chase Bank",
                "Account_1": "ACC789012",
                "Amount_Received": 15000.50,
                "Receiving_Currency": "USD",
                "Amount_Paid": 15000.50,
                "Payment_Currency": "USD",
                "Payment_type": "Wire",
                "Sender_bank_location": "USA",
                "Receiver_bank_location": "UK"
            },
            "customer": {
                "transaction_count": 25,
                "total_amount": 125000.00,
                "avg_amount": 5000.00,
                # ... (all 19 features)
            },
            "network": {
                "in_degree": 12,
                "out_degree": 8,
                "degree_centrality": 0.0069,
                # ... (all 11 features)
            }
        },
        "note": "Send POST request to /api/v1/predict with this structure"
    }
