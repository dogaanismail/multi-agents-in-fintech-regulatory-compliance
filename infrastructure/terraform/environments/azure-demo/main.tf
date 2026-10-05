module "cluster" {
  source = "../../modules/azure-cluster"

  name               = var.name
  location           = var.location
  kubernetes_version = var.kubernetes_version
  node_vm_size       = var.node_vm_size
  node_count         = var.node_count
  node_disk_size_gb  = 64
  api_allowed_cidrs  = var.api_allowed_cidrs

  tags = {
    Project     = "bank-solution"
    Environment = var.name
    ManagedBy   = "terraform"
  }
}

module "platform" {
  source = "../../modules/platform-bootstrap"

  kubernetes_directory = "${path.root}/../../../kubernetes"
  environment          = "cloud-demo"
  repository_url       = var.repository_url
  release              = var.release
  image_registry       = var.image_registry
  image_tag = trimprefix(var.release, "v")

  depends_on = [module.cluster]
}
