package org.banksolution.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.enums.ConfigCategory;
import org.banksolution.model.request.CreateConfigRequest;
import org.banksolution.model.request.UpdateConfigRequest;
import org.banksolution.model.response.ConfigurationResponse;
import org.banksolution.service.ConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/configurations")
@RequiredArgsConstructor
@Slf4j
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @PostMapping
    public ResponseEntity<@NonNull ConfigurationResponse> createConfiguration(@Valid @RequestBody CreateConfigRequest request) {
        log.info("POST /api/v1/configurations - Creating configuration with key: {}", request.getConfigKey());
        ConfigurationResponse response = configurationService.createConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<@NonNull List<ConfigurationResponse>> getAllConfigurations() {
        log.info("GET /api/v1/configurations - Fetching all configurations");
        return ResponseEntity.ok(configurationService.getAllConfigurations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull ConfigurationResponse> getConfigurationById(@PathVariable UUID id) {
        log.info("GET /api/v1/configurations/{} - Fetching configuration", id);
        return ResponseEntity.ok(configurationService.getConfigurationById(id));
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<@NonNull ConfigurationResponse> getConfigurationByKey(@PathVariable String key) {
        log.info("GET /api/v1/configurations/key/{} - Fetching configuration", key);
        return ResponseEntity.ok(configurationService.getConfigurationByKey(key));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<@NonNull List<ConfigurationResponse>> getConfigurationsByCategory(@PathVariable ConfigCategory category) {
        log.info("GET /api/v1/configurations/category/{} - Fetching configurations", category);
        return ResponseEntity.ok(configurationService.getConfigurationsByCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<@NonNull ConfigurationResponse> updateConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConfigRequest request) {
        log.info("PUT /api/v1/configurations/{} - Updating configuration", id);
        return ResponseEntity.ok(configurationService.updateConfiguration(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID id) {
        log.info("DELETE /api/v1/configurations/{} - Deleting configuration", id);
        configurationService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
