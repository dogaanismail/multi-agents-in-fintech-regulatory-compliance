provider "azurerm" {
  subscription_id = var.subscription_id

  features {}
}

provider "kubernetes" {
  host                   = module.cluster.kubernetes_api.host
  client_certificate     = module.cluster.kubernetes_api.client_certificate
  client_key             = module.cluster.kubernetes_api.client_key
  cluster_ca_certificate = module.cluster.kubernetes_api.cluster_ca_certificate
}

provider "helm" {
  kubernetes = {
    host                   = module.cluster.kubernetes_api.host
    client_certificate     = module.cluster.kubernetes_api.client_certificate
    client_key             = module.cluster.kubernetes_api.client_key
    cluster_ca_certificate = module.cluster.kubernetes_api.cluster_ca_certificate
  }
}
