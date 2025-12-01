"""
Avro Schema Library for AML Fraud Detection System

This package provides centralized Avro schemas for all microservices.
Schemas are located in the schemas/ directory.
"""

__version__ = '1.0.0'
__author__ = 'Ismail Dogan'

import os
import json
from pathlib import Path

# Get schemas directory
SCHEMAS_DIR = Path(__file__).parent.parent / 'schemas'


def load_schema(schema_path: str) -> dict:
    """
    Load an Avro schema from file
    
    Args:
        schema_path: Relative path to schema file (e.g., 'fraud/FraudDetectionRequest.avsc')
    
    Returns:
        Schema dictionary
    
    Example:
        >>> schema = load_schema('fraud/FraudDetectionRequest.avsc')
        >>> print(schema['name'])
        FraudDetectionRequest
    """
    full_path = SCHEMAS_DIR / schema_path
    
    if not full_path.exists():
        raise FileNotFoundError(f"Schema not found: {full_path}")
    
    with open(full_path, 'r') as f:
        return json.load(f)


def get_all_schemas() -> dict:
    """
    Get all available schemas
    
    Returns:
        Dictionary mapping schema names to schema dictionaries
    """
    schemas = {}
    
    for schema_file in SCHEMAS_DIR.rglob('*.avsc'):
        relative_path = schema_file.relative_to(SCHEMAS_DIR)
        schema_name = schema_file.stem
        
        with open(schema_file, 'r') as f:
            schemas[schema_name] = json.load(f)
    
    return schemas


# Commonly used schemas
FRAUD_DETECTION_REQUEST_SCHEMA = load_schema('fraud/FraudDetectionRequest.avsc')
FRAUD_DETECTION_RESPONSE_SCHEMA = load_schema('fraud/FraudDetectionResponse.avsc')
PAYMENT_CREATED_EVENT_SCHEMA = load_schema('payment/PaymentCreatedEvent.avsc')
PAYMENT_COMPLETED_EVENT_SCHEMA = load_schema('payment/PaymentCompletedEvent.avsc')
PAYMENT_BLOCKED_EVENT_SCHEMA = load_schema('payment/PaymentBlockedEvent.avsc')


__all__ = [
    'load_schema',
    'get_all_schemas',
    'FRAUD_DETECTION_REQUEST_SCHEMA',
    'FRAUD_DETECTION_RESPONSE_SCHEMA',
    'PAYMENT_CREATED_EVENT_SCHEMA',
    'PAYMENT_COMPLETED_EVENT_SCHEMA',
    'PAYMENT_BLOCKED_EVENT_SCHEMA',
    'SCHEMAS_DIR',
]
