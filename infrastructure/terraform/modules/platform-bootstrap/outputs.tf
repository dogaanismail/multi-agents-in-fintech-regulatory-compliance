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
    value = {for service, database in local.databases : service => database.role}
}

output "keycloak_admin_password" {
    description = "Password of the Keycloak admin console user (admin)."
    value       = random_password.keycloak_admin.result
    sensitive   = true
}

output "demo_user_password" {
    description = "Password of the demo staff users (viewer, operator, officer, admin, superadmin)."
    value       = random_password.keycloak_demo_users.result
    sensitive   = true
}

output "demo_customer_password" {
    description = "Password of the demo mobile customer (customer@bank-solution.local)."
    value       = random_password.keycloak_demo_customer.result
    sensitive   = true
}
