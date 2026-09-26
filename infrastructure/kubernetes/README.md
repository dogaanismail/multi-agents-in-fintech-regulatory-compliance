# Kubernetes (local: kind)

Helm charts for the whole platform, orchestrated by helmfile. Compose stays the day-to-day dev loop; this is for
integration, scaling and failure testing, and is what EKS/AKS will run later.

## Run it

```bash
brew install kind helm helmfile kubectl
./scripts/cluster-up.sh       # kind cluster "banksolution"
./scripts/build-images.sh     # bootJar -> docker build -> kind load (tag: local); filter: build-images.sh risk
./scripts/deploy.sh           # helmfile sync, environment "local"
./scripts/deploy.sh --selector name=configuration-service    # one release
./scripts/cluster-down.sh
```

Needs roughly 5 GB of Docker memory and free Docker disk; a full Docker VM disk shows up as Kafka/PostgreSQL
crash-loops ("Not enough disk space" on the CNPG cluster).

## Layout

| Path                                 | Holds                                                                                                        |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `charts/library/banksolution-common` | Library chart: ConfigMap, Deployment, Service, Liquibase migration Job, defaults                             |
| `charts/platform/*`                  | Our platform charts: `kafka` (Strimzi KRaft cluster + topics), `schema-registry` (+ schema registration Job) |
| `charts/banking/*`, `charts/ai/*`    | One thin chart per service: `Chart.yaml`, `values.yaml`, `templates/resources.yaml`                          |
| `platform/*`                         | Values for third-party operator charts (Strimzi, CloudNativePG)                                              |
| `environments/<env>/<release>.yaml`  | Per-environment overrides, picked up automatically by release name; optional                                 |
| `helmfile.yaml.gotmpl`               | Releases, order (`needs`) and readiness waits                                                                |

Namespaces: `platform` (Kafka, Schema Registry), `cnpg-system` (operator), `banking`, later `ai` and `observability`.

`schema-registry/schemas` is a symlink to `libraries/avro-schema-library/schemas`, so the `.avsc` files stay the
single source of truth. Subjects to register are listed in that chart's `values.yaml`.

## Adding a PostgreSQL-backed Java service

1. `charts/banking/bank-postgres/values.yaml`: add `{service, database, role}` to `databases`; add its password under
   `credentials` in `environments/local/bank-postgres.yaml`. This creates the role, the database and the
   `<service>-db-credentials` secret.
2. Copy `charts/banking/risk-engine-service`, then set `image.repository`, `containerPort`, `env`, `datasource` and
   `migration.image.repository`. `messaging.enabled: true` injects `KAFKA_BOOTSTRAP_SERVERS` and `SCHEMA_REGISTRY_URL`.
   Declare every topic the service touches under `messaging.consumes` / `messaging.produces`, keyed like its
   `spring.kafka.topics.incoming.<key>` / `outgoing.<key>` properties. These become
   `SPRING_KAFKA_TOPICS_INCOMING_<KEY>` env vars, so the values file is what the pod actually uses.
3. Add its images to `JAVA_IMAGES` in `scripts/build-images.sh` and a release to `helmfile.yaml.gotmpl`.

Kafka bootstrap for services: `bank-kafka-kafka-bootstrap.platform:9092`; Schema Registry:
`http://schema-registry.platform:8081`.
