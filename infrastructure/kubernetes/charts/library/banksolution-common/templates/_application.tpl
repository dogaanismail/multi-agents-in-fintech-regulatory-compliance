{{- define "banksolution-common.application" -}}
{{ include "banksolution-common.configMap" . }}
---
{{ include "banksolution-common.deployment" . }}
---
{{ include "banksolution-common.service" . }}
{{- if include "banksolution-common.kafkaAuthenticationEnabled" . }}
---
{{ include "banksolution-common.kafkaUser" . }}
{{- end }}
{{- if include "banksolution-common.enabled" .Values.networkPolicy }}
---
{{ include "banksolution-common.networkPolicy" . }}
{{- end }}
{{- if .Values.persistentPaths }}
{{ include "banksolution-common.persistentVolumeClaims" . }}
{{- end }}
{{- if include "banksolution-common.enabled" .Values.migration }}
---
{{ include "banksolution-common.migrationJob" . }}
{{- end }}
{{- end -}}
