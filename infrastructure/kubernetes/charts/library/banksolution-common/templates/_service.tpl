{{- define "banksolution-common.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{ include "banksolution-common.labels" . | nindent 4 }}
spec:
  type: {{ .Values.service.type }}
  selector:
    {{ include "banksolution-common.selectorLabels" . | nindent 4 }}
  ports:
    - name: http
      port: {{ .Values.service.port | default .Values.containerPort }}
      targetPort: http
{{- end -}}
