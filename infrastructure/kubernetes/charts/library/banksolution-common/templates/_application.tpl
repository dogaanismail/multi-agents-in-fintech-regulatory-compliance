{{- define "banksolution-common.application" -}}
{{ include "banksolution-common.configMap" . }}
---
{{ include "banksolution-common.deployment" . }}
---
{{ include "banksolution-common.service" . }}
{{- if .Values.persistentPaths }}
{{ include "banksolution-common.persistentVolumeClaims" . }}
{{- end }}
{{- if include "banksolution-common.enabled" .Values.migration }}
---
{{ include "banksolution-common.migrationJob" . }}
{{- end }}
{{- end -}}
