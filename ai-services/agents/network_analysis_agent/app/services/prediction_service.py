"""
Prediction service
Handles account risk predictions using network topology features
"""

import time
import numpy as np
import pandas as pd
from typing import List, Tuple, Dict, Any

from ..models.schemas import AccountRiskInput, AccountRiskPrediction, FeatureContribution
from ..services.model_loader import model_loader
from ..core.logging import logger
from .explainability_service import explainability_service


class PredictionService:
    """Service for making account risk predictions based on network topology"""
    
    def __init__(self):
        pass
    
    def _prepare_features(self, account_input: AccountRiskInput) -> pd.DataFrame:
        """
        Prepare account network features for prediction
        
        Args:
            account_input: Account risk input with topology features
        
        Returns:
            DataFrame with features in correct order
        """
        features = account_input.features.model_dump()
        
        # Create DataFrame with features in the same order as training
        df = pd.DataFrame([features])
        
        # Ensure feature order matches training
        df = df[model_loader.feature_names]
        
        return df
    
    def _calculate_risk_level(self, probability: float) -> str:
        """
        Calculate risk level based on probability
        
        Args:
            probability: Suspicion probability (0-1)
        
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
            probability: Suspicion probability (0-1)
        
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
            probability: Suspicion probability
            risk_level: Calculated risk level
        
        Returns:
            Recommendation string
        """
        if risk_level == "CRITICAL":
            return "IMMEDIATE ACTION - Freeze account and initiate comprehensive investigation"
        elif risk_level == "HIGH":
            return "URGENT REVIEW - Flag for AML investigation and enhanced monitoring"
        elif risk_level == "MEDIUM":
            return "MONITOR CLOSELY - Increase surveillance and transaction limits review"
        else:
            return "STANDARD MONITORING - Continue routine oversight"
    
    def _get_network_indicators(self, features_df: pd.DataFrame) -> Dict[str, Any]:
        """
        Extract key network topology indicators
        
        Args:
            features_df: Account features DataFrame
        
        Returns:
            Dictionary of network indicators
        """
        indicators = {
            "ring_structure": {
                "reciprocity": float(features_df['reciprocity'].iloc[0]),
                "cycle3_count": int(features_df['cycle3_count'].iloc[0]),
                "two_hop_out_reach": int(features_df['two_hop_out_reach'].iloc[0])
            },
            "connectivity": {
                "unique_in_counterparties": int(features_df['unique_in_counterparties'].iloc[0]),
                "unique_out_counterparties": int(features_df['unique_out_counterparties'].iloc[0])
            },
            "money_flow": {
                "in_out_amount_ratio": float(features_df['in_out_amount_ratio'].iloc[0]),
                "forwarding_gap_hours": float(features_df['forwarding_gap_hours'].iloc[0]),
                "in_concentration": float(features_df['in_concentration'].iloc[0]),
                "out_concentration": float(features_df['out_concentration'].iloc[0]),
                "peak_day_share": float(features_df['peak_day_share'].iloc[0])
            }
        }

        # Add risk flags
        risk_flags = []
        if features_df['cycle3_count'].iloc[0] > 0:
            risk_flags.append("Account sits on a directed transaction cycle - possible laundering ring")
        if features_df['reciprocity'].iloc[0] > 0.3:
            risk_flags.append("High reciprocity - money flows back and forth with the same counterparties")
        if 0.7 <= features_df['in_out_amount_ratio'].iloc[0] <= 1.3 and features_df['forwarding_gap_hours'].iloc[
            0] < 24:
            risk_flags.append("Pass-through pattern - forwards received amounts within a day")
        if features_df['unique_out_counterparties'].iloc[0] > 20 and features_df['out_concentration'].iloc[0] < 0.1:
            risk_flags.append("Fan-out pattern - disperses money across many counterparties")
        if features_df['unique_in_counterparties'].iloc[0] > 20 and features_df['in_concentration'].iloc[0] < 0.1:
            risk_flags.append("Fan-in pattern - collects money from many sources")
        if features_df['peak_day_share'].iloc[0] > 0.6 and features_df['unique_in_counterparties'].iloc[0] + \
                features_df['unique_out_counterparties'].iloc[0] > 5:
            risk_flags.append("Bursty activity - most payments concentrated in a single day")

        indicators["risk_flags"] = risk_flags

        return indicators
    
    def predict_single(self, account_input: AccountRiskInput) -> AccountRiskPrediction:
        """
        Predict risk for a single account
        
        Args:
            account_input: Account data with network features
        
        Returns:
            Account risk prediction
        """
        try:
            # Prepare features
            features_df = self._prepare_features(account_input)
            
            # Scale features
            features_scaled = model_loader.scaler.transform(features_df)
            
            # Make prediction
            probability = model_loader.model.predict_proba(features_scaled)[0, 1]
            is_suspicious = bool(probability > 0.5)
            risk_score = float(probability * 100)
            
            # Calculate derived metrics
            risk_level = self._calculate_risk_level(probability)
            confidence = self._calculate_confidence(probability)
            recommendation = self._get_recommendation(probability, risk_level)
            network_indicators = self._get_network_indicators(features_df) if is_suspicious else None
            shap_base_value, contributions = explainability_service.explain(features_scaled, features_df)
            
            return AccountRiskPrediction(
                account_id=account_input.account_id,
                is_suspicious=is_suspicious,
                suspicion_probability=float(probability),
                risk_score=risk_score,
                risk_level=risk_level,
                confidence=confidence,
                recommendation=recommendation,
                network_indicators=network_indicators,
                feature_contributions=[FeatureContribution(**c) for c in contributions] or None,
                shap_base_value=shap_base_value
            )
            
        except Exception as e:
            logger.error(f"Prediction error for account {account_input.account_id}: {str(e)}")
            raise
    
    def predict_batch(self, accounts: List[AccountRiskInput]) -> Tuple[List[AccountRiskPrediction], float]:
        """
        Predict risk for multiple accounts in batch
        
        Args:
            accounts: List of account data
        
        Returns:
            Tuple of (predictions list, processing time in ms)
        """
        start_time = time.time()
        
        try:
            predictions = []
            
            # Process each account
            for account in accounts:
                prediction = self.predict_single(account)
                predictions.append(prediction)
            
            processing_time = (time.time() - start_time) * 1000  # Convert to ms
            
            logger.info(f"Batch prediction completed: {len(accounts)} accounts in {processing_time:.2f}ms")
            
            return predictions, processing_time
            
        except Exception as e:
            logger.error(f"Batch prediction error: {str(e)}")
            raise


# Global prediction service instance
prediction_service = PredictionService()
