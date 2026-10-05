resource "helm_release" "argocd" {
  name             = "argocd"
  namespace        = "argocd"
  create_namespace = true
  repository       = "https://argoproj.github.io/argo-helm"
  chart            = "argo-cd"
  version          = "10.9.2"
  values = [file("${var.kubernetes_directory}/platform/argocd/values.yaml")]
}

resource "helm_release" "bank_solution_apps" {
  name      = "bank-solution-apps"
  namespace = helm_release.argocd.namespace
  chart     = "${var.kubernetes_directory}/charts/gitops/bank-solution-apps"

  set = [
    { name = "repoURL", value = var.repository_url },
    { name = "targetRevision", value = var.release },
    { name = "environment", value = var.environment },
    { name = "images.registry", value = var.image_registry },
    { name = "images.tag", value = var.image_tag },
  ]

  depends_on = [
    helm_release.strimzi,
    helm_release.cloudnative_pg,
    kubernetes_secret_v1.database_credentials,
    kubernetes_secret_v1.kafka_client_credentials,
    kubernetes_secret_v1.kafka_user_password,
    kubernetes_secret_v1.neo4j_credentials,
  ]
}
