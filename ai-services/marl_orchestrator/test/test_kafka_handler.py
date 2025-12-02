"""
Unit tests for FraudDetectionRequestHandler

Tests the handler logic without actual Kafka infrastructure.
"""

import pytest
from unittest.mock import Mock, AsyncMock, patch
from datetime import datetime

from app.handlers.fraud_detection_request_handler import FraudDetectionRequestHandler
from app.models.schemas import CoordinatedDecisionResponse, ActionType, AgentObservation


@pytest.fixture
def mock_fraud_decision_service():
    """Mock FraudDecisionService"""
    mock = AsyncMock()
    return mock


@pytest.fixture
def mock_fraud_response_publisher():
    """Mock FraudDetectionResponsePublisher"""
    mock = Mock()
    return mock


@pytest.fixture
def handler(mock_fraud_decision_service, mock_fraud_response_publisher):
    """Create handler with mocked dependencies"""
    handler = FraudDetectionRequestHandler()
    handler.fraud_decision_service = mock_fraud_decision_service
    handler.fraud_response_publisher = mock_fraud_response_publisher
    return handler


@pytest.fixture
def sample_request():
    """Sample fraud detection request (Avro format)"""
    return {
        'requestId': 'req-12345',
        'transactionId': 'txn-67890',
        'transactionFeatures': {
            'amount': 1500.0,
            'currency': 'USD',
            'transactionType': 'TRANSFER',
            'timestamp': 1701518400000
        },
        'customerFeatures': {
            'customerId': 'cust-111',
            'accountAge': 365,
            'riskScore': 0.3
        },
        'networkFeatures': {
            'accountId': 'acc-222',
            'graphDensity': 0.5,
            'communityId': 'comm-333'
        }
    }


@pytest.fixture
def sample_decision_response():
    """Sample decision response from service"""
    return CoordinatedDecisionResponse(
        transaction_id='txn-67890',
        action=ActionType.BLOCK,
        confidence=0.95,
        maddpg_q_value=0.89,
        transaction_agent_observation=AgentObservation(
            agent_name='transaction',
            is_suspicious=True,
            probability=0.92,
            risk_score=0.88,
            confidence='HIGH',
            response_time_ms=15.5
        ),
        customer_agent_observation=AgentObservation(
            agent_name='customer',
            is_suspicious=True,
            probability=0.85,
            risk_score=0.82,
            confidence='HIGH',
            response_time_ms=12.3
        ),
        network_agent_observation=AgentObservation(
            agent_name='network',
            is_suspicious=False,
            probability=0.45,
            risk_score=0.40,
            confidence='MEDIUM',
            response_time_ms=18.7
        ),
        agent_contributions={
            'transaction': 0.42,
            'customer': 0.38,
            'network': 0.20
        },
        processing_time_ms=125.5,
        timestamp='2024-12-02T10:30:00',
        mode='inference'
    )


@pytest.mark.asyncio
async def test_handle_success(handler, mock_fraud_decision_service, mock_fraud_response_publisher, 
                               sample_request, sample_decision_response):
    """Test successful handling of fraud detection request"""
    # Arrange
    mock_fraud_decision_service.make_decision = AsyncMock(return_value=sample_decision_response)
    mock_fraud_response_publisher.publish = Mock(return_value=True)
    
    # Act
    result = await handler.handle(sample_request)
    
    # Assert
    assert result is True
    mock_fraud_decision_service.make_decision.assert_called_once()
    mock_fraud_response_publisher.publish.assert_called_once()
    
    # Verify service was called with correct parameters
    call_kwargs = mock_fraud_decision_service.make_decision.call_args.kwargs
    assert call_kwargs['transaction_id'] == 'txn-67890'
    assert 'transaction_features' in call_kwargs
    assert 'customer_features' in call_kwargs
    assert 'network_features' in call_kwargs


@pytest.mark.asyncio
async def test_handle_builds_correct_response(handler, mock_fraud_decision_service, 
                                              mock_fraud_response_publisher, sample_request, 
                                              sample_decision_response):
    """Test that handler builds Avro-compliant response"""
    # Arrange
    mock_fraud_decision_service.make_decision = AsyncMock(return_value=sample_decision_response)
    mock_fraud_response_publisher.publish = Mock(return_value=True)
    
    # Act
    await handler.handle(sample_request)
    
    # Assert - verify response structure
    published_response = mock_fraud_response_publisher.publish.call_args.kwargs['response']
    
    assert published_response['requestId'] == 'req-12345'
    assert published_response['transactionId'] == 'txn-67890'
    assert published_response['action'] == 'BLOCK'
    assert published_response['confidence'] == 0.95
    assert published_response['maddpgQValue'] == 0.89
    
    # Verify three separate observation fields (not array)
    assert 'transactionAgentObservation' in published_response
    assert 'customerAgentObservation' in published_response
    assert 'networkAgentObservation' in published_response
    
    # Verify timestamp is in milliseconds (not ISO string)
    assert isinstance(published_response['timestamp'], int)
    assert published_response['timestamp'] > 0
    
    assert published_response['mode'] == 'inference'


@pytest.mark.asyncio
async def test_handle_publisher_failure(handler, mock_fraud_decision_service, 
                                       mock_fraud_response_publisher, sample_request, 
                                       sample_decision_response):
    """Test handling when publisher fails"""
    # Arrange
    mock_fraud_decision_service.make_decision = AsyncMock(return_value=sample_decision_response)
    mock_fraud_response_publisher.publish = Mock(return_value=False)
    
    # Act
    result = await handler.handle(sample_request)
    
    # Assert
    assert result is False


@pytest.mark.asyncio
async def test_handle_service_exception(handler, mock_fraud_decision_service, sample_request):
    """Test handling when service throws exception"""
    # Arrange
    mock_fraud_decision_service.make_decision = AsyncMock(side_effect=Exception("Service error"))
    
    # Act & Assert
    with pytest.raises(Exception, match="Service error"):
        await handler.handle(sample_request)


def test_avro_to_dict_with_dict(handler):
    """Test _avro_to_dict with dictionary input"""
    input_dict = {'key': 'value', 'number': 42}
    result = handler._avro_to_dict(input_dict)
    assert result == input_dict
    assert isinstance(result, dict)


# Skipping avro_to_dict_with_object test as it requires specific Avro record structure


def test_observation_to_avro(handler, sample_decision_response):
    """Test observation conversion to Avro format"""
    observation = sample_decision_response.transaction_agent_observation
    
    result = handler._observation_to_avro(observation)
    
    assert result['agentName'] == 'transaction'
    assert result['isSuspicious'] is True
    assert result['probability'] == 0.92
    assert result['riskScore'] == 0.88
    assert result['confidence'] == 'HIGH'
    assert result['responseTimeMs'] == 15.5


def test_timestamp_conversion(handler, sample_decision_response):
    """Test timestamp conversion from ISO string to milliseconds"""
    # Use a known timestamp for predictable testing
    sample_decision_response.timestamp = '2024-12-02T10:30:00'
    
    response_dict = handler._build_response_with_request_id(
        request_id='test-req',
        decision_response=sample_decision_response
    )
    
    # Timestamp should be integer in milliseconds
    timestamp = response_dict['timestamp']
    assert isinstance(timestamp, int)
    assert timestamp > 0
    
    # Verify conversion is correct (Dec 2, 2024 should be around 1733140200000 ms)
    expected_time = int(datetime.fromisoformat('2024-12-02T10:30:00').timestamp() * 1000)
    assert abs(timestamp - expected_time) < 1000  # Within 1 second tolerance


def test_action_enum_serialization(handler, sample_decision_response):
    """Test that ActionType enum is correctly serialized to string"""
    response_dict = handler._build_response_with_request_id(
        request_id='test-req',
        decision_response=sample_decision_response
    )
    
    # Action should be string value, not enum
    assert response_dict['action'] == 'BLOCK'
    assert isinstance(response_dict['action'], str)
