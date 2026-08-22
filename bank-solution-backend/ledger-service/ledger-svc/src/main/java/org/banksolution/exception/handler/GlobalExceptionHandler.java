package org.banksolution.exception.handler;

import lombok.NonNull;
import org.banksolution.exception.CustomError;
import org.banksolution.exception.LedgerAccountNotFoundException;
import org.banksolution.exception.LedgerAccountPersistenceException;
import org.banksolution.exception.LedgerUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<@NonNull CustomError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        List<CustomError.CustomSubError> subErrors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            subErrors.add(CustomError.CustomSubError.builder()
                    .field(fieldName)
                    .message(error.getDefaultMessage())
                    .build());
        });

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Validation failed")
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LedgerAccountNotFoundException.class)
    protected ResponseEntity<@NonNull CustomError> handleLedgerAccountNotFound(LedgerAccountNotFoundException ex) {
        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.NOT_FOUND.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<@NonNull CustomError> handleIllegalArgument(IllegalArgumentException ex) {
        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LedgerAccountPersistenceException.class)
    protected ResponseEntity<@NonNull CustomError> handleLedgerAccountPersistence(LedgerAccountPersistenceException ex) {
        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.CONFLICT)
                .header(CustomError.Header.PROCESS_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LedgerUnavailableException.class)
    protected ResponseEntity<@NonNull CustomError> handleLedgerUnavailable(LedgerUnavailableException ex) {
        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .header(CustomError.Header.PROCESS_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
