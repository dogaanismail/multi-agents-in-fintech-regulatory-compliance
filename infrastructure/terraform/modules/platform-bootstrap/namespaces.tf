resource "kubernetes_namespace_v1" "platform" {
  for_each = toset(local.argocd_apps.namespaces)

  metadata {
    name = each.key
  }
}
