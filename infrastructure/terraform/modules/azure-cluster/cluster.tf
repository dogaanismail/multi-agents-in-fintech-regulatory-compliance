resource "azurerm_resource_group" "cluster" {
  name     = var.name
  location = var.location
  tags     = var.tags
}

resource "azurerm_kubernetes_cluster" "cluster" {
  name                = var.name
  location            = azurerm_resource_group.cluster.location
  resource_group_name = azurerm_resource_group.cluster.name
  dns_prefix          = var.name
  kubernetes_version  = var.kubernetes_version
  sku_tier            = "Free"
  tags                = var.tags

  default_node_pool {
    name                        = "workers"
    vm_size                     = var.node_vm_size
    node_count                  = var.node_count
    os_disk_size_gb             = var.node_disk_size_gb
    os_sku                      = "Ubuntu"
    temporary_name_for_rotation = "rotation"
  }

  node_provisioning_profile {
    mode = "Manual"
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
    network_data_plane  = "cilium"
    network_policy      = "cilium"
    pod_cidr            = "10.244.0.0/16"
    service_cidr        = "10.0.0.0/16"
    dns_service_ip      = "10.0.0.10"
  }

  dynamic "api_server_access_profile" {
    for_each = length(var.api_allowed_cidrs) > 0 ? [var.api_allowed_cidrs] : []

    content {
      authorized_ip_ranges = api_server_access_profile.value
    }
  }
}
