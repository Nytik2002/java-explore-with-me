package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException exception
    ) {
        return createResponse(
                HttpStatus.NOT_FOUND,
                "The required object was not found.",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException exception
    ) {
        return createResponse(
                HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return createResponse(
                HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<String> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        "Field: "
                                + error.getField()
                                + ". Error: "
                                + error.getDefaultMessage()
                                + ". Value: "
                                + error.getRejectedValue()
                )
                .toList();

        String message = String.join("; ", errors);

        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message,
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        List<String> errors = exception
                .getConstraintViolations()
                .stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage()
                )
                .toList();

        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                String.join("; ", errors),
                errors
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(
            Exception exception
    ) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException exception
    ) {
        return createResponse(
                HttpStatus.NOT_FOUND,
                "The required object was not found.",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception
    ) {
        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error.",
                exception.getMessage(),
                List.of()
        );
    }

    private ResponseEntity<ApiError> createResponse(
            HttpStatus status,
            String reason,
            String message,
            List<String> errors
    ) {
        ApiError apiError = ApiError.builder()
                .errors(errors)
                .message(message)
                .reason(reason)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(status)
                .body(apiError);
    }
}