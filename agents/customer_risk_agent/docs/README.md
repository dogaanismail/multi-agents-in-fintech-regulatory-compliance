# Customer Risk Agent API

**Author:** Ismail Dogan  
**Project:** Master's Thesis - Multi-Agent System for Fintech Regulatory Compliance

AI-powered customer risk assessment agent for Anti-Money Laundering (AML) compliance.

## Overview

The Customer Risk Agent evaluates customer-level risk by analyzing aggregated transaction patterns and behavioral features. It identifies high-risk customers requiring enhanced due diligence (EDD) and regulatory reporting.

## Features

- **Customer Risk Assessment**: Evaluates customer-level risk based on 19 behavioral features
- **Batch Processing**: Assess up to 500 customers simultaneously
- **Risk Levels**: CRITICAL, HIGH, MEDIUM, LOW classification
- **Explainability**: Contributing factors for high-risk flagging
- **RESTful API**: FastAPI with automatic documentation

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Train the Model (if not already done)

Run the Jupyter notebook to train the model:
```bash
jupyter notebook notebooks/01_CustomerRiskAgent_Baseline.ipynb
```

This will create `trained_models/customer_risk_model.pkl`

### 3. Start the API Server

```bash
python main.py
```

The API will be available at `http://localhost:8002`

### 4. Access API Documentation

- **Swagger UI**: http://localhost:8002/docs
- **ReDoc**: http://localhost:8002/redoc

## API Endpoints

### Health Check
```bash
GET /api/v1/health
```

### Assess Single Customer Risk
```bash
POST /api/v1/assess-risk
```

**Request Body:**
```json
{
  "customer_id": "CUST_123456",
  "features": {
    "transaction_count": 25,
    "total_amount": 125000.00,
    "avg_amount": 5000.00,
    "median_amount": 3000.00,
    "max_amount": 25000.00,
    "min_amount": 500.00,
    "std_amount": 5000.00,
    "active_days": 30,
    "transactions_per_day": 0.83,
    "cross_border_ratio": 0.6,
    "cash_transaction_ratio": 0.1,
    "amount_consistency": 1.0,
    "large_transaction_ratio": 0.2,
    "unique_receivers": 15,
    "unique_receiver_countries": 5,
    "receiver_diversity": 0.6,
    "night_transaction_ratio": 0.15,
    "weekend_transaction_ratio": 0.2,
    "unique_currencies": 3
  }
}
```

**Response:**
```json
{
  "customer_id": "CUST_123456",
  "is_high_risk": true,
  "risk_probability": 0.85,
  "risk_score": 85.0,
  "risk_level": "CRITICAL",
  "confidence": "HIGH",
  "recommendation": "IMMEDIATE REVIEW REQUIRED - Flag for enhanced due diligence (EDD)",
  "contributing_factors": [
    "High cross-border activity (60.0%)",
    "Dispersed transaction network (diversity: 0.60)",
    "High geographic diversity (5 countries)"
  ]
}
```

### Batch Assessment
```bash
POST /api/v1/batch-assess-risk
```

## Customer Features

The model uses 19 aggregated features from customer transaction history:

1. **Volume Metrics**: transaction_count, total_amount, avg_amount, median_amount, max_amount, min_amount, std_amount
2. **Velocity**: active_days, transactions_per_day
3. **Behavioral Patterns**: cross_border_ratio, cash_transaction_ratio, amount_consistency, large_transaction_ratio
4. **Network Features**: unique_receivers, unique_receiver_countries, receiver_diversity
5. **Time Patterns**: night_transaction_ratio, weekend_transaction_ratio
6. **Currency Diversity**: unique_currencies

## Model Information

- **Algorithm**: XGBoost Classifier (with SMOTE for class imbalance)
- **Training**: 30-day customer behavioral analysis
- **Features**: 19 aggregated transaction metrics
- **Output**: Binary classification (High-Risk / Low-Risk)

## Docker Support

```bash
# Build Docker image
docker build -t customer-risk-agent .

# Run container
docker run -p 8002:8002 customer-risk-agent
```

## Environment Variables

See `.env.example` for configuration options.

## Testing

```bash
# Run tests
pytest

# Test single customer endpoint
curl -X POST "http://localhost:8002/api/v1/assess-risk" \
  -H "Content-Type: application/json" \
  -d @examples/sample_customer.json
```

## Integration with Multi-Agent System

This agent is part of a multi-agent AML compliance system:
- **Transaction Pattern Agent** (Port 8001): Transaction-level fraud detection
- **Customer Risk Agent** (Port 8002): Customer-level risk assessment
- **Network Analysis Agent** (Port 8003): Network-level suspicious pattern detection

## License

**Author:** Ismail Dogan  
**Master's Thesis Project:** Multi-Agent System in Fintech Regulatory Compliance  
**Year:** 2025
