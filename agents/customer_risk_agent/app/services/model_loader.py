"""
Model loader service
Handles loading of the trained customer risk model and scaler
"""

import pickle
from pathlib import Path
from typing import Optional, Dict, Any

from ..core.config import settings
from ..core.logging import logger


class ModelLoader:
    """Singleton class for loading and managing ML models"""
    
    def __init__(self):
        self.model = None
        self.scaler = None
        self.feature_names = None
        self.model_metadata = None
        self._is_loaded = False
    
    def load_models(self) -> None:
        """
        Load the trained customer risk model and associated artifacts
        
        Raises:
            FileNotFoundError: If model file doesn't exist
            Exception: If loading fails
        """
        model_path = settings.model_dir / settings.model_filename
        
        if not model_path.exists():
            raise FileNotFoundError(
                f"Model file not found: {model_path}\n"
                f"Please ensure you've trained the model by running the Jupyter notebook:\n"
                f"agents/customer_risk_agent/notebooks/01_CustomerRiskAgent_Baseline.ipynb"
            )
        
        try:
            # Load the pickled model artifacts
            logger.info(f"Loading model from: {model_path}")
            
            with open(model_path, 'rb') as f:
                artifacts = pickle.load(f)
            
            # Extract components
            self.model = artifacts['model']
            self.scaler = artifacts['scaler']
            self.feature_names = artifacts['feature_names']
            
            # Store metadata
            self.model_metadata = {
                'model_name': artifacts['model_name'],
                'training_date': artifacts['training_date'],
                'metrics': artifacts['metrics'],
                'training_config': artifacts['training_config'],
                'training_samples': artifacts['training_samples'],
                'test_samples': artifacts['test_samples']
            }
            
            self._is_loaded = True
            
            logger.info(f"✅ Model loaded successfully: {artifacts['model_name']}")
            logger.info(f"   Features: {len(self.feature_names)}")
            logger.info(f"   Training date: {artifacts['training_date']}")
            logger.info(f"   ROC-AUC: {artifacts['metrics']['roc_auc']:.4f}")
            
        except Exception as e:
            logger.error(f"Failed to load model: {str(e)}")
            raise
    
    @property
    def is_loaded(self) -> bool:
        """Check if model is loaded"""
        return self._is_loaded
    
    def get_model_info(self) -> Optional[Dict[str, Any]]:
        """Get model metadata"""
        return self.model_metadata if self._is_loaded else None


# Global model loader instance
model_loader = ModelLoader()
