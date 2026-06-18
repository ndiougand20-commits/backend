package com.rezo.backend.config;

import com.rezo.backend.dto.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + (err.getDefaultMessage() == null ? "valeur invalide" : err.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Donnees invalides";
        }
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Donnees invalides";
        }
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message, request.getRequestURI());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Requete invalide"
                : exception.getMessage();
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acces refuse", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Erreur serveur, reessaie plus tard",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, String path) {
        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(status.value(), code, message, path));
    }
}