{{- define "banksolution-common.migrationJob" -}}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "banksolution-common.name" . }}-db-migration
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
    app.kubernetes.io/component: db-migration
  annotations:
    helm.sh/hook: pre-install,pre-upgrade
    helm.sh/hook-weight: "0"
    helm.sh/hook-delete-policy: before-hook-creation,hook-succeeded
spec:
  backoffLimit: {{ .Values.migration.backoffLimit }}
  activeDeadlineSeconds: {{ .Values.migration.activeDeadlineSeconds }}
  template:
    metadata:
      labels:
        {{- include "banksolution-common.selectorLabels" . | nindent 8 }}
        app.kubernetes.io/component: db-migration
    spec:
      restartPolicy: Never
      enableServiceLinks: false
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
        - name: db-migration
          image: {{ include "banksolution-common.image" .Values.migration.image }}
          imagePullPolicy: {{ .Values.migration.image.pullPolicy }}
          env:
            - name: SPRING_LIQUIBASE_ENABLED
              value: "true"
            {{- include "banksolution-common.datasourceEnv" . | trim | nindent 12 }}
          resources:
            {{- toYaml .Values.migration.resources | nindent 12 }}
          securityContext:
            {{- toYaml .Values.securityContext | nindent 12 }}
{{- end -}}
