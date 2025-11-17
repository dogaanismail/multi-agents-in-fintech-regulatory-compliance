"""
Model loading and management service
"""

import joblib
import json
from pathlib import Path
from typing import Optional, Dict, Any

from ..core.config import settings
from ..core.logging import logger


class ModelLoader:
    """Handles loading and managing ML models and preprocessors"""
    
    def __init__(self):
        self.model = None
        self.preprocessor = None
        self.metadata: Optional[Dict[str, Any]] = None
        self._is_loaded = False
    
    def load_models(self) -> None:
        """
        Load trained model, preprocessor, and metadata
        
        Raises:
            FileNotFoundError: If model files are not found
            Exception: If loading fails
        """
        try:
            model_dir = settings.model_dir
            
            # Load XGBoost model
            model_path = model_dir / settings.model_filename
            if not model_path.exists():
                raise FileNotFoundError(f"Model file not found: {model_path}")
            
            self.model = joblib.load(model_path)
            logger.info(f"✅ Model loaded from: {model_path}")
            
            # Load preprocessor
            preprocessor_path = model_dir / settings.preprocessor_filename
            if not preprocessor_path.exists():
                raise FileNotFoundError(f"Preprocessor file not found: {preprocessor_path}")
            
            self.preprocessor = joblib.load(preprocessor_path)
            logger.info(f"✅ Preprocessor loaded from: {preprocessor_path}")
            
            # Load metadata (optional)
            metadata_path = model_dir / settings.metadata_filename
            if metadata_path.exists():
                with open(metadata_path, 'r') as f:
                    self.metadata = json.load(f)
                logger.info(f"✅ Metadata loaded from: {metadata_path}")
            
            self._is_loaded = True
            logger.info("🚀 Transaction Pattern Agent models are ready!")
            
        except Exception as e:
            logger.error(f"❌ Error loading models: {str(e)}")
            raise
    
    @property
    def is_loaded(self) -> bool:
        """Check if models are loaded"""
        return self._is_loaded and self.model is not None and self.preprocessor is not None
    
    def get_model(self):
        """Get the loaded model"""
        if not self.is_loaded:
            raise RuntimeError("Model not loaded. Call load_models() first.")
        return self.model
    
    def get_preprocessor(self):
        """Get the loaded preprocessor"""
        if not self.is_loaded:
            raise RuntimeError("Preprocessor not loaded. Call load_models() first.")
        return self.preprocessor
    
    def get_metadata(self) -> Optional[Dict[str, Any]]:
        """Get model metadata"""
        return self.metadata


# Global model loader instance
model_loader = ModelLoader()
