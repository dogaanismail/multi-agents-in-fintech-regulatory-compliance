{{- define "banksolution-common.application" -}}
{{ include "banksolution-common.configMap" . }}
---
{{ include "banksolution-common.deployment" . }}
---
{{ include "banksolution-common.service" . }}
{{- if include "banksolution-common.enabled" .Values.migration }}
---
{{ include "banksolution-common.migrationJob" . }}
{{- end }}
{{- end -}}
