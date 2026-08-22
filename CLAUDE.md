# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An event-driven digital-banking platform (10 Java/Spring Boot microservices) wired to a
multi-agent RL compliance core (3 Python ML agents + a MADDPG orchestrator). Everything
between services flows over Kafka with Avro; there is deliberately almost no synchronous
coupling between the banking cluster and the AI cluster. MSc dissertation project — see
`README.md` for the architecture diagrams and evaluation numbers, `TRACING.md` for observability.

Four independently-built codebases, three Docker Compose stacks:

| Codebase | Path | Stack |
|---|---|---|
| Kafka + Avro schemas (must start first) | `libraries/avro-schema-library` | Kafka 7.5, Schema Registry, `docker-compose.yml` |
| Java microservices | `bank-solution-backend` | Java 25, Spring Boot 4.1, Gradle multi-project |
| Python AI services | `ai-services` | Python 3.11, FastAPI, XGBoost/CatBoost, PyTorch |
| React backoffice | `bank-solution-backoffice` | React 18 + TS + Vite |

Plus `simulation_tests/` + `run_simulation.py` — the end-to-end evaluation harness that
drives real payments through the whole running stack.

## Commands

### Java backend (`bank-solution-backend`)

```bash
./gradlew build                     # compile everything
./gradlew test                      # unit tests (integration tests excluded by tag)
./gradlew :payment-engine-service:payment-engine-svc:test          # one module
./gradlew :payment-engine-service:payment-engine-svc:test --tests '*PaymentRiskSagaTest'   # one class
./gradlew :payment-engine-service:payment-engine-svc:integrationTest   # Testcontainers; needs Docker
./gradlew :risk-engine-service:risk-engine-svc:bootRun             # run one service locally
```

`test` and `integrationTest` are deliberately separate: JUnit tests tagged `integration`
(Testcontainers Kafka) are excluded from `test` so CI does not need a Docker daemon.
CI (`.github/workflows/backend-build.yml`) runs only `./gradlew test`.

Module naming: each service is a parent project with `<name>-svc`, and (where it owns a DB)
`<name>-svc-db-migration` + `<name>-svc-db-migration-changelog`. The migration app is a
separate Spring Boot jar that runs Liquibase and exits; Compose runs it before the service.

### Python AI services (`ai-services`)

```bash
cd ai-services/marl_orchestrator
pip install -r requirements.txt
pytest                              # config in pytest.ini; testpaths=test, asyncio_mode=auto
pytest -m unit                      # markers: unit | integration | e2e | slow
pytest test/test_orchestrator.py::test_name
uvicorn main:app --port 1004        # run locally
alembic upgrade head                # migrations (async PostgreSQL)
alembic revision --autogenerate -m "..."
```

The three agents (`ai-services/agents/*`) are plain FastAPI apps with `main.py` +
`requirements.txt` and no test suite of their own.

### Frontend (`bank-solution-backoffice`)

```bash
npm install
npm run dev       # Vite on :3000, proxies /api -> backoffice-gateway on :3030
npm run build     # tsc && vite build
npm run lint      # eslint, --max-warnings 0
```

### Avro schemas / Kafka (`libraries/avro-schema-library`)

```bash
./scripts/setup-complete.sh          # start infra + register schemas + create topics
./scripts/start-infrastructure.sh    # Kafka, Zookeeper, Schema Registry, Kafka UI
./scripts/validate-schemas.sh
./scripts/register-schemas.sh
./scripts/create-kafka-topics.sh
./scripts/generate-java.sh           # regenerate Java classes from .avsc
./scripts/generate-python.sh
python scripts/dlt-tool.py inspect <topic>.DLT      # inspect parked messages
python scripts/dlt-tool.py replay  <topic>.DLT      # re-publish to origin topic
```

### Bringing the whole system up

Order matters — Kafka/Schema Registry first, then the banking stack, then the AI stack
(the AI compose file joins the other two stacks' networks as *external* networks):

```bash
cd libraries/avro-schema-library && ./scripts/setup-complete.sh
cd bank-solution-backend        && docker compose up -d    # + PostgreSQL, Neo4j, Tempo/Loki/Grafana
cd ai-services                  && docker compose up -d
```

Then, against a fully running stack:

```bash
python run_simulation.py --customers 100 --payments 100    # quick smoke
python run_simulation.py                                   # full 10K-payment evaluation
```

Ports are listed in `README.md` (services 5001–5009 + gateway 3030, agents 1001–1004,
Grafana 3009, Kafka UI 8080).

## Architecture

### The payment → compliance chain

One payment traverses this chain; every arrow is Kafka except where noted:

```
payment-svc --payment-created-events--> payment-engine-svc (Axon aggregate + sagas)
  --risk.assessment.requested--> risk-engine-svc
      (Feign HTTP: account-svc, customer-profile-svc, network-topology-svc → feature extraction)
  --fraud.analysis.requested--> marl-orchestrator (Python)
      (httpx: transaction-pattern-agent 1001, customer-risk-agent 1002, network-analysis-agent 1003)
  --fraud.analysis.completed--> risk-engine-svc
  --risk.assessment.completed--> payment-engine-svc → PROCEED / ESCALATE / BLOCK
  --account charge / payment-completed-events--> account-svc, payment-history-svc,
                                                 customer-profile-svc, network-topology-svc
```

Compliance-officer decisions in the backoffice flow back the other way:
payment-engine-svc publishes `agent.manual.feedback`, which the orchestrator consumes as
reward signal for MADDPG training.

Key consequences to keep in mind when changing anything:

- **`payment-engine-service` is the only Axon service.** It owns the CQRS/event-sourced
  `PaymentAggregate` and two sagas (`PaymentRiskSaga`, `AccountChargeSaga`) under
  `domain/payment/`. Sagas carry deadlines (e.g. a 1-minute risk-assessment timeout) — a
  new terminal event must be handled there or the saga leaks. Every other service is a
  plain Spring Boot + JPA service with `service/`, `repository/`, `entity/`, `integration/`
  (Feign clients), and `infrastructure/messaging/kafka/{producer,consumer}` packages.
- **Message contracts live in one place.** `libraries/avro-schema-library/schemas/*.avsc`
  is the single source of truth. Java consumes it as the `:avro-schema-library-java`
  Gradle project (included from `settings.gradle` via a relative `projectDir`; classes are
  generated by the `generateAvro` task at compile time). Python consumes the generated
  package under `libraries/avro-schema-library/python`. Changing a schema means:
  edit `.avsc` → `register-schemas.sh` → rebuild both sides.
- **All Kafka consumers have DLQ + retry.** Each service's `config/KafkaConsumerConfig`
  installs a `DeadLetterPublishingRecoverer` that parks failures on `<topic>.DLT`, and the
  Python orchestrator mirrors the same convention. Consumers are expected to be idempotent
  because replay is at-least-once.
- **`:common`** holds shared enums (`Currency`, `PaymentType`, `AgentType`, …), `CustomResponse`,
  and — importantly — the OpenTelemetry/Micrometer tracing and Prometheus starters as `api`
  dependencies, so every service inherits tracing by depending on it.
- **`backoffice-gateway`** is the only thing the React app talks to; it aggregates the 10
  services plus the orchestrator.
- **`configuration-service`** serves hot-reloadable settings (reward multipliers, thresholds,
  training interval). The orchestrator pre-warms them at startup via
  `app/core/dynamic_config.py` and falls back to env-var defaults if the service is down —
  don't hardcode these values in either language.

### MADDPG orchestrator internals (`ai-services/marl_orchestrator`)

- `app/` is the service layer: `api/` (FastAPI routers: health, inference, training),
  `consumers/` + `producers/` (Kafka/Avro), `services/` (agent HTTP clients,
  `agent_orchestrator`, `fraud_decision_service`, `reward_calculator_service`,
  `experience_buffer_service`, `offline_trainer_service`), `repositories/`, `models/`.
- `maddpg/` is the learning core, independent of FastAPI: `core/` (coordinator,
  decision_maker, trainer, network_manager, state_manager, model_persistence) and `networks/`
  (3 actors + 1 shared critic, CTDE).
- `MADDPG_MODE` env var switches inference vs training; weights persist to the
  `marl_trained_models` volume.

### Tracing

One trace spans Java and Python, over HTTP, Feign, and Kafka. Java services enable Kafka
observation explicitly on their *custom* beans (`template.setObservationEnabled(true)` in
`KafkaProducerConfig`, `factory.getContainerProperties().setObservationEnabled(true)` in
`KafkaConsumerConfig`) because Boot's auto-config doesn't apply to hand-built beans. Python
propagates `traceparent` manually across the Kafka boundary in `app/core/telemetry.py` and
the consumer/publisher wrappers. If you add a new producer or consumer, wire this up or the
trace breaks at that hop.

## Conventions

- Java test methods are `should…`-prefixed; test fixtures are `create…`-prefixed and live in
  a `fixtures/` package (see `payment-engine-svc/src/test`). Testcontainers tests are
  `@Tag("integration")`.
- Dependency versions are centralised in `bank-solution-backend/gradle.properties`, not
  inline in module `build.gradle` files.
- Service config uses `${ENV_VAR:local-default}` in `application.properties` so the same jar
  runs locally and in Compose; Kafka topic names are properties, not string literals.
- `payment-engine-service` currently holds the only Java tests; new tests elsewhere should
  follow its layout.
