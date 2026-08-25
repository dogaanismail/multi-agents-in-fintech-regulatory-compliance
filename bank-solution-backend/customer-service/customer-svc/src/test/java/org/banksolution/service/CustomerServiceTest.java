package org.banksolution.service;

import org.banksolution.entity.CustomerEntity;
import org.banksolution.exception.CustomerAlreadyExistsException;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.model.request.CustomerCreateRequest;
import org.banksolution.model.request.CustomerUpdateRequest;
import org.banksolution.model.response.CustomerResponse;
import org.banksolution.model.response.PageResponse;
import org.banksolution.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerCreateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerEntity;
import static org.banksolution.fixtures.CustomerFixtures.createCustomerUpdateRequest;
import static org.banksolution.fixtures.CustomerFixtures.createUniqueEmail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-26T12:00:00Z");

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void createServiceWithFixedClock() {
        customerService = new CustomerService(customerRepository, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCreateTheCustomerWhenTheEmailIsFree() {
        String email = createUniqueEmail();
        CustomerCreateRequest customerCreateRequest = createCustomerCreateRequest(email);
        when(customerRepository.existsCustomerEntityByEmail(email)).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse customerResponse = customerService.createCustomer(customerCreateRequest);

        ArgumentCaptor<CustomerEntity> customerEntityCaptor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerRepository).save(customerEntityCaptor.capture());
        assertThat(customerEntityCaptor.getValue().getEmail()).isEqualTo(email);
        assertThat(customerResponse.getEmail()).isEqualTo(email);
        assertThat(customerResponse.getFirstName()).isEqualTo(customerCreateRequest.getFirstName());
    }

    @Test
    void shouldRejectACreateForAnEmailThatAlreadyExists() {
        String email = createUniqueEmail();
        CustomerCreateRequest customerCreateRequest = createCustomerCreateRequest(email);
        when(customerRepository.existsCustomerEntityByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(customerCreateRequest))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining(email);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldReturnTheCustomerWhenItExists() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        customerEntity.setId(UUID.randomUUID());
        when(customerRepository.findById(customerEntity.getId())).thenReturn(Optional.of(customerEntity));

        CustomerResponse customerResponse = customerService.getCustomerById(customerEntity.getId());

        assertThat(customerResponse.getId()).isEqualTo(customerEntity.getId());
        assertThat(customerResponse.getEmail()).isEqualTo(customerEntity.getEmail());
    }

    @Test
    void shouldFailWithTheMissingIdWhenTheCustomerDoesNotExist() {
        UUID unknownCustomerId = UUID.randomUUID();
        when(customerRepository.findById(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(unknownCustomerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(unknownCustomerId.toString());
    }

    @Test
    void shouldWrapThePageContentAndMetadataInThePageResponse() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        customerEntity.setId(UUID.randomUUID());
        PageRequest pageRequest = PageRequest.of(0, 2);
        when(customerRepository.findAll(pageRequest))
                .thenReturn(new PageImpl<>(List.of(customerEntity), pageRequest, 3));

        PageResponse<CustomerResponse> customerResponsePage = customerService.getAllCustomers(pageRequest);

        assertThat(customerResponsePage.getContent())
                .extracting(CustomerResponse::getId)
                .containsExactly(customerEntity.getId());
        assertThat(customerResponsePage.getPage().getSize()).isEqualTo(2);
        assertThat(customerResponsePage.getPage().getNumber()).isZero();
        assertThat(customerResponsePage.getPage().getTotalElements()).isEqualTo(3);
        assertThat(customerResponsePage.getPage().getTotalPages()).isEqualTo(2);
        assertThat(customerResponsePage.getPage().isFirst()).isTrue();
        assertThat(customerResponsePage.getPage().isLast()).isFalse();
        assertThat(customerResponsePage.getPage().isEmpty()).isFalse();
    }

    @Test
    void shouldUpdateTheCustomerWithoutCheckingUniquenessWhenTheEmailIsUnchanged() {
        String email = createUniqueEmail();
        CustomerEntity customerEntity = createCustomerEntity(email);
        customerEntity.setId(UUID.randomUUID());
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(email);
        when(customerRepository.findById(customerEntity.getId())).thenReturn(Optional.of(customerEntity));
        when(customerRepository.save(customerEntity)).thenReturn(customerEntity);

        CustomerResponse customerResponse =
                customerService.updateCustomer(customerEntity.getId(), customerUpdateRequest);

        verify(customerRepository, never()).existsCustomerEntityByEmail(any());
        assertThat(customerResponse.getFirstName()).isEqualTo(customerUpdateRequest.getFirstName());
        assertThat(customerResponse.getCustomerStatus()).isEqualTo(customerUpdateRequest.getCustomerStatus());
    }

    @Test
    void shouldUpdateTheCustomerWhenTheNewEmailIsFree() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        customerEntity.setId(UUID.randomUUID());
        String newEmail = createUniqueEmail();
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(newEmail);
        when(customerRepository.findById(customerEntity.getId())).thenReturn(Optional.of(customerEntity));
        when(customerRepository.existsCustomerEntityByEmail(newEmail)).thenReturn(false);
        when(customerRepository.save(customerEntity)).thenReturn(customerEntity);

        CustomerResponse customerResponse =
                customerService.updateCustomer(customerEntity.getId(), customerUpdateRequest);

        assertThat(customerResponse.getEmail()).isEqualTo(newEmail);
    }

    @Test
    void shouldRejectAnUpdateToAnEmailOwnedByAnotherCustomer() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        customerEntity.setId(UUID.randomUUID());
        String takenEmail = createUniqueEmail();
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(takenEmail);
        when(customerRepository.findById(customerEntity.getId())).thenReturn(Optional.of(customerEntity));
        when(customerRepository.existsCustomerEntityByEmail(takenEmail)).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateCustomer(customerEntity.getId(), customerUpdateRequest))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining(takenEmail);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldFailAnUpdateForACustomerThatDoesNotExist() {
        UUID unknownCustomerId = UUID.randomUUID();
        CustomerUpdateRequest customerUpdateRequest = createCustomerUpdateRequest(createUniqueEmail());
        when(customerRepository.findById(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(unknownCustomerId, customerUpdateRequest))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(unknownCustomerId.toString());
    }

    @Test
    void shouldStampTheSoftDeletionWithTheClockTimeAndAReason() {
        CustomerEntity customerEntity = createCustomerEntity(createUniqueEmail());
        customerEntity.setId(UUID.randomUUID());
        when(customerRepository.findById(customerEntity.getId())).thenReturn(Optional.of(customerEntity));

        customerService.deleteCustomer(customerEntity.getId());

        verify(customerRepository).save(customerEntity);
        assertThat(customerEntity.getDeletedAt()).isEqualTo(FIXED_NOW);
        assertThat(customerEntity.getDeletedReason()).isEqualTo("Soft deleted by user");
    }

    @Test
    void shouldFailADeleteForACustomerThatDoesNotExist() {
        UUID unknownCustomerId = UUID.randomUUID();
        when(customerRepository.findById(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(unknownCustomerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(unknownCustomerId.toString());

        verify(customerRepository, never()).save(any());
    }
}
