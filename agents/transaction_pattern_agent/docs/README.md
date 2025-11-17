# Transaction Pattern Agent - FastAPI Service

## Overview
FastAPI-based microservice for detecting suspicious transaction patterns using the trained XGBoost model (89.39% recall).

## Architecture
- **Framework**: FastAPI
- **Model**: XGBoost Classifier
- **Threshold**: 0.13 (optimized for 85-90% recall)
- **Port**: 8001

## Features
- ✅ Single transaction prediction
- ✅ Batch transaction processing (up to 1000 transactions)
- ✅ Real-time fraud probability scoring
- ✅ Risk-based recommendations
- ✅ Model metadata and performance metrics
- ✅ Health check endpoint
- ✅ Interactive API documentation (Swagger/ReDoc)

## Installation

### Local Development

1. **Install dependencies:**
```bash
cd agents/transaction_pattern_agent
pip install -r requirements.txt
```

2. **Ensure trained model exists:**
```bash
ls trained_models/
# Should contain:
# - xgboost_transaction_pattern_agent.pkl
# - preprocessor.pkl
# - xgboost_metadata.json
```

3. **Run the application:**
```bash
python app.py
# Or using uvicorn directly:
uvicorn app:app --host 0.0.0.0 --port 8001 --reload
```

### Docker Deployment

1. **Build the Docker image:**
```bash
docker build -t transaction-pattern-agent .
```

2. **Run the container:**
```bash
docker run -d -p 8001:8001 --name transaction-pattern-agent transaction-pattern-agent
```

3. **Check container health:**
```bash
docker ps
docker logs transaction-pattern-agent
```

### Docker Compose (Multi-Agent System)

```bash
# From project root
docker-compose up -d transaction-pattern-agent
```

## API Endpoints

### 1. Root Endpoint
```
GET /
```
Returns API information and available endpoints.

### 2. Health Check
```
GET /health
```
**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "preprocessor_loaded": true,
  "timestamp": "2024-11-17T10:30:00",
  "model_info": {
    "type": "XGBClassifier",
    "threshold": 0.13
  }
}
```

### 3. Model Information
```
GET /api/v1/model-info
```
Returns detailed model metadata and performance metrics.

### 4. Single Transaction Prediction
```
POST /api/v1/predict
```

**Request Body:**
```json
{
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
```

**Response:**
```json
{
  "is_suspicious": true,
  "fraud_probability": 0.87,
  "risk_score": 87.0,
  "confidence": "HIGH",
  "recommendation": "IMMEDIATE_REVIEW_REQUIRED - High risk transaction",
  "threshold_used": 0.13
}
```

### 5. Batch Transaction Prediction
```
POST /api/v1/batch-predict
```

**Request Body:**
```json
{
  "transactions": [
    {
      "Date": "2024-01-15",
      "Time": "14:30:00",
      ...
    },
    {
      "Date": "2024-01-16",
      "Time": "09:15:00",
      ...
    }
  ]
}
```

**Response:**
```json
{
  "total_transactions": 100,
  "suspicious_count": 23,
  "legitimate_count": 77,
  "average_risk_score": 34.5,
  "predictions": [...],
  "processing_time_ms": 125.3
}
```

## Interactive Documentation

Once the service is running, access:

- **Swagger UI**: http://localhost:8001/docs
- **ReDoc**: http://localhost:8001/redoc

## Testing with cURL

### Health Check
```bash
curl -X GET "http://localhost:8001/health"
```

### Single Prediction
```bash
curl -X POST "http://localhost:8001/api/v1/predict" \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

## Testing with Python

```python
import requests

# Health check
response = requests.get("http://localhost:8001/health")
print(response.json())

# Single prediction
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
```

## Performance

- **Single Prediction**: ~5-10ms
- **Batch (100 transactions)**: ~100-150ms
- **Model Accuracy**: 89.39% fraud detection rate
- **False Positive Rate**: 75.2%

## Risk Scoring

| Probability | Risk Score | Confidence | Action |
|-------------|-----------|------------|--------|
| ≥ 0.90 | 90-100 | HIGH | Immediate Review Required |
| 0.70-0.89 | 70-89 | HIGH | Manual Review Required |
| 0.50-0.69 | 50-69 | MEDIUM | Automated Review |
| 0.13-0.49 | 13-49 | LOW | Flag for Monitoring |
| < 0.13 | 0-12 | LOW | Approve |

## Logging

Application logs include:
- Model loading status
- Prediction requests
- Error tracking
- Performance metrics

View logs:
```bash
# Docker
docker logs transaction-pattern-agent

# Local
# Logs printed to stdout
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | 8001 | Service port |
| `MODEL_PATH` | ./trained_models | Path to model files |
| `LOG_LEVEL` | info | Logging level |

## Troubleshooting

### Model Not Loading
```bash
# Check if model files exist
ls -la trained_models/

# Expected files:
# - xgboost_transaction_pattern_agent.pkl
# - preprocessor.pkl
# - xgboost_metadata.json
```

### Port Already in Use
```bash
# Change port in app.py or use environment variable
PORT=8005 python app.py
```

### Docker Build Issues
```bash
# Clean build
docker build --no-cache -t transaction-pattern-agent .
```

## Next Steps

1. **Async Communication**: Add message queue integration (RabbitMQ/Kafka)
2. **Agent Orchestration**: Implement inter-agent communication
3. **Monitoring**: Add Prometheus metrics and Grafana dashboards
4. **Authentication**: Implement JWT/OAuth2 security
5. **Rate Limiting**: Add request throttling
6. **Caching**: Implement Redis for frequently accessed data

## License
Part of Master's Thesis: Multi-Agents in Fintech Regulatory Compliance
