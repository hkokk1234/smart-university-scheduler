package com.smartuniversity.scheduler.platform.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.time.Instant;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied", request);
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException error, HttpServletRequest request) {
        return respond(error.getStatus(), error.getCode(), error.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        var fields =new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        error.getBindingResult().getGlobalErrors().forEach(field ->
                fields.putIfAbsent(field.getObjectName(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR",
                "Request validation failed", request.getRequestURI(), fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraints(ConstraintViolationException error, HttpServletRequest request) {
        var fields =new LinkedHashMap<String, String>();
        error.getConstraintViolations().forEach(violation ->
                fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR",
                "Request validation failed", request.getRequestURI(), fields));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class, HandlerMethodValidationException.class})
    ResponseEntity<ApiError> badRequest(Exception error, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request parameters", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed JSON or unsupported request fields", request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, TransactionSystemException.class})
    ResponseEntity<ApiError> conflict(Exception error, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "CONFLICT", "The change conflicts with stored data or database constraints", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> stale(HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "CONFLICT", "Section was changed; reload it before updating", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> missing(HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Endpoint not found", request);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not supported for this endpoint", request);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMedia(HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Use the content type required by this endpoint", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(HttpServletRequest request) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Choose a PDF or Excel file up to 5 MB", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(HttpServletRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message, request.getRequestURI()));
    }
}
