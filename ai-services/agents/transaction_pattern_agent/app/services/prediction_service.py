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
    """
    Handles transaction preprocessing and fraud prediction
    
    NOTE: Model trained with ISO standardized codes:
    - Currencies: ISO 4217 (3-letter: USD, EUR, GBP, etc.)
    - Countries: ISO 3166-1 alpha-2 (2-letter: US, GB, TR, etc.)
    
    API must send data in ISO format to match training data.
    """
    
    @staticmethod
    def preprocess_transaction(transaction: TransactionInput) -> pd.DataFrame:
        """
        Preprocess a single transaction for model input
        
        Args:
            transaction: Transaction input data (aligned with Avro schema)
        
        Returns:
            Preprocessed DataFrame ready for model
            
        Note:
            - Model expects 7 core features: Amount, Payment_currency, Received_currency,
              Sender_bank_location, Receiver_bank_location, Payment_type, plus derived time features
            - Account numbers and bank names are NOT used for prediction
            - ISO codes expected: ISO 4217 (currencies), ISO 3166-1 alpha-2 (countries)
        """
        
        data = {
            'Time': transaction.time,
            'Date': transaction.date,
            'Amount': transaction.amount,
            'Payment_currency': transaction.paymentCurrency,
            'Received_currency': transaction.receivedCurrency,
            'Sender_bank_location': transaction.senderBankLocation,
            'Receiver_bank_location': transaction.receiverBankLocation,
            'Payment_type': transaction.paymentType
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
                prediction.payment_id = f"PAY_{idx+1:04d}"
                predictions.append(prediction)
            
            # Calculate processing time
            processing_time = (datetime.now() - start_time).total_seconds() * 1000
            
            return predictions, processing_time
            
        except Exception as e:
            logger.error(f"Batch prediction error: {str(e)}")
            raise


# Global prediction service instance
prediction_service = PredictionService()
