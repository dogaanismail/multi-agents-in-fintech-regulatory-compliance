"""
Prediction service
Handles account risk predictions using network topology features
"""

import time
import numpy as np
import pandas as pd
from typing import List, Tuple, Dict, Any

from ..models.schemas import AccountRiskInput, AccountRiskPrediction
from ..services.model_loader import model_loader
from ..core.logging import logger


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
            "centrality_metrics": {
                "pagerank": float(features_df['pagerank'].iloc[0]),
                "eigenvector_centrality": float(features_df['eigenvector_centrality'].iloc[0]),
                "betweenness_centrality": float(features_df['betweenness_centrality'].iloc[0]),
                "closeness_centrality": float(features_df['closeness_centrality'].iloc[0])
            },
            "connectivity": {
                "in_degree": int(features_df['in_degree'].iloc[0]),
                "out_degree": int(features_df['out_degree'].iloc[0]),
                "total_degree": int(features_df['in_degree'].iloc[0]) + int(features_df['out_degree'].iloc[0])
            },
            "network_position": {
                "clustering_coefficient": float(features_df['clustering_coefficient'].iloc[0]),
                "community_id": int(features_df['community'].iloc[0])
            }
        }
        
        # Add risk flags
        risk_flags = []
        if features_df['eigenvector_centrality'].iloc[0] > 0.02:
            risk_flags.append("High eigenvector centrality - connected to important nodes")
        if features_df['pagerank'].iloc[0] > 0.001:
            risk_flags.append("High PageRank - influential position in network")
        if features_df['betweenness_centrality'].iloc[0] > 0.01:
            risk_flags.append("High betweenness - acts as bridge between network clusters")
        if features_df['out_degree'].iloc[0] > 50:
            risk_flags.append("High out-degree - dispersed transaction pattern")
        if features_df['in_degree'].iloc[0] > 50:
            risk_flags.append("High in-degree - receiving from many sources")
        
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
            
            return AccountRiskPrediction(
                account_id=account_input.account_id,
                is_suspicious=is_suspicious,
                suspicion_probability=float(probability),
                risk_score=risk_score,
                risk_level=risk_level,
                confidence=confidence,
                recommendation=recommendation,
                network_indicators=network_indicators
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
