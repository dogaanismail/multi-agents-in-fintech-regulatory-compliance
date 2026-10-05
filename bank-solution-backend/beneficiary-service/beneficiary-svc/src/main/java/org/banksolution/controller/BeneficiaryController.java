package org.banksolution.controller;

import org.banksolution.servicesecurity.RequiresPermission;
import org.banksolution.servicesecurity.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.model.request.BeneficiaryCoordinateRequest;
import org.banksolution.model.request.BeneficiaryCreateRequest;
import org.banksolution.model.request.BeneficiaryUpdateRequest;
import org.banksolution.model.response.BeneficiaryResponse;
import org.banksolution.service.BeneficiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Beneficiaries")
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Validated
@Slf4j
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @Operation(summary = "Create a beneficiary")
    @PostMapping
    @RequiresPermission(Permissions.BENEFICIARY_WRITE)
    public ResponseEntity<@NonNull BeneficiaryResponse> createBeneficiary(
            @Valid @RequestBody BeneficiaryCreateRequest beneficiaryCreateRequest) {

        log.info("POST /api/v1/beneficiaries - Creating beneficiary for customer: {}", beneficiaryCreateRequest.getCustomerId());
        BeneficiaryResponse beneficiaryResponse = beneficiaryService.createBeneficiary(beneficiaryCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(beneficiaryResponse);
    }

    @Operation(summary = "Get a beneficiary by id")
    @GetMapping("/{beneficiaryId}")
    @RequiresPermission(Permissions.CUSTOMER_READ)
    public ResponseEntity<@NonNull BeneficiaryResponse> getBeneficiaryById(@PathVariable UUID beneficiaryId) {
        log.info("GET /api/v1/beneficiaries/{} - Fetching beneficiary", beneficiaryId);
        return ResponseEntity.ok(beneficiaryService.getBeneficiaryById(beneficiaryId));
    }

    @Operation(summary = "List a customer's beneficiaries")
    @GetMapping
    @RequiresPermission(Permissions.CUSTOMER_READ)
    public ResponseEntity<@NonNull List<BeneficiaryResponse>> getBeneficiariesByCustomerId(@RequestParam UUID customerId) {
        log.info("GET /api/v1/beneficiaries?customerId={} - Fetching beneficiaries", customerId);
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByCustomerId(customerId));
    }

    @Operation(summary = "Update a beneficiary")
    @PutMapping("/{beneficiaryId}")
    @RequiresPermission(Permissions.BENEFICIARY_WRITE)
    public ResponseEntity<@NonNull BeneficiaryResponse> updateBeneficiary(
            @PathVariable UUID beneficiaryId,
            @Valid @RequestBody BeneficiaryUpdateRequest beneficiaryUpdateRequest) {

        log.info("PUT /api/v1/beneficiaries/{} - Updating beneficiary", beneficiaryId);
        return ResponseEntity.ok(beneficiaryService.updateBeneficiary(beneficiaryId, beneficiaryUpdateRequest));
    }

    @Operation(summary = "Soft delete a beneficiary")
    @DeleteMapping("/{beneficiaryId}")
    @RequiresPermission(Permissions.BENEFICIARY_WRITE)
    public ResponseEntity<@NonNull Void> deleteBeneficiary(
            @PathVariable UUID beneficiaryId,
            @RequestParam(defaultValue = BeneficiaryService.DEFAULT_DELETED_REASON)
            @Size(max = 500, message = "Reason must not exceed 500 characters.") String reason) {

        log.info("DELETE /api/v1/beneficiaries/{} - Soft deleting beneficiary", beneficiaryId);
        beneficiaryService.deleteBeneficiary(beneficiaryId, reason);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add a coordinate to a beneficiary")
    @PostMapping("/{beneficiaryId}/coordinates")
    @RequiresPermission(Permissions.BENEFICIARY_WRITE)
    public ResponseEntity<@NonNull BeneficiaryResponse> addBeneficiaryCoordinate(
            @PathVariable UUID beneficiaryId,
            @Valid @RequestBody BeneficiaryCoordinateRequest beneficiaryCoordinateRequest) {

        log.info("POST /api/v1/beneficiaries/{}/coordinates - Adding coordinate", beneficiaryId);
        BeneficiaryResponse beneficiaryResponse = beneficiaryService.addBeneficiaryCoordinate(beneficiaryId, beneficiaryCoordinateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(beneficiaryResponse);
    }

    @Operation(summary = "Soft delete a beneficiary coordinate")
    @DeleteMapping("/{beneficiaryId}/coordinates/{beneficiaryCoordinateId}")
    @RequiresPermission(Permissions.BENEFICIARY_WRITE)
    public ResponseEntity<@NonNull Void> deleteBeneficiaryCoordinate(
            @PathVariable UUID beneficiaryId,
            @PathVariable UUID beneficiaryCoordinateId,
            @RequestParam(defaultValue = BeneficiaryService.DEFAULT_DELETED_REASON)
            @Size(max = 500, message = "Reason must not exceed 500 characters.") String reason) {

        log.info("DELETE /api/v1/beneficiaries/{}/coordinates/{} - Soft deleting coordinate", beneficiaryId, beneficiaryCoordinateId);
        beneficiaryService.deleteBeneficiaryCoordinate(beneficiaryId, beneficiaryCoordinateId, reason);
        return ResponseEntity.noContent().build();
    }
}
