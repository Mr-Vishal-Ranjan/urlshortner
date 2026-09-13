package com.bitly.urlshortner.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central exception handler for all REST controllers.
 * Ensures no 500 Internal Server Error leaks to the frontend with an
 * unformatted stack trace; every error case returns a consistent JSON body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // 400 – Validation failures (@NotBlank, etc.)
    // ------------------------------------------------------------------

    /**
     * Handles validation errors triggered by @Valid on @RequestBody.
     * Returns the list of field-level error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    // ------------------------------------------------------------------
    // 400 – Malformed / unreadable JSON body
    // ------------------------------------------------------------------

    /**
     * Handles cases where the request body is missing or cannot be parsed.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed",
                null
        );
    }

    // ------------------------------------------------------------------
    // 400 – Illegal argument (e.g. invalid URI format)
    // ------------------------------------------------------------------

    /**
     * Handles IllegalArgumentException thrown when creating a URI from an
     * invalid URL string (e.g., spaces or invalid characters).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // ------------------------------------------------------------------
    // 404 – Short URL not found
    // ------------------------------------------------------------------

    /**
     * Handles the case where a hash does not map to any stored URL.
     */
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUrlNotFound(
            UrlNotFoundException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    // ------------------------------------------------------------------
    // 500 – Unexpected errors (last-resort safety net)
    // ------------------------------------------------------------------

    /**
     * Catch-all handler so any unhandled exception still returns a clean
     * JSON response instead of a raw Spring error page.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                null
        );
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, List<String> errors) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toEpochMilli());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
