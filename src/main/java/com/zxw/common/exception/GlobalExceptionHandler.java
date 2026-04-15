package com.zxw.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxw.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request parameters");
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage() == null ? "System error" : ex.getMessage(),
                request
        );
    }

    private ResponseEntity<?> buildErrorResponse(int status, String message, HttpServletRequest request) {
        ApiResponse<Void> payload = ApiResponse.fail(message);
        if (acceptsEventStream(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(buildEventStreamErrorBody(status, message));
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
    }

    private boolean acceptsEventStream(HttpServletRequest request) {
        String accept = request == null ? null : request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private String toJson(ApiResponse<Void> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"success\":false,\"message\":\"System error\",\"data\":null}";
        }
    }

    private String buildEventStreamErrorBody(int status, String message) {
        try {
            var event = objectMapper.createObjectNode();
            event.put("type", "error");
            var error = objectMapper.createObjectNode();
            error.put("message", message == null ? "System error" : message);
            error.put("type", status == 429 ? "rate_limit_error" : "server_error");
            error.put("status", status);
            event.set("error", error);

            var completedEvent = objectMapper.createObjectNode();
            completedEvent.put("type", "response.completed");
            var response = objectMapper.createObjectNode();
            response.put("id", "resp_" + UUID.randomUUID().toString().replace("-", ""));
            response.put("object", "response");
            response.put("created_at", Instant.now().getEpochSecond());
            response.put("status", "failed");
            response.set("error", error.deepCopy());
            completedEvent.set("response", response);

            return "data: " + objectMapper.writeValueAsString(event) + "\n\n"
                    + "data: " + objectMapper.writeValueAsString(completedEvent) + "\n\n"
                    + "data: [DONE]\n\n";
        } catch (Exception ex) {
            return "data: {\"type\":\"error\",\"error\":{\"message\":\"System error\",\"type\":\"server_error\",\"status\":500}}\n\n"
                    + "data: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp_fallback\",\"object\":\"response\",\"created_at\":0,\"status\":\"failed\",\"error\":{\"message\":\"System error\",\"type\":\"server_error\",\"status\":500}}}\n\n"
                    + "data: [DONE]\n\n";
        }
    }
}
