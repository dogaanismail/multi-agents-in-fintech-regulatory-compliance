# Transaction Pattern Agent - Project Structure

## 📁 Project Organization

```
transaction_pattern_agent/
├── app/
│   ├── __init__.py
│   ├── api/                    # API routes and endpoints
│   │   ├── __init__.py
│   │   ├── health.py          # Health check endpoints
│   │   ├── model.py           # Model info endpoints
│   │   └── predictions.py     # Prediction endpoints
│   ├── core/                   # Core configurations
│   │   ├── __init__.py
│   │   ├── config.py          # Application settings
│   │   └── logging.py         # Logging configuration
│   ├── models/                 # Pydantic models/schemas
│   │   ├── __init__.py
│   │   └── schemas.py         # Request/response schemas
│   └── services/               # Business logic
│       ├── __init__.py
│       ├── model_loader.py    # ML model loading
│       └── prediction_service.py  # Prediction logic
├── trained_models/             # ML models and artifacts
│   ├── xgboost_transaction_pattern_agent.pkl
│   ├── preprocessor.pkl
│   └── xgboost_metadata.json
├── notebooks/                  # Jupyter notebooks for training
├── data/                       # Training data
├── main.py                     # FastAPI application entry point
├── requirements.txt            # Python dependencies
├── Dockerfile                  # Docker configuration
├── .env.example               # Environment variables template
└── README.md                  # This file
```

## 🏗️ Architecture Overview

### Single Responsibility Principle

Each module has a clear, focused purpose:

- **`app/api/`**: HTTP request handling and routing
- **`app/core/`**: Configuration and cross-cutting concerns
- **`app/models/`**: Data validation and serialization
- **`app/services/`**: Business logic and ML inference

### Key Components

#### 1. **Core Configuration (`app/core/`)**

**`config.py`** - Centralized settings management
- Uses `pydantic-settings` for type-safe configuration
- Supports environment variables
- Provides default values

**`logging.py`** - Structured logging
- Consistent logging format
- Configurable log levels
- Easy to extend

#### 2. **Data Models (`app/models/`)**

**`schemas.py`** - Pydantic schemas
- Input validation
- Output serialization
- API documentation
- Type safety

#### 3. **Services (`app/services/`)**

**`model_loader.py`** - Model management
- Lazy loading of ML models
- Singleton pattern
- Error handling

**`prediction_service.py`** - Prediction logic
- Transaction preprocessing
- Fraud detection
- Risk scoring
- Recommendations

#### 4. **API Routes (`app/api/`)**

**`health.py`** - System health
- Model status
- Readiness checks

**`model.py`** - Model metadata
- Performance metrics
- Hyperparameters

**`predictions.py`** - Core functionality
- Single predictions
- Batch predictions
- Error handling

## 🚀 Quick Start

### 1. Installation

```bash
cd transaction_pattern_agent
pip install -r requirements.txt
```

### 2. Configuration

```bash
# Copy environment template
cp .env.example .env

# Edit configuration (optional)
nano .env
```

### 3. Run Application

```bash
# Development mode
python main.py

# Production mode with Uvicorn
uvicorn main:app --host 0.0.0.0 --port 8001 --workers 4

# With Docker
docker build -t transaction-pattern-agent .
docker run -p 8001:8001 transaction-pattern-agent
```

## 📡 API Usage

### Access Documentation

- **Swagger UI**: http://localhost:8001/docs
- **ReDoc**: http://localhost:8001/redoc

### Example: Single Prediction

```python
import requests

transaction = {
    "Date": "2024-01-15",
    "Time": "14:30:00",
    "From_Bank": "HSBC Bank",
    "Account": "ACC123456",
    "To_Bank": "Chase Bank",
    "Account_1": "ACC789012",
    "Amount_Received": 15000.50,
    "Receiving_Currency": "USD",
    "Amount_Paid": 15000.50,
    "Payment_Currency": "USD",
    "Payment_type": "Wire",
    "Sender_bank_location": "USA",
    "Receiver_bank_location": "UK"
}

response = requests.post(
    "http://localhost:8001/api/v1/predict",
    json=transaction
)

print(response.json())
# {
#   "is_suspicious": true,
#   "fraud_probability": 0.87,
#   "risk_score": 87.0,
#   "confidence": "HIGH",
#   "recommendation": "IMMEDIATE_REVIEW_REQUIRED",
#   "threshold_used": 0.13
# }
```

## 🔧 Development

### Adding New Features

1. **New Endpoint**:
   - Create route in `app/api/`
   - Define schemas in `app/models/schemas.py`
   - Register in `app/api/__init__.py`

2. **New Service**:
   - Create service in `app/services/`
   - Implement business logic
   - Use dependency injection

3. **Configuration**:
   - Add setting to `app/core/config.py`
   - Update `.env.example`
   - Document in README

### Code Style

```python
# Use type hints
def predict_single(transaction: TransactionInput) -> TransactionPrediction:
    ...

# Use descriptive names
def calculate_confidence(probability: float) -> str:
    ...

# Add docstrings
"""
Calculate confidence level based on probability

Args:
    probability: Fraud probability (0-1)

Returns:
    Confidence level: LOW, MEDIUM, or HIGH
"""
```

## 🧪 Testing

### Unit Tests

```bash
pytest tests/unit/
```

### Integration Tests

```bash
pytest tests/integration/
```

### Load Tests

```bash
# Using Apache Bench
ab -n 1000 -c 10 http://localhost:8001/health

# Using Locust
locust -f tests/load/locustfile.py
```

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8001/health
```

### Model Info

```bash
curl http://localhost:8001/api/v1/model-info
```

### Logs

```bash
# Docker
docker logs -f transaction-pattern-agent

# Local
tail -f logs/app.log
```

## 🔒 Security

### Best Practices

1. **Environment Variables**: Never commit `.env` files
2. **API Keys**: Use environment variables for secrets
3. **CORS**: Configure properly for production
4. **Rate Limiting**: Implement for public APIs
5. **Input Validation**: Always validate with Pydantic

## 📈 Performance

### Optimization Tips

1. **Model Loading**: Load once on startup (done ✅)
2. **Batch Processing**: Use for multiple predictions
3. **Caching**: Cache frequent predictions
4. **Async**: Use async endpoints for I/O operations

### Benchmarks

- Single prediction: ~5-10ms
- Batch (100): ~100-150ms
- Throughput: ~200 req/sec (single worker)

## 🐛 Troubleshooting

### Model Not Loading

```bash
# Check model files exist
ls -la trained_models/

# Check permissions
chmod 644 trained_models/*.pkl
```

### Import Errors

```bash
# Reinstall dependencies
pip install -r requirements.txt --force-reinstall
```

### Port Already in Use

```bash
# Change port in .env or command line
PORT=8005 python main.py
```

## 🚢 Deployment

### Docker

```bash
docker build -t transaction-pattern-agent:v1.0.0 .
docker push your-registry/transaction-pattern-agent:v1.0.0
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transaction-pattern-agent
spec:
  replicas: 3
  selector:
    matchLabels:
      app: transaction-pattern-agent
  template:
    metadata:
      labels:
        app: transaction-pattern-agent
    spec:
      containers:
      - name: api
        image: transaction-pattern-agent:v1.0.0
        ports:
        - containerPort: 8001
```

## 📚 Additional Resources

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Pydantic Documentation](https://docs.pydantic.dev/)
- [XGBoost Documentation](https://xgboost.readthedocs.io/)

## 📝 License

Part of Master's Thesis: Multi-Agents in Fintech Regulatory Compliance
