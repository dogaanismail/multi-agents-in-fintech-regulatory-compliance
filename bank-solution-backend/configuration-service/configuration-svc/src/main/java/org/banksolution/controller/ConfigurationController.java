package org.banksolution.controller;

import org.banksolution.servicesecurity.RequiresPermission;
import org.banksolution.servicesecurity.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.enums.ConfigCategory;
import org.banksolution.model.request.CreateConfigRequest;
import org.banksolution.model.request.UpdateConfigRequest;
import org.banksolution.model.response.ConfigAuditLogResponse;
import org.banksolution.model.response.ConfigurationResponse;
import org.banksolution.service.ConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Configurations")
@RequestMapping("/api/v1/configurations")
@RequiredArgsConstructor
@Slf4j
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @Operation(summary = "Create a configuration")
    @PostMapping
    @RequiresPermission(Permissions.CONFIGURATION_WRITE)
    public ResponseEntity<@NonNull ConfigurationResponse> createConfiguration(@Valid @RequestBody CreateConfigRequest request) {
        log.info("POST /api/v1/configurations - Creating configuration with key: {}", request.getConfigKey());
        ConfigurationResponse response = configurationService.createConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List all configurations")
    @GetMapping
    @RequiresPermission(Permissions.CONFIGURATION_READ)
    public ResponseEntity<@NonNull List<ConfigurationResponse>> getAllConfigurations() {
        log.info("GET /api/v1/configurations - Fetching all configurations");
        return ResponseEntity.ok(configurationService.getAllConfigurations());
    }

    @Operation(summary = "Get a configuration by id")
    @GetMapping("/{id}")
    @RequiresPermission(Permissions.CONFIGURATION_READ)
    public ResponseEntity<@NonNull ConfigurationResponse> getConfigurationById(@PathVariable UUID id) {
        log.info("GET /api/v1/configurations/{} - Fetching configuration", id);
        return ResponseEntity.ok(configurationService.getConfigurationById(id));
    }

    @Operation(summary = "Get a configuration by key")
    @GetMapping("/key/{key}")
    @RequiresPermission(Permissions.CONFIGURATION_READ)
    public ResponseEntity<@NonNull ConfigurationResponse> getConfigurationByKey(@PathVariable String key) {
        log.info("GET /api/v1/configurations/key/{} - Fetching configuration", key);
        return ResponseEntity.ok(configurationService.getConfigurationByKey(key));
    }

    @Operation(summary = "List configurations by category")
    @GetMapping("/category/{category}")
    @RequiresPermission(Permissions.CONFIGURATION_READ)
    public ResponseEntity<@NonNull List<ConfigurationResponse>> getConfigurationsByCategory(@PathVariable ConfigCategory category) {
        log.info("GET /api/v1/configurations/category/{} - Fetching configurations", category);
        return ResponseEntity.ok(configurationService.getConfigurationsByCategory(category));
    }

    @Operation(summary = "Update a configuration")
    @PutMapping("/{id}")
    @RequiresPermission(Permissions.CONFIGURATION_WRITE)
    public ResponseEntity<@NonNull ConfigurationResponse> updateConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConfigRequest request) {
        log.info("PUT /api/v1/configurations/{} - Updating configuration", id);
        return ResponseEntity.ok(configurationService.updateConfiguration(id, request));
    }

    @Operation(summary = "Delete a configuration")
    @DeleteMapping("/{id}")
    @RequiresPermission(Permissions.CONFIGURATION_WRITE)
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID id) {
        log.info("DELETE /api/v1/configurations/{} - Deleting configuration", id);
        configurationService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the audit log for a key")
    @GetMapping("/key/{key}/audit-log")
    @RequiresPermission(Permissions.CONFIGURATION_READ)
    public ResponseEntity<@NonNull List<ConfigAuditLogResponse>> getAuditLogByKey(@PathVariable String key) {
        log.info("GET /api/v1/configurations/key/{}/audit-log - Fetching audit log", key);
        return ResponseEntity.ok(configurationService.getAuditLogByKey(key));
    }
}
