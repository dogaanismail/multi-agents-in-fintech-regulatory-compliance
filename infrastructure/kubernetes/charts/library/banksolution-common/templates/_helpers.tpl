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
{{- if .registry -}}
{{- printf "%s/%s:%s" .registry .repository .tag -}}
{{- else -}}
{{- printf "%s:%s" .repository .tag -}}
{{- end -}}
{{- end -}}

{{- define "banksolution-common.topicEnvName" -}}
{{- printf (index . 0) (index . 1) (index . 2) | upper | replace "-" "_" | replace "." "_" -}}
{{- end -}}

{{- define "banksolution-common.datasourceEnv" -}}
{{- if include "banksolution-common.enabled" .Values.datasource }}
{{- if eq .Values.datasource.driver "spring" }}
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
{{- else if eq .Values.datasource.driver "sqlalchemy-asyncpg" }}
- name: DATABASE_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.datasource.credentialsSecret }}
      key: username
- name: DATABASE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.datasource.credentialsSecret }}
      key: password
- name: DATABASE_URL
  value: {{ printf "postgresql+asyncpg://$(DATABASE_USERNAME):$(DATABASE_PASSWORD)@%s:%v/%s" .Values.datasource.host .Values.datasource.port .Values.datasource.database | quote }}
{{- else }}
{{- fail (printf "unsupported datasource.driver %q" .Values.datasource.driver) }}
{{- end }}
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

{{- define "banksolution-common.persistentVolumeClaimName" -}}
{{- printf "%s-%s" (include "banksolution-common.name" .context) (base .path | lower | replace "_" "-") -}}
{{- end -}}

{{- define "banksolution-common.volumeMounts" -}}
{{- range $index, $path := .context.Values.writablePaths }}
- name: writable-{{ $index }}
  mountPath: {{ $path }}
{{- end }}
{{- if .includePersistent }}
{{- range $index, $volume := .context.Values.persistentPaths }}
- name: persistent-{{ $index }}
  mountPath: {{ $volume.path }}
{{- end }}
{{- end }}
{{- end -}}

{{- define "banksolution-common.volumes" -}}
{{- range $index, $path := .context.Values.writablePaths }}
- name: writable-{{ $index }}
  emptyDir: {}
{{- end }}
{{- if .includePersistent }}
{{- range $index, $volume := .context.Values.persistentPaths }}
- name: persistent-{{ $index }}
  persistentVolumeClaim:
    claimName: {{ include "banksolution-common.persistentVolumeClaimName" (dict "context" $.context "path" $volume.path) }}
{{- end }}
{{- end }}
{{- end -}}

{{- define "banksolution-common.waitForDatabaseInitContainer" -}}
{{- if and (include "banksolution-common.enabled" .Values.datasource) (include "banksolution-common.enabled" .Values.datasource.waitForDatabase) }}
- name: wait-for-database
  image: {{ .Values.datasource.waitForDatabase.image }}
  imagePullPolicy: IfNotPresent
  command:
    - sh
    - -c
    - until psql --no-psqlrc --quiet --command "select 1" >/dev/null 2>&1; do echo "waiting for database $PGDATABASE on $PGHOST"; sleep 2; done
  env:
    - name: PGHOST
      value: {{ .Values.datasource.host | quote }}
    - name: PGPORT
      value: {{ .Values.datasource.port | quote }}
    - name: PGDATABASE
      value: {{ .Values.datasource.database | quote }}
    - name: PGCONNECT_TIMEOUT
      value: "3"
    - name: PGUSER
      valueFrom:
        secretKeyRef:
          name: {{ .Values.datasource.credentialsSecret }}
          key: username
    - name: PGPASSWORD
      valueFrom:
        secretKeyRef:
          name: {{ .Values.datasource.credentialsSecret }}
          key: password
  resources:
    requests:
      cpu: 10m
      memory: 16Mi
    limits:
      memory: 64Mi
  securityContext:
    {{- toYaml .Values.securityContext | nindent 4 }}
{{- end }}
{{- end -}}
