output "configure_kubectl" {
  description = "Adds the cluster to your kubeconfig."
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${module.cluster.cluster_name}"
}

output "argocd_admin_password" {
  description = "Prints the initial ArgoCD admin password."
  value       = "kubectl -n ${module.platform.argocd_namespace} get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 --decode"
}

output "leftover_volumes" {
  description = "Lists EBS volumes this environment created that outlived the cluster."
  value       = "aws ec2 describe-volumes --region ${var.region} --filters Name=tag:bank-solution/cluster,Values=${module.cluster.cluster_name} --query 'Volumes[].VolumeId'"
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
