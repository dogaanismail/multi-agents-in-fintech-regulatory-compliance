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

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private static final String SOFT_DELETED_BY_USER_REASON = "Soft deleted by user";

    private final CustomerRepository customerRepository;
    private final Clock clock;

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest customerCreateRequest) {
        log.info("Creating customer with email: {}", customerCreateRequest.getEmail());

        if (customerRepository.existsCustomerEntityByEmail(customerCreateRequest.getEmail())) {
            throw new CustomerAlreadyExistsException(customerCreateRequest.getEmail());
        }

        CustomerEntity customerEntity = CustomerMapper.toCustomerEntity(customerCreateRequest);
        CustomerEntity savedCustomerEntity = customerRepository.save(customerEntity);

        log.info("Customer created successfully with id: {}", savedCustomerEntity.getId());
        return CustomerMapper.toCustomerResponse(savedCustomerEntity);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID customerId) {
        log.info("Fetching customer with id: {}", customerId);

        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        return CustomerMapper.toCustomerResponse(customerEntity);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        log.info("Fetching all customers with pagination");

        Page<@NonNull CustomerResponse> customerResponses = customerRepository.findAll(pageable)
                .map(CustomerMapper::toCustomerResponse);

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
    public CustomerResponse updateCustomer(UUID customerId, CustomerUpdateRequest customerUpdateRequest) {
        log.info("Updating customer with id: {}", customerId);

        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (!customerEntity.getEmail().equals(customerUpdateRequest.getEmail()) &&
                customerRepository.existsCustomerEntityByEmail(customerUpdateRequest.getEmail())) {
            throw new CustomerAlreadyExistsException(customerUpdateRequest.getEmail());
        }

        CustomerMapper.updateCustomerEntity(customerEntity, customerUpdateRequest);
        CustomerEntity updatedCustomerEntity = customerRepository.save(customerEntity);

        log.info("Customer updated successfully with id: {}", customerId);
        return CustomerMapper.toCustomerResponse(updatedCustomerEntity);
    }

    @Transactional
    public void deleteCustomer(UUID customerId) {
        log.info("Soft deleting customer with id: {}", customerId);

        CustomerEntity customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customerEntity.setDeletedAt(clock.instant());
        customerEntity.setDeletedReason(SOFT_DELETED_BY_USER_REASON);
        customerRepository.save(customerEntity);

        log.info("Customer soft deleted successfully with id: {}", customerId);
    }

}
