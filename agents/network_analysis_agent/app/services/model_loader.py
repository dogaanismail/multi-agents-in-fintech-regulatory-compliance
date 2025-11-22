"""
Model loader service
Loads and manages the trained CatBoost model, scaler, and metadata
"""

import pickle
import json
from pathlib import Path
from typing import Any, Dict, List, Optional

from ..core.config import settings
from ..core.logging import logger


class ModelLoader:
    """Service for loading and managing ML model artifacts"""
    
    def __init__(self):
        self.model: Optional[Any] = None
        self.scaler: Optional[Any] = None
        self.metadata: Optional[Dict] = None
        self.feature_names: Optional[List[str]] = None
        self._loaded = False
    
    def load_models(self) -> None:
        """
        Load model, scaler, and metadata from disk
        
        Raises:
            FileNotFoundError: If model files are not found
            Exception: If loading fails
        """
        try:
            model_path = settings.model_dir / settings.model_filename
            scaler_path = settings.model_dir / settings.scaler_filename
            metadata_path = settings.model_dir / settings.metadata_filename
            
            # Check if files exist
            if not model_path.exists():
                raise FileNotFoundError(f"Model file not found: {model_path}")
            if not scaler_path.exists():
                raise FileNotFoundError(f"Scaler file not found: {scaler_path}")
            if not metadata_path.exists():
                raise FileNotFoundError(f"Metadata file not found: {metadata_path}")
            
            # Load model
            logger.info(f"Loading model from {model_path}")
            with open(model_path, 'rb') as f:
                self.model = pickle.load(f)
            
            # Load scaler
            logger.info(f"Loading scaler from {scaler_path}")
            with open(scaler_path, 'rb') as f:
                self.scaler = pickle.load(f)
            
            # Load metadata
            logger.info(f"Loading metadata from {metadata_path}")
            with open(metadata_path, 'r') as f:
                self.metadata = json.load(f)
            
            # Extract feature names from metadata
            self.feature_names = self.metadata.get('features', {}).get('list', [])
            
            self._loaded = True
            
            logger.info("✅ Model artifacts loaded successfully")
            logger.info(f"   Model: {self.metadata.get('model_info', {}).get('name', 'Unknown')}")
            logger.info(f"   Features: {len(self.feature_names)}")
            logger.info(f"   ROC-AUC: {self.metadata.get('performance', {}).get('roc_auc', 0):.4f}")
            
        except Exception as e:
            logger.error(f"❌ Failed to load model artifacts: {str(e)}")
            raise
    
    def is_loaded(self) -> bool:
        """Check if models are loaded"""
        return self._loaded and self.model is not None and self.scaler is not None
    
    def get_model_info(self) -> Dict[str, Any]:
        """
        Get model information
        
        Returns:
            Dictionary with model information
        """
        if not self.is_loaded():
            return {"status": "not_loaded"}
        
        return {
            "model_name": self.metadata.get('model_info', {}).get('name', 'Unknown'),
            "model_type": self.metadata.get('model_info', {}).get('type', 'Unknown'),
            "version": self.metadata.get('model_info', {}).get('version', 'Unknown'),
            "created_at": self.metadata.get('model_info', {}).get('created_at', 'Unknown'),
            "num_features": len(self.feature_names) if self.feature_names else 0,
            "feature_names": self.feature_names,
            "performance": self.metadata.get('performance', {}),
            "network_stats": self.metadata.get('network_stats', {}),
            "training_config": self.metadata.get('config', {})
        }


# Global model loader instance
model_loader = ModelLoader()
