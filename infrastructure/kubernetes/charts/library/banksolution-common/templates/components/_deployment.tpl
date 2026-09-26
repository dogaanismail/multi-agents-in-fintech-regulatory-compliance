{{- define "banksolution-common.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  {{- if .Values.persistentPaths }}
  strategy:
    type: Recreate
  {{- end }}
  selector:
    matchLabels:
      {{- include "banksolution-common.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "banksolution-common.labels" . | nindent 8 }}
      annotations:
        checksum/config: {{ include "banksolution-common.configMapData" . | sha256sum }}
    spec:
      enableServiceLinks: false
      terminationGracePeriodSeconds: {{ .Values.terminationGracePeriodSeconds }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      {{- $initContainers := include "banksolution-common.waitForDatabaseInitContainer" . | trim }}
      {{- if $initContainers }}
      initContainers:
        {{- $initContainers | nindent 8 }}
      {{- end }}
      containers:
        - name: {{ include "banksolution-common.name" . }}
          image: {{ include "banksolution-common.image" .Values.image }}
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - name: http
              containerPort: {{ .Values.containerPort }}
          envFrom:
            - configMapRef:
                name: {{ include "banksolution-common.name" . }}
          {{- $env := cat (include "banksolution-common.datasourceEnv" .) (include "banksolution-common.secretEnv" .) | trim }}
          {{- if or $env .Values.extraEnv }}
          env:
            {{- include "banksolution-common.datasourceEnv" . | trim | nindent 12 }}
            {{- include "banksolution-common.secretEnv" . | trim | nindent 12 }}
            {{- with .Values.extraEnv }}
            {{- toYaml . | nindent 12 }}
            {{- end }}
          {{- end }}
          startupProbe:
            {{- toYaml .Values.probes.startup | nindent 12 }}
          livenessProbe:
            {{- toYaml .Values.probes.liveness | nindent 12 }}
          readinessProbe:
            {{- toYaml .Values.probes.readiness | nindent 12 }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          securityContext:
            {{- toYaml .Values.securityContext | nindent 12 }}
          {{- $volumeMounts := include "banksolution-common.volumeMounts" (dict "context" . "includePersistent" true) | trim }}
          {{- if $volumeMounts }}
          volumeMounts:
            {{- $volumeMounts | nindent 12 }}
      volumes:
        {{- include "banksolution-common.volumes" (dict "context" . "includePersistent" true) | trim | nindent 8 }}
          {{- end }}
{{- end -}}
