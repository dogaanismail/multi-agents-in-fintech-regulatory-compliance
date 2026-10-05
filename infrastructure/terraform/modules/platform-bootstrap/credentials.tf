resource "random_password" "database" {
  for_each = local.databases

  length  = 32
  special = false
}

resource "kubernetes_secret_v1" "database_credentials" {
  for_each = local.databases

  metadata {
    name      = "${each.key}-db-credentials"
    namespace = kubernetes_namespace_v1.platform[each.value.namespace].metadata[0].name
    labels = {
      "cnpg.io/reload" = "true"
    }
  }

  type = "kubernetes.io/basic-auth"

  data = {
    username = each.value.role
    password = random_password.database[each.key].result
  }
}

resource "random_password" "kafka" {
  for_each = local.kafka_users

  length  = 32
  special = false
}

resource "kubernetes_secret_v1" "kafka_client_credentials" {
  for_each = local.kafka_users

  metadata {
    name      = "${each.key}-kafka-credentials"
    namespace = kubernetes_namespace_v1.platform[each.value].metadata[0].name
  }

  type = "kubernetes.io/basic-auth"

  data = {
    username           = each.key
    password           = random_password.kafka[each.key].result
    "sasl.jaas.config" = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${each.key}\" password=\"${random_password.kafka[each.key].result}\";"
  }
}

resource "kubernetes_secret_v1" "kafka_user_password" {
  for_each = local.kafka_users

  metadata {
    name      = "${each.key}-kafka-password"
    namespace = kubernetes_namespace_v1.platform[local.kafka_namespace].metadata[0].name
  }

  data = {
    password = random_password.kafka[each.key].result
  }
}

resource "random_password" "neo4j" {
  length  = 32
  special = false
}

resource "kubernetes_secret_v1" "neo4j_credentials" {
  metadata {
    name      = local.neo4j_credentials_secret
    namespace = kubernetes_namespace_v1.platform[local.neo4j_store.namespace].metadata[0].name
  }

  data = {
    NEO4J_AUTH = "neo4j/${random_password.neo4j.result}"
    username   = "neo4j"
    password   = random_password.neo4j.result
  }
}
