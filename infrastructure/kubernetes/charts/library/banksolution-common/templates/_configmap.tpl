{{- define "banksolution-common.configMap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
data:
  {{- range $name, $value := .Values.env }}
  {{ $name }}: {{ $value | toString | quote }}
  {{- end }}
{{- end -}}
