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
/**
 * 全局异常处理器。
 * 负责把业务异常、参数校验异常和系统异常统一转换成标准响应。
 */
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex, HttpServletRequest request) {
        // 业务异常直接按自带状态码返回
        return buildErrorResponse(ex.getStatus(), ex.getMessage(), request);
    }

    /**
     * 处理请求体参数校验异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // 只取第一条字段校验错误返回给前端
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request parameters");
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), message, request);
    }

    /**
     * 处理路径参数和查询参数校验异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        // 处理路径参数、查询参数等约束校验失败
        return buildErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request);
    }

    /**
     * 兜底处理未捕获异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex, HttpServletRequest request) {
        // 兜底处理未捕获异常，避免直接把堆栈暴露给前端
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage() == null ? "System error" : ex.getMessage(),
                request
        );
    }

    /**
     * 根据请求类型构造统一错误响应。
     */
    private ResponseEntity<?> buildErrorResponse(int status, String message, HttpServletRequest request) {
        // 对 SSE 请求和普通 JSON 请求分别返回不同格式的错误体
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

    /**
     * 判断当前请求是否期望 SSE 响应。
     */
    private boolean acceptsEventStream(HttpServletRequest request) {
        // 根据 Accept 头判断是否是事件流请求
        String accept = request == null ? null : request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * 把统一响应对象序列化为 JSON。
     */
    private String toJson(ApiResponse<Void> payload) {
        // 序列化失败时返回一个兜底 JSON 字符串
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"success\":false,\"message\":\"System error\",\"data\":null}";
        }
    }

    /**
     * 构造 SSE 场景下的错误事件流内容。
     */
    private String buildEventStreamErrorBody(int status, String message) {
        // SSE 场景下模拟标准事件流错误返回，兼容流式客户端处理逻辑
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
