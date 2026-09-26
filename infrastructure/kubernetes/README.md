# Kubernetes (local: kind)

Helm charts for the whole platform, orchestrated by helmfile. Compose stays the day-to-day dev loop; this is for
integration, scaling and failure testing, and is what EKS/AKS will run later.

## Run it

```bash
brew install kind helm helmfile kubectl
./scripts/cluster-up.sh                      # kind cluster "banksolution"
./scripts/build-images.sh                    # build + kind load every image (tag: local); filter: build-images.sh risk
./scripts/deploy.sh --concurrency 2          # helmfile sync, environment "local"
./scripts/deploy.sh --selector name=payment-service
./scripts/cluster-down.sh
```

Keep `--concurrency` low for full syncs: a rolling update briefly runs old and new pods side by side.

Needs about 13 GB of Docker memory and free Docker disk; a full Docker VM disk shows up as Kafka/PostgreSQL
crash-loops ("Not enough disk space" on the CNPG cluster).

## Layout

| Path                                  | Holds                                                                                   |
|---------------------------------------|-----------------------------------------------------------------------------------------|
| `charts/service-types/*`              | One chart per kind of service; its `values.yaml` is the complete definition of the type |
| `services/<namespace>/<release>.yaml` | One file per service: only what differs from its type                                   |
| `charts/library/banksolution-common`  | Building blocks the types assemble: ConfigMap, Deployment, Service, migration Job       |
| `charts/data/*`                       | Stateful stores: `postgres` (CNPG), `neo4j` (official chart), `tigerbeetle`             |
| `data/<namespace>/<release>.yaml`     | One file per store instance: `bank-postgres`, `ai-postgres` (their databases)           |
| `charts/platform/*`                   | `kafka` (Strimzi KRaft + topics), `schema-registry` (+ schema registration Job)         |
| `platform/*`                          | Values for third-party operator charts (Strimzi, CloudNativePG)                         |
| `environments/<env>/services.yaml`    | Overrides applied to every service in that environment                                  |
| `environments/<env>/<release>.yaml`   | Overrides for one data/platform release (credentials, sizes); optional                  |
| `helmfile.yaml.gotmpl`                | Releases: which type each service is, order (`needs`), readiness waits                  |

### Service types

| Type                | Gives you                                                                                           | Services            |
|---------------------|-----------------------------------------------------------------------------------------------------|---------------------|
| `java-microservice` | Tuned JVM, Actuator probes, optional `datasource`, `messaging`, `migration`                         | 10 banking services |
| `gateway`           | Tuned JVM, Actuator probes; routes as env vars                                                      | backoffice-gateway  |
| `frontend`          | nginx on :80, probes on `/`, 64 Mi                                                                  | backoffice-ui       |
| `ai-service`        | Python with optional SQLAlchemy `datasource`, Kafka, Alembic `migration.command`, `persistentPaths` | marl-orchestrator   |
| `ai-agent`          | Python, probes on `/api/v1/health`, non-root                                                        | ML agents           |

A block that a type does not declare is off. Anything in a type can be overridden per service (e.g. `ledger-service`
adds `jvm.extraOptions` and an `Unconfined` seccomp profile; `schema-registry` uses `extraEnv` for a Downward API
host name).

Namespaces: `platform`, `cnpg-system`, `banking`, `ai`, later `observability`.

`schema-registry/schemas` is a symlink to `libraries/avro-schema-library/schemas`, so the `.avsc` files stay the
single source of truth. Subjects to register are listed in that chart's `values.yaml`.

## Adding a service

1. Write `services/<namespace>/<name>.yaml`: image, `containerPort`, `env`, and the blocks it needs.
   - `datasource`: also add `{service, database, role}` to `data/banking/bank-postgres.yaml` and its password
     to `environments/local/bank-postgres.yaml`. That creates the role, the database and `<name>-db-credentials`.
   - `messaging`: list every topic under `consumes` / `produces`, keyed like the service's
     `spring.kafka.topics.incoming.<key>` / `outgoing.<key>`. They become `SPRING_KAFKA_TOPICS_*` env vars, so this
     file is what the pod actually uses.
   - `migration`: the Liquibase image, run as a pre-install/pre-upgrade Job.
2. Add a release to `helmfile.yaml.gotmpl` with `chart: charts/service-types/<type>` and `inherit: [template: service]`.
3. Add the image to `scripts/build-images.sh` (`JAVA_IMAGES` for Gradle modules, `DOCKERFILE_IMAGES` otherwise).

After editing the library chart, run `helm dependency update` on each type chart and on `schema-registry`: they
vendor a packaged copy of it.

Kafka bootstrap: `bank-kafka-kafka-bootstrap.platform:9092`. Schema Registry: `http://schema-registry.platform:8081`.
