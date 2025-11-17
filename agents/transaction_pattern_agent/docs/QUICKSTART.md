# Quick Start Guide - Transaction Pattern Agent

## 🎯 Get Started in 5 Minutes

### Step 1: Install Dependencies

```bash
cd agents/transaction_pattern_agent
pip install -r requirements.txt
```

### Step 2: Verify Model Files

```bash
ls trained_models/
# Expected output:
# xgboost_transaction_pattern_agent.pkl
# preprocessor.pkl
# xgboost_metadata.json
```

### Step 3: Run the Application

```bash
python main.py
```

Expected output:
```
🚀 Starting Transaction Pattern Agent...
Environment: development
Version: 1.0.0
✅ Model loaded from: trained_models/xgboost_transaction_pattern_agent.pkl
✅ Preprocessor loaded from: trained_models/preprocessor.pkl
✅ Metadata loaded from: trained_models/xgboost_metadata.json
🚀 Transaction Pattern Agent models are ready!
✅ Models loaded successfully
INFO:     Started server process
INFO:     Uvicorn running on http://0.0.0.0:8001
```

### Step 4: Test the API

Open your browser: **http://localhost:8001/docs**

Or test with curl:

```bash
# Health check
curl http://localhost:8001/health

# Test prediction
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

## 🐍 Python Client Example

```python
import requests

# Initialize client
BASE_URL = "http://localhost:8001/api/v1"

# Health check
health = requests.get("http://localhost:8001/health")
print("Service Status:", health.json()["status"])

# Make prediction
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

response = requests.post(f"{BASE_URL}/predict", json=transaction)
result = response.json()

print(f"Suspicious: {result['is_suspicious']}")
print(f"Probability: {result['fraud_probability']:.2%}")
print(f"Risk Score: {result['risk_score']:.1f}/100")
print(f"Recommendation: {result['recommendation']}")
```

## 🐳 Docker Quick Start

```bash
# Build image
docker build -t transaction-pattern-agent .

# Run container
docker run -d -p 8001:8001 --name tpa transaction-pattern-agent

# View logs
docker logs -f tpa

# Stop container
docker stop tpa
```

## 📦 Project Structure at a Glance

```
app/
├── api/              # Routes (health, model, predictions)
├── core/             # Config & logging
├── models/           # Pydantic schemas
└── services/         # Business logic (model_loader, prediction_service)

main.py               # Application entry point
```

## 🔗 Useful Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/` | GET | API info |
| `/health` | GET | Health status |
| `/docs` | GET | Swagger UI |
| `/redoc` | GET | ReDoc UI |
| `/api/v1/model-info` | GET | Model metadata |
| `/api/v1/predict` | POST | Single prediction |
| `/api/v1/batch-predict` | POST | Batch predictions |

## 🎓 Next Steps

1. **Read**: `STRUCTURE.md` for detailed architecture
2. **Explore**: API documentation at `/docs`
3. **Customize**: Settings in `app/core/config.py`
4. **Extend**: Add new features following single responsibility pattern

## 💡 Tips

- Use `.env` file for configuration (see `.env.example`)
- Check logs for debugging
- Use batch endpoint for multiple transactions
- Monitor `/health` for service status

## ❓ Common Issues

**Port 8001 already in use?**
```bash
PORT=8005 python main.py
```

**Model not found?**
```bash
# Ensure you're in the right directory
cd agents/transaction_pattern_agent
python main.py
```

**Import errors?**
```bash
pip install -r requirements.txt
```

## 📞 Need Help?

- Check `STRUCTURE.md` for architecture details
- View `/docs` for API documentation
- Review logs for error messages
