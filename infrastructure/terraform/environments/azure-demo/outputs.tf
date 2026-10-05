output "configure_kubectl" {
  description = "Adds the cluster to your kubeconfig."
  value       = "az aks get-credentials --resource-group ${module.cluster.resource_group_name} --name ${module.cluster.cluster_name}"
}

output "argocd_admin_password" {
  description = "Prints the initial ArgoCD admin password."
  value       = "kubectl -n ${module.platform.argocd_namespace} get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 --decode"
}

output "kafka_users" {
  description = "Services that received generated Kafka credentials."
  value       = module.platform.kafka_users
}

output "database_roles" {
  description = "PostgreSQL roles that received generated credentials."
  value       = module.platform.database_roles
}

output "keycloak_admin_password" {
  description = "Password of the Keycloak admin console user (admin); print with terraform output -raw."
  value       = module.platform.keycloak_admin_password
  sensitive   = true
}

output "demo_user_password" {
  description = "Password of the demo staff users; print with terraform output -raw."
  value       = module.platform.demo_user_password
  sensitive   = true
}
