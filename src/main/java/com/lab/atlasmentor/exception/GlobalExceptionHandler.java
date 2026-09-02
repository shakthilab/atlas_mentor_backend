package com.lab.atlasmentor.exception;

import com.lab.atlasmentor.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Safe, intentional business errors (message is user-facing) ──────────

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return status(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return status(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return status(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return status(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({UnauthorizedAccessException.class, BranchSecurityException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(RuntimeException ex) {
        return status(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({InvalidAssignmentException.class, InvalidStatusTransitionException.class, ValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleDomainValidation(RuntimeException ex) {
        return badRequest(ex.getMessage());
    }

    /**
     * Backstop above TaskAttachmentUploadService's own per-category size checks -
     * trips only if a request exceeds spring.servlet.multipart.max-file-size/
     * max-request-size (currently 30MB) before our code ever runs.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return badRequest("File is too large to upload.");
    }

    // ── Rate limiting ────────────────────────────────────────────────────────

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooManyRequests(TooManyRequestsException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(ApiResponse.error(ex.getMessage(), "429"));
    }

    // ── Framework / infrastructure errors (generic message to client) ────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return badRequest(message.isBlank() ? "Validation failed" : message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return badRequest(ex.getMessage());
    }

    /**
     * A path/query parameter that couldn't be converted to its declared type - e.g.
     * GET /api/role-templates/abc, where {id} is a Long. Without this handler it falls
     * through to the generic RuntimeException handler below and surfaces as a raw 500,
     * which is the wrong signal for what is really a malformed request from the caller.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "a different type";
        return badRequest("Invalid value '" + ex.getValue() + "' for '" + ex.getName() + "' - expected " + expectedType + ".");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return status(HttpStatus.CONFLICT, "Request could not be completed due to a data conflict.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return status(HttpStatus.FORBIDDEN, "Access denied.");
    }

    /**
     * Catch-all for any RuntimeException not matched above (JPA errors, NPEs, etc.).
     * Full stack trace is logged; only a generic message is sent to the client.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(message, "400"));
    }

    private ResponseEntity<ApiResponse<Void>> status(HttpStatus httpStatus, String message) {
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(message, String.valueOf(httpStatus.value())));
    }
}
