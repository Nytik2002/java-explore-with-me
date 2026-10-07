package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException exception) {
        return createError(
                exception.getMessage(),
                "The required object was not found.",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException exception) {
        return createError(
                exception.getMessage(),
                "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(
            DataIntegrityViolationException exception
    ) {
        return createError(
                exception.getMessage(),
                "Integrity constraint has been violated.",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        "Field: " + error.getField()
                                + ". Error: " + error.getDefaultMessage()
                                + ". Value: " + error.getRejectedValue()
                )
                .toList();

        return ApiError.builder()
                .errors(errors)
                .message(errors.stream().collect(Collectors.joining("; ")))
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        return createError(
                exception.getMessage(),
                "Incorrectly made request.",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(Exception exception) {
        return createError(
                exception.getMessage(),
                "Incorrectly made request.",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        return createError(
                exception.getMessage(),
                "Internal server error.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ApiError createError(
            String message,
            String reason,
            HttpStatus status
    ) {
        return ApiError.builder()
                .errors(List.of())
                .message(message)
                .reason(reason)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}