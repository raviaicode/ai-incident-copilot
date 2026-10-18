package dev.incidentcopilot.api;

import dev.incidentcopilot.api.tools.InvestigationSourceException;
import java.util.NoSuchElementException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ApiError> badRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", exception.getMessage()));
    }
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<ApiError> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", exception.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<ApiError> conflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("INVALID_STATE", exception.getMessage()));
    }
    @ExceptionHandler({InvestigationSourceException.class, RestClientException.class}) ResponseEntity<ApiError> sourceUnavailable(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError("SOURCE_UNAVAILABLE", exception.getMessage()));
    }
    record ApiError(String code, String message) {}
}
