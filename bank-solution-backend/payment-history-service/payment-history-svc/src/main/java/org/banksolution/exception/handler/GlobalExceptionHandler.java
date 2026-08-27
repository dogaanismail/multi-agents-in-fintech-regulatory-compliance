package org.banksolution.exception.handler;

import lombok.NonNull;
import org.banksolution.exception.CustomError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<@NonNull CustomError> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Invalid value for " + methodArgumentTypeMismatchException.getName()
                        + ": " + methodArgumentTypeMismatchException.getValue())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<@NonNull CustomError> handleMissingParameter(
            MissingServletRequestParameterException missingServletRequestParameterException) {

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Missing request parameter: " + missingServletRequestParameterException.getParameterName())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);
    }
}
