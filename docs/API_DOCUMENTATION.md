# Multi-Agent AML System - API Documentation

**Author:** Ismail Dogan  
**Project:** Master's Thesis - Multi-Agent System for Fintech Regulatory Compliance  
**Last Updated:** November 2025

---

## Overview

This document provides comprehensive API documentation for all three specialized agents in the Multi-Agent AML Compliance System.

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Multi-Agent AML System                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────┐│
│  │ Transaction      │  │ Customer Risk    │  │ Network    ││
│  │ Pattern Agent    │  │ Agent            │  │ Analysis   ││
│  │                  │  │                  │  │ Agent      ││
│  │ Port: 8001       │  │ Port: 8002       │  │ Port: 8003 ││
│  │ Model: XGBoost   │  │ Model: XGBoost   │  │ Model: TBD ││
│  │ Recall: 89.10%   │  │ ROC-AUC: 90.51%  │  │            ││
│  └──────────────────┘  └──────────────────┘  └────────────┘│
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 1. Transaction Pattern Agent

### Base URL
```
http://localhost:8001
```

### Documentation URLs
- **Swagger UI**: http://localhost:8001/docs
- **ReDoc**: http://localhost:8001/redoc

### Endpoints

#### 1.1 Health Check
```http
GET /api/v1/health
```

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "preprocessor_loaded": true,
  "timestamp": "2025-11-21T23:44:25.222Z",
  "model_info": {
    "model_name": "XGBoost",
    "training_date": "2025-11-21T23:34:56.706194"
  }
}
```

#### 1.2 Model Information
```http
GET /api/v1/model-info
```

**Response:**
```json
{
  "model_name": "XGBoost Transaction Pattern Agent",
  "model_type": "XGBClassifier",
  "training_date": "2025-11-21",
  "dataset": "SAML-D (9.5M+ transactions)",
  "performance_metrics": {
    "recall": 0.8910,
    "precision": 0.1234,
    "f1_score": 0.2170
  }
}
```

#### 1.3 Predict Single Transaction
```http
POST /api/v1/predict
Content-Type: application/json
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
  "transaction_id": null,
  "is_suspicious": true,
  "fraud_probability": 0.85,
  "risk_score": 85.0,
  "confidence": "HIGH",
  "recommendation": "FLAG FOR REVIEW - High probability of fraudulent activity",
  "threshold_used": 0.13
}
```

#### 1.4 Batch Predict Transactions
```http
POST /api/v1/batch-predict
Content-Type: application/json
```

**Request Body:**
```json
{
  "transactions": [
    { /* transaction 1 */ },
    { /* transaction 2 */ }
  ]
}
```

---

## 2. Customer Risk Agent

### Base URL
```
http://localhost:8002
```

### Documentation URLs
- **Swagger UI**: http://localhost:8002/docs
- **ReDoc**: http://localhost:8002/redoc

### Endpoints

#### 2.1 Health Check
```http
GET /api/v1/health
```

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "scaler_loaded": true,
  "timestamp": "2025-11-21T23:44:25.222Z",
  "model_info": {
    "model_name": "XGBoost",
    "training_date": "2025-11-21T23:34:56.706194",
    "roc_auc": 0.9051
  }
}
```

#### 2.2 Model Information
```http
GET /api/v1/model-info
```

**Response:**
```json
{
  "model_name": "XGBoost",
  "model_type": "XGBClassifier",
  "training_date": "2025-11-21T23:34:56.706194",
  "performance_metrics": {
    "recall": 0.8500,
    "precision": 0.0500,
    "f1_score": 0.0943,
    "roc_auc": 0.9051
  },
  "training_config": {
    "timeframe_days": 30,
    "min_transactions": 3,
    "random_state": 42,
    "smote_applied": true
  },
  "feature_names": [
    "transaction_count",
    "total_amount",
    "avg_amount",
    // ... 16 more features
  ],
  "num_features": 19
}
```

#### 2.3 Assess Single Customer Risk
```http
POST /api/v1/assess-risk
Content-Type: application/json
```

**Request Body:**
```json
{
  "customer_id": "CUST_123456",
  "features": {
    "transaction_count": 45,
    "total_amount": 350000.00,
    "avg_amount": 7777.78,
    "median_amount": 5000.00,
    "max_amount": 50000.00,
    "min_amount": 1000.00,
    "std_amount": 8500.00,
    "active_days": 30,
    "transactions_per_day": 1.5,
    "cross_border_ratio": 0.75,
    "cash_transaction_ratio": 0.05,
    "amount_consistency": 1.09,
    "large_transaction_ratio": 0.35,
    "unique_receivers": 25,
    "unique_receiver_countries": 8,
    "receiver_diversity": 0.55,
    "night_transaction_ratio": 0.28,
    "weekend_transaction_ratio": 0.18,
    "unique_currencies": 5
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
    "High cross-border activity (75.0%)",
    "Unusual timing patterns (28.0% at night)",
    "High proportion of large transactions (35.0%)",
    "High geographic diversity (8 countries)",
    "Dispersed transaction network (diversity: 0.55)"
  ]
}
```

#### 2.4 Batch Assess Customer Risk
```http
POST /api/v1/batch-assess-risk
Content-Type: application/json
```

**Request Body:**
```json
{
  "customers": [
    { 
      "customer_id": "CUST_001",
      "features": { /* ... */ }
    },
    { 
      "customer_id": "CUST_002",
      "features": { /* ... */ }
    }
  ]
}
```

**Response:**
```json
{
  "total_customers": 2,
  "high_risk_count": 1,
  "low_risk_count": 1,
  "average_risk_score": 45.5,
  "predictions": [
    { /* customer 1 result */ },
    { /* customer 2 result */ }
  ],
  "processing_time_ms": 45.2
}
```

---

## 3. Network Analysis Agent

### Base URL
```
http://localhost:8003
```

### Documentation URLs
- **Swagger UI**: http://localhost:8003/docs
- **ReDoc**: http://localhost:8003/redoc

### Status
⚠️ **Under Development** - Network Analysis Agent API documentation will be added once implementation is complete.

---

## Customer Risk Agent - Feature Definitions

### 19 Aggregated Features (30-day window):

| Feature | Description | Type | Range |
|---------|-------------|------|-------|
| `transaction_count` | Total number of transactions | Integer | > 0 |
| `total_amount` | Sum of all transaction amounts | Float | > 0 |
| `avg_amount` | Average transaction amount | Float | > 0 |
| `median_amount` | Median transaction amount | Float | > 0 |
| `max_amount` | Maximum transaction amount | Float | > 0 |
| `min_amount` | Minimum transaction amount | Float | > 0 |
| `std_amount` | Standard deviation of amounts | Float | ≥ 0 |
| `active_days` | Number of days with transactions | Integer | > 0 |
| `transactions_per_day` | Transaction velocity | Float | > 0 |
| `cross_border_ratio` | Ratio of cross-border transactions | Float | 0-1 |
| `cash_transaction_ratio` | Ratio of cash transactions | Float | 0-1 |
| `amount_consistency` | Amount consistency indicator | Float | ≥ 0 |
| `large_transaction_ratio` | Ratio of transactions > $10k | Float | 0-1 |
| `unique_receivers` | Number of unique receiver accounts | Integer | ≥ 0 |
| `unique_receiver_countries` | Number of unique countries | Integer | ≥ 0 |
| `receiver_diversity` | Receiver diversity metric | Float | 0-1 |
| `night_transaction_ratio` | Ratio of night transactions (10pm-6am) | Float | 0-1 |
| `weekend_transaction_ratio` | Ratio of weekend transactions | Float | 0-1 |
| `unique_currencies` | Number of unique currencies | Integer | ≥ 0 |

---

## Error Codes

All agents use standard HTTP status codes:

| Code | Meaning | Example |
|------|---------|---------|
| 200 | Success | Request processed successfully |
| 400 | Bad Request | Invalid input data |
| 503 | Service Unavailable | Model not loaded |
| 500 | Internal Server Error | Prediction failure |

---

## Docker Deployment

### Start All Agents
```bash
docker-compose up -d
```

### Start Individual Agent
```bash
# Transaction Pattern Agent
docker-compose up -d transaction-pattern-agent

# Customer Risk Agent
docker-compose up -d customer-risk-agent

# Network Analysis Agent
docker-compose up -d network-analysis-agent
```

### Check Health
```bash
# Transaction Pattern Agent
curl http://localhost:8001/api/v1/health

# Customer Risk Agent
curl http://localhost:8002/api/v1/health

# Network Analysis Agent
curl http://localhost:8003/api/v1/health
```

---

## Testing

### Transaction Pattern Agent
```bash
cd agents/transaction_pattern_agent
python test_api.py
```

### Customer Risk Agent
```bash
cd agents/customer_risk_agent
python test_api.py
```

---

## Rate Limits

| Agent | Max Batch Size | Recommended Batch |
|-------|----------------|-------------------|
| Transaction Pattern | 1000 | 100-500 |
| Customer Risk | 500 | 50-100 |
| Network Analysis | TBD | TBD |

---

## Support

For issues or questions:
- **Author**: Ismail Dogan
- **Project**: Master's Thesis - Multi-Agent System for Fintech Regulatory Compliance
- **Repository**: [GitHub Link]

---

**Last Updated:** November 21, 2025
