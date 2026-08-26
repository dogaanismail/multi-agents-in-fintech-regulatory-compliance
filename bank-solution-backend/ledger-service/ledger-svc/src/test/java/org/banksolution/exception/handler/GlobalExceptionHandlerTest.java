package org.banksolution.exception.handler;

import org.banksolution.exception.CustomError;
import org.banksolution.exception.InsufficientLedgerFundsException;
import org.banksolution.exception.LedgerAccountNotFoundException;
import org.banksolution.exception.LedgerAccountPersistenceException;
import org.banksolution.exception.LedgerPostingException;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldTurnFieldValidationFailuresIntoABadRequestWithSubErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createLedgerAccountRequest");
        bindingResult.addError(new FieldError("createLedgerAccountRequest", "currency", "must not be null"));

        ResponseEntity<CustomError> customErrorResponse = globalExceptionHandler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult));

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getSubErrors())
                .extracting(CustomError.CustomSubError::getField, CustomError.CustomSubError::getMessage)
                .containsExactly(tuple("currency", "must not be null"));
    }

    @Test
    void shouldMapEachLedgerFailureToItsHttpStatus() {
        UUID ledgerAccountId = UUID.randomUUID();
        UUID clientTransactionId = UUID.randomUUID();

        assertResponse(globalExceptionHandler.handleLedgerAccountNotFound(new LedgerAccountNotFoundException(ledgerAccountId)),
                HttpStatus.NOT_FOUND, CustomError.Header.NOT_FOUND, ledgerAccountId.toString());
        assertResponse(globalExceptionHandler.handleIllegalArgument(new IllegalArgumentException("WALLET is not an internal account type")),
                HttpStatus.BAD_REQUEST, CustomError.Header.VALIDATION_ERROR, "WALLET");
        assertResponse(globalExceptionHandler.handleLedgerAccountPersistence(new LedgerAccountPersistenceException("ExistsWithDifferentLedger")),
                HttpStatus.CONFLICT, CustomError.Header.PROCESS_ERROR, "ExistsWithDifferentLedger");
        assertResponse(globalExceptionHandler.handlePendingAuthorisationNotFound(new PendingAuthorisationNotFoundException(clientTransactionId)),
                HttpStatus.NOT_FOUND, CustomError.Header.NOT_FOUND, clientTransactionId.toString());
        assertResponse(globalExceptionHandler.handleInsufficientLedgerFunds(new InsufficientLedgerFundsException(ledgerAccountId)),
                HttpStatus.UNPROCESSABLE_ENTITY, CustomError.Header.PROCESS_ERROR, ledgerAccountId.toString());
        assertResponse(globalExceptionHandler.handleLedgerPosting(new LedgerPostingException("AccountsMustBeDifferent")),
                HttpStatus.CONFLICT, CustomError.Header.PROCESS_ERROR, "AccountsMustBeDifferent");
        assertResponse(globalExceptionHandler.handleLedgerUnavailable(new LedgerUnavailableException(new InterruptedException("cluster down"))),
                HttpStatus.SERVICE_UNAVAILABLE, CustomError.Header.PROCESS_ERROR, "cluster down");
    }

    private static void assertResponse(
            ResponseEntity<CustomError> customErrorResponse,
            HttpStatus expectedHttpStatus,
            CustomError.Header expectedHeader,
            String expectedMessageFragment) {

        assertThat(customErrorResponse.getStatusCode()).isEqualTo(expectedHttpStatus);
        assert customErrorResponse.getBody() != null;
        assertThat(customErrorResponse.getBody().getHttpStatus()).isEqualTo(expectedHttpStatus);
        assertThat(customErrorResponse.getBody().getHeader()).isEqualTo(expectedHeader.getName());
        assertThat(customErrorResponse.getBody().getMessage()).contains(expectedMessageFragment);
    }
}
