"""
Prediction service
Handles customer risk predictions using the loaded model
"""

import time
import numpy as np
import pandas as pd
from typing import List, Tuple

from ..models.schemas import CustomerRiskInput, CustomerRiskPrediction
from ..services.model_loader import model_loader
from ..core.logging import logger


class PredictionService:
    """Service for making customer risk predictions"""
    
    def __init__(self):
        pass
    
    def _prepare_features(self, customer_input: CustomerRiskInput) -> pd.DataFrame:
        """
        Prepare customer features for prediction
        
        Args:
            customer_input: Customer risk input with features
        
        Returns:
            DataFrame with features in correct order
        """
        features = customer_input.features.model_dump()
        
        # Create DataFrame with features in the same order as training
        df = pd.DataFrame([features])
        
        # Ensure feature order matches training
        df = df[model_loader.feature_names]
        
        return df
    
    def _calculate_risk_level(self, probability: float) -> str:
        """
        Calculate risk level based on probability
        
        Args:
            probability: Risk probability (0-1)
        
        Returns:
            Risk level string
        """
        if probability >= 0.8:
            return "CRITICAL"
        elif probability >= 0.6:
            return "HIGH"
        elif probability >= 0.4:
            return "MEDIUM"
        else:
            return "LOW"
    
    def _calculate_confidence(self, probability: float) -> str:
        """
        Calculate confidence level based on probability distance from threshold
        
        Args:
            probability: Risk probability (0-1)
        
        Returns:
            Confidence level string
        """
        # Distance from 0.5 threshold
        distance = abs(probability - 0.5)
        
        if distance >= 0.3:
            return "HIGH"
        elif distance >= 0.15:
            return "MEDIUM"
        else:
            return "LOW"
    
    def _get_recommendation(self, probability: float, risk_level: str) -> str:
        """
        Get recommendation based on risk assessment
        
        Args:
            probability: Risk probability
            risk_level: Calculated risk level
        
        Returns:
            Recommendation string
        """
        if risk_level == "CRITICAL":
            return "IMMEDIATE REVIEW REQUIRED - Flag for enhanced due diligence (EDD) and regulatory reporting"
        elif risk_level == "HIGH":
            return "PRIORITY REVIEW - Escalate to compliance team for investigation"
        elif risk_level == "MEDIUM":
            return "MONITOR CLOSELY - Increase transaction monitoring frequency"
        else:
            return "STANDARD MONITORING - Continue regular oversight"
    
    def _get_contributing_factors(self, features_df: pd.DataFrame) -> List[str]:
        """
        Identify key contributing risk factors (simplified version)
        In production, this could use SHAP values for more accuracy
        
        Args:
            features_df: Customer features DataFrame
        
        Returns:
            List of contributing factors
        """
        factors = []
        
        # Check key risk indicators
        if features_df['cross_border_ratio'].iloc[0] > 0.5:
            factors.append(f"High cross-border activity ({features_df['cross_border_ratio'].iloc[0]:.1%})")
        
        if features_df['night_transaction_ratio'].iloc[0] > 0.3:
            factors.append(f"Unusual timing patterns ({features_df['night_transaction_ratio'].iloc[0]:.1%} at night)")
        
        if features_df['large_transaction_ratio'].iloc[0] > 0.3:
            factors.append(f"High proportion of large transactions ({features_df['large_transaction_ratio'].iloc[0]:.1%})")
        
        if features_df['unique_receiver_countries'].iloc[0] > 10:
            factors.append(f"High geographic diversity ({features_df['unique_receiver_countries'].iloc[0]} countries)")
        
        if features_df['receiver_diversity'].iloc[0] > 0.7:
            factors.append(f"Dispersed transaction network (diversity: {features_df['receiver_diversity'].iloc[0]:.2f})")
        
        if features_df['transactions_per_day'].iloc[0] > 3:
            factors.append(f"High transaction velocity ({features_df['transactions_per_day'].iloc[0]:.1f} txns/day)")
        
        return factors[:5]  # Return top 5 factors
    
    def predict_single(self, customer_input: CustomerRiskInput) -> CustomerRiskPrediction:
        """
        Predict risk for a single customer
        
        Args:
            customer_input: Customer data with features
        
        Returns:
            Customer risk prediction
        """
        try:
            # Prepare features
            features_df = self._prepare_features(customer_input)
            
            # Scale features
            features_scaled = model_loader.scaler.transform(features_df)
            
            # Make prediction
            probability = model_loader.model.predict_proba(features_scaled)[0, 1]
            is_high_risk = bool(probability > 0.5)
            risk_score = float(probability * 100)
            
            # Calculate derived metrics
            risk_level = self._calculate_risk_level(probability)
            confidence = self._calculate_confidence(probability)
            recommendation = self._get_recommendation(probability, risk_level)
            contributing_factors = self._get_contributing_factors(features_df) if is_high_risk else None
            
            return CustomerRiskPrediction(
                customer_id=customer_input.customer_id,
                is_high_risk=is_high_risk,
                risk_probability=float(probability),
                risk_score=risk_score,
                risk_level=risk_level,
                confidence=confidence,
                recommendation=recommendation,
                contributing_factors=contributing_factors
            )
            
        except Exception as e:
            logger.error(f"Prediction error for customer {customer_input.customer_id}: {str(e)}")
            raise
    
    def predict_batch(self, customers: List[CustomerRiskInput]) -> Tuple[List[CustomerRiskPrediction], float]:
        """
        Predict risk for multiple customers in batch
        
        Args:
            customers: List of customer data
        
        Returns:
            Tuple of (predictions list, processing time in ms)
        """
        start_time = time.time()
        
        try:
            predictions = []
            
            # Process each customer
            for customer in customers:
                prediction = self.predict_single(customer)
                predictions.append(prediction)
            
            processing_time = (time.time() - start_time) * 1000  # Convert to ms
            
            logger.info(f"Batch prediction completed: {len(customers)} customers in {processing_time:.2f}ms")
            
            return predictions, processing_time
            
        except Exception as e:
            logger.error(f"Batch prediction error: {str(e)}")
            raise


# Global prediction service instance
prediction_service = PredictionService()
