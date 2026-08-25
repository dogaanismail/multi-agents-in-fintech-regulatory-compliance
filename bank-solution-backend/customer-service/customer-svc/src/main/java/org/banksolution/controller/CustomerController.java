package org.banksolution.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;
import org.banksolution.model.response.PageResponse;
import org.banksolution.service.CustomerService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<@NonNull CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerCreateRequest customerCreateRequest) {

        log.info("POST /api/v1/customers - Creating customer with email: {}", customerCreateRequest.getEmail());
        CustomerResponse customerResponse = customerService.createCustomer(customerCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull CustomerResponse> getCustomerById(@PathVariable UUID id) {
        log.info("GET /api/v1/customers/{} - Fetching customer", id);
        CustomerResponse customerResponse = customerService.getCustomerById(id);
        return ResponseEntity.ok(customerResponse);
    }

    @GetMapping
    public ResponseEntity<@NonNull PageResponse<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        log.info("GET /api/v1/customers - Fetching all customers with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<CustomerResponse> customerResponsePage = customerService.getAllCustomers(pageable);
        return ResponseEntity.ok(customerResponsePage);
    }

    @PutMapping("/{id}")
    public ResponseEntity<@NonNull CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerUpdateRequest customerUpdateRequest
    ) {
        log.info("PUT /api/v1/customers/{} - Updating customer", id);
        CustomerResponse customerResponse = customerService.updateCustomer(id, customerUpdateRequest);
        return ResponseEntity.ok(customerResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull Void> deleteCustomer(@PathVariable UUID id) {
        log.info("DELETE /api/v1/customers/{} - Soft deleting customer", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
