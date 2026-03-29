# Multi-Agents in Fintech Regulatory Compliance

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **MSc Computer Science — University of Liverpool**
>
> A cooperative multi-agent reinforcement learning system for Anti-Money Laundering detection, built as a production-grade, event-driven microservices ecosystem.

---

## 📖 Overview

Traditional AML systems rely on static rules with false-positive rates exceeding 95 %, costing billions in manual review while sophisticated laundering schemes go undetected. This project replaces the rule-based paradigm with three specialised ML agents that learn to cooperate through Multi-Agent Deep Deterministic Policy Gradient (MADDPG), producing a single auditable compliance decision for every payment.

The system is designed to be **adaptive** (agents learn from officer feedback at runtime), **collaborative** (independent agents cover different fraud dimensions), and **explainable** (TreeSHAP + Integrated Gradients provide per-decision attribution).

---

## 🏗️ System Architecture

The platform runs as **22 Docker containers** — 10 Java microservices, 3 Python ML agents, 1 MADDPG orchestrator, and supporting infrastructure — communicating exclusively through Apache Kafka with Avro schemas (zero synchronous coupling).

<p align="center">
  <img src="docs/design/high_level_system_design.png" alt="High-Level System Design" width="800"/>
</p>

### Payment Processing Lifecycle

Every payment traverses a deterministic compliance workflow orchestrated by Axon Framework's saga pattern, producing over **300,000 immutable domain events** in a single simulation run.

<p align="center">
  <img src="docs/design/payment_processing_lifecycle.png" alt="Payment Processing Lifecycle" width="800"/>
</p>

### Agent Decomposition

Three specialised agents each analyse fraud from a different dimension:

| Agent | Model | Key Features | Port |
|-------|-------|-------------|------|
| **Transaction Pattern Agent** | XGBoost | 57 features (after one-hot), trained on 9.5M+ transactions | 8001 |
| **Customer Risk Agent** | XGBoost + SMOTE | 19 behavioural features aggregated over 30-day sliding window | 8002 |
| **Network Analysis Agent** | CatBoost | 11 graph-topology features (PageRank, centrality, clustering) — deliberately volume-free | 8003 |

<p align="center">
  <img src="docs/design/agent_decomposition.png" alt="Agent Decomposition" width="800"/>
</p>

### MADDPG Orchestrator

A centralised-training, decentralised-execution (CTDE) architecture: three Actor networks (one per agent) + one shared Critic (~415 K parameters total). Training uses a **three-tier reward function** — automated heuristics, officer review, and decision overrides — with configurable multipliers hot-reloadable at runtime.

<p align="center">
  <img src="docs/design/maddpg_training_loop.png" alt="MADDPG Training Loop" width="800"/>
</p>

<p align="center">
  <img src="docs/design/reward_calculation_design.png" alt="Reward Calculation Design" width="800"/>
</p>

---

## 📊 Evaluation Results

Evaluated on **10,000 synthetic payments** across five money-laundering typologies:

| Metric | Value |
|--------|-------|
| **System Recall** | 97.3 % |
| **Smurfing** | 99.3 % |
| **Fan-out** | 100 % |
| **Layering** | 99.6 % |
| **High-value** | 100 % |
| **Round-trip** | 76 % (hardest typology) |
| **Precision** | 11 % (by design — missed fraud carries regulatory risk; false alerts route to human review) |
| **Training Convergence** | 18 episodes, 99.5 % critic-loss reduction |
| **Median Latency** | 779 ms per decision |

<p align="center">
  <img src="simulation_tests/reports/charts/figB_detection_by_typology.png" alt="Detection by Typology" width="600"/>
</p>

<p align="center">
  <img src="simulation_tests/reports/charts/figP_training_convergence.png" alt="Training Convergence" width="600"/>
</p>

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 25, Spring Boot 4.0, Axon Framework 4.12 (saga pattern) |
| **ML Agents** | Python 3.11, FastAPI, XGBoost, CatBoost |
| **MARL** | PyTorch 2.5.1, MADDPG (custom implementation) |
| **Explainability** | SHAP 0.51, Integrated Gradients |
| **Messaging** | Apache Kafka 7.5, Confluent Schema Registry, Avro |
| **Databases** | PostgreSQL 16.2, Neo4j 5.26 |
| **Frontend** | React 18, TypeScript, Tailwind CSS, Vite |
| **Infrastructure** | Docker, Docker Compose |

---

## 📂 Repository Structure

```
├── ai-services/
│   ├── agents/
│   │   ├── transaction_pattern_agent/   # XGBoost — port 8001
│   │   ├── customer_risk_agent/         # XGBoost — port 8002
│   │   └── network_analysis_agent/      # CatBoost — port 8003
│   └── marl_orchestrator/               # MADDPG orchestrator — port 8000
├── bank-solution-backend/
│   ├── account-service/
│   ├── customer-service/
│   ├── customer-profile-service/
│   ├── configuration-service/
│   ├── payment-service/
│   ├── payment-engine-service/
│   ├── payment-history-service/
│   ├── risk-engine-service/
│   ├── network-topology-service/
│   └── backoffice-gateway/
├── bank-solution-backoffice/            # React backoffice UI
├── libraries/
│   └── avro-schema-library/            # Shared Avro schemas
├── simulation_tests/                    # 10K-payment evaluation suite
├── data/                                # SAML-D dataset (9.5M+ rows)
└── docs/design/                         # Architecture diagrams
```

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 25+
- Python 3.11+
- Node.js 18+

### Quick Start

```bash
# 1. Start infrastructure (Kafka, PostgreSQL, Neo4j)
cd bank-solution-backend && docker compose up -d

# 2. Start backend microservices
./gradlew bootRun

# 3. Start AI agents & orchestrator
cd ai-services && docker compose up -d

# 4. Start backoffice UI
cd bank-solution-backoffice && npm install && npm run dev
```

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

This dissertation was completed as part of the MSc in Computer Science at the **University of Liverpool**. Sincere gratitude to **Dr. Chunyan Mu** for her guidance and support throughout this research.