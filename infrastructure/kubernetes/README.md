# Kubernetes (local: kind)

Helm charts for the whole platform, deployed by ArgoCD from this Git repository. helmfile only bootstraps the
cluster (operators + ArgoCD + the app definitions). Compose stays the day-to-day dev loop; this is for integration,
scaling and failure testing, and is what EKS/AKS will run later.

## Run it

```bash
brew install kind helm helmfile kubectl
./scripts/cluster-up.sh                      # kind cluster "banksolution"
./scripts/build-images.sh                    # build + kind load every image (tag: local); filter: build-images.sh risk
./scripts/deploy.sh                          # bootstrap: Strimzi, CloudNativePG, ArgoCD, ApplicationSets
./scripts/argocd-ui.sh                       # http://localhost:8443, prints the admin password
./scripts/cluster-down.sh
```

ArgoCD then syncs every store and service from `main`. Changes go live by **commit → push → sync** (ArgoCD polls
every ~60 s; "Refresh" in the UI is instant). `selfHeal` is off, so `kubectl scale` experiments are shown as
OutOfSync instead of being reverted. Rebuilding an image under the same `local` tag does not change the manifest:
`kubectl rollout restart deploy/<name>` picks it up.

Before pushing, run `./scripts/validate.sh` (needs `helm`, `kubeconform`, Python with PyYAML). It renders every app
exactly as ArgoCD will and checks all resources against the Kubernetes and CRD schemas in strict mode; the
`Kubernetes Validate` workflow runs the same script on every push touching `infrastructure/kubernetes/**`.

Services with a `datasource` get a `wait-for-database` init container (Deployment and migration Job) that loops
until the service's own login can query its own database, so a fresh sync never races the store it depends on.

In the UI each service is one app: Deployment → Pod, ConfigMap, Service, PVC, and its last `db-migration` Job (kept
after success, replaced by the next run).

Needs about 13 GB of Docker memory and free Docker disk; a full Docker VM disk shows up as Kafka/PostgreSQL
crash-loops ("Not enough disk space" on the CNPG cluster).

## Layout

| Path                                  | Holds                                                                                               |
|---------------------------------------|-----------------------------------------------------------------------------------------------------|
| `charts/service-types/*`              | One chart per kind of service; its `values.yaml` is the complete definition of the type             |
| `services/<namespace>/<release>.yaml` | One file per service: only what differs from its type                                               |
| `charts/library/banksolution-common`  | Building blocks the types assemble: ConfigMap, Deployment, Service, migration Job                   |
| `charts/data/*`                       | Stateful stores: `postgres` (CNPG), `neo4j` (official chart), `tigerbeetle`                         |
| `data/<namespace>/<release>.yaml`     | One file per store instance: `bank-postgres`, `ai-postgres` (their databases)                       |
| `charts/platform/*`                   | `kafka` (Strimzi KRaft + topics), `schema-registry` (+ schema registration Job), `priority-classes` |
| `platform/*`                          | Values for third-party charts (Strimzi, CloudNativePG, ArgoCD)                                      |
| `environments/<env>/services.yaml`    | Overrides applied to every service in that environment                                              |
| `environments/<env>/<release>.yaml`   | Overrides for one data/platform release (credentials, sizes); optional                              |
| `charts/gitops/bank-solution-apps`    | ArgoCD project + ApplicationSets: `stores` (list) and `services` (one app per file)                 |
| `helmfile.yaml.gotmpl`                | Bootstrap only: operators, ArgoCD, `bank-solution-apps`                                             |

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

Namespaces: `platform`, `cnpg-system`, `argocd`, `banking`, `ai`, later `observability`. A new store goes in the
`stores` list of `charts/gitops/bank-solution-apps/values.yaml`.

`schema-registry/schemas` is a symlink to `libraries/avro-schema-library/schemas`, so the `.avsc` files stay the
single source of truth. Subjects to register are listed in that chart's `values.yaml`.

## Network policies

Every service gets a NetworkPolicy: it may open connections to exactly what it declares (`networkPolicy.egress`), and
it accepts connections on its port only from the callers it declares (`networkPolicy.ingress`, e.g. payment-engine only
from `backoffice-gateway`). Kubelet probes and `kubectl port-forward` are unaffected by ingress rules. `backoffice-ui`
declares no ingress, as the public entry point. Kafka's listener only admits pods labelled
`banksolution.io/kafka-client: "true"`, which the library sets on every service with `messaging`.

- `networkPolicy.egress` in the service file lists what it calls: `account-service` (same namespace),
  `banking/configuration-service` (another namespace), or a peer name.
- Peers (`dns`, `kafka`, `schema-registry`, `bank-postgres`, `ai-postgres`, `neo4j`, `tigerbeetle`, `internet-https`)
  are defined per environment under `networkPeers` in `environments/<env>/services.yaml`, so a cloud environment can
  point `kafka` at a managed broker's CIDR instead of in-cluster pods.
- Added automatically: `dns` (type `baseline`), the database peer when `datasource` is on, `kafka` and
  `schema-registry` when `messaging` is on.

`./scripts/verify-network-policies.sh` probes real connections from inside the pods (e.g. `marl-orchestrator` →
`bank-postgres` must be refused) against a running cluster.

## Kafka authentication and ACLs

Kafka's `plain` listener (9092) requires SCRAM-SHA-512 and authorization is `simple`: a client may only do what its
`KafkaUser` allows. Every service with `messaging` gets one, rendered by the library chart in `platform` next to Kafka:

- Read on each `consumes` topic, Write on its `<topic>.DLT`, Write on each `produces` topic and `otherProducedTopics`.
- Read on `messaging.consumerGroup`; payment-engine also gets its `transactionalIdPrefix` (Axon's Kafka producer).
- Credentials: `messaging.authentication.credentials.<release>` in `environments/<env>/services.yaml` becomes
  `<release>-kafka-credentials` in the service's namespace (Java reads `SPRING_KAFKA_*`, Python `KAFKA_SASL_*`).

Schema Registry has its own `KafkaUser` for `_schemas`; `kafka-admin` is a super user for operations.
Compose and the Testcontainers tests stay on PLAINTEXT, because the clients only add SASL when those variables are set.

`./scripts/verify-kafka-acls.sh` probes the live broker with each service's own credentials: a service can describe
its topics, but cannot write another service's topic, join another service's group, or connect anonymously.

On one node, a redeploy surges a second copy of every service, and the new copies wait for Kafka credentials that only
a running broker can issue. The broker, the entity operator and the Strimzi operator therefore run under the
`platform-critical` PriorityClass, so they preempt application pods instead of staying `Pending`. The operator's leader
lease is 60 s (default 15 s): under a saturated node it would otherwise lose the lease and restart mid-reconciliation.

## Keycloak

`charts/platform/keycloak` runs Keycloak in `platform` with its own CloudNativePG database (`identity-postgres`) and
imports both realms from `infrastructure/keycloak` (linked into the chart as `realms/`). Pods in `banking` and `ai`
reach it as `keycloak:8180` through an `ExternalName` alias, which keeps the token issuer identical to Docker Compose.
Every service type has `keycloak` in its network-policy baseline. Keycloak runs at `platform-critical` priority: without
it nobody signs in and services cannot get or validate tokens.

Secrets: the `local` environment lets the chart create them (`environments/local/keycloak.yaml`); `cloud-demo` turns
that off and Terraform generates them. Each machine client's secret lands in its service's namespace as
`<service>-keycloak-client`; the chart fails to render if `bank-internal-realm.json` gains a client that has no
namespace in `serviceClients`.

To sign in on kind: add `127.0.0.1 keycloak` to `/etc/hosts`, then

```bash
kubectl -n platform port-forward svc/keycloak 8180:8180
kubectl -n banking port-forward svc/backoffice-ui 6060:80
```

and open http://localhost:6060 (users and roles are in `SECURITY.md`).

## Adding a service

1. Write `services/<namespace>/<name>.yaml`, starting with `serviceType: <type>`, then image, `containerPort`, `env`,
   and the blocks it needs. ArgoCD creates the app from the file on the next sync.
   - `datasource`: also add `{service, database, role}` to `data/banking/bank-postgres.yaml` and its password
     to `environments/local/bank-postgres.yaml`. That creates the role, the database and `<name>-db-credentials`.
   - `messaging`: list every topic under `consumes` / `produces`, keyed like the service's
     `spring.kafka.topics.incoming.<key>` / `outgoing.<key>`. They become `SPRING_KAFKA_TOPICS_*` env vars, so this
     file is what the pod actually uses.
   - `migration`: the Liquibase image, run as a pre-install/pre-upgrade Job.
   - `networkPolicy.egress`: every other service or peer it calls; anything not listed is blocked.
   - `networkPolicy.ingress`: every service that calls it; add the new service to the callee's list as well.
   - Kafka: `messaging.consumerGroup` for a consumer, plus its password under
     `messaging.authentication.credentials` in `environments/<env>/services.yaml`.
2. Add the image to `scripts/build-images.sh` (`JAVA_IMAGES` for Gradle modules, `DOCKERFILE_IMAGES` otherwise).

After editing the library chart, run `helm dependency update` on each type chart and on `schema-registry`: they
vendor a packaged copy of it.

Kafka bootstrap: `bank-kafka-kafka-bootstrap.platform:9092`. Schema Registry: `http://schema-registry.platform:8081`.
