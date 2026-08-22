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

Module naming: each service is a parent project with `<name>-svc`, and (where it owns a
PostgreSQL schema) `<name>-svc-db-migration` + `<name>-svc-db-migration-changelog`. The
migration app is a separate Spring Boot jar that runs Liquibase and exits; Compose runs it
before the service. Services whose store is not PostgreSQL have **only** the `-svc` module —
`network-topology-service` (Neo4j) and `ledger-service` (TigerBeetle).

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

### `ledger-service` + TigerBeetle

The double-entry ledger (issue #99). TigerBeetle is the source of truth for balances and
movements; the service holds **no relational schema at all**.

- **No registry table, because ids are derived.** `LedgerAccountIds.deriveWalletAccountId`
  / `deriveInternalAccountId` and `LedgerTransferIds.deriveTransferId` hash a stable seed
  (`wallet:<accountId>:<currency>`, `internal:<TYPE>:<currency>`,
  `posting:<clientTransactionId>:<TYPE>`) into a UUID. Anything holding the domain
  identifiers can compute the ledger id without a lookup, so listing is "derive the expected
  ids, one batch `lookupAccounts`". Never generate a random id for an account or transfer.
- **Derived transfer ids are the idempotency mechanism.** A redelivered posting instruction
  re-derives the same id and TigerBeetle answers `Exists` instead of double-posting.
  `TigerBeetleStatuses` treats `Exists`, `PendingTransferAlreadyPosted` and
  `PendingTransferAlreadyVoided` as success; `ExceedsCredits` becomes
  `InsufficientLedgerFundsException` (422).
- **Overdraft is enforced by the ledger**, via `DEBITS_MUST_NOT_EXCEED_CREDITS` on wallet
  accounts — not by a read-modify-write balance check.
- `PostingInstructionType` (banking vocabulary: `INBOUND_AUTHORISATION`, `SETTLEMENT`, …)
  maps many-to-one onto `TransferType` (the TigerBeetle mechanic: `PENDING`, `POST_PENDING`,
  `VOID_PENDING`, `SINGLE_PHASE`). Dispatch on `TransferType`.
- One TigerBeetle `ledger` per currency (`Currency.numericCode`, ISO 4217). Amounts are
  u128 minor units — convert only through `MoneyUtils`, which honours `Currency.exponent`
  (JPY is 0, not 2). Cross-currency is deliberately **not** supported yet: a transfer cannot
  cross ledgers, so FX needs linked transfers via FX position accounts (see #68).
- `HISTORY` must be set when an account is created; it cannot be added later.

Operational facts that are easy to lose:

- TigerBeetle needs `io_uring`, which Docker's default seccomp profile blocks. Compose sets
  `security_opt: [seccomp=unconfined]`; without it `format` fails outright on macOS.
- The Java client loads a native library via `System.load`, so JDK 25 (JEP 472) needs
  `--enable-native-access=ALL-UNNAMED` — set in the Dockerfile's `JAVA_OPTS` and on the
  Gradle `JavaExec`/`Test` tasks.
- **The client only accepts literal IP addresses.** `tigerbeetle:3000` and `localhost:3000`
  both fail with `InitializationException`. `TigerBeetleAddresses.resolve` converts
  host:port to ip:port before the client is constructed.
- Client and server versions must match; `tigerBeetleVersion` in `gradle.properties` and the
  image tag in `docker-compose.yml` move together.

### Tracing

One trace spans Java and Python, over HTTP, Feign, and Kafka. Java services enable Kafka
observation explicitly on their *custom* beans (`template.setObservationEnabled(true)` in
`KafkaProducerConfig`, `factory.getContainerProperties().setObservationEnabled(true)` in
`KafkaConsumerConfig`) because Boot's auto-config doesn't apply to hand-built beans. Python
propagates `traceparent` manually across the Kafka boundary in `app/core/telemetry.py` and
the consumer/publisher wrappers. If you add a new producer or consumer, wire this up or the
trace breaks at that hop.

## Conventions

### Naming

Explicit beats short — these services are read far more often than written, and the ledger
domain is dense enough without abbreviation.

- Spell out what a method returns or does: `toLedgerPostingInstruction`, not `toInstruction`;
  `applyPostingInstruction`, not `post`; `findLedgerTransfersByClientTransactionId`, not
  `findAll`; `deriveWalletAccountId`, not `wallet`.
- No vague factory names (`of`, `from`) on domain types — `LedgerAccount.newWallet`,
  `LedgerPostingInstruction.settlement(...)`.
- **Mapper methods name what they produce**, always — never bare `toEntity`, `toEntities`,
  `toResponse` or `toDto`. `toAccountWalletEntities`, `toAccountResponse`,
  `toLedgerPostingInstruction`, `toLedgerPostingResponse`. This also rules out overloads that
  differ only by argument type, which read identically at the call site.
- Avoid imported jargon. "Leg" was replaced by `CustomerAccountMovementRequest`, which says
  what the object actually holds.
- Name variables for the layer they belong to: `ledgerTransfer` vs `transferBatch`,
  `customerAccountId` vs `ledgerAccountId`.

### Formatting

When a method signature does not fit on one line, put **each parameter on its own line** and
leave a **blank line before the body**. Never pack several parameters onto a continuation
line.

```java
// yes
public static CreateLedgerPostingInstructionRequest createInboundAuthorisation(
        UUID clientTransactionId,
        UUID accountId,
        BigDecimal amount,
        Currency currency) {

    return CreateLedgerPostingInstructionRequest.builder()
            .clientTransactionId(clientTransactionId)
            .inboundAuthorisation(createCustomerAccountMovement(accountId, amount, currency))
            .build();
}

// no
public static CreateLedgerPostingInstructionRequest createOutboundAuthorisation(
        UUID clientTransactionId, UUID accountId, BigDecimal amount, Currency currency) {
    return CreateLedgerPostingInstructionRequest.builder()
            ...
}
```

Signatures that fit on one line stay on one line, with no blank line after `{`.

### Comments

Write almost none. Comment the *why* when it is not recoverable from the code — a TigerBeetle
constraint, a JEP, a deliberate deviation. Never restate what the next line does. Prefer
extracting a well-named method over explaining a block.

### Structure

- `controller/` → `service/` → `repository/` (all TigerBeetle/JPA access), with `domain/`,
  `mapper/`, `model/{request,response}/`, `enums/`, `exception/handler/`, `config/`, and
  `infrastructure/messaging/kafka/{producer,consumer}`. No hexagonal `...Port` interfaces —
  no service in this repo uses them.
- Controllers return `ResponseEntity<@NonNull XResponse>` directly. Errors go through a
  per-service `GlobalExceptionHandler` building `CustomError` from `:common`.
- Dependency versions are centralised in `bank-solution-backend/gradle.properties`, not
  inline in module `build.gradle` files.
- Service config uses `${ENV_VAR:local-default}` in `application.properties` so the same jar
  runs locally and in Compose; Kafka topic names are properties, not string literals.

### Tests

- Method names are `should…`-prefixed and describe behaviour, not the method under test.
  Fixtures are `create…`-prefixed statics in a `fixtures/` package.
- Unit tests need no Spring context: domain, enums, mappers, id derivation, `MoneyUtils`.
- Integration tests extend `BaseIntegrationTest` and are `@Tag("integration")`, so they are
  excluded from `./gradlew test` and CI stays Docker-free. The layout (see
  `ledger-svc/src/test`) is:
  - `common/annotations/IntegrationTest` — `@SpringBootTest` + `@ContextConfiguration`
    (initializers) + `@ActiveProfiles("test")` + `@AutoConfigureMockMvc`
  - `common/initializers/…Initializer` — starts the container **once** in a `static` field
    and injects its address with `TestPropertyValues`; Spring caches the context, so all
    test classes share one container (first class pays ~8s, the rest are ~0.1s)
  - `common/containers/…Container`, `common/BaseIntegrationTest` (MockMvc + ObjectMapper)
- Prefer specific AssertJ assertions: `assertThat(values()).allMatch(...)` over
  `assertThat(stream.allMatch(...)).isTrue()`; the former names the offending element.
- Derive expected values from the code under test rather than hardcoding magic numbers
  (`SEEDED_CURRENCIES * LedgerAccountType.internalTypes().length`, not `12`).
- **Mappers get their own unit tests**, one assertion group per translation rule, because
  the mappers are where the ledger's meaning is encoded: u128 ↔ UUID, integer minor units ↔
  `BigDecimal` (per-currency exponent — check both GBP and JPY), nanosecond timestamps ↔
  `Instant`, and the zero-id-means-null convention. `TransferBatch` is fully settable from
  outside the TigerBeetle package, so `LedgerTransferMapper` is unit-testable end to end.
  `AccountBatch` balance setters are package-private, so balance mapping is covered by the
  repository integration tests instead — say so in the test class comment.
- Integration tests share one container and Spring context, so they must not depend on
  global state. Use a fresh random `customerAccountId` per test, and assert on **deltas**
  (balance before vs after) rather than absolute totals of shared internal accounts.

### SonarQube rules that come up

- **S5778** — an `assertThatThrownBy` / `assertThrows` lambda may contain only one throwing
  invocation. Hoist any setup (e.g. `new BigDecimal("1.005")`) out of the lambda.
- **S2095** — a Testcontainers singleton is flagged as an unclosed resource. Suppress with
  `@SuppressWarnings({"resource", "java:S2095"})` (IntelliJ reads the first token, Sonar the
  second) and say why in a comment: Ryuk removes it at JVM exit.
- **S1192** — extract any string literal used 3+ times in a file (URLs, JSON paths).
- Test classes and methods stay package-private (**S5786**); abstract bases are exempt from
  **S2187**.

### Spring Boot 4 traps

Boot 4 split many modules apart; these cost real debugging time:

- `@AutoConfigureMockMvc` is **not** in `spring-boot-test-autoconfigure` (which now ships
  only `jdbc` and `json` slices). It lives in `org.springframework.boot.webmvc.test.autoconfigure`,
  artifact `spring-boot-webmvc-test`.
- Boot 4.1 ships **Jackson 3**. Autowire `tools.jackson.databind.ObjectMapper`;
  `com.fasterxml.jackson.databind.ObjectMapper` has no bean even though serialisation works.
- `HealthIndicator` / `Health` are in `org.springframework.boot.health.contributor`.
- When probing an external system in a `HealthIndicator`, use the async client API with a
  timeout. The TigerBeetle client blocks indefinitely when its cluster is down, which would
  wedge `/actuator/health` permanently.
