# Quick Start Guide - Customer Risk Agent

**Author:** Ismail Dogan  
**Project:** Multi-Agent System for Fintech Regulatory Compliance

---

## 🚀 Quick Start (5 Minutes)

### Prerequisites
- Python 3.10+
- Trained model (from Jupyter notebook)

### Step 1: Install Dependencies
```bash
cd agents/customer_risk_agent
pip install -r requirements.txt
```

### Step 2: Verify Model Exists
```bash
ls trained_models/customer_risk_model.pkl
```

If the file doesn't exist, train the model first:
```bash
jupyter notebook notebooks/01_CustomerRiskAgent_Baseline.ipynb
# Run all cells (Cell 49 saves the model)
```

### Step 3: Start the API
```bash
python main.py
```

You should see:
```
INFO:     Uvicorn running on http://0.0.0.0:8002
✅ Model loaded successfully: XGBoost
   Features: 19
   Training date: 2025-11-21T23:34:56.706194
   ROC-AUC: 0.9051
```

### Step 4: Test the API

#### Option A: Use Swagger UI (Recommended)
Open in browser: http://localhost:8002/docs

#### Option B: Use curl
```bash
# Health check
curl http://localhost:8002/api/v1/health

# Test with high-risk customer
curl -X POST http://localhost:8002/api/v1/assess-risk \
  -H "Content-Type: application/json" \
  -d @examples/high_risk_customer.json
```

#### Option C: Use Test Script
```bash
python test_api.py
```

---

## 📊 Understanding the Response

### Example Response
```json
{
  "customer_id": "CUST_HIGH_RISK_001",
  "is_high_risk": true,
  "risk_probability": 0.85,
  "risk_score": 85.0,
  "risk_level": "CRITICAL",
  "confidence": "HIGH",
  "recommendation": "IMMEDIATE REVIEW REQUIRED - Flag for enhanced due diligence (EDD)",
  "contributing_factors": [
    "High cross-border activity (75.0%)",
    "Unusual timing patterns (28.0% at night)",
    "High geographic diversity (8 countries)"
  ]
}
```

### Risk Levels
- **CRITICAL** (80-100%): Immediate review required
- **HIGH** (60-80%): Priority review, escalate to compliance
- **MEDIUM** (40-60%): Monitor closely
- **LOW** (0-40%): Standard monitoring

---

## 🐳 Docker Deployment

### Build Docker Image
```bash
docker build -t customer-risk-agent .
```

### Run Container
```bash
docker run -p 8002:8002 customer-risk-agent
```

### Using Docker Compose (Recommended)
```bash
# From project root
docker-compose up customer-risk-agent
```

---

## 🔧 Configuration

### Environment Variables
Create `.env` file (see `.env.example`):
```bash
PORT=8002
LOG_LEVEL=info
ENVIRONMENT=development
MIN_TRANSACTIONS=3
ANALYSIS_TIMEFRAME_DAYS=30
```

### Changing Port
Edit `app/core/config.py`:
```python
port: int = 8002  # Change to desired port
```

---

## 📝 Creating Customer Features

The API expects **pre-aggregated** features. Here's how to calculate them from raw transactions:

### Example: Python Script
```python
import pandas as pd

def calculate_customer_features(customer_transactions_df):
    """
    Calculate customer features from transaction history
    
    Args:
        customer_transactions_df: DataFrame with customer's transactions
    
    Returns:
        Dict with 19 features
    """
    features = {
        "transaction_count": len(customer_transactions_df),
        "total_amount": customer_transactions_df['Amount'].sum(),
        "avg_amount": customer_transactions_df['Amount'].mean(),
        "median_amount": customer_transactions_df['Amount'].median(),
        "max_amount": customer_transactions_df['Amount'].max(),
        "min_amount": customer_transactions_df['Amount'].min(),
        "std_amount": customer_transactions_df['Amount'].std(),
        "active_days": (customer_transactions_df['Date'].max() - 
                       customer_transactions_df['Date'].min()).days + 1,
        "transactions_per_day": len(customer_transactions_df) / 30,
        "cross_border_ratio": (customer_transactions_df['Payment_type'] == 'Cross-border').mean(),
        "cash_transaction_ratio": ((customer_transactions_df['Payment_type'] == 'Cash Deposit') | 
                                   (customer_transactions_df['Payment_type'] == 'Cash Withdrawal')).mean(),
        "amount_consistency": customer_transactions_df['Amount'].std() / customer_transactions_df['Amount'].mean(),
        "large_transaction_ratio": (customer_transactions_df['Amount'] > 10000).mean(),
        "unique_receivers": customer_transactions_df['Receiver_account'].nunique(),
        "unique_receiver_countries": customer_transactions_df['Receiver_bank_location'].nunique(),
        "receiver_diversity": customer_transactions_df['Receiver_account'].nunique() / len(customer_transactions_df),
        "night_transaction_ratio": ((customer_transactions_df['hour'] >= 22) | 
                                   (customer_transactions_df['hour'] <= 6)).mean(),
        "weekend_transaction_ratio": (customer_transactions_df['day_of_week'] >= 5).mean(),
        "unique_currencies": customer_transactions_df['Payment_currency'].nunique()
    }
    return features

# Usage
customer_txns = df[df['Sender_account'] == 'CUST_123456']
features = calculate_customer_features(customer_txns)
```

---

## 🧪 Testing

### Run All Tests
```bash
python test_api.py
```

### Test Individual Endpoints
```bash
# Health
curl http://localhost:8002/api/v1/health

# Model Info
curl http://localhost:8002/api/v1/model-info

# Single Customer (High Risk)
curl -X POST http://localhost:8002/api/v1/assess-risk \
  -H "Content-Type: application/json" \
  -d @examples/high_risk_customer.json

# Single Customer (Low Risk)
curl -X POST http://localhost:8002/api/v1/assess-risk \
  -H "Content-Type: application/json" \
  -d @examples/low_risk_customer.json
```

---

## 🔍 Troubleshooting

### Problem: "Model not loaded"
**Solution:** 
```bash
# Check if model file exists
ls trained_models/customer_risk_model.pkl

# If missing, train the model
jupyter notebook notebooks/01_CustomerRiskAgent_Baseline.ipynb
```

### Problem: "ModuleNotFoundError: No module named 'fastapi'"
**Solution:**
```bash
pip install -r requirements.txt
```

### Problem: Port 8002 already in use
**Solution:**
```bash
# Check what's using port 8002
lsof -i :8002

# Kill the process or change port in config.py
```

### Problem: Pydantic warnings about "model_" namespace
**Solution:** These are warnings, not errors. The API works correctly. To suppress:
```python
# In app/core/config.py, add:
class Config:
    protected_namespaces = ('settings_',)
```

---

## 📚 Next Steps

1. ✅ **Integrate with Transaction Pattern Agent** (Port 8001)
2. ✅ **Add Network Analysis Agent** (Port 8003)
3. 🔄 **Build Multi-Agent Orchestrator**
4. 🔄 **Create React UI for Analysts**
5. 🔄 **Deploy to Production**

---

## 📖 Additional Resources

- **API Documentation**: See `docs/API_DOCUMENTATION.md`
- **Agent README**: See `README.md`
- **Model Training**: See `notebooks/01_CustomerRiskAgent_Baseline.ipynb`
- **Docker Compose**: See project root `docker-compose.yml`

---

**Need Help?**  
Author: Ismail Dogan  
Master's Thesis Project - 2025
