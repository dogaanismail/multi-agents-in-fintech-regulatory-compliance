package org.banksolution.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.banksolution.exception.BeneficiaryCoordinateNotFoundException;
import org.banksolution.exception.BeneficiaryNotFoundException;
import org.banksolution.exception.BeneficiaryValidationException;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.exception.CustomerServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldListTheRuleViolationAsASubErrorWith400() {
        BeneficiaryValidationException beneficiaryValidationException = new BeneficiaryValidationException(
                List.of("use DELETE to delete a beneficiary"));

        ResponseEntity<CustomError> response = globalExceptionHandler.handleBeneficiaryValidation(beneficiaryValidationException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert response.getBody() != null;

        assertThat(response.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getMessage)
                .containsExactly("use DELETE to delete a beneficiary");
    }

    @Test
    void shouldAnswerMalformedJsonWith400RatherThan404() {
        HttpInputMessage httpInputMessage = new MockHttpInputMessage(new byte[0]);
        HttpMessageNotReadableException httpMessageNotReadableException =
                new HttpMessageNotReadableException("JSON parse error", httpInputMessage);

        ResponseEntity<CustomError> response = globalExceptionHandler.handleMalformedRequest(httpMessageNotReadableException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldAnswerAMissingBeneficiaryOrCoordinateWith404() {
        ResponseEntity<CustomError> beneficiaryResponse =
                globalExceptionHandler.handleNotFound(new BeneficiaryNotFoundException(UUID.randomUUID()));
        ResponseEntity<CustomError> coordinateResponse = globalExceptionHandler.handleNotFound(
                new BeneficiaryCoordinateNotFoundException(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(beneficiaryResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(coordinateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldAnswerAnUnknownCustomerWith422() {
        ResponseEntity<CustomError> response = globalExceptionHandler.handleCustomerNotFound(new CustomerNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void shouldAnswerAnUnreachableCustomerServiceWith503() {
        CustomerServiceUnavailableException customerServiceUnavailableException =
                new CustomerServiceUnavailableException(UUID.randomUUID(), new IllegalStateException("connection refused"));

        ResponseEntity<CustomError> response = globalExceptionHandler.handleCustomerServiceUnavailable(customerServiceUnavailableException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldAnswerAConcurrentModificationWith409() {
        ObjectOptimisticLockingFailureException objectOptimisticLockingFailureException =
                new ObjectOptimisticLockingFailureException("beneficiary", UUID.randomUUID());

        ResponseEntity<CustomError> response = globalExceptionHandler.handleOptimisticLockingFailure(objectOptimisticLockingFailureException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldAnswerAnUnexpectedErrorWith500WithoutLeakingItsMessage() {
        ResponseEntity<CustomError> response = globalExceptionHandler.handleUnexpectedException(new IllegalStateException("secret internals"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assert response.getBody() != null;
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error");
    }
}
