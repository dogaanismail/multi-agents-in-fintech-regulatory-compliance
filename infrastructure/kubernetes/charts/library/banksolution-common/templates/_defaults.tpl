{{- define "banksolution-common.defaultValues" -}}
replicaCount: 1
nameOverride: ""
image:
  repository: ""
  tag: local
  pullPolicy: IfNotPresent
containerPort: 8080
service:
  type: ClusterIP
  port: null
env: {}
secretEnv: []
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    memory: 512Mi
podSecurityContext:
  runAsNonRoot: true
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
  seccompProfile:
    type: RuntimeDefault
securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
terminationGracePeriodSeconds: 45
probes:
  startup:
    httpGet:
      path: /actuator/health/liveness
      port: http
    periodSeconds: 5
    failureThreshold: 36
  liveness:
    httpGet:
      path: /actuator/health/liveness
      port: http
    periodSeconds: 10
    failureThreshold: 3
  readiness:
    httpGet:
      path: /actuator/health/readiness
      port: http
    periodSeconds: 10
    failureThreshold: 3
datasource:
  enabled: false
  host: ""
  port: 5432
  database: ""
  credentialsSecret: ""
messaging:
  enabled: false
  bootstrapServers: bank-kafka-kafka-bootstrap.platform:9092
  schemaRegistryUrl: http://schema-registry.platform:8081
migration:
  enabled: false
  image:
    repository: ""
    tag: local
    pullPolicy: IfNotPresent
  env: {}
  backoffLimit: 6
  activeDeadlineSeconds: 600
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      memory: 256Mi
{{- end -}}

{{- define "banksolution-common.application" -}}
{{- $defaults := fromYaml (include "banksolution-common.defaultValues" .) -}}
{{- $values := mergeOverwrite $defaults (deepCopy .Values) -}}
{{- $context := dict "Values" $values "Release" .Release "Chart" .Chart "Capabilities" .Capabilities -}}
{{ include "banksolution-common.configMap" $context }}
---
{{ include "banksolution-common.deployment" $context }}
---
{{ include "banksolution-common.service" $context }}
{{- if $values.migration.enabled }}
---
{{ include "banksolution-common.migrationJob" $context }}
{{- end }}
{{- end -}}
