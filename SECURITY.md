# Security

Every HTTP call in the platform carries a JWT that the receiver validates itself, even inside the cluster. Network
policies and Kafka ACLs (see `infrastructure/kubernetes/README.md`) are a second line, not the only one.

## Identities

Keycloak holds one realm per population, so a token from one can never pass as another (different issuer and keys):

| Realm            | Who                       | How they get a token                       | Status   |
|------------------|---------------------------|--------------------------------------------|----------|
| `bank-staff`     | Backoffice employees      | Browser login through `backoffice-gateway` | In place |
| `bank-internal`  | Our services              | Client credentials, one client per service | In place |
| `bank-customers` | Retail customers (mobile) | Authorization code + PKCE                  | Planned  |
| `bank-partners`  | Partner fintechs          | Client credentials, scopes, rate limits    | Planned  |

The realm files are `infrastructure/keycloak/*-realm.json`. Secrets and demo passwords are `${ENV}` placeholders that
Keycloak fills in on import. Docker Compose and the kind `local` environment supply local values; in the cloud
environments Terraform generates every secret (`terraform output -raw demo_user_password`).

Every deployment uses the issuer `http://keycloak:8180/realms/<realm>`. In Kubernetes, Keycloak runs in `platform`
(chart `charts/platform/keycloak`, database `identity-postgres`) and an `ExternalName` Service named `keycloak` in
`banking` and `ai` points at it, so pods resolve the same name as Compose. Browsers need `127.0.0.1 keycloak` in
`/etc/hosts` and a port-forward to 8180.

## Permissions and roles

Code checks **permissions** (`payment.review`, `customer.create`, `ledger.post`, …), never role names. Permissions are
client roles of the `bank-platform` client and reach services in the token's `permissions` claim. A **role** is a named
set of permissions (a Keycloak composite), so a role can change in Keycloak without a code change or deploy.
`super-admin` holds only `iam.manage`: whoever manages access cannot also move money unless given a business role too.

## How a request is checked

```
browser ──session cookie──▶ backoffice-gateway ──staff JWT (TokenRelay)──▶ service
                              checks the permission                        checks issuer, audience, permission
service ──service JWT (client credentials)──▶ service
```

- **backoffice-gateway** signs staff in (OIDC + PKCE), keeps an HttpOnly `SameSite=Lax` session so tokens never reach
  the browser, requires a CSRF token on writes, and checks the permission of every `/api/v1` call; unlisted endpoints
  are denied.
- **Every Java service** uses `security-library/service-security`: it accepts a token only from a trusted issuer
  (`banksolution.security.trusted-issuers`) and addressed to one of its audiences (`banksolution.security.audiences`,
  its own name plus `bank-platform` for staff tokens relayed by the gateway). Each endpoint declares its permission
  with `@RequiresPermission(Permissions.X)`.
- **Service-to-service calls** use the caller's own machine identity. A Feign client opts in with
  `configuration = ServiceTokenFeignConfiguration.class`; the token is fetched once and cached until it expires.
  External APIs (the exchange-rate API) never receive our tokens. Each machine client holds only the permissions it
  needs and names only the services it calls as audiences, for example `risk-engine-service`: `account.read`,
  `customer.read`, `risk.read` for account, customer-profile and network-topology.
- **The MARL orchestrator** validates tokens the same way (`app/core/security.py`) and calls the agents and
  configuration-service with its own client-credentials token (`app/core/service_token.py`). **The agents** trust only
  `bank-internal` tokens addressed to them.
- **Audit fields come from the token.** payment-engine records the officer who approved, rejected or overrode a
  payment from the JWT; the API no longer accepts a name in the request body.

Kafka messages carry no tokens: each service has its own SCRAM login and ACLs, and identities in events (such as
`approvedBy`) are taken from the validated JWT at the HTTP edge.

## Adding things

- **An endpoint:** annotate it with `@RequiresPermission`. Use an existing permission, or add one to
  `Permissions` (Java), the gateway's `Permission` enum and the `bank-platform` client roles in both realm files.
  `PermissionCatalogTest` fails if the gateway and the staff realm drift apart.
- **A service:** depend on `:security-library:service-security`, set `banksolution.security.audiences`, and add
  `@Import(EveryPermissionMockMvcConfiguration.class)` to its integration-test base.
- **A caller of another service:** add a client to `bank-internal-realm.json` with only the permissions and audiences
  it needs, give it a `SERVICE_CLIENT_SECRET`, and set the `bank-internal` client registration in its properties.

## Not done yet

- The gateway relays the staff token to every service. Token exchange (RFC 8693) would give each downstream call a
  token for that service only.
- A role change reaches a signed-in staff user at their next login; services see it within one access-token lifetime (5
  minutes).
- Transport between pods is not encrypted (planned: service-mesh mTLS).
