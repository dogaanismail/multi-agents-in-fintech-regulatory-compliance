provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "bank-solution"
      Environment = var.name
      ManagedBy   = "terraform"
    }
  }
}

locals {
  kubernetes_api_token_command = {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = ["eks", "get-token", "--cluster-name", module.cluster.cluster_name, "--region", var.region]
  }
}

provider "kubernetes" {
  host = module.cluster.cluster_endpoint
  cluster_ca_certificate = base64decode(module.cluster.cluster_certificate_authority_data)

  exec {
    api_version = local.kubernetes_api_token_command.api_version
    command     = local.kubernetes_api_token_command.command
    args        = local.kubernetes_api_token_command.args
  }
}

provider "helm" {
  kubernetes = {
    host = module.cluster.cluster_endpoint
    cluster_ca_certificate = base64decode(module.cluster.cluster_certificate_authority_data)
    exec = local.kubernetes_api_token_command
  }
}
