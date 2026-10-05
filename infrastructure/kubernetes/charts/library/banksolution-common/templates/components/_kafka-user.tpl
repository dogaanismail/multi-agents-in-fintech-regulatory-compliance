{{- define "banksolution-common.kafkaAuthenticationEnabled" -}}
{{- if and (include "banksolution-common.enabled" .Values.messaging) (include "banksolution-common.enabled" .Values.messaging.authentication) }}true{{ end -}}
{{- end -}}

{{- define "banksolution-common.kafkaCredentialsSecretName" -}}
{{- printf "%s-kafka-credentials" (include "banksolution-common.name" .) -}}
{{- end -}}

{{- define "banksolution-common.kafkaPassword" -}}
{{- $name := include "banksolution-common.name" . -}}
{{- required (printf "messaging.authentication.credentials.%s is required when Kafka authentication is enabled" $name) (get .Values.messaging.authentication.credentials $name) -}}
{{- end -}}

{{- define "banksolution-common.kafkaAcl" -}}
- resource:
    type: {{ .type }}
    name: {{ .name | quote }}
    patternType: {{ .patternType | default "literal" }}
  operations:
    {{- toYaml .operations | nindent 4 }}
{{- end -}}

{{- define "banksolution-common.kafkaUser" -}}
{{- $name := include "banksolution-common.name" . -}}
{{- $authentication := .Values.messaging.authentication -}}
{{- $password := include "banksolution-common.kafkaPassword" . -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ include "banksolution-common.kafkaCredentialsSecretName" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
type: kubernetes.io/basic-auth
stringData:
  username: {{ $name | quote }}
  password: {{ $password | quote }}
  sasl.jaas.config: {{ printf "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";" $name $password | quote }}
---
apiVersion: v1
kind: Secret
metadata:
  name: {{ printf "%s-kafka-password" $name }}
  namespace: {{ $authentication.kafkaNamespace }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
type: Opaque
stringData:
  password: {{ $password | quote }}
---
apiVersion: kafka.strimzi.io/v1
kind: KafkaUser
metadata:
  name: {{ $name }}
  namespace: {{ $authentication.kafkaNamespace }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
    strimzi.io/cluster: {{ $authentication.kafkaCluster }}
spec:
  authentication:
    type: scram-sha-512
    password:
      valueFrom:
        secretKeyRef:
          name: {{ printf "%s-kafka-password" $name }}
          key: password
  authorization:
    type: simple
    acls:
      {{- range $key, $topic := .Values.messaging.consumes }}
      {{- include "banksolution-common.kafkaAcl" (dict "type" "topic" "name" $topic "operations" (list "Read" "Describe")) | nindent 6 }}
      {{- include "banksolution-common.kafkaAcl" (dict "type" "topic" "name" (printf "%s.DLT" $topic) "operations" (list "Write" "Describe")) | nindent 6 }}
      {{- end }}
      {{- range $key, $topic := .Values.messaging.produces }}
      {{- include "banksolution-common.kafkaAcl" (dict "type" "topic" "name" $topic "operations" (list "Write" "Describe")) | nindent 6 }}
      {{- end }}
      {{- range .Values.messaging.otherProducedTopics }}
      {{- include "banksolution-common.kafkaAcl" (dict "type" "topic" "name" . "operations" (list "Write" "Describe")) | nindent 6 }}
      {{- end }}
      {{- if .Values.messaging.consumerGroup }}
      {{- include "banksolution-common.kafkaAcl" (dict "type" "group" "name" .Values.messaging.consumerGroup "operations" (list "Read")) | nindent 6 }}
      {{- end }}
      {{- if .Values.messaging.transactionalIdPrefix }}
      {{- include "banksolution-common.kafkaAcl" (dict "type" "transactionalId" "name" .Values.messaging.transactionalIdPrefix "patternType" "prefix" "operations" (list "Write" "Describe")) | nindent 6 }}
      {{- end }}
{{- end -}}

{{- define "banksolution-common.kafkaEnv" -}}
{{- if include "banksolution-common.kafkaAuthenticationEnabled" . }}
{{- $authentication := .Values.messaging.authentication }}
{{- $secretName := include "banksolution-common.kafkaCredentialsSecretName" . }}
- name: SPRING_KAFKA_SECURITY_PROTOCOL
  value: {{ $authentication.securityProtocol | quote }}
- name: SPRING_KAFKA_PROPERTIES_SASL_MECHANISM
  value: {{ $authentication.mechanism | quote }}
- name: SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: sasl.jaas.config
- name: KAFKA_SECURITY_PROTOCOL
  value: {{ $authentication.securityProtocol | quote }}
- name: KAFKA_SASL_MECHANISM
  value: {{ $authentication.mechanism | quote }}
- name: KAFKA_SASL_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: username
- name: KAFKA_SASL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ $secretName }}
      key: password
{{- end }}
{{- end -}}
