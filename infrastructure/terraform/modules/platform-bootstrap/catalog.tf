locals {
  argocd_apps = yamldecode(file("${var.kubernetes_directory}/charts/gitops/bank-solution-apps/values.yaml"))

  services = {
    for path in fileset(var.kubernetes_directory, "services/*/*.yaml") :
    trimsuffix(basename(path), ".yaml") => {
      namespace = basename(dirname(path))
      values = yamldecode(file("${var.kubernetes_directory}/${path}"))
    }
  }

  kafka_users = {
    for name, service in local.services : name => service.namespace
    if try(service.values.messaging.enabled, false)
  }

  postgres_stores = [
    for store in local.argocd_apps.stores : store
    if store.chart == "charts/data/postgres"
  ]

  databases = merge([
    for store in local.postgres_stores : {
      for database in yamldecode(file("${var.kubernetes_directory}/data/${store.namespace}/${store.name}.yaml")).databases :
      database.service => {
        namespace = store.namespace
        role      = database.role
      }
    }
  ]...)

  neo4j_store = one([
    for store in local.argocd_apps.stores : store
    if store.chart == "charts/data/neo4j"
  ])

  neo4j_credentials_secret = yamldecode(file("${var.kubernetes_directory}/charts/data/neo4j/values.yaml")).credentialsSecret

  kafka_namespace = yamldecode(file("${var.kubernetes_directory}/charts/service-types/java-microservice/values.yaml")).messaging.authentication.kafkaNamespace
}
