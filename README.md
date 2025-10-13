# Multi-Agent System for AML Compliance

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> Master's thesis project implementing an adaptive & explainable Multi-Agent System (MAS) for AML. Features Python agents trained with MARL, a Java/Spring Boot simulated environment, & a React UI. Built on a microservices architecture.

---

## 📖 Project Overview

This repository contains the source code for the master's thesis project, "Multi-Agents in Fintech Regulatory Compliance." The project addresses the limitations of traditional, static, and opaque Anti-Money Laundering (AML) systems. As financial crime becomes more sophisticated and coordinated, a new approach is needed.

This project designs, develops, and evaluates a novel framework for AML compliance based on a **Multi-Agent System (MAS)**. The system is built to be:
* **Adaptive:** Agents learn and adapt to new, unseen money laundering typologies using Multi-Agent Reinforcement Learning (MARL).
* **Collaborative:** Specialized, independent agents work together to achieve a more holistic detection capability than any single model could.
* **Explainable:** An integrated Explainable AI (XAI) component provides transparent, human-readable justifications for the system's collective decisions, fostering trust and auditability.

## ✨ Core Features

* **Specialized Multi-Agent System (MAS):** Three distinct agents collaborate for robust detection:
    * `TransactionPatternAgent`: Analyses the characteristics of individual transactions.
    * `CustomerRiskAgent`: Assesses the risk profiles of the sender and receiver.
    * `NetworkAnalysisAgent`: Examines the relationships between accounts over time using graph-based analysis.
* **Adaptive Learning with MARL:** Agents are trained using the **Multi-Agent Deep Deterministic Policy Gradient (MADDPG)** algorithm in a simulated environment to develop cooperative policies.
* **Microservices Architecture:** The entire system is built on a decoupled, event-driven architecture using Docker, ensuring scalability and resilience.
* **Explainable AI (XAI):** A dedicated component synthesizes findings from all agents to produce a clear narrative for each flagged transaction, designed for a human analyst.

## 🏗️ System Architecture

The system operates on an event-driven model where components communicate asynchronously through a central message bus (e.g., Apache Kafka).

**High-Level Flow:**
`[Simulated Banking Environment (Java)]` → `[Event Bus]` → `[Specialized AI Agents (Python)]` → `[Event Bus]` → `[Orchestration Service]` → `[Analyst UI (React)]`

## 🛠️ Tech Stack

| Category          | Technology                                         |
| ----------------- | -------------------------------------------------- |
| **AI Agents** | Python, TensorFlow / PyTorch, FastAPI              |
| **MARL Training** | Python, MADDPG Algorithm                           |
| **Environment** | Java, Spring Boot                                  |
| **Frontend** | React, JavaScript                                  |
| **Communication** | Apache Kafka (or similar event bus)                |
| **Data Storage** | Redis (Real-time), PostgreSQL (Persistent)         |
| **DevOps** | Docker                                             |

## 📂 Repository Structure

The repository is structured as a monorepo to contain all project components in one place.


## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.