package org.banksolution.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.CustomerEntity;
import org.banksolution.exception.CustomerAlreadyExistsException;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.mapper.CustomerMapper;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;
import org.banksolution.model.response.PageResponse;
import org.banksolution.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        if (customerRepository.existsCustomerEntityByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(request.getEmail());
        }

        CustomerEntity entity = CustomerMapper.toEntity(request);
        CustomerEntity savedEntity = customerRepository.save(entity);

        log.info("Customer created successfully with id: {}", savedEntity.getId());
        return CustomerMapper.toResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        log.info("Fetching customer with id: {}", id);

        CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        return CustomerMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        log.info("Fetching all customers with pagination");

        Page<@NonNull CustomerResponse> customerResponses = customerRepository.findAll(pageable)
                .map(CustomerMapper::toResponse);

        return PageResponse.<CustomerResponse>builder()
                .content(customerResponses.getContent())
                .page(PageResponse.PageMetadata.builder()
                        .size(customerResponses.getSize())
                        .number(customerResponses.getNumber())
                        .totalElements(customerResponses.getTotalElements())
                        .totalPages(customerResponses.getTotalPages())
                        .first(customerResponses.isFirst())
                        .last(customerResponses.isLast())
                        .empty(customerResponses.isEmpty())
                        .build())
                .build();
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerUpdateRequest request) {
        log.info("Updating customer with id: {}", id);

        CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        if (!entity.getEmail().equals(request.getEmail()) &&
                customerRepository.existsCustomerEntityByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(request.getEmail());
        }

        CustomerMapper.updateEntity(entity, request);
        CustomerEntity updatedEntity = customerRepository.save(entity);

        log.info("Customer updated successfully with id: {}", id);
        return CustomerMapper.toResponse(updatedEntity);
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        log.info("Soft deleting customer with id: {}", id);

        CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        entity.setDeletedAt(Instant.now());
        entity.setDeletedReason("Soft deleted by user");
        customerRepository.save(entity);

        log.info("Customer soft deleted successfully with id: {}", id);
    }

}


