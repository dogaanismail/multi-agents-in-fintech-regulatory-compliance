output "cluster_name" {
  description = "Name of the AKS cluster."
  value       = azurerm_kubernetes_cluster.cluster.name
}

output "resource_group_name" {
  description = "Resource group holding the cluster."
  value       = azurerm_resource_group.cluster.name
}

output "kubernetes_api" {
  description = "Endpoint and client certificate of the Kubernetes API."
  sensitive   = true
  value = {
    host = azurerm_kubernetes_cluster.cluster.kube_config[0].host
    client_certificate = base64decode(azurerm_kubernetes_cluster.cluster.kube_config[0].client_certificate)
    client_key = base64decode(azurerm_kubernetes_cluster.cluster.kube_config[0].client_key)
    cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.cluster.kube_config[0].cluster_ca_certificate)
  }
}
