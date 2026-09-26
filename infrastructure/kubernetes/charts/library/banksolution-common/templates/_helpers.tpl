{{- define "banksolution-common.enabled" -}}
{{- if and . .enabled }}true{{ end -}}
{{- end -}}

{{- define "banksolution-common.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "banksolution-common.selectorLabels" -}}
app.kubernetes.io/name: {{ include "banksolution-common.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "banksolution-common.labels" -}}
{{ include "banksolution-common.selectorLabels" . }}
app.kubernetes.io/part-of: bank-solution
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.image.tag | quote }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "banksolution-common.image" -}}
{{- printf "%s:%s" .repository .tag -}}
{{- end -}}

{{- define "banksolution-common.topicEnvName" -}}
{{- printf "SPRING_KAFKA_TOPICS_%s_%s" (index . 0) (index . 1) | upper | replace "-" "_" | replace "." "_" -}}
{{- end -}}

{{- define "banksolution-common.datasourceEnv" -}}
{{- if include "banksolution-common.enabled" .Values.datasource }}
- name: SPRING_DATASOURCE_URL
  value: {{ printf "jdbc:postgresql://%s:%v/%s" .Values.datasource.host .Values.datasource.port .Values.datasource.database | quote }}
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.datasource.credentialsSecret }}
      key: username
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.datasource.credentialsSecret }}
      key: password
- name: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
  value: {{ .Values.datasource.maxPoolSize | quote }}
{{- end }}
{{- end -}}

{{- define "banksolution-common.secretEnv" -}}
{{- range .Values.secretEnv }}
- name: {{ .name }}
  valueFrom:
    secretKeyRef:
      name: {{ .secretName }}
      key: {{ .key }}
{{- end }}
{{- end -}}
