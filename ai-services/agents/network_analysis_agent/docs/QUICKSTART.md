# Network Analysis Agent - Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### Step 1: Install Dependencies

```bash
cd agents/network_analysis_agent
pip install -r requirements.txt
```

### Step 2: Verify Model Files

Ensure trained models are in `trained_models/`:
```bash
ls -lh trained_models/
```

You should see:
- `network_analysis_catboost_model.pkl` (219 KB)
- `network_analysis_catboost_scaler.pkl` (1 KB)
- `network_analysis_catboost_metadata.json` (3 KB)

### Step 3: Start the Server

```bash
python main.py
```

Output should show:
```
🚀 Starting Network Analysis Agent...
✅ Models loaded successfully
INFO:     Started server process
INFO:     Uvicorn running on http://0.0.0.0:8003
```

### Step 4: Test the API

Open a new terminal and run:

```bash
python test_api.py
```

Or use curl:

```bash
# Health check
curl http://localhost:8003/api/v1/health

# Model info
curl http://localhost:8003/api/v1/model

# Test prediction
curl -X POST http://localhost:8003/api/v1/predict \
  -H "Content-Type: application/json" \
  -d @examples/high_risk_account.json
```

### Step 5: View Interactive Docs

Open in browser:
- **Swagger UI**: http://localhost:8003/docs
- **ReDoc**: http://localhost:8003/redoc

## 📊 Example Usage

### Python Client

```python
import requests
import json

# Make prediction
account_data = {
    "account_id": "ACC_123456",
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

response = requests.post(
    "http://localhost:8003/api/v1/predict",
    json=account_data
)

result = response.json()
print(f"Account: {result['account_id']}")
print(f"Suspicious: {result['is_suspicious']}")
print(f"Risk Score: {result['risk_score']:.2f}")
print(f"Risk Level: {result['risk_level']}")
```

### cURL

```bash
curl -X POST http://localhost:8003/api/v1/predict \
  -H "Content-Type: application/json" \
  -d '{
    "account_id": "ACC_123456",
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
  }'
```

## 🐳 Docker Deployment

### Option 1: Single Service

```bash
# Build
docker build -t network-analysis-agent .

# Run
docker run -p 8003:8003 network-analysis-agent
```

### Option 2: Multi-Agent System

From the project root:

```bash
# Start all agents
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs network-analysis-agent

# Stop all
docker-compose down
```

## 📝 Understanding the Response

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
    "centrality_metrics": {
      "pagerank": 0.00089,
      "eigenvector_centrality": 0.0234,
      "betweenness_centrality": 0.0012,
      "closeness_centrality": 0.4521
    },
    "connectivity": {
      "in_degree": 45,
      "out_degree": 38,
      "total_degree": 83
    },
    "network_position": {
      "clustering_coefficient": 0.0156,
      "community_id": 5
    },
    "risk_flags": [
      "High eigenvector centrality - connected to important nodes",
      "High out-degree - dispersed transaction pattern"
    ]
  }
}
```

### Key Fields:

- **is_suspicious**: Boolean flag (true/false)
- **suspicion_probability**: 0-1 probability
- **risk_score**: 0-100 normalized score
- **risk_level**: LOW / MEDIUM / HIGH / CRITICAL
- **confidence**: Prediction confidence (LOW / MEDIUM / HIGH)
- **recommendation**: Suggested action
- **network_indicators**: Detailed topology metrics (only for suspicious accounts)

## 🔧 Troubleshooting

### Model Not Found
```
Error: Model file not found
```
**Solution**: Run the Jupyter notebook to train and save the model first:
```bash
jupyter notebook notebooks/02_NetworkAnalysisAgent_Baseline.ipynb
```

### Port Already in Use
```
Error: Address already in use
```
**Solution**: Change port in `.env` or kill the process:
```bash
lsof -ti:8003 | xargs kill -9
```

### Import Errors
```
Import "fastapi" could not be resolved
```
**Solution**: Reinstall dependencies:
```bash
pip install -r requirements.txt --force-reinstall
```

## 📚 Next Steps

1. **Integrate with other agents**: Combine with Transaction Pattern Agent and Customer Risk Agent
2. **Add monitoring**: Set up Prometheus/Grafana for metrics
3. **Scale horizontally**: Deploy multiple instances with load balancer
4. **Add authentication**: Implement JWT tokens or API keys
5. **Enable caching**: Use Redis for frequently queried accounts

## 📞 Support

For issues or questions:
- Check the main [README.md](README.md)
- Review API documentation at `/docs`
- Check logs in `logs/` directory

## ✅ Verification Checklist

- [ ] Dependencies installed
- [ ] Model files present in `trained_models/`
- [ ] Server starts without errors
- [ ] Health check returns `200 OK`
- [ ] Test predictions work
- [ ] API documentation accessible
- [ ] Docker build succeeds (if using Docker)

---

**Ready to use!** 🎉

For production deployment, review security settings and configure authentication.
