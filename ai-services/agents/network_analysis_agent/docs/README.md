# Network Analysis Agent API

## Overview

The **Network Analysis Agent** is an AI-powered microservice that detects suspicious accounts using network topology analysis. It analyzes transaction network structure through centrality metrics, clustering coefficients, and community detection to identify potentially fraudulent behavior patterns.

## Features

- **Network Topology Analysis**: Uses 11 topology features (centrality, clustering, community)
- **CatBoost Model**: Trained on 289K+ accounts with 0.8466 ROC-AUC
- **Real-time Predictions**: Single account and batch processing
- **RESTful API**: FastAPI-based with automatic documentation
- **Production Ready**: Docker support, health checks, logging

## Quick Start

### Prerequisites

- Python 3.10+
- Trained model files in `trained_models/`:
  - `network_analysis_catboost_model.pkl`
  - `network_analysis_catboost_scaler.pkl`
  - `network_analysis_catboost_metadata.json`

### Installation

```bash
# Install dependencies
pip install -r requirements.txt

# Run the server
python main.py
```

The API will be available at `http://localhost:8003`

### Docker Deployment

```bash
# Build image
docker build -t network-analysis-agent .

# Run container
docker run -p 8003:8003 network-analysis-agent
```

## API Endpoints

### Health Check
```bash
GET /api/v1/health
```

### Model Information
```bash
GET /api/v1/model
GET /api/v1/model/features
```

### Predictions

**Single Account:**
```bash
POST /api/v1/predict
```

**Request Body:**
```json
{
  "account_id": "ACC_789012",
  "features": {
    "in_degree": 45,
    "out_degree": 38,
    "degree_centrality": 0.0285,
    "in_degree_centrality": 0.0155,
    "out_degree_centrality": 0.0131,
    "betweenness_centrality": 0.0012,
    "closeness_centrality": 0.4521,
    "pagerank": 0.00089,
    "eigenvector_centrality": 0.0234,
    "clustering_coefficient": 0.0156,
    "community": 5
  }
}
```

**Response:**
```json
{
  "account_id": "ACC_789012",
  "is_suspicious": true,
  "suspicion_probability": 0.78,
  "risk_score": 78.0,
  "risk_level": "HIGH",
  "confidence": "HIGH",
  "recommendation": "URGENT REVIEW - Flag for AML investigation",
  "network_indicators": {
    "centrality_metrics": { ... },
    "connectivity": { ... },
    "risk_flags": [ ... ]
  }
}
```

**Batch Prediction:**
```bash
POST /api/v1/predict/batch
```

## Network Features

The model uses 11 topology-based features:

1. **in_degree**: Number of incoming connections
2. **out_degree**: Number of outgoing connections
3. **degree_centrality**: Normalized degree centrality
4. **in_degree_centrality**: Incoming connection centrality
5. **out_degree_centrality**: Outgoing connection centrality
6. **betweenness_centrality**: Bridge position in network
7. **closeness_centrality**: Proximity to other accounts
8. **pagerank**: Network importance score
9. **eigenvector_centrality**: Influence through connections
10. **clustering_coefficient**: Network clustering
11. **community**: Community membership ID

## Risk Levels

- **CRITICAL** (≥80%): Immediate freeze and investigation
- **HIGH** (60-80%): Urgent AML review required
- **MEDIUM** (40-60%): Enhanced monitoring
- **LOW** (<40%): Standard oversight

## Testing

Run the test suite:

```bash
python test_api.py
```

Example files are provided in `examples/`:
- `high_risk_account.json`
- `low_risk_account.json`

## API Documentation

Interactive API documentation is available at:
- Swagger UI: `http://localhost:8003/docs`
- ReDoc: `http://localhost:8003/redoc`

## Model Performance

- **ROC-AUC**: 0.8466
- **Recall**: 72.6%
- **F1-Score**: 0.424
- **Test Accuracy**: 80% (sample validation)

## Architecture

```
network_analysis_agent/
├── app/
│   ├── api/          # API endpoints
│   ├── core/         # Configuration & logging
│   ├── models/       # Pydantic schemas
│   └── services/     # Business logic
├── examples/         # Example requests
├── trained_models/   # Model artifacts
├── main.py          # FastAPI application
└── test_api.py      # API tests
```

## Configuration

Configuration via environment variables (see `.env.example`):
- `PORT`: Server port (default: 8003)
- `MAX_BATCH_SIZE`: Maximum batch size (default: 500)
- `LOG_LEVEL`: Logging level (default: info)

## Logging

Logs are written to:
- Console: INFO level
- File: `logs/network_analysis_agent_YYYYMMDD.log` (DEBUG level)

## Integration

The Network Analysis Agent is part of a multi-agent AML compliance system:
- **Transaction Pattern Agent** (port 8001): Analyzes transaction patterns
- **Customer Risk Agent** (port 8002): Assesses customer-level risk
- **Network Analysis Agent** (port 8003): Detects suspicious accounts via network topology

## License

Part of Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance

## Author

Ismail Dogan
