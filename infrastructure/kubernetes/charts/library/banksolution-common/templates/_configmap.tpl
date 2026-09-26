{{- define "banksolution-common.configMap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
data:
  {{- if .Values.messaging.enabled }}
  KAFKA_BOOTSTRAP_SERVERS: {{ .Values.messaging.bootstrapServers | quote }}
  SCHEMA_REGISTRY_URL: {{ .Values.messaging.schemaRegistryUrl | quote }}
  {{- end }}
  {{- range $name, $value := .Values.env }}
  {{ $name }}: {{ $value | toString | quote }}
  {{- end }}
{{- end -}}
