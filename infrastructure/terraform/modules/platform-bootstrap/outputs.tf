output "argocd_namespace" {
  description = "Namespace ArgoCD runs in."
  value       = helm_release.argocd.namespace
}

output "kafka_users" {
  description = "Services that received Kafka credentials, by namespace."
  value       = local.kafka_users
}

output "database_roles" {
  description = "PostgreSQL roles that received credentials, by service."
  value       = {for service, database in local.databases : service => database.role}
}
