variable "release" {
  description = "Release to deploy: the git tag ArgoCD syncs and whose images the Publish Images workflow pushed, for example v1.0.0."
  type        = string

  validation {
    condition = can(regex("^v[0-9]+\\.[0-9]+\\.[0-9]+$", var.release))
    error_message = "release must be a version tag such as v1.0.0."
  }
}

variable "subscription_id" {
  description = "Azure subscription to deploy into."
  type        = string
}

variable "location" {
  description = "Azure region to deploy into."
  type        = string
  default     = "westeurope"
}

variable "name" {
  description = "Name of the demo environment; used for the resource group, the cluster and resource tags."
  type        = string
  default     = "bank-solution-demo"
}

variable "kubernetes_version" {
  description = "AKS Kubernetes version (major.minor)."
  type        = string
  default     = "1.36"
}

variable "node_vm_size" {
  description = "VM size of the worker nodes."
  type        = string
  default     = "Standard_D4s_v5"
}

variable "node_count" {
  description = "Number of worker nodes."
  type        = number
  default     = 3
}

variable "api_allowed_cidrs" {
  description = "CIDR blocks allowed to reach the Kubernetes API, for example your office IP as x.x.x.x/32. Empty leaves it open."
  type = list(string)
  default = []
}

variable "repository_url" {
  description = "Git repository ArgoCD syncs from."
  type        = string
  default     = "https://github.com/dogaanismail/multi-agents-in-fintech-regulatory-compliance.git"
}

variable "image_registry" {
  description = "Registry and namespace the Publish Images workflow pushes to."
  type        = string
  default     = "ghcr.io/dogaanismail/bank-solution"
}
