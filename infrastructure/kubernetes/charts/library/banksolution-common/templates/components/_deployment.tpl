{{- define "banksolution-common.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
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
          {{- with .Values.writablePaths }}
          volumeMounts:
            {{- range $index, $path := . }}
            - name: writable-{{ $index }}
              mountPath: {{ $path }}
            {{- end }}
          {{- end }}
      {{- with .Values.writablePaths }}
      volumes:
        {{- range $index, $path := . }}
        - name: writable-{{ $index }}
          emptyDir: {}
        {{- end }}
      {{- end }}
{{- end -}}
