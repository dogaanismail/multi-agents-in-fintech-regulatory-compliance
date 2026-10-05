variable "name" {
  description = "Name shared by the VPC, the EKS cluster and everything tagged for it."
  type        = string
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version. Keep it inside EKS standard support to avoid extended-support charges."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the cluster VPC."
  type        = string
}

variable "availability_zone_count" {
  description = "Number of availability zones the nodes are spread across. EKS needs at least two."
  type        = number
}

variable "node_instance_type" {
  description = "EC2 instance type of the worker nodes."
  type        = string
}

variable "node_count" {
  description = "Number of worker nodes."
  type        = number
}

variable "node_disk_size_gb" {
  description = "Root volume size of each worker node, which holds the container images."
  type        = number
}

variable "api_allowed_cidrs" {
  description = "CIDR blocks allowed to reach the public Kubernetes API endpoint."
  type = list(string)
}
