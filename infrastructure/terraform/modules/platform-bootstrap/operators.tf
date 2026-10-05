resource "helm_release" "priority_classes" {
  name      = "priority-classes"
  namespace = kubernetes_namespace_v1.platform["platform"].metadata[0].name
  chart     = "${var.kubernetes_directory}/charts/platform/priority-classes"
}

resource "helm_release" "strimzi" {
  name       = "strimzi-operator"
  namespace  = kubernetes_namespace_v1.platform["platform"].metadata[0].name
  repository = "oci://quay.io/strimzi-helm"
  chart      = "strimzi-kafka-operator"
  version    = "1.2.0"
    values = [file("${var.kubernetes_directory}/platform/strimzi/values.yaml")]

  depends_on = [helm_release.priority_classes]
}

resource "helm_release" "cloudnative_pg" {
  name             = "cloudnative-pg"
  namespace        = "cnpg-system"
  create_namespace = true
  repository       = "https://cloudnative-pg.github.io/charts"
  chart            = "cloudnative-pg"
  version          = "0.29.1"
    values = [file("${var.kubernetes_directory}/platform/cloudnative-pg/values.yaml")]
}
