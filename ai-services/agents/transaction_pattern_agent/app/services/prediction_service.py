"""
Prediction service for transaction fraud detection
"""

import pandas as pd
import numpy as np
from typing import List, Tuple
from datetime import datetime

from ..models.schemas import TransactionInput, TransactionPrediction
from ..core.config import settings
from ..core.logging import logger
from .model_loader import model_loader


class PredictionService:
    """Handles transaction preprocessing and fraud prediction"""
    
    # Mapping dictionaries to convert API-friendly values to training data values
    CURRENCY_MAPPING = {
        'UK pound': 'UK pounds',
        'UK Pound': 'UK pounds',
        'GBP': 'UK pounds',
        'US Dollar': 'US dollar',
        'USD': 'US dollar',
        # Add pass-through for exact matches
        'UK pounds': 'UK pounds',
        'US dollar': 'US dollar',
        'Euro': 'Euro',
        'Yen': 'Yen',
        'Dirham': 'Dirham',
        'Swiss franc': 'Swiss franc',
        'Turkish lira': 'Turkish lira',
        'Indian rupee': 'Indian rupee',
        'Pakistani rupee': 'Pakistani rupee',
        'Mexican Peso': 'Mexican Peso',
        'Naira': 'Naira',
        'Albanian lek': 'Albanian lek',
        'Moroccan dirham': 'Moroccan dirham'
    }
    
    COUNTRY_MAPPING = {
        'United Kingdom': 'UK',
        'United States': 'USA',
        'United Arab Emirates': 'UAE',
        # Add pass-through for exact matches
        'UK': 'UK',
        'USA': 'USA',
        'UAE': 'UAE',
        'France': 'France',
        'Germany': 'Germany',
        'Italy': 'Italy',
        'Spain': 'Spain',
        'Netherlands': 'Netherlands',
        'Switzerland': 'Switzerland',
        'Austria': 'Austria',
        'Turkey': 'Turkey',
        'India': 'India',
        'Pakistan': 'Pakistan',
        'Japan': 'Japan',
        'Mexico': 'Mexico',
        'Nigeria': 'Nigeria',
        'Morocco': 'Morocco',
        'Albania': 'Albania'
    }
    
    @staticmethod
    def normalize_value(value: str, mapping: dict) -> str:
        """
        Normalize a categorical value using the provided mapping
        
        Args:
            value: Original value from API
            mapping: Dictionary mapping API values to training data values
        
        Returns:
            Normalized value matching training data
        """
        # Try exact match first
        if value in mapping:
            return mapping[value]
        
        # Try case-insensitive match
        for key, mapped_value in mapping.items():
            if key.lower() == value.lower():
                return mapped_value
        
        # Return original if no mapping found (will be handled as unknown by preprocessor)
        logger.warning(f"No mapping found for value: {value}")
        return value
    
    @staticmethod
    def preprocess_transaction(transaction: TransactionInput) -> pd.DataFrame:
        """
        Preprocess a single transaction for model input
        
        Args:
            transaction: Transaction input data
        
        Returns:
            Preprocessed DataFrame ready for model
        """
        # Convert to DataFrame (matching actual CSV column names)
        # CSV columns: Time,Date,Sender_account,Receiver_account,Amount,Payment_currency,Received_currency,
        #              Sender_bank_location,Receiver_bank_location,Payment_type,Is_laundering,Laundering_type
        
        # Calculate Amount as average of received and paid (to match training data)
        amount = (transaction.Amount_Received + transaction.Amount_Paid) / 2
        
        # Normalize categorical values to match training data exactly
        payment_currency = PredictionService.normalize_value(
            transaction.Payment_Currency, 
            PredictionService.CURRENCY_MAPPING
        )
        receiving_currency = PredictionService.normalize_value(
            transaction.Receiving_Currency,
            PredictionService.CURRENCY_MAPPING
        )
        sender_location = PredictionService.normalize_value(
            transaction.Sender_bank_location,
            PredictionService.COUNTRY_MAPPING
        )
        receiver_location = PredictionService.normalize_value(
            transaction.Receiver_bank_location,
            PredictionService.COUNTRY_MAPPING
        )
        
        data = {
            'Time': transaction.Time,
            'Date': transaction.Date,
            'Amount': amount,
            'Payment_currency': payment_currency,
            'Received_currency': receiving_currency,
            'Sender_bank_location': sender_location,
            'Receiver_bank_location': receiver_location,
            'Payment_type': transaction.Payment_type
        }
        
        df = pd.DataFrame([data])
        
        # Feature engineering (matching training process in notebook)
        df['Date'] = pd.to_datetime(df['Date'])
        df['Time'] = pd.to_datetime(df['Time'], format='%H:%M:%S')
        df['hour'] = df['Time'].dt.hour
        df['day_of_week'] = df['Date'].dt.day_name()
        df['date'] = df['Date'].dt.date
        
        return df
    
    @staticmethod
    def calculate_confidence(probability: float) -> str:
        """
        Calculate confidence level based on probability
        
        Args:
            probability: Fraud probability (0-1)
        
        Returns:
            Confidence level: LOW, MEDIUM, or HIGH
        """
        if probability >= 0.8:
            return "HIGH"
        elif probability >= 0.5:
            return "MEDIUM"
        else:
            return "LOW"
    
    @staticmethod
    def get_recommendation(is_suspicious: bool, probability: float) -> str:
        """
        Generate recommendation based on prediction
        
        Args:
            is_suspicious: Whether transaction is flagged
            probability: Fraud probability
        
        Returns:
            Recommended action string
        """
        if is_suspicious:
            if probability >= 0.9:
                return "IMMEDIATE_REVIEW_REQUIRED - High risk transaction"
            elif probability >= 0.7:
                return "MANUAL_REVIEW_REQUIRED - Elevated risk detected"
            else:
                return "AUTOMATED_REVIEW - Moderate risk, verify transaction"
        else:
            return "APPROVE - Low risk transaction"
    
    @staticmethod
    def predict_single(transaction: TransactionInput) -> TransactionPrediction:
        """
        Predict fraud probability for a single transaction
        
        Args:
            transaction: Transaction input data
        
        Returns:
            Prediction with fraud probability and recommendations
        
        Raises:
            RuntimeError: If model is not loaded
            Exception: If prediction fails
        """
        try:
            # Get model and preprocessor
            model = model_loader.get_model()
            preprocessor = model_loader.get_preprocessor()
            
            # Preprocess transaction
            df = PredictionService.preprocess_transaction(transaction)
            
            # Transform using preprocessor
            X_transformed = preprocessor.transform(df)
            
            # Get prediction probability
            probability = model.predict_proba(X_transformed)[0, 1]
            
            # Apply optimal threshold
            is_suspicious = probability >= settings.optimal_threshold
            
            # Calculate risk score (0-100)
            risk_score = probability * 100
            
            # Generate response
            return TransactionPrediction(
                is_suspicious=is_suspicious,
                fraud_probability=float(probability),
                risk_score=float(risk_score),
                confidence=PredictionService.calculate_confidence(probability),
                recommendation=PredictionService.get_recommendation(is_suspicious, probability),
                threshold_used=settings.optimal_threshold
            )
            
        except Exception as e:
            logger.error(f"Prediction error: {str(e)}")
            raise
    
    @staticmethod
    def predict_batch(transactions: List[TransactionInput]) -> Tuple[List[TransactionPrediction], float]:
        """
        Predict fraud probability for multiple transactions
        
        Args:
            transactions: List of transaction input data
        
        Returns:
            Tuple of (predictions list, processing time in ms)
        
        Raises:
            RuntimeError: If model is not loaded
            Exception: If prediction fails
        """
        start_time = datetime.now()
        predictions = []
        
        try:
            for idx, transaction in enumerate(transactions):
                prediction = PredictionService.predict_single(transaction)
                prediction.transaction_id = f"TXN_{idx+1:04d}"
                predictions.append(prediction)
            
            # Calculate processing time
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            return predictions, processing_time
            
        except Exception as e:
            logger.error(f"Batch prediction error: {str(e)}")
            raise


# Global prediction service instance
prediction_service = PredictionService()
