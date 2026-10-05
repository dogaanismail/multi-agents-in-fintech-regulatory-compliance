module "cluster" {
  source = "../../modules/aws-cluster"

  name                    = var.name
  kubernetes_version      = var.kubernetes_version
  vpc_cidr                = "10.0.0.0/16"
  availability_zone_count = 2
  node_instance_type      = var.node_instance_type
  node_count              = var.node_count
  node_disk_size_gb       = 50
  api_allowed_cidrs       = var.api_allowed_cidrs
}

module "platform" {
  source = "../../modules/platform-bootstrap"

  kubernetes_directory = "${path.root}/../../../kubernetes"
  environment          = "aws-demo"
  repository_url       = var.repository_url
  release              = var.release
  image_registry       = var.image_registry
  image_tag = trimprefix(var.release, "v")

  depends_on = [module.cluster]
}
