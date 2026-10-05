variable "name" {
  description = "Name shared by the resource group and the AKS cluster."
  type        = string
}

variable "location" {
  description = "Azure region."
  type        = string
}

variable "kubernetes_version" {
  description = "AKS Kubernetes version (major.minor); AKS picks the latest supported patch."
  type        = string
}

variable "node_vm_size" {
  description = "VM size of the worker nodes."
  type        = string
}

variable "node_count" {
  description = "Number of worker nodes."
  type        = number
}

variable "node_disk_size_gb" {
  description = "OS disk size of each worker node, which holds the container images."
  type        = number
}

variable "api_allowed_cidrs" {
  description = "CIDR blocks allowed to reach the Kubernetes API. Empty leaves the API open to any address."
  type = list(string)
}

variable "tags" {
  description = "Tags applied to the resource group and the cluster."
  type = map(string)
}
