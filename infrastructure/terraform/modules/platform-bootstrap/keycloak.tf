locals {
  keycloak_chart_values = yamldecode(file("${var.kubernetes_directory}/charts/platform/keycloak/values.yaml"))

  keycloak_store = one([
    for store in local.argocd_apps.stores : store
    if store.chart == "charts/platform/keycloak"
  ])

  keycloak_service_clients = local.keycloak_chart_values.serviceClients
  keycloak_backoffice      = local.keycloak_chart_values.backofficeClient
}

resource "random_password" "keycloak_admin" {
  length  = 32
  special = false
}

resource "random_password" "keycloak_demo_users" {
  length  = 20
  special = false
}

resource "random_password" "keycloak_demo_customer" {
  length      = 20
  special     = false
  min_upper   = 1
  min_lower   = 1
  min_numeric = 1
}

resource "random_password" "keycloak_backoffice_client" {
  length  = 40
  special = false
}

resource "random_password" "keycloak_service_client" {
  for_each = local.keycloak_service_clients

  length  = 40
  special = false
}

resource "kubernetes_secret_v1" "keycloak_admin" {
  metadata {
    name      = "keycloak-admin"
    namespace = kubernetes_namespace_v1.platform[local.keycloak_store.namespace].metadata[0].name
  }

  type = "kubernetes.io/basic-auth"

  data = {
    username = "admin"
    password = random_password.keycloak_admin.result
  }
}

resource "kubernetes_secret_v1" "keycloak_realm_secrets" {
  metadata {
    name      = "keycloak-realm-secrets"
    namespace = kubernetes_namespace_v1.platform[local.keycloak_store.namespace].metadata[0].name
  }

  data = merge(
    {
      BACKOFFICE_CLIENT_SECRET        = random_password.keycloak_backoffice_client.result
      KEYCLOAK_DEMO_USER_PASSWORD     = random_password.keycloak_demo_users.result
      KEYCLOAK_DEMO_CUSTOMER_PASSWORD = random_password.keycloak_demo_customer.result
    },
    {
      for service, _ in local.keycloak_service_clients :
      "${upper(replace(service, "-", "_"))}_CLIENT_SECRET" => random_password.keycloak_service_client[service].result
    }
  )
}

resource "kubernetes_secret_v1" "backoffice_keycloak_client" {
  metadata {
    name      = "${local.keycloak_backoffice.service}-keycloak-client"
    namespace = kubernetes_namespace_v1.platform[local.keycloak_backoffice.namespace].metadata[0].name
  }

  data = {
    client-secret = random_password.keycloak_backoffice_client.result
  }
}

resource "kubernetes_secret_v1" "service_keycloak_client" {
  for_each = local.keycloak_service_clients

  metadata {
    name      = "${each.key}-keycloak-client"
    namespace = kubernetes_namespace_v1.platform[each.value].metadata[0].name
  }

  data = {
    client-secret = random_password.keycloak_service_client[each.key].result
  }
}
