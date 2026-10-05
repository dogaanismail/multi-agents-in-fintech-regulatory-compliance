variable "kubernetes_directory" {
  description = "Path to infrastructure/kubernetes, the same tree ArgoCD syncs from."
  type        = string
}

variable "environment" {
  description = "Directory under infrastructure/kubernetes/environments whose values ArgoCD applies."
  type        = string
}

variable "repository_url" {
  description = "Git repository ArgoCD syncs from."
  type        = string
}

variable "release" {
  description = "Git tag ArgoCD syncs, for example v1.0.0."
  type        = string
}

variable "image_registry" {
  description = "Registry and namespace the service images are pulled from."
  type        = string
}

variable "image_tag" {
  description = "Image tag every service runs; published by the Publish Images workflow for the release."
  type        = string
}
