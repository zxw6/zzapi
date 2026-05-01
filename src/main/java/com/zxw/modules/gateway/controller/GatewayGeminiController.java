package com.zxw.modules.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zxw.common.exception.BusinessException;
import com.zxw.modules.gateway.service.GatewayChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@Api(tags = "Gemini兼容接口")
/**
 * Gemini 协议兼容控制器。
 * 把 Gemini 风格请求转换为内部 OpenAI 风格协议，再交给统一网关处理。
 */
public class GatewayGeminiController {

    private final GatewayChatService gatewayChatService;
    private final ObjectMapper objectMapper;

    public GatewayGeminiController(GatewayChatService gatewayChatService, ObjectMapper objectMapper) {
        this.gatewayChatService = gatewayChatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping({"/v1beta/models/{model}:generateContent", "/v1/models/{model}:generateContent"})
    @ApiOperation("Gemini generateContent")
    /**
     * 处理 Gemini 非流式生成请求。
     */
    public ResponseEntity<String> generateContent(@PathVariable String model,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestHeader(value = "x-goog-api-key", required = false) String xGoogApiKey,
                                                  @RequestParam(value = "key", required = false) String key,
                                                  @RequestBody String body,
                                                  HttpServletRequest request) throws Exception {
        // 将 Gemini 请求转换为内部 OpenAI 协议后转发
        String resolvedAuthorization = resolveAuthorization(authorization, xGoogApiKey, key);
        String openAiRequest = buildChatCompletionsRequest(model, body, false);
        ResponseEntity<?> response = gatewayChatService.chatCompletions(resolvedAuthorization, openAiRequest, request);
        return convertGenerateContentResponse(response, model);
    }

    @PostMapping({"/v1beta/models/{model}:streamGenerateContent", "/v1/models/{model}:streamGenerateContent"})
    @ApiOperation("Gemini streamGenerateContent")
    /**
     * 处理 Gemini 流式生成请求。
     */
    public ResponseEntity<String> streamGenerateContent(@PathVariable String model,
                                                        @RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestHeader(value = "x-goog-api-key", required = false) String xGoogApiKey,
                                                        @RequestParam(value = "key", required = false) String key,
                                                        @RequestParam(value = "alt", required = false) String alt,
                                                        @RequestBody String body,
                                                        HttpServletRequest request) throws Exception {
        // 将 Gemini 流式请求转换为内部 SSE 响应
        String resolvedAuthorization = resolveAuthorization(authorization, xGoogApiKey, key);
        String openAiRequest = buildChatCompletionsRequest(model, body, true);
        ResponseEntity<?> response = gatewayChatService.chatCompletions(resolvedAuthorization, openAiRequest, request);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyAsString(response.getBody()));
        }
        String eventStream = bodyAsString(response.getBody());
        String geminiEventStream = convertChatCompletionsSseToGemini(eventStream, model);
        return ResponseEntity.status(response.getStatusCode())
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                .body(geminiEventStream);
    }

    /**
     * 把内部响应转换成 Gemini 普通响应结构。
     */
    private ResponseEntity<String> convertGenerateContentResponse(ResponseEntity<?> response, String model) throws Exception {
        String body = bodyAsString(response.getBody());
        if (!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }
        JsonNode openAi = objectMapper.readTree(body);
        ObjectNode payload = buildGeminiGenerateContentPayload(openAi, model, false);
        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(payload));
    }

    /**
     * 构造 Gemini `generateContent` 返回体。
     */
    private ObjectNode buildGeminiGenerateContentPayload(JsonNode openAi, String model, boolean deltaOnly) {
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode candidates = payload.putArray("candidates");

        for (JsonNode choice : openAi.path("choices")) {
            ObjectNode candidate = candidates.addObject();
            candidate.put("index", choice.path("index").asInt(0));
            String finishReason = mapFinishReason(choice.path("finish_reason").asText(null));
            if (finishReason != null && !finishReason.isBlank()) {
                candidate.put("finishReason", finishReason);
            }

            ObjectNode content = candidate.putObject("content");
            content.put("role", "model");
            ArrayNode parts = content.putArray("parts");

            String text = deltaOnly
                    ? choice.path("delta").path("content").asText("")
                    : choice.path("message").path("content").asText("");
            if (!text.isBlank()) {
                ObjectNode part = parts.addObject();
                part.put("text", text);
            }
        }

        JsonNode usage = openAi.path("usage");
        if (!usage.isMissingNode()) {
            ObjectNode usageMetadata = payload.putObject("usageMetadata");
            usageMetadata.put("promptTokenCount", usage.path("prompt_tokens").asInt(0));
            usageMetadata.put("candidatesTokenCount", usage.path("completion_tokens").asInt(0));
            usageMetadata.put("totalTokenCount", usage.path("total_tokens").asInt(0));
        }
        payload.put("modelVersion", model);
        return payload;
    }

    /**
     * 把 OpenAI 风格 SSE 流转换成 Gemini SSE 流。
     */
    private String convertChatCompletionsSseToGemini(String eventStream, String model) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (String rawLine : eventStream.split("(?<=\\n)")) {
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String payload = trimmed.substring(5).trim();
            if (payload.isBlank()) {
                continue;
            }
            if ("[DONE]".equals(payload)) {
                builder.append("data: ").append("{\"done\":true,\"modelVersion\":\"").append(model).append("\"}").append("\n\n");
                continue;
            }

            JsonNode openAi = tryReadJson(payload);
            if (openAi == null) {
                continue;
            }
            ObjectNode gemini = buildGeminiGenerateContentPayload(openAi, model, true);
            builder.append("data: ").append(objectMapper.writeValueAsString(gemini)).append("\n\n");
        }
        return builder.toString();
    }

    /**
     * 把 Gemini 请求体改写为 chat completions 请求体。
     */
    private String buildChatCompletionsRequest(String model, String body, boolean stream) throws Exception {
        JsonNode gemini = objectMapper.readTree(body);
        ObjectNode openAi = objectMapper.createObjectNode();
        openAi.put("model", model);
        openAi.put("stream", stream);

        JsonNode generationConfig = firstObject(gemini, "generationConfig", "generation_config");
        if (generationConfig != null) {
            if (generationConfig.has("temperature")) {
                openAi.set("temperature", generationConfig.get("temperature"));
            }
            if (generationConfig.has("topP")) {
                openAi.set("top_p", generationConfig.get("topP"));
            }
            if (generationConfig.has("maxOutputTokens")) {
                openAi.set("max_tokens", generationConfig.get("maxOutputTokens"));
            }
        }

        ArrayNode messages = openAi.putArray("messages");
        appendSystemInstruction(messages, firstObject(gemini, "systemInstruction", "system_instruction"));

        JsonNode contents = gemini.path("contents");
        if (contents.isArray()) {
            for (JsonNode content : contents) {
                String role = mapGeminiRole(content.path("role").asText("user"));
                String text = flattenGeminiParts(content.path("parts"));
                if (text.isBlank()) {
                    continue;
                }
                ObjectNode message = messages.addObject();
                message.put("role", role);
                message.put("content", text);
            }
        }

        if (messages.isEmpty()) {
            throw new BusinessException(400, "Gemini 请求缺少 contents");
        }
        return objectMapper.writeValueAsString(openAi);
    }

    private void appendSystemInstruction(ArrayNode messages, JsonNode instruction) {
        if (instruction == null || instruction.isMissingNode() || instruction.isNull()) {
            return;
        }
        String text = flattenGeminiParts(instruction.path("parts"));
        if (text.isBlank()) {
            return;
        }
        ObjectNode message = messages.addObject();
        message.put("role", "system");
        message.put("content", text);
    }

    private JsonNode firstObject(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = root.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String flattenGeminiParts(JsonNode parts) {
        if (parts == null || !parts.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.hasNonNull("text")) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(part.path("text").asText(""));
                continue;
            }
            if (part.has("inlineData") || part.has("inline_data")) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append("[inline binary data omitted]");
            }
            if (part.has("fileData") || part.has("file_data")) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append("[file data omitted]");
            }
        }
        return builder.toString().trim();
    }

    private String mapGeminiRole(String role) {
        String normalized = role == null ? "" : role.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "model" -> "assistant";
            case "system" -> "system";
            default -> "user";
        };
    }

    private String mapFinishReason(String finishReason) {
        if (finishReason == null || finishReason.isBlank()) {
            return null;
        }
        return switch (finishReason.toLowerCase(Locale.ROOT)) {
            case "stop" -> "STOP";
            case "length" -> "MAX_TOKENS";
            case "content_filter" -> "SAFETY";
            case "tool_calls" -> "STOP";
            default -> finishReason.toUpperCase(Locale.ROOT);
        };
    }

    private String resolveAuthorization(String authorization, String xGoogApiKey, String key) {
        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }
        String actualKey = firstNonBlank(xGoogApiKey, key);
        if (actualKey == null) {
            return null;
        }
        return actualKey.startsWith("Bearer ") ? actualKey : "Bearer " + actualKey;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String bodyAsString(Object body) throws Exception {
        if (body == null) {
            return "";
        }
        if (body instanceof String text) {
            return text;
        }
        return objectMapper.writeValueAsString(body);
    }

    private JsonNode tryReadJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            return null;
        }
    }
}
