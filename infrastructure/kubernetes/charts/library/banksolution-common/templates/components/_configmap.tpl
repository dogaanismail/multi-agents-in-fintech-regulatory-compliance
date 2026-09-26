{{- define "banksolution-common.configMapData" -}}
{{- if include "banksolution-common.enabled" .Values.jvm }}
JAVA_OPTS: {{ trim (printf "%s %s" .Values.jvm.options (.Values.jvm.extraOptions | default "")) | quote }}
MALLOC_ARENA_MAX: "2"
{{- end }}
{{- if include "banksolution-common.enabled" .Values.messaging }}
KAFKA_BOOTSTRAP_SERVERS: {{ .Values.messaging.bootstrapServers | quote }}
SCHEMA_REGISTRY_URL: {{ .Values.messaging.schemaRegistryUrl | quote }}
{{- range $key, $topic := .Values.messaging.consumes }}
{{ include "banksolution-common.topicEnvName" (list "incoming" $key) }}: {{ $topic | quote }}
{{- end }}
{{- range $key, $topic := .Values.messaging.produces }}
{{ include "banksolution-common.topicEnvName" (list "outgoing" $key) }}: {{ $topic | quote }}
{{- end }}
{{- end }}
{{- range $name, $value := .Values.env }}
{{ $name }}: {{ $value | toString | quote }}
{{- end }}
{{- end -}}

{{- define "banksolution-common.configMap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
data:
  {{- include "banksolution-common.configMapData" . | nindent 2 }}
{{- end -}}
