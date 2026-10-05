{{- define "banksolution-common.networkPolicyPeerNames" -}}
{{- $names := concat (.Values.networkPolicy.baseline | default list) (.Values.networkPolicy.egress | default list) -}}
{{- if include "banksolution-common.enabled" .Values.datasource -}}
{{- $names = append $names .Values.datasource.networkPeer -}}
{{- end -}}
{{- if include "banksolution-common.enabled" .Values.messaging -}}
{{- $names = concat $names .Values.messaging.networkPeers -}}
{{- end -}}
{{- $names | uniq | toJson -}}
{{- end -}}

{{- define "banksolution-common.networkPolicyRule" -}}
{{- $peers := .context.Values.networkPeers | default dict -}}
{{- $peer := get $peers .name -}}
{{- if $peer }}
- to:
    {{- if $peer.ipBlock }}
    - ipBlock:
        {{- toYaml $peer.ipBlock | nindent 8 }}
    {{- else }}
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: {{ $peer.namespace }}
      podSelector:
        matchLabels:
          {{- toYaml $peer.podLabels | nindent 10 }}
    {{- end }}
  {{- with $peer.ports }}
  ports:
    {{- toYaml . | nindent 4 }}
  {{- end }}
{{- else }}
{{- $parts := splitList "/" .name -}}
{{- $namespace := ternary (first $parts) .context.Release.Namespace (eq (len $parts) 2) -}}
- to:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: {{ $namespace }}
      podSelector:
        matchLabels:
          app.kubernetes.io/name: {{ last $parts }}
{{- end }}
{{- end -}}

{{- define "banksolution-common.networkPolicyIngressRule" -}}
{{- $parts := splitList "/" .name -}}
{{- $namespace := ternary (first $parts) .context.Release.Namespace (eq (len $parts) 2) -}}
- from:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: {{ $namespace }}
      podSelector:
        matchLabels:
          app.kubernetes.io/name: {{ last $parts }}
  ports:
    - protocol: TCP
      port: {{ .context.Values.containerPort }}
{{- end -}}

{{- define "banksolution-common.networkPolicy" -}}
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: {{ include "banksolution-common.name" . }}
  labels:
    {{- include "banksolution-common.labels" . | nindent 4 }}
spec:
  podSelector:
    matchLabels:
      {{- include "banksolution-common.selectorLabels" . | nindent 6 }}
  policyTypes:
    - Egress
    {{- if .Values.networkPolicy.ingress }}
    - Ingress
    {{- end }}
  {{- with .Values.networkPolicy.ingress }}
  ingress:
    {{- range $name := . }}
    {{- include "banksolution-common.networkPolicyIngressRule" (dict "context" $ "name" $name) | nindent 4 }}
    {{- end }}
  {{- end }}
  egress:
    {{- range $name := include "banksolution-common.networkPolicyPeerNames" . | fromJsonArray }}
    {{- include "banksolution-common.networkPolicyRule" (dict "context" $ "name" $name) | nindent 4 }}
    {{- end }}
{{- end -}}
