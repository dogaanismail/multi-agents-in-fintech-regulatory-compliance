{{- define "keycloak.clientSecretEnvName" -}}
{{- printf "%s_CLIENT_SECRET" (. | upper | replace "-" "_") -}}
{{- end -}}

{{- define "keycloak.clientSecretName" -}}
{{- printf "%s-keycloak-client" . -}}
{{- end -}}

{{- define "keycloak.realmServiceClients" -}}
{{- $realm := .Files.Get "realms/bank-internal-realm.json" | fromJson -}}
{{- $serviceClients := list -}}
{{- range $realm.clients }}
{{- if .serviceAccountsEnabled }}
{{- $serviceClients = append $serviceClients .clientId -}}
{{- end }}
{{- end }}
{{- $serviceClients | toJson -}}
{{- end -}}
