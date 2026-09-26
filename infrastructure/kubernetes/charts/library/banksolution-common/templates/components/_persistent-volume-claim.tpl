{{- define "banksolution-common.persistentVolumeClaims" -}}
{{- range $volume := .Values.persistentPaths }}
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: {{ include "banksolution-common.persistentVolumeClaimName" (dict "context" $ "path" $volume.path) }}
  labels:
    {{- include "banksolution-common.labels" $ | nindent 4 }}
  annotations:
    helm.sh/resource-policy: keep
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: {{ $volume.size }}
{{- end }}
{{- end -}}
