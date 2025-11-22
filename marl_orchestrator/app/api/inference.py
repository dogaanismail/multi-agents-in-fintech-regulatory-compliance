"""
Inference endpoint - Coordinated decision making
"""

from fastapi import APIRouter, HTTPException, status
import time
from datetime import datetime

from ..models.schemas import (
    CoordinatedDecisionRequest,
    CoordinatedDecisionResponse,
    ActionType
)
from ..services.agent_orchestrator import agent_orchestrator
from maddpg.core import maddpg_coordinator
from ..core.logging import logger
from ..core.config import settings

router = APIRouter()


@router.post("/predict", response_model=CoordinatedDecisionResponse, tags=["Inference"])
async def coordinated_predict(request: CoordinatedDecisionRequest):
    """
    Make coordinated AML decision using MADDPG
    
    Workflow:
    1. Query all 3 detection agents in parallel
    2. Convert observations to state vector
    3. MADDPG decides coordinated action
    4. Return decision with confidence and contributions
    
    Args:
        request: Transaction, customer, and network features
    
    Returns:
        Coordinated decision with action, confidence, and agent contributions
    """
    start_time = time.time()
    
    try:
        # Step 1: Get observations from all 3 detection agents (parallel)
        observations = await agent_orchestrator.get_all_observations(
            transaction_features=request.transaction.model_dump(),
            customer_features=request.customer.model_dump(),
            network_features=request.network.model_dump()
        )
        
        logger.info(f"Received observations from all agents")
        logger.info(f"  Transaction: {observations['transaction'].probability:.3f}")
        logger.info(f"  Customer: {observations['customer'].probability:.3f}")
        logger.info(f"  Network: {observations['network'].probability:.3f}")
        
        # Step 2: MADDPG makes coordinated decision
        decision = maddpg_coordinator.decide({
            'transaction': {
                'probability': observations['transaction'].probability,
                'risk_score': observations['transaction'].risk_score
            },
            'customer': {
                'probability': observations['customer'].probability,
                'risk_score': observations['customer'].risk_score
            },
            'network': {
                'probability': observations['network'].probability,
                'risk_score': observations['network'].risk_score
            }
        })
        
        processing_time = (time.time() - start_time) * 1000  # ms
        
        logger.info(f"MADDPG Decision: {decision['action']} (confidence: {decision['confidence']:.3f})")
        logger.info(f"Processing time: {processing_time:.2f}ms")
        
        return CoordinatedDecisionResponse(
            transaction_id=request.transaction_id,
            action=ActionType(decision['action']),
            confidence=decision['confidence'],
            maddpg_q_value=decision['q_value'],
            transaction_agent_observation=observations['transaction'],
            customer_agent_observation=observations['customer'],
            network_agent_observation=observations['network'],
            agent_contributions=decision['contributions'],
            processing_time_ms=processing_time,
            timestamp=datetime.now().isoformat(),
            mode=settings.maddpg_mode
        )
        
    except Exception as e:
        logger.error(f"Prediction error: {str(e)}")
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
