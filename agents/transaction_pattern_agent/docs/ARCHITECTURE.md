# Architecture Diagram - Transaction Pattern Agent

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT (HTTP Requests)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                       main.py (FastAPI App)                      │
│  - Lifespan management                                          │
│  - CORS middleware                                              │
│  - Route registration                                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      API LAYER (app/api/)                        │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │  health.py   │  │   model.py   │  │   predictions.py     │ │
│  │              │  │              │  │                      │ │
│  │ GET /health  │  │ GET /model   │  │ POST /predict        │ │
│  │              │  │    -info     │  │ POST /batch-predict  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘ │
└─────────┼──────────────────┼─────────────────────┼──────────────┘
          │                  │                     │
          ↓                  ↓                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                   SERVICE LAYER (app/services/)                  │
│                                                                  │
│  ┌───────────────────────────┐  ┌────────────────────────────┐ │
│  │   model_loader.py         │  │   prediction_service.py    │ │
│  │                           │  │                            │ │
│  │ - load_models()           │  │ - predict_single()         │ │
│  │ - get_model()             │  │ - predict_batch()          │ │
│  │ - get_preprocessor()      │  │ - preprocess_transaction() │ │
│  │ - get_metadata()          │  │ - calculate_confidence()   │ │
│  │                           │  │ - get_recommendation()     │ │
│  └───────────┬───────────────┘  └──────────────┬─────────────┘ │
└──────────────┼──────────────────────────────────┼───────────────┘
               │                                  │
               ↓                                  ↓
┌─────────────────────────────────────────────────────────────────┐
│                   DATA LAYER (app/models/)                       │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     schemas.py                            │  │
│  │                                                           │  │
│  │  - TransactionInput          (Pydantic Model)            │  │
│  │  - TransactionPrediction     (Pydantic Model)            │  │
│  │  - BatchTransactionInput     (Pydantic Model)            │  │
│  │  - BatchPredictionResponse   (Pydantic Model)            │  │
│  │  - HealthResponse            (Pydantic Model)            │  │
│  │  - ModelInfoResponse         (Pydantic Model)            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────────────────┐
│                  CONFIGURATION (app/core/)                       │
│                                                                  │
│  ┌──────────────────┐         ┌──────────────────────────────┐ │
│  │   config.py      │         │        logging.py            │ │
│  │                  │         │                              │ │
│  │ - Settings class │         │ - setup_logging()            │ │
│  │ - Environment    │         │ - Logger configuration       │ │
│  │   variables      │         │                              │ │
│  └──────────────────┘         └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ML MODELS (trained_models/)                   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  xgboost_transaction_pattern_agent.pkl                   │  │
│  │  preprocessor.pkl                                        │  │
│  │  xgboost_metadata.json                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Request Flow

### Single Prediction Flow

```
1. Client Request
   POST /api/v1/predict
   {transaction_data}
        ↓
2. API Layer (predictions.py)
   - Validate input (Pydantic)
   - Check model loaded
        ↓
3. Service Layer (prediction_service.py)
   - preprocess_transaction()
   - Apply preprocessor transformation
   - Model prediction
   - Calculate risk score
   - Generate recommendation
        ↓
4. Response
   {
     is_suspicious: bool,
     fraud_probability: float,
     risk_score: float,
     confidence: str,
     recommendation: str
   }
```

### Batch Prediction Flow

```
1. Client Request
   POST /api/v1/batch-predict
   {transactions: [tx1, tx2, ...]}
        ↓
2. API Layer (predictions.py)
   - Validate batch (max 1000)
   - Check model loaded
        ↓
3. Service Layer (prediction_service.py)
   - Loop through transactions
   - Process each individually
   - Aggregate statistics
        ↓
4. Response
   {
     total_transactions: int,
     suspicious_count: int,
     legitimate_count: int,
     average_risk_score: float,
     predictions: [...],
     processing_time_ms: float
   }
```

## 🏗️ Layered Architecture

```
┌─────────────────────────────────────┐
│      Presentation Layer             │  ← API Routes (FastAPI)
│      (app/api/)                     │
├─────────────────────────────────────┤
│      Business Logic Layer           │  ← Services (Processing)
│      (app/services/)                │
├─────────────────────────────────────┤
│      Data/Model Layer               │  ← Schemas (Validation)
│      (app/models/)                  │
├─────────────────────────────────────┤
│      Infrastructure Layer           │  ← Config, Logging
│      (app/core/)                    │
└─────────────────────────────────────┘
```

## 📦 Module Dependencies

```
main.py
  └── app/api/
        ├── health.py
        │     └── app/services/model_loader.py
        │           └── app/core/config.py
        │
        ├── model.py
        │     └── app/services/model_loader.py
        │           └── app/core/config.py
        │
        └── predictions.py
              ├── app/services/prediction_service.py
              │     ├── app/models/schemas.py
              │     ├── app/services/model_loader.py
              │     └── app/core/config.py
              └── app/models/schemas.py
```

## 🔌 Dependency Injection Pattern

```python
# Global instances (Singleton pattern)
model_loader = ModelLoader()  # services/model_loader.py
prediction_service = PredictionService()  # services/prediction_service.py

# Injected into routes
@router.post("/predict")
async def predict(transaction: TransactionInput):
    # Uses global prediction_service
    return prediction_service.predict_single(transaction)
```

## 🎯 Single Responsibility Visualization

```
┌──────────────────┐
│   health.py      │  → Health checks ONLY
└──────────────────┘

┌──────────────────┐
│   model.py       │  → Model info ONLY
└──────────────────┘

┌──────────────────┐
│  predictions.py  │  → Predictions ONLY
└──────────────────┘

┌──────────────────┐
│ model_loader.py  │  → Model loading ONLY
└──────────────────┘

┌──────────────────┐
│prediction_svc.py │  → Prediction logic ONLY
└──────────────────┘

┌──────────────────┐
│   schemas.py     │  → Data validation ONLY
└──────────────────┘

┌──────────────────┐
│   config.py      │  → Configuration ONLY
└──────────────────┘

┌──────────────────┐
│   logging.py     │  → Logging setup ONLY
└──────────────────┘
```

## 🚀 Startup Sequence

```
1. main.py starts
   ↓
2. FastAPI app created
   ↓
3. Lifespan context manager
   ↓
4. model_loader.load_models()
   ├── Load XGBoost model
   ├── Load preprocessor
   └── Load metadata
   ↓
5. Register routes
   ↓
6. Start Uvicorn server
   ↓
7. Ready to accept requests ✅
```

## 🔒 Error Handling Flow

```
API Layer
  └── Try/Catch
        ├── ValidationError → 422 Unprocessable Entity
        ├── Service error → 500 Internal Server Error
        └── Model not loaded → 503 Service Unavailable

Service Layer
  └── Try/Catch
        ├── Log error
        └── Raise exception (caught by API layer)
```

## 📊 Data Flow

```
Raw Transaction Data
        ↓
  [TransactionInput]
  Pydantic validation
        ↓
  pandas DataFrame
  Feature engineering
        ↓
  Preprocessor transform
  (OneHotEncoder + StandardScaler)
        ↓
  XGBoost model
  predict_proba()
        ↓
  Probability (0-1)
        ↓
  Risk Score (0-100)
  Confidence Level
  Recommendation
        ↓
  [TransactionPrediction]
  Pydantic serialization
        ↓
  JSON Response
```

---

**This architecture ensures**:
- ✅ Clean separation of concerns
- ✅ Easy to test each layer
- ✅ Easy to extend functionality
- ✅ Production-ready structure
- ✅ Maintainable codebase
