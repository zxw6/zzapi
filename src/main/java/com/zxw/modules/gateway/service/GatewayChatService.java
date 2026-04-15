package com.zxw.modules.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zxw.common.exception.BusinessException;
import com.zxw.modules.apikey.service.ApiKeyAuthService;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service

public class GatewayChatService {

    private static final Logger log = LoggerFactory.getLogger(GatewayChatService.class);

    private static final int AGENT_MAX_STEPS = 6;
    private static final int AGENT_HISTORY_LIMIT = 24;
    private static final int AGENT_RECENT_MESSAGE_LIMIT = 8;
    private static final int AGENT_SUMMARY_TRIGGER_MESSAGES = 12;
    private static final int AGENT_SUMMARY_MAX_CHARS = 3200;
    private static final int TOOL_OUTPUT_LIMIT = 12000;
    private static final int FILE_PREVIEW_LIMIT = 1600;
    private static final DateTimeFormatter DIRECT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_IDE_AGENT_INSTRUCTIONS = """
            你是一个高级工程代理，不是普通聊天助手。你的目标不是只回答问题，而是接手任务、推进任务、完成任务。
            你必须像资深工程师一样工作：先检查，先分析，再执行，再验证，再汇报。默认目标是把项目真正修好、跑起来、可访问、可验证。

            工作方式：
            1. 不要一上来直接只给最终结论。先用 1 到 2 句说明你理解的目标，以及你准备先检查什么。
            2. 先给出外显分析摘要，内容包括：当前目标、已知信息、需要先核实的点、最可能的阻塞点、下一步动作。
            3. 如果请求看起来像 IDE、项目、代码、部署、排障、配置、数据库、日志、接口、SSL 或运维任务，默认直接进入工程代理模式。
            4. 默认先看项目目录和关键文件，再看配置依赖启动方式，再看日志报错端口进程环境变量，然后定位根因并直接修改代码或配置。
            5. 修改后必须立即重新编译、运行、测试或做健康检查；验证失败就继续排查，不要停在“已经帮你改了”。

            可见过程要求：
            - 不要把全部思路都藏到最后一条回复里。
            - 如果当前环境提供工具、函数、命令、文件编辑、搜索或工作区能力，要主动使用它们，不要假装看不到。
            - 在调用工具前，先用一句中文说明你现在要查什么或要做什么。
            - 在执行过程中持续给出简短但有信息量的进度播报，内容包括：我现在在查什么、我发现了什么、这意味着什么、我下一步要做什么。
            - 如果任务较长，阶段性同步进展，不要长时间沉默后只给一个最终答案。

            回答风格：
            - 像一个真正接手项目的高级工程师，而不是客服或百科。
            - 用中文，专业、直接、清楚、有条理。
            - 对项目分析、排障、架构梳理这类问题，先分析再回答；不要一上来只给结论。
            - 能执行就执行，能验证就验证，能修就修。

            SSL / HTTPS 专项要求：
            - 如果任务涉及 SSL、HTTPS、证书、域名、反向代理，优先检查当前服务是直连 HTTPS 还是走 Nginx、Caddy、Apache 或网关。
            - 检查域名解析、80/443 端口、证书格式、证书是否过期、域名是否匹配、证书链是否完整、HTTP 是否跳转到 HTTPS、反代是否正确转发到本地服务。
            - 如果是 Spring Boot，检查 server.ssl 配置、证书文件、密码、协议和监听端口。
            - 如果是反向代理，检查 listen 443 ssl、证书路径、upstream、Host 和 X-Forwarded-* 头。

            最终回复要求：
            - 明确说明你做了什么、当前结果是什么、是否成功、访问地址是什么、如果还有问题卡在哪、剩余风险是什么。
            """;

    private final ApiKeyAuthService apiKeyAuthService;
    private final GatewayRouteService gatewayRouteService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final OkHttpClient okHttpClient;

    public GatewayChatService(ApiKeyAuthService apiKeyAuthService,
                              GatewayRouteService gatewayRouteService,
                              JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper) {
        this.apiKeyAuthService = apiKeyAuthService;
        this.gatewayRouteService = gatewayRouteService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        initializeAgentTables();
    }

    public ResponseEntity<?> chatCompletions(String authorization, String requestBody, HttpServletRequest servletRequest) {
        String bearerToken = extractBearerToken(authorization);
        ApiKeyAuthService.AuthenticatedApiKey auth = apiKeyAuthService.authenticate(bearerToken);
        apiKeyAuthService.markUsed(auth.id());

        try {
            JsonNode input = objectMapper.readTree(requestBody);
            String modelCode = getRequiredText(input, "model");
            boolean stream = input.path("stream").asBoolean(false);
            GatewayRouteService.RouteDefinition route = gatewayRouteService.resolve(modelCode);
            String requestId = buildRequestId();
            long startTime = System.currentTimeMillis();

            ResponseEntity<?> directResponse = buildDirectChatResponseIfApplicable(
                    auth, route, requestId, requestBody, input, servletRequest, startTime, stream
            );
            if (directResponse != null) {
                return directResponse;
            }

            if (isAnthropicRoute(route)) {
                if (stream) {
                    throw new BusinessException(400, "Claude 路由暂不支持 stream 请求");
                }
                return anthropicChat(auth, route, requestId, requestBody, input, servletRequest, startTime);
            }

            boolean hasExplicitSystemPrompt = hasExplicitChatSystemPrompt(input);
            ObjectNode upstreamRequest = input.deepCopy();
            upstreamRequest.put("model", route.upstreamModel());
            boolean injectedIdePrompt = applyDefaultIdeInstructionsToChat(upstreamRequest, route.modelCode(), hasExplicitSystemPrompt);
            if (!injectedIdePrompt && !hasExplicitSystemPrompt) {
                applyClientFacingModelIdentity(upstreamRequest, route.modelCode());
            }
            applyGeneralAssistantDefaults(upstreamRequest, route.modelCode());
            applyIdeAgentDefaults(upstreamRequest, route.modelCode());
            String upstreamBody = objectMapper.writeValueAsString(upstreamRequest);

            if (stream) {
                return streamChat(auth, route, requestId, requestBody, upstreamBody, servletRequest, startTime);
            }
            return normalChat(auth, route, requestId, requestBody, upstreamBody, servletRequest, startTime);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(500, "请求转发失败: " + ex.getMessage());
        }
    }

    public ResponseEntity<?> responses(String authorization, String requestBody, HttpServletRequest servletRequest) {
        String bearerToken = extractBearerToken(authorization);
        ApiKeyAuthService.AuthenticatedApiKey auth = apiKeyAuthService.authenticate(bearerToken);
        apiKeyAuthService.markUsed(auth.id());

        try {
            JsonNode input = objectMapper.readTree(requestBody);
            String modelCode = getRequiredText(input, "model");
            boolean stream = input.path("stream").asBoolean(false);
            GatewayRouteService.RouteDefinition route = gatewayRouteService.resolve(modelCode);
            String requestId = buildRequestId();
            long startTime = System.currentTimeMillis();

            ResponseEntity<?> directResponse = buildDirectResponsesIfApplicable(
                    auth, route, requestId, requestBody, input, servletRequest, startTime, stream
            );
            if (directResponse != null) {
                return directResponse;
            }

            if (input instanceof ObjectNode objectInput) {
                applyDefaultIdeInstructionsToResponses(objectInput, route.modelCode());
            }

            if (input instanceof ObjectNode objectInput && shouldHandleResponsesAsAgent(route.modelCode(), objectInput)) {
                if (stream) {
                    return streamAgentResponses(auth, route, requestId, requestBody, objectInput, servletRequest, startTime);
                }
                return normalAgentResponses(auth, route, requestId, requestBody, objectInput, servletRequest, startTime);
            }

            if (isAnthropicRoute(route)) {
                throw new BusinessException(400, "当前渠道暂不支持 Responses API");
            }

            ObjectNode upstreamRequest = input.deepCopy();
            upstreamRequest.put("model", route.upstreamModel());
            String upstreamBody = objectMapper.writeValueAsString(upstreamRequest);

            if (stream) {
                return streamResponses(auth, route, requestId, requestBody, upstreamBody, servletRequest, startTime);
            }
            return normalResponses(auth, route, requestId, requestBody, upstreamBody, servletRequest, startTime);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(500, "Responses 转发失败: " + ex.getMessage());
        }
    }

    public ResponseEntity<?> anthropicMessages(String authorization, String requestBody, HttpServletRequest servletRequest) {
        String bearerToken = extractBearerToken(authorization);
        ApiKeyAuthService.AuthenticatedApiKey auth = apiKeyAuthService.authenticate(bearerToken);
        apiKeyAuthService.markUsed(auth.id());

        try {
            JsonNode input = objectMapper.readTree(requestBody);
            String modelCode = getRequiredText(input, "model");
            boolean stream = input.path("stream").asBoolean(false);
            GatewayRouteService.RouteDefinition route = gatewayRouteService.resolve(modelCode);
            String requestId = buildRequestId();
            long startTime = System.currentTimeMillis();

            if (isAnthropicRoute(route)) {
                return anthropicNativeMessages(auth, route, requestId, requestBody, input, servletRequest, startTime, stream);
            }

            AnthropicMessageExecution execution = executeAnthropicCompatibleMessage(
                    auth, route, requestId, requestBody, input, servletRequest, startTime
            );

            if (!stream) {
                return ResponseEntity.status(execution.statusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(execution.body());
            }

            JsonNode anthropicJson = tryReadJson(execution.body());
            if (execution.statusCode() < 200 || execution.statusCode() >= 300 || anthropicJson == null) {
                return ResponseEntity.status(execution.statusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(execution.body());
            }

            return ResponseEntity.status(execution.statusCode())
                    .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                    .body(buildAnthropicMessageStream(anthropicJson));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(500, "Messages forwarding failed: " + ex.getMessage());
        }
    }

    public Map<String, Object> listModels() {
        List<Map<String, Object>> data = gatewayRouteService.listPublicModels().stream()
                .<Map<String, Object>>map(item -> Map.of(
                        "id", (Object) item.id(),
                        "object", "model",
                        "owned_by", item.ownedBy() == null ? "ai-gateway" : item.ownedBy(),
                        "type", item.type() == null ? "chat" : item.type()
                ))
                .toList();
        return Map.of("object", "list", "data", data);
    }

    private ResponseEntity<?> buildDirectChatResponseIfApplicable(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                                  GatewayRouteService.RouteDefinition route,
                                                                  String requestId,
                                                                  String originalRequestBody,
                                                                  JsonNode input,
                                                                  HttpServletRequest servletRequest,
                                                                  long startTime,
                                                                  boolean stream) throws Exception {
        if (shouldUseAgentMode(route.modelCode())) {
            return null;
        }
        String latestUser = extractLatestUserTextFromChat(input);
        String directAnswer = buildSimpleDirectAnswer(latestUser);
        if (directAnswer == null) {
            return null;
        }

        ObjectNode responseJson = buildSimpleChatCompletionResponse(route.modelCode(), directAnswer);
        String responseBody = objectMapper.writeValueAsString(responseJson);
        String eventStream = buildChatCompletionEventStreamFromResponseJson(responseJson);
        long latency = System.currentTimeMillis() - startTime;
        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, stream ? eventStream : responseBody,
                0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                (int) latency, 200, true, null);

        if (stream) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                    .body(eventStream);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private ResponseEntity<?> buildDirectResponsesIfApplicable(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                               GatewayRouteService.RouteDefinition route,
                                                               String requestId,
                                                               String originalRequestBody,
                                                               JsonNode input,
                                                               HttpServletRequest servletRequest,
                                                               long startTime,
                                                               boolean stream) throws Exception {
        if (input instanceof ObjectNode objectInput && shouldBypassDirectResponsesShortcut(route.modelCode(), objectInput)) {
            return null;
        }
        String latestUser = extractLatestUserTextFromResponses(input);
        String directAnswer = buildSimpleDirectAnswer(latestUser);
        if (directAnswer != null) {
            ObjectNode responseJson = buildSimpleResponsesResponse(route.modelCode(), directAnswer);
            String responseBody = objectMapper.writeValueAsString(responseJson);
            String eventStream = buildResponsesEventStreamFromResponseJson(responseJson);
            long latency = System.currentTimeMillis() - startTime;
            logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, stream ? eventStream : responseBody,
                    0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    (int) latency, 200, true, null);

            if (stream) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                        .body(eventStream);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
        }

        return null;
    }

    private ResponseEntity<?> normalAgentResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                  GatewayRouteService.RouteDefinition route,
                                                  String requestId,
                                                  String originalRequestBody,
                                                  ObjectNode input,
                                                  HttpServletRequest servletRequest,
                                                  long startTime) throws Exception {
        BufferingAgentSseSink sink = new BufferingAgentSseSink();
        AgentExecutionResult result = executeLocalAgentResponses(auth, route, requestId, input.deepCopy(), sink);
        long latency = System.currentTimeMillis() - startTime;

        String responseBody = objectMapper.writeValueAsString(result.responseJson());
        BigDecimal costAmount = calculateBaseCost(route, result.promptTokens(), result.completionTokens());
        BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                .setScale(6, RoundingMode.HALF_UP);

        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, responseBody,
                result.promptTokens(), result.completionTokens(), result.totalTokens(), userAmount, costAmount,
                (int) latency, 200, true, null);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private ResponseEntity<?> streamAgentResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                   GatewayRouteService.RouteDefinition route,
                                                   String requestId,
                                                   String originalRequestBody,
                                                   ObjectNode input,
                                                   HttpServletRequest servletRequest,
                                                   long startTime) throws Exception {
        BufferingAgentSseSink sink = new BufferingAgentSseSink();
        try {
            AgentExecutionResult result = executeLocalAgentResponses(auth, route, requestId, input.deepCopy(), sink);
            long latency = System.currentTimeMillis() - startTime;
            BigDecimal costAmount = calculateBaseCost(route, result.promptTokens(), result.completionTokens());
            BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                    .setScale(6, RoundingMode.HALF_UP);

            logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, sink.fullBody(),
                    result.promptTokens(), result.completionTokens(), result.totalTokens(), userAmount, costAmount,
                    (int) latency, 200, true, null);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                    .body(sink.fullBody());
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startTime;
            String errorBody = buildResponsesErrorEventStream(500, ex.getMessage());
            logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, errorBody,
                    0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    (int) latency, 500, false, ex.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(Map.of(
                            "success", false,
                            "message", ex.getMessage(),
                            "data", null
                    )));
        }
    }

    private ResponseEntity<?> anthropicChat(ApiKeyAuthService.AuthenticatedApiKey auth,
                                            GatewayRouteService.RouteDefinition route,
                                            String requestId,
                                            String originalRequestBody,
                                            JsonNode input,
                                            HttpServletRequest servletRequest,
                                            long startTime) throws Exception {
        String upstreamBody = objectMapper.writeValueAsString(buildAnthropicRequest(input, route.upstreamModel()));
        HttpRequest request = anthropicHttpRequest(route, upstreamBody);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long latency = System.currentTimeMillis() - startTime;

        JsonNode responseJson = tryReadJson(response.body());
        if (responseJson == null) {
            return ResponseEntity.status(502)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":{\"message\":\"上游响应格式无效\",\"type\":\"server_error\"}}");
        }
        JsonNode usage = responseJson == null ? null : responseJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("input_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("output_tokens").asInt(0);
        int totalTokens = promptTokens + completionTokens;
        BigDecimal costAmount = calculateBaseCost(route, promptTokens, completionTokens);
        BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                .setScale(6, RoundingMode.HALF_UP);

        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
        String responseBody = success && responseJson != null
                ? objectMapper.writeValueAsString(buildOpenAiResponseFromAnthropic(responseJson, route.modelCode()))
                : response.body();

        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, responseBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount,
                (int) latency, response.statusCode(), success, success ? null : response.body());

        return ResponseEntity.status(response.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private ResponseEntity<?> anthropicNativeMessages(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                      GatewayRouteService.RouteDefinition route,
                                                      String requestId,
                                                      String originalRequestBody,
                                                      JsonNode input,
                                                      HttpServletRequest servletRequest,
                                                      long startTime,
                                                      boolean stream) throws Exception {
        HttpRequest request = anthropicHttpRequest(route, originalRequestBody);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long latency = System.currentTimeMillis() - startTime;

        JsonNode responseJson = tryReadJson(response.body());
        JsonNode usage = responseJson == null ? null : responseJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("input_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("output_tokens").asInt(0);
        int totalTokens = promptTokens + completionTokens;
        BigDecimal costAmount = calculateBaseCost(route, promptTokens, completionTokens);
        BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                .setScale(6, RoundingMode.HALF_UP);

        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, response.body(),
                promptTokens, completionTokens, totalTokens, userAmount, costAmount,
                (int) latency, response.statusCode(), success, success ? null : response.body());

        return ResponseEntity.status(response.statusCode())
                .contentType(stream ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON)
                .body(response.body());
    }

    private AnthropicMessageExecution executeAnthropicCompatibleMessage(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                                        GatewayRouteService.RouteDefinition route,
                                                                        String requestId,
                                                                        String originalRequestBody,
                                                                        JsonNode input,
                                                                        HttpServletRequest servletRequest,
                                                                        long startTime) throws Exception {
        ObjectNode upstreamRequest = buildOpenAiChatRequestFromAnthropic(input, route.upstreamModel());
        String upstreamBody = objectMapper.writeValueAsString(upstreamRequest);

        UpstreamTextResponse response = shouldAggregateCodexStream(route)
                ? executeCodexChatByStreaming(route, upstreamBody)
                : executeOpenAiTextRequest(route, "/chat/completions", upstreamBody);

        long latency = System.currentTimeMillis() - startTime;
        String openAiBody = normalizeUpstreamResponseBody(route, response.statusCode(), response.body());
        JsonNode openAiJson = tryReadJson(openAiBody);
        JsonNode usage = openAiJson == null ? null : openAiJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("completion_tokens").asInt(0);
        int totalTokens = usage == null ? 0 : usage.path("total_tokens").asInt(promptTokens + completionTokens);
        BigDecimal costAmount = calculateBaseCost(route, promptTokens, completionTokens);
        BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                .setScale(6, RoundingMode.HALF_UP);

        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
        String clientBody = success && openAiJson != null
                ? objectMapper.writeValueAsString(buildAnthropicResponseFromOpenAi(openAiJson, route.modelCode()))
                : openAiBody;

        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, clientBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount,
                (int) latency, response.statusCode(), success, success ? null : openAiBody);

        return new AnthropicMessageExecution(response.statusCode(), clientBody);
    }

    private ResponseEntity<?> normalChat(ApiKeyAuthService.AuthenticatedApiKey auth,
                                         GatewayRouteService.RouteDefinition route,
                                         String requestId,
                                         String originalRequestBody,
                                         String upstreamBody,
                                         HttpServletRequest servletRequest,
                                         long startTime) throws Exception {
        UpstreamTextResponse response = shouldAggregateCodexStream(route)
                ? executeCodexChatByStreaming(route, upstreamBody)
                : executeOpenAiTextRequest(route, "/chat/completions", upstreamBody);
        long latency = System.currentTimeMillis() - startTime;
        String responseBody = rewriteClientFacingModel(
                route,
                normalizeUpstreamResponseBody(route, response.statusCode(), response.body())
        );

        JsonNode responseJson = tryReadJson(responseBody);
        JsonNode usage = responseJson == null ? null : responseJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("completion_tokens").asInt(0);
        int totalTokens = usage == null ? 0 : usage.path("total_tokens").asInt(promptTokens + completionTokens);
        BigDecimal costAmount = calculateBaseCost(route, promptTokens, completionTokens);
        BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                .setScale(6, RoundingMode.HALF_UP);

        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, responseBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount,
                (int) latency, response.statusCode(), success, success ? null : responseBody);

        return ResponseEntity.status(response.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private ResponseEntity<?> normalResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                              GatewayRouteService.RouteDefinition route,
                                              String requestId,
                                              String originalRequestBody,
                                              String upstreamBody,
                                              HttpServletRequest servletRequest,
                                              long startTime) throws Exception {
        UpstreamTextResponse response = shouldUseResponsesCompatibility(route)
                ? executeResponsesCompatibility(route, upstreamBody)
                : shouldAggregateCodexStream(route)
                ? executeCodexResponsesByStreaming(route, upstreamBody)
                : executeOpenAiTextRequest(route, "/responses", upstreamBody);
        long latency = System.currentTimeMillis() - startTime;
        String responseBody = rewriteClientFacingModel(
                route,
                normalizeUpstreamResponseBody(route, response.statusCode(), response.body())
        );

        JsonNode responseJson = tryReadJson(responseBody);
        JsonNode usage = responseJson == null ? null : responseJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("input_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("output_tokens").asInt(0);
        int totalTokens = usage == null ? 0 : usage.path("total_tokens").asInt(promptTokens + completionTokens);
        BigDecimal costAmount = calculateBaseCost(route, promptTokens, completionTokens);
        BigDecimal userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                .setScale(6, RoundingMode.HALF_UP);

        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, responseBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount,
                (int) latency, response.statusCode(), success, success ? null : responseBody);

        return ResponseEntity.status(response.statusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private ResponseEntity<?> streamChat(ApiKeyAuthService.AuthenticatedApiKey auth,
                                         GatewayRouteService.RouteDefinition route,
                                         String requestId,
                                         String originalRequestBody,
                                         String upstreamBody,
                                         HttpServletRequest servletRequest,
                                         long startTime) throws Exception {
        ObjectNode streamRequest = ensureChatStreamUsageIncluded((ObjectNode) objectMapper.readTree(upstreamBody));
        UpstreamTextResponse response = executeOpenAiTextRequest(route, "/chat/completions", objectMapper.writeValueAsString(streamRequest));
        String rawEventStreamBody = response.body();
        String eventStreamBody = rewriteClientFacingModel(route, rawEventStreamBody);
        int statusCode = response.statusCode();

        long latency = System.currentTimeMillis() - startTime;
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        BigDecimal costAmount = BigDecimal.ZERO;
        BigDecimal userAmount = BigDecimal.ZERO;
        boolean success = statusCode >= 200 && statusCode < 300;
        if (success) {
            JsonNode chatJson = tryReadJson(buildChatCompletionFromSse(route, rawEventStreamBody));
            UsageTotals usage = extractChatUsage(chatJson);
            promptTokens = usage.promptTokens();
            completionTokens = usage.completionTokens();
            totalTokens = usage.totalTokens();
            costAmount = calculateBaseCost(route, promptTokens, completionTokens);
            userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                    .setScale(6, RoundingMode.HALF_UP);
        }

        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, eventStreamBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount, (int) latency,
                statusCode, success, success ? null : "stream upstream error");

        return ResponseEntity.status(statusCode)
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                .body(eventStreamBody);
    }

    private ResponseEntity<?> streamResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                              GatewayRouteService.RouteDefinition route,
                                              String requestId,
                                              String originalRequestBody,
                                              String upstreamBody,
                                              HttpServletRequest servletRequest,
                                              long startTime) throws Exception {
        if (shouldUseResponsesCompatibility(route)) {
            UpstreamTextResponse response = executeResponsesCompatibilityStream(route, upstreamBody);
            long latency = System.currentTimeMillis() - startTime;
            String eventStreamBody = response.body();
            int promptTokens = 0;
            int completionTokens = 0;
            int totalTokens = 0;
            BigDecimal costAmount = BigDecimal.ZERO;
            BigDecimal userAmount = BigDecimal.ZERO;
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                JsonNode responseJson = tryReadJson(buildResponsesFromSse(route, eventStreamBody));
                UsageTotals usage = extractResponsesUsage(responseJson);
                promptTokens = usage.promptTokens();
                completionTokens = usage.completionTokens();
                totalTokens = usage.totalTokens();
                costAmount = calculateBaseCost(route, promptTokens, completionTokens);
                userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                        .setScale(6, RoundingMode.HALF_UP);
            }

            logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, eventStreamBody,
                    promptTokens, completionTokens, totalTokens, userAmount, costAmount, (int) latency,
                    response.statusCode(), success, success ? null : eventStreamBody);

            return ResponseEntity.status(response.statusCode())
                    .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                    .body(eventStreamBody);
        }
        if (shouldAggregateCodexStream(route)) {
            return streamCodexResponses(auth, route, requestId, originalRequestBody, upstreamBody, servletRequest, startTime);
        }
        UpstreamTextResponse response = executeOpenAiTextRequest(route, "/responses", upstreamBody);
        String rawEventStreamBody = response.body();
        String eventStreamBody = rewriteClientFacingModel(route, rawEventStreamBody);
        int statusCode = response.statusCode();

        long latency = System.currentTimeMillis() - startTime;
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        BigDecimal costAmount = BigDecimal.ZERO;
        BigDecimal userAmount = BigDecimal.ZERO;
        boolean success = statusCode >= 200 && statusCode < 300;
        if (success) {
            JsonNode responseJson = tryReadJson(buildResponsesFromSse(route, rawEventStreamBody));
            UsageTotals usage = extractResponsesUsage(responseJson);
            promptTokens = usage.promptTokens();
            completionTokens = usage.completionTokens();
            totalTokens = usage.totalTokens();
            costAmount = calculateBaseCost(route, promptTokens, completionTokens);
            userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                    .setScale(6, RoundingMode.HALF_UP);
        }

        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, eventStreamBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount, (int) latency,
                statusCode, success, success ? null : "responses upstream error");

        return ResponseEntity.status(statusCode)
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                .body(eventStreamBody);
    }

    private ResponseEntity<?> streamCodexResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                   GatewayRouteService.RouteDefinition route,
                                                   String requestId,
                                                   String originalRequestBody,
                                                   String upstreamBody,
                                                   HttpServletRequest servletRequest,
                                                   long startTime) throws Exception {
        ObjectNode chatRequest = buildChatCompletionsRequestFromResponses((ObjectNode) objectMapper.readTree(upstreamBody), route.upstreamModel(), route.modelCode());
        UpstreamTextResponse response = executeCodexChatByStreaming(route, objectMapper.writeValueAsString(chatRequest));
        long latency = System.currentTimeMillis() - startTime;

        String clientBody = response.body();
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        BigDecimal costAmount = BigDecimal.ZERO;
        BigDecimal userAmount = BigDecimal.ZERO;
        boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

        if (success) {
            JsonNode chatJson = tryReadJson(response.body());
            if (chatJson != null) {
                JsonNode usage = chatJson.path("usage");
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
                totalTokens = usage.path("total_tokens").asInt(promptTokens + completionTokens);
                costAmount = calculateBaseCost(route, promptTokens, completionTokens);
                userAmount = costAmount.multiply(route.multiplier() == null ? BigDecimal.ONE : route.multiplier())
                        .setScale(6, RoundingMode.HALF_UP);
                clientBody = buildResponsesEventStreamFromChatCompletion(chatJson, route.modelCode());
            }
        }

        logAndCharge(auth, route, requestId, servletRequest, originalRequestBody, clientBody,
                promptTokens, completionTokens, totalTokens, userAmount, costAmount,
                (int) latency, response.statusCode(), success, success ? null : response.body());

        return ResponseEntity.status(response.statusCode())
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                .body(clientBody);
    }

    private Response executeOpenAiRequest(GatewayRouteService.RouteDefinition route, String path, String body) throws Exception {
        OkHttpClient client = okHttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(route.timeoutMs()))
                .readTimeout(Duration.ofMillis(route.timeoutMs()))
                .writeTimeout(Duration.ofMillis(route.timeoutMs()))
                .callTimeout(Duration.ofMillis(route.timeoutMs()))
                .build();

        Request request = new Request.Builder()
                .url(route.baseUrl() + path)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + route.providerToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .post(RequestBody.create(body, okhttp3.MediaType.parse("application/json; charset=utf-8")))
                .build();

        try {
            return client.newCall(request).execute();
        } catch (Exception ex) {
            String message = buildUpstreamErrorMessage(route, path, ex);
            log.warn(message, ex);
            throw new BusinessException(isTimeoutException(ex) ? 504 : 502, message);
        }
    }

    private UpstreamTextResponse executeOpenAiRequestWithOkHttp(GatewayRouteService.RouteDefinition route, String path, String body) throws Exception {
        try (Response response = executeOpenAiRequest(route, path, body)) {
            return new UpstreamTextResponse(
                    response.code(),
                    response.body() == null ? "" : response.body().string()
            );
        }
    }

    private UpstreamTextResponse executeOpenAiTextRequest(GatewayRouteService.RouteDefinition route, String path, String body) throws Exception {
        return isOfficialOpenAiRoute(route)
                ? executeOpenAiRequestWithPowerShell(route, path, body)
                : executeOpenAiRequestWithOkHttp(route, path, body);
    }

    private UpstreamTextResponse executeOpenAiRequestWithPowerShell(GatewayRouteService.RouteDefinition route, String path, String body) throws Exception {
        Path bodyFile = Files.createTempFile("zxw-openai-body-", ".json");
        try {
            Files.writeString(bodyFile, body, StandardCharsets.UTF_8);

            String script = """
                    $headers = @{ Authorization = ('Bearer ' + $env:ZXW_OPENAI_TOKEN) }
                    $body = [System.IO.File]::ReadAllText($env:ZXW_OPENAI_BODY, [System.Text.Encoding]::UTF8)
                    try {
                        $resp = Invoke-WebRequest -Method Post -Uri $env:ZXW_OPENAI_URL -Headers $headers -ContentType 'application/json' -Body $body -ErrorAction Stop
                        [PSCustomObject]@{ statusCode = [int]$resp.StatusCode; content = $resp.Content } | ConvertTo-Json -Compress -Depth 5
                    } catch {
                        $resp = $_.Exception.Response
                        if ($resp) {
                            $stream = $resp.GetResponseStream()
                            $reader = New-Object System.IO.StreamReader($stream)
                            $text = $reader.ReadToEnd()
                            $reader.Close()
                            [PSCustomObject]@{ statusCode = [int]$resp.StatusCode; content = $text } | ConvertTo-Json -Compress -Depth 5
                        } else {
                            throw
                        }
                    }
                    """;

            ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script);
            processBuilder.environment().put("ZXW_OPENAI_TOKEN", route.providerToken());
            processBuilder.environment().put("ZXW_OPENAI_URL", route.baseUrl() + path);
            processBuilder.environment().put("ZXW_OPENAI_BODY", bodyFile.toString());

            Process process = processBuilder.start();
            String stdout;
            String stderr;
            try (InputStream stdoutStream = process.getInputStream();
                 InputStream stderrStream = process.getErrorStream()) {
                stdout = new String(readAllBytes(stdoutStream), StandardCharsets.UTF_8).trim();
                stderr = new String(readAllBytes(stderrStream), StandardCharsets.UTF_8).trim();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && stdout.isBlank()) {
                throw new BusinessException(502, "OpenAI 上游请求失败: " + (stderr.isBlank() ? "PowerShell exited with " + exitCode : stderr));
            }

            JsonNode result = objectMapper.readTree(stdout);
            return new UpstreamTextResponse(
                    result.path("statusCode").asInt(500),
                    result.path("content").asText("")
            );
        } finally {
            Files.deleteIfExists(bodyFile);
        }
    }

    private boolean isOfficialOpenAiRoute(GatewayRouteService.RouteDefinition route) {
        return route.baseUrl() != null && route.baseUrl().toLowerCase(Locale.ROOT).contains("api.openai.com");
    }

    private String buildUpstreamErrorMessage(GatewayRouteService.RouteDefinition route, String path, Exception ex) {
        String category = isTimeoutException(ex) ? "upstream timeout" : "upstream request failed";
        return category
                + ": provider=" + safeValue(route.providerName())
                + ", model=" + safeValue(route.modelCode())
                + ", upstreamModel=" + safeValue(route.upstreamModel())
                + ", url=" + safeValue(route.baseUrl()) + safeValue(path)
                + ", timeoutMs=" + route.timeoutMs()
                + ", reason=" + describeException(ex);
    }

    private boolean isTimeoutException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String className = current.getClass().getName();
            if (className.contains("Timeout")
                    || className.contains("InterruptedIOException")
                    || className.contains("SocketTimeout")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String describeException(Throwable ex) {
        List<String> parts = new ArrayList<>();
        Throwable current = ex;
        while (current != null) {
            String type = current.getClass().getSimpleName();
            String message = current.getMessage();
            String text = (message == null || message.isBlank()) ? type : type + ": " + message;
            if (parts.isEmpty() || !parts.get(parts.size() - 1).equals(text)) {
                parts.add(text);
            }
            current = current.getCause();
        }
        return parts.isEmpty() ? "Unknown error" : String.join(" <- ", parts);
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private boolean shouldAggregateCodexStream(GatewayRouteService.RouteDefinition route) {
        return !isOfficialOpenAiRoute(route)
                && route.upstreamModel() != null
                && route.upstreamModel().toLowerCase(Locale.ROOT).contains("codex");
    }

    private boolean shouldKeepNativeResponsesRouting(GatewayRouteService.RouteDefinition route) {
        if (route == null) {
            return false;
        }
        String publicModel = route.modelCode() == null ? "" : route.modelCode().toLowerCase(Locale.ROOT);
        String upstreamModel = route.upstreamModel() == null ? "" : route.upstreamModel().toLowerCase(Locale.ROOT);
        return publicModel.contains("codex")
                || publicModel.contains("gpt-5.4")
                || upstreamModel.contains("codex")
                || upstreamModel.contains("gpt-5.4");
    }

    private boolean shouldUseResponsesCompatibility(GatewayRouteService.RouteDefinition route) {
        return route != null
                && !isAnthropicRoute(route)
                && !isOfficialOpenAiRoute(route)
                && !shouldKeepNativeResponsesRouting(route)
                && !shouldAggregateCodexStream(route);
    }

    private UpstreamTextResponse executeResponsesCompatibility(GatewayRouteService.RouteDefinition route, String body) throws Exception {
        ObjectNode chatRequest = buildChatCompletionsRequestFromResponses((ObjectNode) objectMapper.readTree(body), route.upstreamModel(), route.modelCode());
        UpstreamTextResponse chatResponse = executeOpenAiTextRequest(route, "/chat/completions", objectMapper.writeValueAsString(chatRequest));
        String chatBody = rewriteClientFacingModel(route, normalizeUpstreamResponseBody(route, chatResponse.statusCode(), chatResponse.body()));
        if (chatResponse.statusCode() < 200 || chatResponse.statusCode() >= 300) {
            return new UpstreamTextResponse(chatResponse.statusCode(), chatBody);
        }

        JsonNode chatJson = tryReadJson(chatBody);
        if (chatJson == null) {
            return new UpstreamTextResponse(502, "{\"error\":{\"message\":\"Failed to convert chat completion to responses output\",\"type\":\"server_error\"}}");
        }
        return new UpstreamTextResponse(chatResponse.statusCode(), objectMapper.writeValueAsString(buildResponsesFromChatCompletion(chatJson, route.modelCode())));
    }

    private UpstreamTextResponse executeResponsesCompatibilityStream(GatewayRouteService.RouteDefinition route, String body) throws Exception {
        UpstreamTextResponse response = executeResponsesCompatibility(route, body);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return new UpstreamTextResponse(response.statusCode(), buildResponsesErrorEventStream(response.statusCode(), extractUpstreamErrorMessage(response.body())));
        }

        JsonNode responseJson = tryReadJson(response.body());
        if (responseJson == null) {
            return new UpstreamTextResponse(502, buildResponsesErrorEventStream(502, "响应格式转换失败"));
        }
        return new UpstreamTextResponse(response.statusCode(), buildResponsesEventStreamFromResponseJson(responseJson));
    }

    private UpstreamTextResponse executeCodexChatByStreaming(GatewayRouteService.RouteDefinition route, String body) throws Exception {
        ObjectNode streamRequest = ensureChatStreamUsageIncluded((ObjectNode) objectMapper.readTree(body));

        try (Response response = executeOpenAiRequest(route, "/chat/completions", objectMapper.writeValueAsString(streamRequest))) {
            String rawBody = response.body() == null ? "" : response.body().string();
            if (response.code() < 200 || response.code() >= 300) {
                return new UpstreamTextResponse(response.code(), rawBody);
            }
            return new UpstreamTextResponse(response.code(), buildChatCompletionFromSse(route, rawBody));
        }
    }

    private UpstreamTextResponse executeCodexResponsesByStreaming(GatewayRouteService.RouteDefinition route, String body) throws Exception {
        ObjectNode chatRequest = buildChatCompletionsRequestFromResponses((ObjectNode) objectMapper.readTree(body), route.upstreamModel(), route.modelCode());
        UpstreamTextResponse chatResponse = executeCodexChatByStreaming(route, objectMapper.writeValueAsString(chatRequest));
        if (chatResponse.statusCode() < 200 || chatResponse.statusCode() >= 300) {
            return chatResponse;
        }
        JsonNode chatJson = tryReadJson(chatResponse.body());
        if (chatJson == null) {
            return new UpstreamTextResponse(502, "{\"error\":{\"message\":\"Failed to convert chat completion to responses output\",\"type\":\"server_error\"}}");
        }
        return new UpstreamTextResponse(chatResponse.statusCode(), objectMapper.writeValueAsString(buildResponsesFromChatCompletion(chatJson, route.modelCode())));
    }

    private String buildChatCompletionFromSse(GatewayRouteService.RouteDefinition route, String sseBody) throws Exception {
        StringBuilder contentBuilder = new StringBuilder();
        String responseId = buildRequestId();
        long created = Instant.now().getEpochSecond();
        String finishReason = "stop";
        String nativeFinishReason = "stop";
        JsonNode usage = null;
        Map<Integer, ObjectNode> toolCallMap = new LinkedHashMap<>();

        for (String rawLine : sseBody.split("\\r?\\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.startsWith("data:")) {
                continue;
            }

            String payload = line.substring(5).trim();
            if (payload.isBlank() || "[DONE]".equals(payload)) {
                continue;
            }

            JsonNode chunk = tryReadJson(payload);
            if (chunk == null) {
                continue;
            }

            responseId = chunk.path("id").asText(responseId);
            created = chunk.path("created").asLong(created);
            if (chunk.has("usage") && !chunk.path("usage").isNull()) {
                usage = chunk.path("usage");
            }

            JsonNode choice = chunk.path("choices").isArray() && !chunk.path("choices").isEmpty()
                    ? chunk.path("choices").get(0)
                    : null;
            if (choice == null) {
                continue;
            }

            String deltaText = choice.path("delta").path("content").asText("");
            if (!deltaText.isEmpty()) {
                contentBuilder.append(deltaText);
            }

            JsonNode deltaToolCalls = choice.path("delta").path("tool_calls");
            if (deltaToolCalls.isArray()) {
                for (JsonNode toolCallDelta : deltaToolCalls) {
                    int index = toolCallDelta.path("index").asInt(toolCallMap.size());
                    ObjectNode toolCall = toolCallMap.computeIfAbsent(index, ignored -> {
                        ObjectNode createdToolCall = objectMapper.createObjectNode();
                        createdToolCall.put("id", "");
                        createdToolCall.put("type", "function");
                        ObjectNode function = objectMapper.createObjectNode();
                        function.put("name", "");
                        function.put("arguments", "");
                        createdToolCall.set("function", function);
                        return createdToolCall;
                    });

                    if (toolCallDelta.hasNonNull("id")) {
                        toolCall.put("id", toolCallDelta.path("id").asText(""));
                    }
                    JsonNode functionDelta = toolCallDelta.path("function");
                    if (functionDelta.isObject()) {
                        ObjectNode function = (ObjectNode) toolCall.path("function");
                        if (functionDelta.hasNonNull("name")) {
                            function.put("name", functionDelta.path("name").asText(""));
                        }
                        if (functionDelta.hasNonNull("arguments")) {
                            function.put("arguments", function.path("arguments").asText("") + functionDelta.path("arguments").asText(""));
                        }
                    }
                }
            }

            if (choice.hasNonNull("finish_reason")) {
                finishReason = choice.path("finish_reason").asText(finishReason);
            }
            if (choice.hasNonNull("native_finish_reason")) {
                nativeFinishReason = choice.path("native_finish_reason").asText(nativeFinishReason);
            }
        }

        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "assistant");
        message.put("content", contentBuilder.toString());
        if (!toolCallMap.isEmpty()) {
            ArrayNode toolCalls = objectMapper.createArrayNode();
            toolCallMap.values().forEach(toolCalls::add);
            message.set("tool_calls", toolCalls);
        }

        ObjectNode choice = objectMapper.createObjectNode();
        choice.put("index", 0);
        choice.set("message", message);
        choice.put("finish_reason", finishReason);
        choice.put("native_finish_reason", nativeFinishReason);

        ArrayNode choices = objectMapper.createArrayNode();
        choices.add(choice);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", responseId);
        response.put("object", "chat.completion");
        response.put("created", created);
        response.put("model", route.modelCode());
        response.set("choices", choices);
        if (usage != null) {
            response.set("usage", usage);
        }
        return objectMapper.writeValueAsString(response);
    }

    private String buildResponsesFromSse(GatewayRouteService.RouteDefinition route, String sseBody) throws Exception {
        StringBuilder outputText = new StringBuilder();
        String responseId = buildRequestId();
        long createdAt = Instant.now().getEpochSecond();
        JsonNode usage = null;
        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "");

        for (String rawLine : sseBody.split("\\r?\\n")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (!line.startsWith("data:")) {
                continue;
            }

            String payload = line.substring(5).trim();
            if (payload.isBlank() || "[DONE]".equals(payload)) {
                continue;
            }

            JsonNode event = tryReadJson(payload);
            if (event == null) {
                continue;
            }

            String type = event.path("type").asText("");
            if ("response.output_text.delta".equals(type)) {
                outputText.append(event.path("delta").asText(""));
                continue;
            }
            if ("response.output_text.done".equals(type) && outputText.isEmpty()) {
                outputText.append(event.path("text").asText(""));
                continue;
            }
            if ("response.output_item.added".equals(type) || "response.output_item.done".equals(type)) {
                JsonNode item = event.path("item");
                if ("message".equals(item.path("type").asText(""))) {
                    messageId = item.path("id").asText(messageId);
                    String itemText = extractResponseOutputText(item.path("content"));
                    if (!itemText.isBlank()) {
                        outputText.setLength(0);
                        outputText.append(itemText);
                    }
                }
                continue;
            }
            if ("response.created".equals(type) || "response.in_progress".equals(type) || "response.completed".equals(type)) {
                JsonNode response = event.path("response");
                responseId = response.path("id").asText(responseId);
                createdAt = response.path("created_at").asLong(createdAt);
                if (response.has("usage") && !response.path("usage").isNull()) {
                    usage = response.path("usage");
                }
            }
        }

        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "output_text");
        textPart.put("text", outputText.toString());
        textPart.set("annotations", objectMapper.createArrayNode());

        ArrayNode content = objectMapper.createArrayNode();
        content.add(textPart);

        ObjectNode message = objectMapper.createObjectNode();
        message.put("id", messageId);
        message.put("type", "message");
        message.put("status", "completed");
        message.put("role", "assistant");
        message.set("content", content);

        ArrayNode output = objectMapper.createArrayNode();
        output.add(message);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", responseId);
        response.put("object", "response");
        response.put("created_at", createdAt);
        response.put("status", "completed");
        response.put("model", route.modelCode());
        response.set("output", output);
        if (usage != null) {
            response.set("usage", usage);
        }
        return objectMapper.writeValueAsString(response);
    }

    private String normalizeUpstreamResponseBody(GatewayRouteService.RouteDefinition route, int statusCode, String body) throws Exception {
        if (body != null && !body.isBlank()) {
            return body;
        }
        if (isOfficialOpenAiRoute(route) && statusCode == 429) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", "OpenAI upstream returned 429. Please check project billing, quota, or rate limits.");
            error.put("type", "rate_limit_error");

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("error", error);
            return objectMapper.writeValueAsString(wrapper);
        }
        if (isOfficialOpenAiRoute(route) && statusCode == 401) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", "OpenAI upstream returned 401. Please check whether the provider API key is valid.");
            error.put("type", "invalid_api_key");

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("error", error);
            return objectMapper.writeValueAsString(wrapper);
        }
        return body == null ? "" : body;
    }

    private String rewriteClientFacingModel(GatewayRouteService.RouteDefinition route, String body) throws Exception {
        if (body == null || body.isBlank() || route == null || route.modelCode() == null || route.modelCode().isBlank()) {
            return body == null ? "" : body;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("data:")) {
            return rewriteSseModelField(body, route.modelCode());
        }
        return rewriteJsonModelField(body, route.modelCode());
    }

    private String rewriteJsonModelField(String body, String modelCode) throws Exception {
        JsonNode json = tryReadJson(body);
        if (json == null || !json.isObject()) {
            return body;
        }
        ObjectNode copy = ((ObjectNode) json).deepCopy();
        if (copy.has("model")) {
            copy.put("model", modelCode);
        }
        JsonNode responseNode = copy.path("response");
        if (responseNode.isObject() && responseNode.has("model")) {
            ((ObjectNode) responseNode).put("model", modelCode);
        }
        return objectMapper.writeValueAsString(copy);
    }

    private String rewriteSseModelField(String eventStreamBody, String modelCode) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (String rawLine : eventStreamBody.split("(?<=\\n)")) {
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                builder.append(line);
                continue;
            }

            String payload = trimmed.substring(5).trim();
            if (payload.isBlank() || "[DONE]".equals(payload)) {
                builder.append(line);
                continue;
            }

            JsonNode json = tryReadJson(payload);
            if (json == null || !json.isObject()) {
                builder.append(line);
                continue;
            }

            ObjectNode copy = ((ObjectNode) json).deepCopy();
            if (copy.has("model")) {
                copy.put("model", modelCode);
            }
            JsonNode responseNode = copy.path("response");
            if (responseNode.isObject() && responseNode.has("model")) {
                ((ObjectNode) responseNode).put("model", modelCode);
            }

            String newline = line.endsWith("\r\n") ? "\r\n" : (line.endsWith("\n") ? "\n" : "");
            builder.append("data: ").append(objectMapper.writeValueAsString(copy)).append(newline);
        }
        return builder.toString();
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private HttpRequest baseHttpRequest(GatewayRouteService.RouteDefinition route, String path, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(route.baseUrl() + path))
                .timeout(Duration.ofMillis(route.timeoutMs()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + route.providerToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest anthropicHttpRequest(GatewayRouteService.RouteDefinition route, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(resolveAnthropicEndpoint(route.baseUrl())))
                .timeout(Duration.ofMillis(route.timeoutMs()))
                .header("x-api-key", route.providerToken())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + route.providerToken())
                .header("anthropic-version", "2023-06-01")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private ObjectNode buildAnthropicRequest(JsonNode input, String upstreamModel) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", upstreamModel);
        request.put("max_tokens", input.path("max_tokens").asInt(1024));

        if (input.hasNonNull("temperature")) {
            request.set("temperature", input.get("temperature"));
        }
        if (input.hasNonNull("top_p")) {
            request.set("top_p", input.get("top_p"));
        }

        ArrayNode stopSequences = normalizeStopSequences(input.get("stop"));
        if (stopSequences != null && !stopSequences.isEmpty()) {
            request.set("stop_sequences", stopSequences);
        }

        String systemText = collectSystemPrompt(input.path("messages"));
        if (!systemText.isBlank()) {
            request.put("system", systemText);
        }

        ArrayNode messages = objectMapper.createArrayNode();
        for (JsonNode item : input.path("messages")) {
            String role = item.path("role").asText("");
            if ("system".equals(role) || role.isBlank()) {
                continue;
            }
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }

            String content = flattenMessageContent(item.get("content"));
            if (content.isBlank()) {
                continue;
            }

            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", role);
            message.put("content", content);
            messages.add(message);
        }

        if (messages.isEmpty()) {
            throw new BusinessException(400, "messages 娑撳秷鍏樻稉铏光敄");
        }

        request.set("messages", messages);
        return request;
    }

    private void copyResponsesToolConfig(ObjectNode input, ObjectNode request) {
        if (input == null || request == null) {
            return;
        }
        if (input.hasNonNull("tools") && input.path("tools").isArray()) {
            request.set("tools", transformResponsesToolsToChatTools((ArrayNode) input.get("tools")));
        }
        if (input.has("tool_choice") && !input.path("tool_choice").isNull()) {
            request.set("tool_choice", transformResponsesToolChoiceToChatToolChoice(input.get("tool_choice")));
        }
        if (input.has("parallel_tool_calls") && !input.path("parallel_tool_calls").isNull()) {
            request.set("parallel_tool_calls", input.get("parallel_tool_calls").deepCopy());
        }
    }

    private ArrayNode transformResponsesToolsToChatTools(ArrayNode tools) {
        ArrayNode transformed = objectMapper.createArrayNode();
        for (JsonNode tool : tools) {
            if (!"function".equals(tool.path("type").asText(""))) {
                transformed.add(tool.deepCopy());
                continue;
            }

            ObjectNode item = objectMapper.createObjectNode();
            item.put("type", "function");
            ObjectNode function = objectMapper.createObjectNode();
            function.put("name", tool.path("name").asText(""));
            if (tool.hasNonNull("description")) {
                function.put("description", tool.path("description").asText(""));
            }
            JsonNode parameters = tool.get("parameters");
            function.set("parameters", parameters == null || parameters.isNull() ? objectMapper.createObjectNode() : parameters.deepCopy());
            item.set("function", function);
            transformed.add(item);
        }
        return transformed;
    }

    private JsonNode transformResponsesToolChoiceToChatToolChoice(JsonNode toolChoice) {
        if (toolChoice == null || toolChoice.isNull()) {
            return null;
        }
        if (toolChoice.isTextual()) {
            return toolChoice.deepCopy();
        }
        if (!toolChoice.isObject()) {
            return toolChoice.deepCopy();
        }

        ObjectNode object = (ObjectNode) toolChoice;
        if (!"function".equals(object.path("type").asText(""))) {
            return object.deepCopy();
        }

        ObjectNode transformed = objectMapper.createObjectNode();
        transformed.put("type", "function");
        ObjectNode function = objectMapper.createObjectNode();
        JsonNode functionNode = object.path("function");
        if (functionNode.isObject()) {
            function.put("name", functionNode.path("name").asText(""));
        } else if (object.hasNonNull("name")) {
            function.put("name", object.path("name").asText(""));
        }
        transformed.set("function", function);
        return transformed;
    }

    private ObjectNode buildOpenAiChatRequestFromAnthropic(JsonNode input, String upstreamModel) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", upstreamModel);
        request.put("stream", false);

        if (input.hasNonNull("max_tokens")) {
            request.set("max_tokens", input.get("max_tokens"));
        }
        if (input.hasNonNull("temperature")) {
            request.set("temperature", input.get("temperature"));
        }
        if (input.hasNonNull("top_p")) {
            request.set("top_p", input.get("top_p"));
        }

        JsonNode stopSequences = input.get("stop_sequences");
        if (stopSequences != null && !stopSequences.isNull()) {
            request.set("stop", stopSequences);
        }

        ArrayNode messages = objectMapper.createArrayNode();
        String systemText = flattenMessageContent(input.get("system"));
        if (!systemText.isBlank()) {
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemText);
            messages.add(systemMessage);
        }

        for (JsonNode item : input.path("messages")) {
            String role = item.path("role").asText("");
            if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                continue;
            }

            String content = flattenMessageContent(item.get("content"));
            if (content.isBlank()) {
                continue;
            }

            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", role);
            message.put("content", content);
            messages.add(message);
        }

        if (messages.isEmpty()) {
            throw new BusinessException(400, "messages cannot be empty");
        }

        request.set("messages", messages);
        return request;
    }

    private ObjectNode buildOpenAiResponseFromAnthropic(JsonNode anthropicResponse, String modelCode) {
        int promptTokens = anthropicResponse.path("usage").path("input_tokens").asInt(0);
        int completionTokens = anthropicResponse.path("usage").path("output_tokens").asInt(0);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", anthropicResponse.path("id").asText(buildRequestId()));
        response.put("object", "chat.completion");
        response.put("created", Instant.now().getEpochSecond());
        response.put("model", modelCode);

        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "assistant");
        message.put("content", extractAnthropicText(anthropicResponse.path("content")));

        ObjectNode choice = objectMapper.createObjectNode();
        choice.set("message", message);
        choice.put("index", 0);
        choice.put("finish_reason", mapAnthropicStopReason(anthropicResponse.path("stop_reason").asText("end_turn")));

        ArrayNode choices = objectMapper.createArrayNode();
        choices.add(choice);
        response.set("choices", choices);

        ObjectNode usage = objectMapper.createObjectNode();
        usage.put("prompt_tokens", promptTokens);
        usage.put("completion_tokens", completionTokens);
        usage.put("total_tokens", promptTokens + completionTokens);
        response.set("usage", usage);
        return response;
    }

    private ObjectNode buildChatCompletionsRequestFromResponses(ObjectNode input, String upstreamModel, String modelCode) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", upstreamModel);
        request.put("stream", false);

        boolean hasTokenLimit = false;
        if (input.hasNonNull("temperature")) {
            request.set("temperature", input.get("temperature"));
        }
        if (input.hasNonNull("top_p")) {
            request.set("top_p", input.get("top_p"));
        }
        if (input.hasNonNull("max_output_tokens")) {
            request.set("max_tokens", input.get("max_output_tokens"));
            hasTokenLimit = true;
        } else if (input.hasNonNull("max_tokens")) {
            request.set("max_tokens", input.get("max_tokens"));
            hasTokenLimit = true;
        }
        if (input.hasNonNull("stop")) {
            request.set("stop", input.get("stop"));
        }

        boolean hasExplicitSystemPrompt = hasExplicitResponsesInstructions(input);
        ArrayNode messages = buildMessagesFromResponsesInput(input);
        if (!hasExplicitSystemPrompt) {
            prependClientFacingModelIdentity(messages, modelCode);
        }
        boolean lightweightSimpleQuery = shouldUseLightweightCodexPrompt(modelCode, messages);
        if (lightweightSimpleQuery) {
            messages = buildLightweightCodexMessages(messages);
            if (!hasTokenLimit) {
                request.put("max_tokens", 128);
            }
            if (!input.hasNonNull("temperature")) {
                request.put("temperature", 0.2);
            }
        }

        if (messages.isEmpty()) {
            throw new BusinessException(400, "responses input cannot be empty");
        }

        request.set("messages", messages);
        copyResponsesToolConfig(input, request);
        applyIdeAgentDefaults(request, modelCode);
        return request;
    }

    private void applyClientFacingModelIdentity(ObjectNode request, String modelCode) {
        if (request == null || modelCode == null || modelCode.isBlank()) {
            return;
        }
        JsonNode messagesNode = request.path("messages");
        if (!(messagesNode instanceof ArrayNode messages)) {
            return;
        }
        prependClientFacingModelIdentity(messages, modelCode);
    }

    private void prependClientFacingModelIdentity(ArrayNode messages, String modelCode) {
        if (messages == null || modelCode == null || modelCode.isBlank()) {
            return;
        }
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", """
                你是 %s。用户问你是什么模型就说 %s。不提上游供应商和网关。用简体中文。

                ## 对话风格

                像一个资深同事在帮忙，不是在写报告。

                调用工具前，content 里必须写一句话说你要做什么，不能为空。
                例如："我先看看这个文件的结构。" 或 "让我搜一下相关的引用。"

                最终回答要自然、详细、有条理。具体要求：

                1. 开头先自然地说一下你的发现或结论（像在跟人聊天）
                2. 然后用要点展开，每个要点单独一行，前面加序号或圆点
                3. 文件路径和代码用 `反引号`
                4. 每个要点要解释清楚是什么、为什么，不能只列名字
                5. 最后一段给下一步建议
                6. 回答不能少于 6 句话

                好的回答示范：

                我看了一下整个项目，这是一个基于 Spring Boot + MongoDB 的服务端项目，结构比较清晰。

                主要发现：

                1. 入口类在 `src/main/java/com/sl/MongoDBApplication.java`，标准的 Spring Boot 启动类，没有额外自定义逻辑
                2. 实体层有两个类：`Person.java` 用了 `@Document` 和 `@GeoSpatialIndexed` 做 MongoDB 映射；`Address.java` 是一个简单值对象，只有三个 String 字段
                3. 服务层的 `PersonServiceImpl.java` 注入了 `MongoTemplate`，实现了 CRUD 和分页查询
                4. 配置文件 `application.yml` 连接的是 `192.168.150.101:27017` 的 MongoDB，数据库名是 `sl`
                5. 目前没有 REST 控制层，服务接口还没暴露为 API

                下一步可以考虑加一个 `PersonController` 把服务暴露出去，或者先补充单元测试。

                坏的回答示范（不能这样写）：
                "这是一个 Spring Boot MongoDB 项目，包含实体、服务和配置。如需了解更多请告诉我。"
                ← 太短、太敷衍、没有任何有价值的细节
                """.formatted(modelCode, modelCode).trim());
        messages.insert(0, systemMessage);
    }

    private void applyIdeAgentDefaults(ObjectNode request, String modelCode) {
        // IDE clients should be forwarded as-is. Do not inject server-side agent defaults.
    }

    private boolean applyDefaultIdeInstructionsToChat(ObjectNode request, String modelCode, boolean hasExplicitSystemPrompt) {
        return false;
    }

    private void applyGeneralAssistantDefaults(ObjectNode request, String modelCode) {
        if (request == null || !shouldUseAgentMode(modelCode)) {
            return;
        }
        JsonNode messagesNode = request.path("messages");
        if (!(messagesNode instanceof ArrayNode messages) || messages.isEmpty()) {
            return;
        }
        if (looksLikeIdeCodingTask(messages) || hasRequestTools(request) || hasSystemLikeMessage(messages)) {
            return;
        }

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", """
                You are a helpful assistant.
                - Answer in simplified Chinese by default.
                - When the user question is already clear, answer directly instead of asking for clarification.
                - Prefer concise, practical explanations.
                """);
        messages.insert(0, systemMessage);

        if (!request.hasNonNull("temperature")) {
            request.put("temperature", 0.3);
        }
        if (!request.hasNonNull("max_tokens")) {
            request.put("max_tokens", 2000);
        }
    }

    private boolean hasSystemLikeMessage(ArrayNode messages) {
        if (messages == null) {
            return false;
        }
        for (JsonNode message : messages) {
            String role = message.path("role").asText("");
            if ("system".equalsIgnoreCase(role) || "developer".equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExplicitChatSystemPrompt(JsonNode input) {
        if (!(input instanceof ObjectNode objectNode)) {
            return false;
        }
        JsonNode messagesNode = objectNode.path("messages");
        if (!(messagesNode instanceof ArrayNode messages)) {
            return false;
        }
        return hasSystemLikeMessage(messages);
    }

    private boolean hasRequestTools(ObjectNode request) {
        JsonNode tools = request.path("tools");
        return tools != null && tools.isArray() && !tools.isEmpty();
    }

    private boolean looksLikeIdeCodingTask(ArrayNode messages) {
        String text = flattenMessagesForIntent(messages).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return false;
        }
        return text.contains("code")
                || text.contains("debug")
                || text.contains("bug")
                || text.contains("fix")
                || text.contains("refactor")
                || text.contains("compile")
                || text.contains("build")
                || text.contains("test")
                || text.contains("stack trace")
                || text.contains("exception")
                || text.contains("java")
                || text.contains("python")
                || text.contains("typescript")
                || text.contains("javascript")
                || text.contains("spring")
                || text.contains("maven")
                || text.contains("gradle")
                || text.contains("代码")
                || text.contains("报错")
                || text.contains("编译")
                || text.contains("构建")
                || text.contains("调试")
                || text.contains("修复")
                || text.contains("排查")
                || text.contains("重构")
                || text.contains("测试")
                || text.contains("项目")
                || text.contains("文件")
                || text.contains("接口");
    }

    private String flattenMessagesForIntent(ArrayNode messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode message : messages) {
            String text = flattenMessageContent(message.path("content"));
            if (text.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private ArrayNode buildMessagesFromResponsesInput(ObjectNode input) {
        ArrayNode messages = objectMapper.createArrayNode();

        String instructions = flattenResponsesInputText(input.path("instructions"));
        if (!instructions.isBlank()) {
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", sanitizeDeveloperInstructions(instructions));
            messages.add(systemMessage);
        }

        JsonNode inputNode = input.path("input");
        if (inputNode == null || inputNode.isNull()) {
            return messages;
        }

        if (inputNode.isTextual()) {
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", inputNode.asText(""));
            messages.add(userMessage);
            return messages;
        }

        if (!inputNode.isArray()) {
            String text = flattenResponsesInputText(inputNode);
            if (!text.isBlank()) {
                ObjectNode userMessage = objectMapper.createObjectNode();
                userMessage.put("role", "user");
                userMessage.put("content", text);
                messages.add(userMessage);
            }
            return messages;
        }

        for (JsonNode item : inputNode) {
            if (item == null || item.isNull()) {
                continue;
            }

            String itemType = item.path("type").asText("");
            if ("function_call".equals(itemType)) {
                ObjectNode assistantMessage = objectMapper.createObjectNode();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", "");

                ArrayNode toolCalls = objectMapper.createArrayNode();
                ObjectNode toolCall = objectMapper.createObjectNode();
                toolCall.put("id", firstNonBlank(item.path("call_id").asText(""), item.path("id").asText("call_" + UUID.randomUUID().toString().replace("-", ""))));
                toolCall.put("type", "function");
                ObjectNode function = objectMapper.createObjectNode();
                function.put("name", item.path("name").asText(""));
                function.put("arguments", item.path("arguments").asText("{}"));
                toolCall.set("function", function);
                toolCalls.add(toolCall);
                assistantMessage.set("tool_calls", toolCalls);
                messages.add(assistantMessage);
                continue;
            }

            if ("function_call_output".equals(itemType)) {
                ObjectNode toolMessage = objectMapper.createObjectNode();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", firstNonBlank(item.path("call_id").asText(""), item.path("id").asText("")));
                toolMessage.put("content", firstNonBlank(item.path("output").asText(""), flattenResponsesInputText(item.path("content"))));
                messages.add(toolMessage);
                continue;
            }

            if ("message".equals(item.path("type").asText("")) || item.has("role")) {
                String role = mapResponsesRole(item.path("role").asText("user"));
                String content = flattenResponsesInputText(item.path("content"));
                if (content.isBlank()) {
                    continue;
                }
                ObjectNode message = objectMapper.createObjectNode();
                message.put("role", role);
                message.put("content", "system".equals(role) ? sanitizeDeveloperInstructions(content) : content);
                messages.add(message);
                continue;
            }

            String text = flattenResponsesInputText(item);
            if (text.isBlank()) {
                continue;
            }
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", text);
            messages.add(userMessage);
        }

        return messages;
    }

    private void applyDefaultIdeInstructionsToResponses(ObjectNode input, String modelCode) {
        // Keep Responses requests untouched so IDE clients can use their own tool and workflow prompts.
    }

    private boolean hasExplicitResponsesInstructions(ObjectNode input) {
        if (input == null) {
            return false;
        }
        if (!flattenResponsesInputText(input.path("instructions")).isBlank()) {
            return true;
        }
        JsonNode inputNode = input.path("input");
        if (!(inputNode instanceof ArrayNode items)) {
            return false;
        }
        for (JsonNode item : items) {
            String role = item.path("role").asText("");
            if ("system".equalsIgnoreCase(role) || "developer".equalsIgnoreCase(role)) {
                String content = flattenResponsesInputText(item.path("content"));
                if (!content.isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String mapResponsesRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if ("assistant".equals(normalized)) {
            return "assistant";
        }
        if ("system".equals(normalized) || "developer".equals(normalized)) {
            return "system";
        }
        return "user";
    }

    private boolean shouldUseLightweightCodexPrompt(String modelCode, ArrayNode messages) {
        return false;
    }

    private ArrayNode buildLightweightCodexMessages(ArrayNode messages) {
        String latestUser = "";
        for (JsonNode message : messages) {
            if ("user".equals(message.path("role").asText("")) && !message.path("content").asText("").isBlank()) {
                latestUser = message.path("content").asText("");
            }
        }

        ArrayNode optimized = objectMapper.createArrayNode();
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个直接、准确、简洁的中文助手。对于时间、日期、常识和简短问答，直接给出结果，不要先解释工具、权限、沙箱或内部提示。");
        optimized.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", latestUser);
        optimized.add(userMessage);
        return optimized;
    }

    private boolean looksLikeSimpleGeneralQuestion(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim();
        if (normalized.isBlank() || normalized.length() > 80) {
            return false;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("```") || lower.contains("powershell") || lower.contains("bash")
                || lower.contains("python") || lower.contains("java") || lower.contains("sql")
                || lower.contains("代码") || lower.contains("脚本") || lower.contains("报错")
                || lower.contains("接口") || lower.contains("api") || lower.contains("curl")
                || lower.contains("{") || lower.contains("}") || lower.contains("<")
                || lower.contains(">") || lower.contains("/") || lower.contains("\\")) {
            return false;
        }

        return lower.contains("几点")
                || lower.contains("时间")
                || lower.contains("日期")
                || lower.contains("今天")
                || lower.contains("明天")
                || lower.contains("天气")
                || lower.contains("你好")
                || lower.contains("在吗")
                || lower.contains("是谁")
                || lower.contains("是什么")
                || lower.contains("干嘛")
                || lower.contains("能做什么");
    }

    private ObjectNode buildResponsesFromChatCompletion(JsonNode chatResponse, String modelCode) {
        JsonNode choice = chatResponse.path("choices").isArray() && !chatResponse.path("choices").isEmpty()
                ? chatResponse.path("choices").get(0)
                : objectMapper.createObjectNode();
        JsonNode usageNode = chatResponse.path("usage");

        ArrayNode output = objectMapper.createArrayNode();
        JsonNode messageNode = choice.path("message");
        String text = flattenMessageContent(messageNode.path("content"));
        if (!text.isBlank()) {
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "output_text");
            textPart.put("text", text);
            textPart.set("annotations", objectMapper.createArrayNode());

            ArrayNode content = objectMapper.createArrayNode();
            content.add(textPart);

            ObjectNode message = objectMapper.createObjectNode();
            message.put("id", "msg_" + UUID.randomUUID().toString().replace("-", ""));
            message.put("type", "message");
            message.put("status", "completed");
            message.put("role", "assistant");
            message.set("content", content);
            output.add(message);
        }

        JsonNode toolCalls = messageNode.path("tool_calls");
        if (toolCalls.isArray()) {
            for (JsonNode toolCall : toolCalls) {
                ObjectNode item = objectMapper.createObjectNode();
                item.put("id", toolCall.path("id").asText("fc_" + UUID.randomUUID().toString().replace("-", "")));
                item.put("type", "function_call");
                item.put("status", "completed");
                item.put("call_id", toolCall.path("id").asText(""));
                item.put("name", toolCall.path("function").path("name").asText(""));
                item.put("arguments", toolCall.path("function").path("arguments").asText("{}"));
                output.add(item);
            }
        }

        ObjectNode usage = objectMapper.createObjectNode();
        int promptTokens = usageNode.path("prompt_tokens").asInt(0);
        int completionTokens = usageNode.path("completion_tokens").asInt(0);
        usage.put("input_tokens", promptTokens);
        usage.put("output_tokens", completionTokens);
        usage.put("total_tokens", usageNode.path("total_tokens").asInt(promptTokens + completionTokens));

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", chatResponse.path("id").asText(buildRequestId()));
        response.put("object", "response");
        response.put("created_at", chatResponse.path("created").asLong(Instant.now().getEpochSecond()));
        response.put("status", "completed");
        response.put("model", modelCode);
        response.set("output", output);
        response.set("usage", usage);
        return response;
    }

    private String buildResponsesEventStreamFromChatCompletion(JsonNode chatResponse, String modelCode) throws Exception {
        ObjectNode completedResponse = buildResponsesFromChatCompletion(chatResponse, modelCode);

        ObjectNode createdEvent = objectMapper.createObjectNode();
        createdEvent.put("type", "response.created");
        ObjectNode createdResponse = completedResponse.deepCopy();
        createdResponse.put("status", "in_progress");
        createdResponse.remove("output");
        createdResponse.remove("usage");
        createdEvent.set("response", createdResponse);

        ObjectNode completedEvent = objectMapper.createObjectNode();
        completedEvent.put("type", "response.completed");
        completedEvent.set("response", completedResponse);

        StringBuilder builder = new StringBuilder()
                .append("data: ").append(objectMapper.writeValueAsString(createdEvent)).append("\n\n");

        JsonNode outputNode = completedResponse.path("output");
        if (outputNode.isArray()) {
            for (int i = 0; i < outputNode.size(); i++) {
                JsonNode item = outputNode.get(i);
                String type = item.path("type").asText("");

                ObjectNode itemAdded = objectMapper.createObjectNode();
                itemAdded.put("type", "response.output_item.added");
                itemAdded.put("output_index", i);
                ObjectNode addedItem = item.deepCopy();
                addedItem.put("status", "in_progress");
                if ("message".equals(type)) {
                    addedItem.set("content", objectMapper.createArrayNode());
                }
                itemAdded.set("item", addedItem);
                builder.append("data: ").append(objectMapper.writeValueAsString(itemAdded)).append("\n\n");

                if ("message".equals(type)) {
                    String messageId = item.path("id").asText("msg_" + UUID.randomUUID().toString().replace("-", ""));
                    String text = extractResponseOutputText(item.path("content"));

                    ObjectNode deltaEvent = objectMapper.createObjectNode();
                    deltaEvent.put("type", "response.output_text.delta");
                    deltaEvent.put("item_id", messageId);
                    deltaEvent.put("output_index", i);
                    deltaEvent.put("content_index", 0);
                    deltaEvent.put("delta", text);
                    builder.append("data: ").append(objectMapper.writeValueAsString(deltaEvent)).append("\n\n");

                    ObjectNode doneEvent = objectMapper.createObjectNode();
                    doneEvent.put("type", "response.output_text.done");
                    doneEvent.put("item_id", messageId);
                    doneEvent.put("output_index", i);
                    doneEvent.put("content_index", 0);
                    doneEvent.put("text", text);
                    builder.append("data: ").append(objectMapper.writeValueAsString(doneEvent)).append("\n\n");
                }

                ObjectNode itemDone = objectMapper.createObjectNode();
                itemDone.put("type", "response.output_item.done");
                itemDone.put("output_index", i);
                itemDone.set("item", item);
                builder.append("data: ").append(objectMapper.writeValueAsString(itemDone)).append("\n\n");
            }
        }

        return builder
                .append("data: ").append(objectMapper.writeValueAsString(completedEvent)).append("\n\n")
                .append("data: [DONE]\n\n")
                .toString();
    }

    private ObjectNode buildAnthropicResponseFromOpenAi(JsonNode openAiResponse, String modelCode) {
        JsonNode choice = openAiResponse.path("choices").isArray() && !openAiResponse.path("choices").isEmpty()
                ? openAiResponse.path("choices").get(0)
                : objectMapper.createObjectNode();
        JsonNode usageNode = openAiResponse.path("usage");

        int promptTokens = usageNode.path("prompt_tokens").asInt(0);
        int completionTokens = usageNode.path("completion_tokens").asInt(0);
        String content = flattenMessageContent(choice.path("message").path("content"));
        String stopReason = mapOpenAiStopReason(choice.path("finish_reason").asText("stop"));

        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", content);

        ArrayNode contentArray = objectMapper.createArrayNode();
        contentArray.add(textPart);

        ObjectNode usage = objectMapper.createObjectNode();
        usage.put("input_tokens", promptTokens);
        usage.put("output_tokens", completionTokens);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", openAiResponse.path("id").asText("msg_" + UUID.randomUUID().toString().replace("-", "")));
        response.put("type", "message");
        response.put("role", "assistant");
        response.put("model", modelCode);
        response.set("content", contentArray);
        response.put("stop_reason", stopReason);
        response.putNull("stop_sequence");
        response.set("usage", usage);
        return response;
    }

    private ArrayNode normalizeStopSequences(JsonNode stopNode) {
        if (stopNode == null || stopNode.isNull()) {
            return null;
        }

        ArrayNode array = objectMapper.createArrayNode();
        if (stopNode.isTextual() && !stopNode.asText().isBlank()) {
            array.add(stopNode.asText());
            return array;
        }

        if (stopNode.isArray()) {
            for (JsonNode item : stopNode) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    array.add(item.asText());
                }
            }
        }
        return array;
    }

    private String collectSystemPrompt(JsonNode messages) {
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : messages) {
            if (!"system".equals(item.path("role").asText(""))) {
                continue;
            }

            String text = flattenMessageContent(item.get("content"));
            if (text.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private String flattenMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if ("text".equals(item.path("type").asText("")) && item.hasNonNull("text")) {
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(item.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        if (contentNode.isObject() && contentNode.hasNonNull("text")) {
            return contentNode.path("text").asText("");
        }
        return contentNode.asText("");
    }

    private String extractAnthropicText(JsonNode contentArray) {
        if (contentArray == null || contentArray.isNull()) {
            return "";
        }
        if (contentArray.isTextual()) {
            return contentArray.asText();
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : contentArray) {
            if (!"text".equals(item.path("type").asText(""))) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(item.path("text").asText(""));
        }
        return builder.toString();
    }

    private String extractResponseOutputText(JsonNode contentArray) {
        if (contentArray == null || contentArray.isNull()) {
            return "";
        }
        if (!contentArray.isArray()) {
            return contentArray.asText("");
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : contentArray) {
            if (!"output_text".equals(item.path("type").asText(""))) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(item.path("text").asText(""));
        }
        return builder.toString();
    }

    private String flattenResponsesInputText(JsonNode inputNode) {
        if (inputNode == null || inputNode.isNull()) {
            return "";
        }
        if (inputNode.isTextual()) {
            return inputNode.asText();
        }
        if (inputNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : inputNode) {
                if (item.isTextual()) {
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(item.asText(""));
                    continue;
                }

                if ("message".equals(item.path("type").asText(""))) {
                    String role = item.path("role").asText("user");
                    String text = flattenResponsesInputText(item.path("content"));
                    if (text.isBlank()) {
                        continue;
                    }
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    if ("assistant".equals(role)) {
                        builder.append("Assistant: ");
                    } else if ("system".equals(role)) {
                        builder.append("System: ");
                    } else {
                        builder.append("User: ");
                    }
                    builder.append(text);
                    continue;
                }

                if ("input_text".equals(item.path("type").asText("")) || "output_text".equals(item.path("type").asText(""))) {
                    String text = item.path("text").asText("");
                    if (text.isBlank()) {
                        continue;
                    }
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(text);
                    continue;
                }

                String fallback = flattenMessageContent(item);
                if (!fallback.isBlank()) {
                    if (builder.length() > 0) {
                        builder.append("\n");
                    }
                    builder.append(fallback);
                }
            }
            return builder.toString();
        }
        if (inputNode.isObject()) {
            if (inputNode.has("text")) {
                return inputNode.path("text").asText("");
            }
            if (inputNode.has("content")) {
                return flattenResponsesInputText(inputNode.path("content"));
            }
        }
        return inputNode.asText("");
    }

    private String sanitizeDeveloperInstructions(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.isBlank()) {
            return "";
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean codexStyle = lower.contains("<permissions instructions>")
                || lower.contains("<collaboration_mode>")
                || lower.contains("<skills_instructions>")
                || lower.contains("you are codex")
                || lower.contains("sandbox")
                || lower.contains("spawn_agent")
                || lower.contains("tool definitions");
        if (codexStyle) {
            String gatewayNote = """

                    Gateway note:
                    - Keep following the client-provided tool definitions and workflow instructions.
                    - Use only tools and tool results that are actually present in the request or returned by the client.
                    - If file, search, edit, or shell tools are available, use them instead of claiming you cannot access local files.
                    - Any workspace operations refer to the user's machine exposed by the client tools, not an invented server workspace.
                    - Do not mention upstream vendors or gateway routing details.
                    """;
            String merged = normalized + gatewayNote;
            return merged.length() > 24000 ? merged.substring(0, 24000) : merged;
        }

        return normalized.length() > 4000 ? normalized.substring(0, 4000) : normalized;
    }

    private String buildDefaultIdeAgentSystemPrompt(String modelCode) {
        return ("""
                你对外显示为 %s。用户问你是什么模型时，回答 %s，不要提上游供应商或网关。

                %s
                """.formatted(modelCode, modelCode, DEFAULT_IDE_AGENT_INSTRUCTIONS)).trim();
    }

    private boolean shouldUseAgentMode(String modelCode) {
        String normalized = modelCode == null ? "" : modelCode.toLowerCase(Locale.ROOT);
        return normalized.contains("codex") || normalized.contains("gpt-5.4");
    }

    private boolean shouldBypassDirectResponsesShortcut(String modelCode, ObjectNode input) {
        if (input == null || !shouldUseAgentMode(modelCode)) {
            return false;
        }
        return hasRequestTools(input)
                || hasWorkspaceHint(input)
                || isExplicitGatewayAgentRequest(input)
                || hasExplicitResponsesInstructions(input);
    }

    private boolean shouldHandleResponsesAsAgent(String modelCode, ObjectNode input) {
        if (input == null || !shouldUseAgentMode(modelCode)) {
            return false;
        }
        if (hasRequestTools(input)) {
            return false;
        }
        if (isExplicitGatewayAgentRequest(input)) {
            return true;
        }

        String latestUserText = extractLatestUserTextFromResponses(input);
        if (looksLikeSimpleGeneralQuestion(latestUserText)) {
            return false;
        }
        if (hasWorkspaceHint(input)) {
            return true;
        }
        return looksLikeWorkspaceAgentRequest(latestUserText);
    }

    private boolean looksLikeWorkspaceAgentRequest(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains(".java")
                || normalized.contains(".kt")
                || normalized.contains(".groovy")
                || normalized.contains(".xml")
                || normalized.contains(".yml")
                || normalized.contains(".yaml")
                || normalized.contains(".properties")
                || normalized.contains(".js")
                || normalized.contains(".ts")
                || normalized.contains(".tsx")
                || normalized.contains(".jsx")
                || normalized.contains(".py")
                || normalized.contains(".go")
                || normalized.contains(".rs")
                || normalized.contains(".cpp")
                || normalized.contains(".c")
                || normalized.contains(".h")
                || normalized.contains("pom.xml")
                || normalized.contains("package.json")
                || normalized.contains("application.yml")
                || normalized.contains("application.yaml")
                || normalized.contains("workspace")
                || normalized.contains("repo")
                || normalized.contains("project")
                || normalized.contains("directory")
                || normalized.contains("folder")
                || normalized.contains("file")
                || normalized.contains("path")
                || normalized.contains("read ")
                || normalized.contains("open ")
                || normalized.contains("scan ")
                || normalized.contains("search ")
                || normalized.contains("edit ")
                || normalized.contains("modify")
                || normalized.contains("change ")
                || normalized.contains("patch")
                || normalized.contains("replace ")
                || normalized.contains("write ")
                || normalized.contains("run ")
                || normalized.contains("build ")
                || normalized.contains("test ")
                || normalized.contains("fix ")
                || normalized.contains("debug")
                || normalized.contains("项目")
                || normalized.contains("仓库")
                || normalized.contains("代码库")
                || normalized.contains("工作区")
                || normalized.contains("目录")
                || normalized.contains("文件")
                || normalized.contains("路径")
                || normalized.contains("读取")
                || normalized.contains("打开")
                || normalized.contains("查看")
                || normalized.contains("扫描")
                || normalized.contains("搜索")
                || normalized.contains("查找")
                || normalized.contains("修改")
                || normalized.contains("改一下")
                || normalized.contains("修一下")
                || normalized.contains("替换")
                || normalized.contains("新增")
                || normalized.contains("创建")
                || normalized.contains("运行")
                || normalized.contains("编译")
                || normalized.contains("构建")
                || normalized.contains("测试")
                || normalized.contains("调试")
                || normalized.contains("排查")
                || normalized.contains("修复");
    }

    private boolean isExplicitGatewayAgentRequest(ObjectNode input) {
        JsonNode metadata = input.path("metadata");
        return isTrueNode(metadata.get("gateway_agent"))
                || isTrueNode(metadata.get("server_agent"))
                || isTrueNode(input.get("gateway_agent"))
                || isTrueNode(input.get("server_agent"));
    }

    private boolean isTrueNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean(false);
        }
        String text = node.asText("");
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private boolean hasWorkspaceHint(ObjectNode input) {
        JsonNode metadata = input.path("metadata");
        return hasNonBlankText(
                metadata.path("workspace_root"),
                metadata.path("cwd"),
                metadata.path("project_root"),
                metadata.path("workspace"),
                metadata.path("root_path"),
                input.path("workspace_root"),
                input.path("cwd"),
                input.path("project_root"),
                input.path("workspace"),
                input.path("root_path")
        );
    }

    private boolean hasNonBlankText(JsonNode... nodes) {
        if (nodes == null) {
            return false;
        }
        for (JsonNode node : nodes) {
            if (node != null && !node.isNull() && !node.asText("").isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String extractLatestUserTextFromResponses(JsonNode input) {
        if (!(input instanceof ObjectNode objectNode)) {
            return "";
        }
        ArrayNode messages = buildMessagesFromResponsesInput(objectNode);
        String latest = "";
        for (JsonNode message : messages) {
            if ("user".equals(message.path("role").asText(""))) {
                String content = message.path("content").asText("");
                if (!content.isBlank()) {
                    latest = content;
                }
            }
        }
        return latest;
    }

    private String extractLatestUserTextFromChat(JsonNode input) {
        if (input == null || input.isNull()) {
            return "";
        }
        String latest = "";
        for (JsonNode item : input.path("messages")) {
            if (!"user".equals(item.path("role").asText(""))) {
                continue;
            }
            String content = flattenMessageContent(item.path("content"));
            if (!content.isBlank()) {
                latest = content;
            }
        }
        return latest;
    }

    private String buildSimpleDirectAnswer(String text) {
        return null;
    }

    private ObjectNode buildSimpleResponsesResponse(String modelCode, String text) {
        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "output_text");
        textPart.put("text", text);
        textPart.set("annotations", objectMapper.createArrayNode());

        ArrayNode content = objectMapper.createArrayNode();
        content.add(textPart);

        ObjectNode message = objectMapper.createObjectNode();
        message.put("id", "msg_" + UUID.randomUUID().toString().replace("-", ""));
        message.put("type", "message");
        message.put("status", "completed");
        message.put("role", "assistant");
        message.set("content", content);

        ArrayNode output = objectMapper.createArrayNode();
        output.add(message);

        ObjectNode usage = objectMapper.createObjectNode();
        usage.put("input_tokens", 0);
        usage.put("output_tokens", 0);
        usage.put("total_tokens", 0);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "resp_" + UUID.randomUUID().toString().replace("-", ""));
        response.put("object", "response");
        response.put("created_at", Instant.now().getEpochSecond());
        response.put("status", "completed");
        response.put("model", modelCode);
        response.set("output", output);
        response.set("usage", usage);
        return response;
    }

    private ObjectNode buildSimpleChatCompletionResponse(String modelCode, String text) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "assistant");
        message.put("content", text);

        ObjectNode choice = objectMapper.createObjectNode();
        choice.put("index", 0);
        choice.set("message", message);
        choice.put("finish_reason", "stop");

        ArrayNode choices = objectMapper.createArrayNode();
        choices.add(choice);

        ObjectNode usage = objectMapper.createObjectNode();
        usage.put("prompt_tokens", 0);
        usage.put("completion_tokens", 0);
        usage.put("total_tokens", 0);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "chatcmpl_" + UUID.randomUUID().toString().replace("-", ""));
        response.put("object", "chat.completion");
        response.put("created", Instant.now().getEpochSecond());
        response.put("model", modelCode);
        response.set("choices", choices);
        response.set("usage", usage);
        return response;
    }

    private String buildResponsesEventStreamFromResponseJson(JsonNode responseJson) throws Exception {
        ObjectNode createdEvent = objectMapper.createObjectNode();
        createdEvent.put("type", "response.created");
        ObjectNode createdResponse = responseJson.deepCopy();
        createdResponse.put("status", "in_progress");
        createdResponse.remove("output");
        createdResponse.remove("usage");
        createdEvent.set("response", createdResponse);

        ObjectNode completedEvent = objectMapper.createObjectNode();
        completedEvent.put("type", "response.completed");
        completedEvent.set("response", responseJson);

        StringBuilder builder = new StringBuilder()
                .append("data: ").append(objectMapper.writeValueAsString(createdEvent)).append("\n\n");

        JsonNode outputNode = responseJson.path("output");
        if (outputNode.isArray()) {
            for (int i = 0; i < outputNode.size(); i++) {
                JsonNode item = outputNode.get(i);
                String type = item.path("type").asText("");

                ObjectNode itemAdded = objectMapper.createObjectNode();
                itemAdded.put("type", "response.output_item.added");
                itemAdded.put("output_index", i);
                ObjectNode addedItem = item.deepCopy();
                addedItem.put("status", "in_progress");
                if ("message".equals(type)) {
                    addedItem.set("content", objectMapper.createArrayNode());
                }
                itemAdded.set("item", addedItem);
                builder.append("data: ").append(objectMapper.writeValueAsString(itemAdded)).append("\n\n");

                if ("message".equals(type)) {
                    String messageId = item.path("id").asText("msg_" + UUID.randomUUID().toString().replace("-", ""));
                    String text = extractResponseOutputText(item.path("content"));

                    ObjectNode deltaEvent = objectMapper.createObjectNode();
                    deltaEvent.put("type", "response.output_text.delta");
                    deltaEvent.put("item_id", messageId);
                    deltaEvent.put("output_index", i);
                    deltaEvent.put("content_index", 0);
                    deltaEvent.put("delta", text);
                    builder.append("data: ").append(objectMapper.writeValueAsString(deltaEvent)).append("\n\n");

                    ObjectNode doneEvent = objectMapper.createObjectNode();
                    doneEvent.put("type", "response.output_text.done");
                    doneEvent.put("item_id", messageId);
                    doneEvent.put("output_index", i);
                    doneEvent.put("content_index", 0);
                    doneEvent.put("text", text);
                    builder.append("data: ").append(objectMapper.writeValueAsString(doneEvent)).append("\n\n");
                }

                ObjectNode itemDone = objectMapper.createObjectNode();
                itemDone.put("type", "response.output_item.done");
                itemDone.put("output_index", i);
                itemDone.set("item", item);
                builder.append("data: ").append(objectMapper.writeValueAsString(itemDone)).append("\n\n");
            }
        }

        return builder
                .append("data: ").append(objectMapper.writeValueAsString(completedEvent)).append("\n\n")
                .append("data: [DONE]\n\n")
                .toString();
    }

    private String buildResponsesErrorEventStream(int status, String message) throws Exception {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "error");

        ObjectNode error = objectMapper.createObjectNode();
        error.put("message", message == null || message.isBlank() ? "System error" : message);
        error.put("type", status == 429 ? "rate_limit_error" : "server_error");
        error.put("status", status);
        event.set("error", error);

        ObjectNode completedEvent = objectMapper.createObjectNode();
        completedEvent.put("type", "response.completed");
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "resp_" + UUID.randomUUID().toString().replace("-", ""));
        response.put("object", "response");
        response.put("created_at", Instant.now().getEpochSecond());
        response.put("status", "failed");
        response.set("error", error.deepCopy());
        completedEvent.set("response", response);

        return new StringBuilder()
                .append("data: ").append(objectMapper.writeValueAsString(event)).append("\n\n")
                .append("data: ").append(objectMapper.writeValueAsString(completedEvent)).append("\n\n")
                .append("data: [DONE]\n\n")
                .toString();
    }

    private String buildChatCompletionEventStreamFromResponseJson(JsonNode responseJson) throws Exception {
        JsonNode choice = responseJson.path("choices").isArray() && !responseJson.path("choices").isEmpty()
                ? responseJson.path("choices").get(0)
                : objectMapper.createObjectNode();
        String responseId = responseJson.path("id").asText("chatcmpl_" + UUID.randomUUID().toString().replace("-", ""));
        String text = flattenMessageContent(choice.path("message").path("content"));

        ObjectNode chunk = objectMapper.createObjectNode();
        chunk.put("id", responseId);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", responseJson.path("created").asLong(Instant.now().getEpochSecond()));
        chunk.put("model", responseJson.path("model").asText(""));
        ArrayNode choices = objectMapper.createArrayNode();
        ObjectNode deltaChoice = objectMapper.createObjectNode();
        deltaChoice.put("index", 0);
        ObjectNode delta = objectMapper.createObjectNode();
        delta.put("role", "assistant");
        delta.put("content", text);
        deltaChoice.set("delta", delta);
        deltaChoice.put("finish_reason", "stop");
        choices.add(deltaChoice);
        chunk.set("choices", choices);

        return "data: " + objectMapper.writeValueAsString(chunk) + "\n\n" + "data: [DONE]\n\n";
    }

    private AgentExecutionResult executeLocalAgentResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                            GatewayRouteService.RouteDefinition route,
                                                            String requestId,
                                                            ObjectNode input,
                                                            AgentSseSink sink) throws Exception {
        long startTimeMs = System.currentTimeMillis();
        Path workspaceRoot = resolveWorkspaceRoot(input);
        AgentSession session = resolveAgentSession(auth, route.modelCode(), input, workspaceRoot);
        ArrayNode requestMessages = buildMessagesFromResponsesInput(input);
        if (requestMessages.isEmpty()) {
            throw new BusinessException(400, "Responses input cannot be empty");
        }

        session = compressSessionHistoryIfNeeded(session, null);
        ArrayNode conversation = loadSessionConversation(session);

        persistMessages(session.id(), requestId, requestMessages, "request");
        appendMessages(conversation, requestMessages);

        sink.emit(buildResponseCreatedEvent(requestId, route.modelCode()));
        sink.emit(buildAgentStatusEvent("session.ready", Map.of(
                "session_id", session.sessionKey(),
                "workspace_root", workspaceRoot.toString(),
                "summary", "已连接工作区，准备就绪"
        )));

        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        String finalText = "";
        List<ObjectNode> toolSummaries = new ArrayList<>();
        List<String> analyses = new ArrayList<>();
        boolean toolReminderInjected = false;

        for (int step = 1; step <= AGENT_MAX_STEPS; step++) {
            sink.emit(buildAgentStatusEvent("thinking", Map.of(
                    "step", step,
                    "session_id", session.sessionKey(),
                    "summary", step == 1 ? "正在分析你的需求..." : "正在思考第 " + step + " 步..."
            )));

            AgentModelTurn turn = executeAgentPlanner(route, conversation, workspaceRoot, step);
            promptTokens += turn.promptTokens();
            completionTokens += turn.completionTokens();
            totalTokens += turn.totalTokens();

            AgentDecision decision = parseAgentDecision(turn.text());
            if ("tool".equalsIgnoreCase(decision.type()) && decision.toolName() != null && !decision.toolName().isBlank()) {
                String toolTitle = decision.title() == null || decision.title().isBlank() ? decision.toolName() : decision.title();
                String callId = "call_" + UUID.randomUUID().toString().replace("-", "");
                String outputItemId = "fco_" + UUID.randomUUID().toString().replace("-", "");

                /* 收集 analysis 文字，最终合并到回答里 */
                String analysisText = decision.analysis() == null ? "" : decision.analysis().trim();
                if (!analysisText.isEmpty()) {
                    analyses.add(analysisText);
                }

                sink.emit(buildAgentStatusEvent("tool.running", Map.of(
                        "step", step,
                        "tool_name", decision.toolName(),
                        "title", toolTitle,
                        "summary", "正在执行：" + toolTitle
                )));
                sink.emit(buildToolCallAddedEvent(step, callId, decision.toolName(), decision.arguments(), toolTitle));

                AgentToolExecution toolExecution = executeAgentTool(
                        session, requestId, step, workspaceRoot, decision.toolName(), toolTitle, decision.arguments()
                );

                sink.emit(buildToolCallDoneEvent(step, callId, decision.toolName(), decision.arguments(), toolTitle));
                sink.emit(buildToolOutputAddedEvent(step, callId, outputItemId, toolExecution));
                for (JsonNode preview : toolExecution.filePreviews()) {
                    if (preview.path("changed").asBoolean(false)) {
                        sink.emit(buildFileChangedEvent(step, preview));
                    }
                }
                sink.emit(buildToolOutputDoneEvent(step, callId, outputItemId, toolExecution));
                sink.emit(buildAgentStatusEvent(toolExecution.success() ? "tool.completed" : "tool.failed", Map.of(
                        "step", step,
                        "tool_name", toolExecution.toolName(),
                        "title", toolExecution.title(),
                        "summary", toolExecution.success()
                                ? "已完成：" + toolExecution.title()
                                : "执行失败：" + toolExecution.title()
                )));

                persistToolConversation(session.id(), requestId, decision.toolName(), decision.arguments(), toolExecution.modelVisibleResult());
                toolSummaries.add(toolExecution.summaryJson().deepCopy());
                conversation.add(buildAssistantToolRequestMessage(decision.toolName(), decision.arguments(), toolTitle));
                conversation.add(buildToolResultMessage(toolExecution.modelVisibleResult()));
                continue;
            }

            String candidateFinalText = firstNonBlank(decision.finalText(), turn.text());
            if (!toolReminderInjected && shouldRetryAgentWithToolReminder(candidateFinalText, input)) {
                ObjectNode reminder = objectMapper.createObjectNode();
                reminder.put("role", "system");
                reminder.put("content", buildAgentToolReminder(workspaceRoot));
                conversation.add(reminder);
                toolReminderInjected = true;
                sink.emit(buildAgentStatusEvent("tool.retry", Map.of(
                        "step", step,
                        "reason", "tool_access_reminder",
                        "summary", "重新尝试，提醒模型使用工具..."
                )));
                continue;
            }
            finalText = candidateFinalText;
            if (!finalText.isBlank()) {
                break;
            }
        }

        if (finalText.isBlank()) {
            finalText = buildAgentFallbackFinalText(toolSummaries);
        }

        if ((toolSummaries.isEmpty() || shouldRetryAgentWithToolReminder(finalText, input))
                && hasWorkspaceHint(input)) {
            HeuristicAgentOutcome heuristicOutcome = tryHandleHeuristicAgentRequest(
                    session, requestId, workspaceRoot, input, sink, toolSummaries
            );
            if (heuristicOutcome != null) {
                finalText = heuristicOutcome.finalText();
            }
        }

        /* 自动生成结构化的执行过程描述，拼到最终回答前面 */
        if (!toolSummaries.isEmpty() && !finalText.isBlank()) {
            StringBuilder narrative = new StringBuilder();

            /* 如果模型有输出 analysis，用第一条作为开场白 */
            if (!analyses.isEmpty() && !analyses.get(0).isBlank()) {
                narrative.append(analyses.get(0)).append("\n\n");
            }

            /* 用 tool title 自动生成执行步骤 */
            narrative.append("**执行过程**：\n");
            for (int i = 0; i < toolSummaries.size(); i++) {
                ObjectNode ts = toolSummaries.get(i);
                String toolName = ts.path("tool_name").asText("tool");
                String title = ts.path("title").asText(toolName);
                boolean ok = ts.path("ok").asBoolean(false);

                narrative.append(i + 1).append(". ");
                narrative.append(ok ? "" : "[失败] ");
                narrative.append(title);

                /* 如果有对应步骤的 analysis，加在后面 */
                if (i + 1 < analyses.size() && !analyses.get(i + 1).isBlank()) {
                    narrative.append(" — ").append(analyses.get(i + 1));
                }
                narrative.append("\n");
            }
            narrative.append("\n");

            /* 最终回答放在执行过程后面 */
            narrative.append(finalText);
            finalText = narrative.toString();
        } else if (!analyses.isEmpty() && !analyses.get(0).isBlank() && !finalText.isBlank()) {
            /* 没有工具调用，但有 analysis，也拼上 */
            finalText = analyses.get(0) + "\n\n" + finalText;
        }

        persistAssistantMessage(session.id(), requestId, finalText);
        updateSessionLastResponse(session.id(), requestId);
        session = compressSessionHistoryIfNeeded(session, sink);

        /* 发送汇总事件，让任何客户端都能展示执行摘要 */
        int commandCount = 0;
        int fileChangeCount = 0;
        for (ObjectNode ts : toolSummaries) {
            String tn = ts.path("tool_name").asText("");
            if ("run_command".equals(tn)) commandCount++;
            if (ts.path("files_changed").asInt(0) > 0) fileChangeCount += ts.path("files_changed").asInt(0);
        }
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        String elapsedStr = elapsedMs < 60_000
                ? (elapsedMs / 1000) + "s"
                : (elapsedMs / 60_000) + "m " + ((elapsedMs % 60_000) / 1000) + "s";

        StringBuilder summaryText = new StringBuilder();
        if (!toolSummaries.isEmpty()) {
            summaryText.append("执行了 ").append(toolSummaries.size()).append(" 个步骤");
            if (commandCount > 0) summaryText.append("，运行了 ").append(commandCount).append(" 条命令");
            if (fileChangeCount > 0) summaryText.append("，修改了 ").append(fileChangeCount).append(" 个文件");
            summaryText.append("，耗时 ").append(elapsedStr);
        } else {
            summaryText.append("直接回答，耗时 ").append(elapsedStr);
        }

        sink.emit(buildAgentStatusEvent("completed", Map.of(
                "total_steps", toolSummaries.size(),
                "commands_run", commandCount,
                "files_changed", fileChangeCount,
                "elapsed", elapsedStr,
                "summary", summaryText.toString()
        )));

        ObjectNode responseJson = buildAgentResponsesResponse(
                requestId, route.modelCode(), finalText, promptTokens, completionTokens, toolSummaries
        );
        sink.emitFinalResponse(responseJson, finalText);
        return new AgentExecutionResult(responseJson, promptTokens, completionTokens, totalTokens);
    }

    private AgentExecutionResult executeAgentResponses(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                       GatewayRouteService.RouteDefinition route,
                                                       String requestId,
                                                       ObjectNode input,
                                                       AgentSseSink sink) throws Exception {
        throw new BusinessException(400, "当前服务已切换为纯网关模式，不再在服务器上扫描、读取、修改或执行本地文件。请在你自己的电脑上安装 Codex，并通过本网关 API 使用你的本地工具。");
    }

    private AgentModelTurn executeAgentPlanner(GatewayRouteService.RouteDefinition route,
                                               ArrayNode conversation,
                                               Path workspaceRoot,
                                               int step) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", route.upstreamModel());
        request.put("stream", false);
        request.put("temperature", 0.1);
        request.put("max_tokens", 2000);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildAgentSystemPrompt(workspaceRoot, step));
        messages.add(systemMessage);

        int start = Math.max(0, conversation.size() - AGENT_HISTORY_LIMIT);
        for (int i = start; i < conversation.size(); i++) {
            messages.add(conversation.get(i));
        }

        request.set("messages", messages);
        String body = objectMapper.writeValueAsString(request);
        UpstreamTextResponse response = shouldAggregateCodexStream(route)
                ? executeCodexChatByStreaming(route, body)
                : executeOpenAiTextRequest(route, "/chat/completions", body);

        String responseBody = normalizeUpstreamResponseBody(route, response.statusCode(), response.body());
        JsonNode responseJson = tryReadJson(responseBody);
        if (response.statusCode() < 200 || response.statusCode() >= 300 || responseJson == null) {
            throw new BusinessException(502, extractUpstreamErrorMessage(responseBody));
        }

        String text = flattenMessageContent(responseJson.path("choices").path(0).path("message").path("content"));
        JsonNode usage = responseJson.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        int totalTokens = usage.path("total_tokens").asInt(promptTokens + completionTokens);
        return new AgentModelTurn(text, promptTokens, completionTokens, totalTokens);
    }

    private String buildAgentSystemPrompt(Path workspaceRoot, int step) {
        return """
                你是 ZXW Agent，一个在 AI Gateway 内运行的编程助手。
                当前步骤：%d
                当前时间：%s
                工作区根目录：%s

                ## 可用工具
                1. list_files {"path":".","max_results":200}
                2. read_file {"path":"src/main/App.java","start_line":1,"end_line":200}
                3. search_code {"query":"关键词","path":"src","max_results":50}
                4. write_file {"path":"notes.txt","content":"...","append":false}
                5. replace_in_file {"path":"pom.xml","search":"旧内容","replace":"新内容","all":false}
                6. run_command {"command":"mvn -q -DskipTests compile","cwd":".","timeout_seconds":30}

                ## 核心规则：必须像人一样先说话再动手

                你不是一个沉默执行命令的机器人。你是一个会思考、会表达的助手。
                每次返回 JSON 时，必须包含 "analysis" 字段——这是你说给用户听的自然语言，
                会直接显示在用户的对话框里。这是最重要的字段。

                ### 调用工具时的 JSON 格式：
                {
                  "type": "tool",
                  "analysis": "我先帮你扫一下整个项目结构，看看有哪些模块和核心入口，然后再针对性地分析。",
                  "title": "扫描项目结构",
                  "tool_name": "list_files",
                  "arguments": {"path": ".", "max_results": 200}
                }

                ### 给出最终回答时的 JSON 格式：
                {
                  "type": "final",
                  "analysis": "",
                  "title": "分析完成",
                  "final_text": "结构化的完整回答..."
                }

                ## analysis 字段怎么写

                1. 第一步（step=1）时，analysis 必须像一个有经验的开发者接到任务后的自然反应：
                   - 好的示例："这个需求我分三步来做：先看一下现有的代码结构，再找到要修改的位置，最后帮你改好。我先扫一遍项目。"
                   - 好的示例："我按'全项目扫一遍'的方式来做，先把项目结构、核心入口、模块快速过一遍，再给你一个有条理的全局结论。"
                   - 好的示例："我来帮你查，先看看 pom.xml 确认一下项目的依赖情况。"
                   - 坏的示例："分析：用户想查看项目结构"（太机械，像在填表）
                   - 坏的示例：""（空的，用户什么都看不到）

                2. 后续步骤的 analysis 要简短说明为什么要执行这一步：
                   - "项目结构看完了，接下来我去读一下核心的 Controller 代码。"
                   - "找到了 3 个相关文件，我先看最关键的那个。"
                   - "代码已经读到了，现在帮你把这段替换掉。"

                3. final 类型的 analysis 留空即可，因为 final_text 本身就是完整回答。

                ## final_text 格式要求

                final_text 内容要有条理、像一个专业开发者在跟你对话：
                - 先用一两句话概括结论或成果
                - 然后用编号列表展开关键细节
                - 涉及到文件路径、代码片段时用 `反引号` 包裹
                - 如果做了修改操作，列出改了什么、改动前后的对比
                - 最后可以加一句后续建议

                ## 行为规则
                - 全部使用简体中文
                - 只返回压缩后的 JSON，不加任何 JSON 以外的文字
                - 用户让你改代码，就必须真的用工具去改，不能只分析
                - 编辑之前先 search_code 或 read_file 看一下
                - 用户只给了文件名（如 MessageVO.java），先搜索定位
                - 路径用相对于工作区根目录的写法
                - 查找失败就换关键词再搜一次
                """.formatted(step, LocalDateTime.now().format(DIRECT_TIME_FORMATTER), workspaceRoot);
    }

    private String buildAgentFallbackFinalText(List<ObjectNode> toolSummaries) {
        if (toolSummaries == null || toolSummaries.isEmpty()) {
            return "我需要更多信息才能继续。请补充具体的目标、文件路径或报错信息，我来帮你处理。";
        }

        StringBuilder builder = new StringBuilder("以上步骤已全部执行完毕，结果如下：\n\n");
        for (int i = 0; i < toolSummaries.size(); i++) {
            JsonNode tool = toolSummaries.get(i);
            String title = tool.path("title").asText(tool.path("tool_name").asText("tool"));
            boolean ok = tool.path("ok").asBoolean(false);
            builder.append(i + 1).append(". **").append(title).append("** — ").append(ok ? "成功" : "失败");
            String preview = summarizeFreeText(tool.path("result_preview").asText(""), 200);
            if (!preview.isBlank()) {
                builder.append("\n   ").append(preview);
            }
            builder.append("\n");
        }
        builder.append("\n如果你需要进一步操作，告诉我下一步要做什么。");
        return builder.toString().trim();
    }

    private HeuristicAgentOutcome tryHandleHeuristicAgentRequest(AgentSession session,
                                                                 String responseId,
                                                                 Path workspaceRoot,
                                                                 ObjectNode input,
                                                                 AgentSseSink sink,
                                                                 List<ObjectNode> toolSummaries) throws Exception {
        String latestUserText = extractLatestUserTextFromResponses(input);
        if (!looksLikeReadFileIntent(latestUserText)) {
            return null;
        }

        String fileHint = extractExplicitFileHint(latestUserText);
        if (fileHint.isBlank()) {
            return null;
        }

        int step = AGENT_MAX_STEPS + 1;
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("path", fileHint);
        arguments.put("start_line", 1);
        arguments.put("end_line", 260);

        String callId = "call_" + UUID.randomUUID().toString().replace("-", "");
        String outputItemId = "fco_" + UUID.randomUUID().toString().replace("-", "");
        String title = "heuristic read_file";

        sink.emit(buildAgentStatusEvent("tool.heuristic", Map.of(
                "step", step,
                "tool_name", "read_file",
                "title", title,
                "summary", "自动识别意图，正在读取文件..."
        )));
        sink.emit(buildToolCallAddedEvent(step, callId, "read_file", arguments, title));
        AgentToolExecution toolExecution = executeReadFileTool(session, responseId, step, workspaceRoot, title, arguments);
        sink.emit(buildToolCallDoneEvent(step, callId, "read_file", arguments, title));
        sink.emit(buildToolOutputAddedEvent(step, callId, outputItemId, toolExecution));
        for (JsonNode preview : toolExecution.filePreviews()) {
            if (preview.path("changed").asBoolean(false)) {
                sink.emit(buildFileChangedEvent(step, preview));
            }
        }
        sink.emit(buildToolOutputDoneEvent(step, callId, outputItemId, toolExecution));

        persistToolConversation(session.id(), responseId, "read_file", arguments, toolExecution.modelVisibleResult());
        toolSummaries.add(toolExecution.summaryJson().deepCopy());

        String finalText = buildHeuristicReadFileAnswer(workspaceRoot, latestUserText, toolExecution);
        return new HeuristicAgentOutcome(toolExecution, finalText);
    }

    private boolean looksLikeReadFileIntent(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("读取")
                || normalized.contains("查看")
                || normalized.contains("看看")
                || normalized.contains("打开")
                || normalized.contains("read")
                || normalized.contains("open ")
                || normalized.contains("show ")
                || normalized.contains("scan")
                || normalized.contains("artifactid")
                || normalized.contains("pom.xml");
    }

    private String extractExplicitFileHint(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("([A-Za-z0-9_./\\\\-]+\\.[A-Za-z0-9_-]+)").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String buildHeuristicReadFileAnswer(Path workspaceRoot,
                                                String latestUserText,
                                                AgentToolExecution toolExecution) throws Exception {
        ObjectNode result = toolExecution.modelVisibleResult();
        if (!result.path("ok").asBoolean(false)) {
            return "我尝试直接读取文件，但失败了：" + firstNonBlank(result.path("message").asText(""), "未知错误");
        }

        String relativePath = result.path("path").asText("");
        Path file = resolveExistingToolFile(workspaceRoot, relativePath);
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String normalizedUserText = latestUserText == null ? "" : latestUserText.toLowerCase(Locale.ROOT);

        if (normalizedUserText.contains("artifactid") && relativePath.toLowerCase(Locale.ROOT).endsWith("pom.xml")) {
            String pomWithoutParent = content.replaceFirst("(?is)<parent>.*?</parent>", "");
            Matcher matcher = Pattern.compile("(?is)<artifactId>\\s*([^<\\s]+)\\s*</artifactId>").matcher(pomWithoutParent);
            if (matcher.find()) {
                return "我已读取 `pom.xml`，项目的 artifactId 是 `" + matcher.group(1).trim() + "`。";
            }
        }

        return "我已读取 `" + relativePath + "`。如果你要我继续提取具体字段、定位问题或直接修改它，我可以继续处理。";
    }

    private boolean shouldRetryAgentWithToolReminder(String text, ObjectNode input) {
        if (text == null || text.isBlank() || !hasWorkspaceHint(input)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("cannot access")
                || normalized.contains("can't access")
                || normalized.contains("no access")
                || normalized.contains("permission")
                || normalized.contains("权限")
                || normalized.contains("无法访问")
                || normalized.contains("不能访问")
                || normalized.contains("没有源码")
                || normalized.contains("未获得源码访问")
                || normalized.contains("无法查看");
    }

    private String buildAgentToolReminder(Path workspaceRoot) {
        return """
                提醒：
                - 你在本次会话中拥有工具访问权限，可以读写文件和执行命令。
                - 工作区根目录：%s
                - 不要回答说你没有文件或代码的访问权限。
                - 只返回压缩后的 JSON。
                - 如果用户问到某个文件，立即用 read_file 或 search_code 去读取。
                - 如果用户要求修改代码，立即用 replace_in_file 或 write_file 去修改。
                """.formatted(workspaceRoot);
    }

    private AgentDecision parseAgentDecision(String rawText) {
        String cleaned = rawText == null ? "" : rawText.trim();
        if (cleaned.startsWith("```")) {
            Matcher matcher = Pattern.compile("^```[a-zA-Z]*\\s*(.*?)\\s*```$", Pattern.DOTALL).matcher(cleaned);
            if (matcher.matches()) {
                cleaned = matcher.group(1).trim();
            }
        }

        JsonNode json = tryReadJson(cleaned);
        if (json == null || !json.isObject()) {
            return new AgentDecision("final", "answer", null, objectMapper.createObjectNode(), cleaned, "");
        }

        String type = json.path("type").asText("final");
        String title = json.path("title").asText(type);
        String toolName = json.path("tool_name").asText("");
        ObjectNode arguments = json.path("arguments").isObject()
                ? (ObjectNode) json.path("arguments")
                : objectMapper.createObjectNode();
        String finalText = json.path("final_text").asText(cleaned);
        String analysis = json.path("analysis").asText("");
        return new AgentDecision(type, title, toolName, arguments, finalText, analysis);
    }

    private AgentToolExecution executeAgentTool(AgentSession session,
                                                String responseId,
                                                int step,
                                                Path workspaceRoot,
                                                String toolName,
                                                String title,
                                                ObjectNode arguments) throws Exception {
        return switch (toolName) {
            case "list_files" -> executeListFilesTool(session, responseId, step, workspaceRoot, title, arguments);
            case "read_file" -> executeReadFileTool(session, responseId, step, workspaceRoot, title, arguments);
            case "search_code" -> executeSearchCodeTool(session, responseId, step, workspaceRoot, title, arguments);
            case "write_file" -> executeWriteFileTool(session, responseId, step, workspaceRoot, title, arguments);
            case "replace_in_file" -> executeReplaceInFileTool(session, responseId, step, workspaceRoot, title, arguments);
            case "run_command" -> executeRunCommandTool(session, responseId, step, workspaceRoot, title, arguments);
            default -> executeUnsupportedTool(session, responseId, step, title, toolName, arguments);
        };
    }

    private AgentToolExecution executeListFilesTool(AgentSession session,
                                                    String responseId,
                                                    int step,
                                                    Path workspaceRoot,
                                                    String title,
                                                    ObjectNode arguments) {
        Path target = resolveToolPath(workspaceRoot, arguments.path("path").asText("."));
        int maxResults = Math.min(Math.max(arguments.path("max_results").asInt(200), 1), 500);

        try {
            List<String> files = Files.walk(target)
                    .filter(Files::isRegularFile)
                    .map(path -> workspaceRoot.relativize(path).toString().replace("\\", "/"))
                    .sorted()
                    .limit(maxResults)
                    .toList();

            ObjectNode result = objectMapper.createObjectNode();
            result.put("ok", true);
            result.put("path", relativizePath(workspaceRoot, target));
            result.put("count", files.size());
            ArrayNode items = objectMapper.createArrayNode();
            files.forEach(items::add);
            result.set("files", items);
            return completeToolExecution(session, responseId, step, "list_files", title, arguments, result, objectMapper.createArrayNode());
        } catch (Exception ex) {
            return completeToolExecution(session, responseId, step, "list_files", title, arguments,
                    simpleToolError(ex.getMessage()), objectMapper.createArrayNode());
        }
    }

    private AgentToolExecution executeReadFileTool(AgentSession session,
                                                   String responseId,
                                                   int step,
                                                   Path workspaceRoot,
                                                   String title,
                                                   ObjectNode arguments) {
        try {
            Path file = resolveExistingToolFile(workspaceRoot, arguments.path("path").asText(""));

            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int startLine = Math.max(arguments.path("start_line").asInt(1), 1);
            int endLine = arguments.hasNonNull("end_line")
                    ? Math.min(arguments.path("end_line").asInt(lines.size()), lines.size())
                    : Math.min(lines.size(), startLine + 199);

            StringBuilder builder = new StringBuilder();
            for (int i = startLine; i <= endLine; i++) {
                builder.append(i).append(": ").append(lines.get(i - 1)).append("\n");
            }

            ObjectNode result = objectMapper.createObjectNode();
            result.put("ok", true);
            result.put("path", relativizePath(workspaceRoot, file));
            result.put("start_line", startLine);
            result.put("end_line", endLine);
            result.put("content", clipText(builder.toString(), TOOL_OUTPUT_LIMIT));
            result.put("truncated", builder.length() > TOOL_OUTPUT_LIMIT);
            return completeToolExecution(session, responseId, step, "read_file", title, arguments, result, objectMapper.createArrayNode());
        } catch (Exception ex) {
            return completeToolExecution(session, responseId, step, "read_file", title, arguments,
                    simpleToolError(ex.getMessage()), objectMapper.createArrayNode());
        }
    }

    private AgentToolExecution executeSearchCodeTool(AgentSession session,
                                                     String responseId,
                                                     int step,
                                                     Path workspaceRoot,
                                                     String title,
                                                     ObjectNode arguments) {
        String query = arguments.path("query").asText("");
        if (query.isBlank()) {
            return completeToolExecution(session, responseId, step, "search_code", title, arguments,
                    simpleToolError("query cannot be empty"), objectMapper.createArrayNode());
        }
        try {
            Path target = resolveToolPath(workspaceRoot, arguments.path("path").asText("."));
            int maxResults = Math.min(Math.max(arguments.path("max_results").asInt(50), 1), 200);
            ProcessResult processResult = runProcess(List.of(
                    "rg", "-n", "--hidden", "--no-ignore", "--max-count", String.valueOf(maxResults), query, target.toString()
            ), workspaceRoot, 30000);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("ok", processResult.exitCode() == 0 || processResult.exitCode() == 1);
            result.put("query", query);
            result.put("path", relativizePath(workspaceRoot, target));
            result.put("exit_code", processResult.exitCode());
            result.put("output", clipText(processResult.stdout(), TOOL_OUTPUT_LIMIT));
            result.put("stderr", clipText(processResult.stderr(), 2000));
            return completeToolExecution(session, responseId, step, "search_code", title, arguments, result, objectMapper.createArrayNode());
        } catch (Exception ex) {
            return completeToolExecution(session, responseId, step, "search_code", title, arguments,
                    simpleToolError(ex.getMessage()), objectMapper.createArrayNode());
        }
    }

    private AgentToolExecution executeWriteFileTool(AgentSession session,
                                                    String responseId,
                                                    int step,
                                                    Path workspaceRoot,
                                                    String title,
                                                    ObjectNode arguments) {
        try {
            String rawPath = arguments.path("path").asText("");
            Path file = resolveToolPath(workspaceRoot, rawPath);
            if (!Files.exists(file)) {
                try {
                    file = resolveExistingToolFile(workspaceRoot, rawPath);
                } catch (BusinessException ignored) {
                    // Keep the originally resolved target so write_file can still create a new file.
                }
            }
            boolean append = arguments.path("append").asBoolean(false);
            String content = arguments.path("content").asText("");
            String before = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";

            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (append) {
                Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            String after = Files.readString(file, StandardCharsets.UTF_8);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("ok", true);
            result.put("path", relativizePath(workspaceRoot, file));
            result.put("append", append);
            result.put("bytes", after.getBytes(StandardCharsets.UTF_8).length);
            ArrayNode previews = objectMapper.createArrayNode();
            previews.add(buildFilePreviewJson(workspaceRoot, file, before, after));
            return completeToolExecution(session, responseId, step, "write_file", title, arguments, result, previews);
        } catch (Exception ex) {
            return completeToolExecution(session, responseId, step, "write_file", title, arguments,
                    simpleToolError(ex.getMessage()), objectMapper.createArrayNode());
        }
    }

    private AgentToolExecution executeReplaceInFileTool(AgentSession session,
                                                        String responseId,
                                                        int step,
                                                        Path workspaceRoot,
                                                        String title,
                                                        ObjectNode arguments) {
        try {
            Path file = resolveExistingToolFile(workspaceRoot, arguments.path("path").asText(""));

            String before = Files.readString(file, StandardCharsets.UTF_8);
            String search = arguments.path("search").asText("");
            String replace = arguments.path("replace").asText("");
            boolean all = arguments.path("all").asBoolean(false);
            if (search.isBlank()) {
                return completeToolExecution(session, responseId, step, "replace_in_file", title, arguments,
                        simpleToolError("search cannot be empty"), objectMapper.createArrayNode());
            }

            String after = all ? before.replace(search, replace) : replaceFirstLiteral(before, search, replace);
            Files.writeString(file, after, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("ok", true);
            result.put("path", relativizePath(workspaceRoot, file));
            result.put("changed", !before.equals(after));
            ArrayNode previews = objectMapper.createArrayNode();
            previews.add(buildFilePreviewJson(workspaceRoot, file, before, after));
            return completeToolExecution(session, responseId, step, "replace_in_file", title, arguments, result, previews);
        } catch (Exception ex) {
            return completeToolExecution(session, responseId, step, "replace_in_file", title, arguments,
                    simpleToolError(ex.getMessage()), objectMapper.createArrayNode());
        }
    }

    private AgentToolExecution executeRunCommandTool(AgentSession session,
                                                     String responseId,
                                                     int step,
                                                     Path workspaceRoot,
                                                     String title,
                                                     ObjectNode arguments) {
        String command = arguments.path("command").asText("");
        if (command.isBlank()) {
            return completeToolExecution(session, responseId, step, "run_command", title, arguments,
                    simpleToolError("command cannot be empty"), objectMapper.createArrayNode());
        }
        try {
            validateSafeCommand(command);
            Path cwd = resolveToolPath(workspaceRoot, arguments.path("cwd").asText("."));
            int timeoutSeconds = Math.min(Math.max(arguments.path("timeout_seconds").asInt(30), 1), 120);

            ProcessResult processResult = runProcess(List.of("powershell.exe", "-NoProfile", "-Command", command), cwd, timeoutSeconds * 1000L);
            ObjectNode result = objectMapper.createObjectNode();
            result.put("ok", processResult.exitCode() == 0);
            result.put("cwd", cwd.toString());
            result.put("exit_code", processResult.exitCode());
            result.put("stdout", clipText(processResult.stdout(), TOOL_OUTPUT_LIMIT));
            result.put("stderr", clipText(processResult.stderr(), 4000));
            return completeToolExecution(session, responseId, step, "run_command", title, arguments, result, objectMapper.createArrayNode());
        } catch (Exception ex) {
            return completeToolExecution(session, responseId, step, "run_command", title, arguments,
                    simpleToolError(ex.getMessage()), objectMapper.createArrayNode());
        }
    }

    private AgentToolExecution executeUnsupportedTool(AgentSession session,
                                                      String responseId,
                                                      int step,
                                                      String title,
                                                      String toolName,
                                                      ObjectNode arguments) {
        ObjectNode result = simpleToolError("Unsupported tool: " + toolName);
        return completeToolExecution(session, responseId, step, toolName, title, arguments, result, objectMapper.createArrayNode());
    }

    private AgentToolExecution completeToolExecution(AgentSession session,
                                                     String responseId,
                                                     int step,
                                                     String toolName,
                                                     String title,
                                                     ObjectNode arguments,
                                                     ObjectNode result,
                                                     ArrayNode previews) {
        saveToolLog(session.id(), responseId, step, toolName, title, arguments, result, previews);
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("tool_name", toolName);
        summary.put("title", title == null || title.isBlank() ? toolName : title);
        summary.put("ok", result.path("ok").asBoolean(false));
        summary.put("result_preview", clipText(result.toString(), 600));
        return new AgentToolExecution(
                toolName,
                title == null || title.isBlank() ? toolName : title,
                result.path("ok").asBoolean(false),
                result,
                previews,
                summary
        );
    }

    private ObjectNode simpleToolError(String message) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("ok", false);
        result.put("message", message);
        return result;
    }

    private ProcessResult runProcess(List<String> command, Path workdir, long timeoutMs) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workdir.toFile());
        Process process = processBuilder.start();

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new BusinessException(504, "Command timed out after " + timeoutMs + " ms");
        }

        String stdout;
        String stderr;
        try (InputStream stdoutStream = process.getInputStream();
             InputStream stderrStream = process.getErrorStream()) {
            stdout = new String(readAllBytes(stdoutStream), StandardCharsets.UTF_8);
            stderr = new String(readAllBytes(stderrStream), StandardCharsets.UTF_8);
        }
        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private void validateSafeCommand(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.contains("format ") || lower.contains("shutdown ") || lower.contains("restart-computer")
                || lower.contains("del /s /q") || lower.contains("rd /s /q")
                || lower.contains("remove-item") && lower.contains("-recurse")
                || lower.contains("git reset --hard")) {
            throw new BusinessException(400, "Unsafe command was rejected");
        }
    }

    private Path resolveWorkspaceRoot(ObjectNode input) {
        JsonNode metadata = input.path("metadata");
        String raw = firstNonBlank(
                metadata.path("workspace_root").asText(""),
                metadata.path("cwd").asText(""),
                metadata.path("project_root").asText(""),
                metadata.path("workspace").asText(""),
                metadata.path("root_path").asText(""),
                input.path("workspace_root").asText(""),
                input.path("cwd").asText(""),
                input.path("project_root").asText(""),
                input.path("workspace").asText(""),
                input.path("root_path").asText(""),
                System.getenv("WORKSPACE_ROOT"),
                System.getProperty("user.dir")
        );
        Path path = Path.of(raw).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
        return path;
    }

    private Path resolveToolPath(Path workspaceRoot, String rawPath) {
        if (rawPath == null || rawPath.isBlank() || ".".equals(rawPath)) {
            return workspaceRoot;
        }
        Path resolved = Path.of(rawPath);
        if (!resolved.isAbsolute()) {
            resolved = workspaceRoot.resolve(rawPath);
        }
        resolved = resolved.normalize().toAbsolutePath();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new BusinessException(403, "Path is outside workspace: " + rawPath);
        }
        return resolved;
    }

    private Path resolveExistingToolFile(Path workspaceRoot, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new BusinessException(400, "path cannot be empty");
        }

        Path direct = resolveToolPath(workspaceRoot, rawPath);
        if (Files.exists(direct)) {
            return direct;
        }

        String normalized = rawPath.replace("\\", "/");
        boolean bareFilename = !normalized.contains("/");
        List<Path> candidates = new ArrayList<>();
        try {
            Files.walk(workspaceRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
                        if (bareFilename) {
                            return fileName.equalsIgnoreCase(rawPath);
                        }
                        String relative = relativizePath(workspaceRoot, path);
                        return relative.equalsIgnoreCase(normalized);
                    })
                    .limit(20)
                    .forEach(candidates::add);
        } catch (Exception ex) {
            throw new BusinessException(500, "Failed to search workspace: " + ex.getMessage());
        }

        if (candidates.size() == 1) {
            return candidates.get(0).toAbsolutePath().normalize();
        }
        if (candidates.size() > 1) {
            throw new BusinessException(409, "Multiple files matched " + rawPath + ": "
                    + candidates.stream().map(path -> relativizePath(workspaceRoot, path)).toList());
        }
        throw new BusinessException(404, "File not found in workspace: " + rawPath);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String relativizePath(Path workspaceRoot, Path target) {
        if (workspaceRoot.equals(target)) {
            return ".";
        }
        return workspaceRoot.relativize(target).toString().replace("\\", "/");
    }

    private String replaceFirstLiteral(String source, String search, String replacement) {
        int index = source.indexOf(search);
        if (index < 0) {
            return source;
        }
        return source.substring(0, index) + replacement + source.substring(index + search.length());
    }

    private ObjectNode buildResponseCreatedEvent(String responseId, String modelCode) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", responseId);
        response.put("object", "response");
        response.put("created_at", Instant.now().getEpochSecond());
        response.put("status", "in_progress");
        response.put("model", modelCode);

        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.created");
        event.set("response", response);
        return event;
    }

    private ObjectNode buildAgentStatusEvent(String kind, Map<String, Object> payload) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.agent.status");
        event.put("kind", kind);
        ObjectNode data = objectMapper.createObjectNode();
        payload.forEach(data::putPOJO);
        event.set("data", data);
        return event;
    }

    private ObjectNode buildToolCallAddedEvent(int step, String callId, String toolName, ObjectNode arguments, String title) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", callId);
        item.put("type", "function_call");
        item.put("status", "in_progress");
        item.put("call_id", callId);
        item.put("name", toolName);
        item.put("arguments", arguments.toString());
        item.put("title", title == null || title.isBlank() ? toolName : title);

        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.output_item.added");
        event.put("output_index", (step - 1) * 2);
        event.set("item", item);
        return event;
    }

    private ObjectNode buildToolCallDoneEvent(int step, String callId, String toolName, ObjectNode arguments, String title) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", callId);
        item.put("type", "function_call");
        item.put("status", "completed");
        item.put("call_id", callId);
        item.put("name", toolName);
        item.put("arguments", arguments.toString());
        item.put("title", title == null || title.isBlank() ? toolName : title);

        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.output_item.done");
        event.put("output_index", (step - 1) * 2);
        event.set("item", item);
        return event;
    }

    private ObjectNode buildToolOutputAddedEvent(int step, String callId, String outputItemId, AgentToolExecution toolExecution) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", outputItemId);
        item.put("type", "function_call_output");
        item.put("status", "in_progress");
        item.put("call_id", callId);
        item.put("name", toolExecution.toolName());
        item.put("output", clipText(toolExecution.modelVisibleResult().toString(), TOOL_OUTPUT_LIMIT));

        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.output_item.added");
        event.put("output_index", (step - 1) * 2 + 1);
        event.set("item", item);
        return event;
    }

    private ObjectNode buildToolOutputDoneEvent(int step, String callId, String outputItemId, AgentToolExecution toolExecution) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", outputItemId);
        item.put("type", "function_call_output");
        item.put("status", "completed");
        item.put("call_id", callId);
        item.put("name", toolExecution.toolName());
        item.put("output", clipText(toolExecution.modelVisibleResult().toString(), TOOL_OUTPUT_LIMIT));

        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.output_item.done");
        event.put("output_index", (step - 1) * 2 + 1);
        event.set("item", item);
        return event;
    }

    private ObjectNode buildFileChangedEvent(int step, JsonNode preview) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("type", "response.agent.file_changed");
        event.put("step", step);
        event.set("file", preview);
        return event;
    }

    private ObjectNode buildAgentResponsesResponse(String responseId,
                                                   String modelCode,
                                                   String finalText,
                                                   int promptTokens,
                                                   int completionTokens,
                                                   List<ObjectNode> toolSummaries) {
        ObjectNode response = buildSimpleResponsesResponse(modelCode, finalText);
        response.put("id", responseId);
        ObjectNode usage = (ObjectNode) response.path("usage");
        usage.put("input_tokens", promptTokens);
        usage.put("output_tokens", completionTokens);
        usage.put("total_tokens", promptTokens + completionTokens);
        ObjectNode metadata = objectMapper.createObjectNode();
        ArrayNode tools = objectMapper.createArrayNode();
        toolSummaries.forEach(tools::add);
        metadata.set("tool_runs", tools);
        response.set("metadata", metadata);
        return response;
    }

    private ObjectNode buildAssistantToolRequestMessage(String toolName, ObjectNode arguments, String title) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "assistant");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "tool");
        payload.put("title", title == null || title.isBlank() ? toolName : title);
        payload.put("tool_name", toolName);
        payload.set("arguments", arguments == null ? objectMapper.createObjectNode() : arguments);
        message.put("content", payload.toString());
        return message;
    }

    private ObjectNode buildToolResultMessage(ObjectNode toolResult) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", "TOOL_RESULT " + toolResult.toString());
        return message;
    }

    private String clipText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...[truncated]";
    }

    private ObjectNode buildFilePreviewJson(Path workspaceRoot, Path file, String before, String after) {
        ObjectNode preview = objectMapper.createObjectNode();
        preview.put("path", relativizePath(workspaceRoot, file));
        preview.put("before_preview", clipText(before, FILE_PREVIEW_LIMIT));
        preview.put("after_preview", clipText(after, FILE_PREVIEW_LIMIT));
        preview.put("changed", !before.equals(after));
        return preview;
    }

    private String extractUpstreamErrorMessage(String responseBody) {
        JsonNode json = tryReadJson(responseBody);
        if (json != null && json.has("error")) {
            JsonNode error = json.path("error");
            return error.path("message").asText("Upstream request failed");
        }
        return responseBody == null || responseBody.isBlank() ? "Upstream request failed" : responseBody;
    }

    private void appendMessages(ArrayNode target, ArrayNode source) {
        for (JsonNode item : source) {
            target.add(item.deepCopy());
        }
    }

    private String mergeSessionSummary(String existingSummary, List<StoredAgentMessage> messages) {
        StringBuilder builder = new StringBuilder();
        if (existingSummary != null && !existingSummary.isBlank()) {
            builder.append(existingSummary.trim()).append("\n");
        }

        for (StoredAgentMessage item : messages) {
            String line = switch (item.sourceType()) {
                case "request" -> "User request: " + summarizeFreeText(item.contentText(), 220);
                case "response" -> "Assistant reply: " + summarizeFreeText(item.contentText(), 220);
                case "tool_call" -> "Tool call: " + (item.toolName() == null ? "unknown" : item.toolName());
                case "tool_result" -> "Tool result: " + summarizeToolResult(item.toolName(), item.contentText());
                default -> item.roleCode() + ": " + summarizeFreeText(item.contentText(), 180);
            };
            if (line.isBlank()) {
                continue;
            }
            builder.append("- ").append(line).append("\n");
            if (builder.length() >= AGENT_SUMMARY_MAX_CHARS * 2) {
                break;
            }
        }

        String summary = builder.toString().trim();
        if (summary.length() <= AGENT_SUMMARY_MAX_CHARS) {
            return summary;
        }
        return summary.substring(summary.length() - AGENT_SUMMARY_MAX_CHARS);
    }

    private String summarizeToolResult(String toolName, String contentText) {
        String raw = contentText == null ? "" : contentText.trim();
        if (raw.startsWith("TOOL_RESULT ")) {
            raw = raw.substring("TOOL_RESULT ".length()).trim();
        }
        JsonNode json = tryReadJson(raw);
        if (json != null && json.isObject()) {
            List<String> parts = new ArrayList<>();
            if (json.has("path")) {
                parts.add("path=" + json.path("path").asText(""));
            }
            if (json.has("query")) {
                parts.add("query=" + json.path("query").asText(""));
            }
            if (json.has("changed")) {
                parts.add("changed=" + json.path("changed").asBoolean(false));
            }
            if (json.has("exit_code")) {
                parts.add("exit=" + json.path("exit_code").asInt(0));
            }
            if (json.has("message")) {
                parts.add("message=" + summarizeFreeText(json.path("message").asText(""), 140));
            }
            if (json.has("stdout")) {
                parts.add("stdout=" + summarizeFreeText(json.path("stdout").asText(""), 140));
            } else if (json.has("output")) {
                parts.add("output=" + summarizeFreeText(json.path("output").asText(""), 140));
            }
            String joined = String.join("; ", parts);
            if (!joined.isBlank()) {
                return toolName + " -> " + joined;
            }
        }
        return toolName + " -> " + summarizeFreeText(raw, 180);
    }

    private String summarizeFreeText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private AgentSession resolveAgentSession(ApiKeyAuthService.AuthenticatedApiKey auth,
                                             String modelCode,
                                             ObjectNode input,
                                             Path workspaceRoot) {
        String previousResponseId = input.path("previous_response_id").asText("");
        if (!previousResponseId.isBlank()) {
            List<AgentSession> existingByResponse = jdbcTemplate.query("""
                    select s.id, s.session_key, s.workspace_root, s.summary_text, s.summary_message_id
                    from agent_sessions s
                    join agent_messages m on m.session_id = s.id
                    where m.response_id = ? and s.user_id = ?
                    order by s.id desc
                    limit 1
                    """, (rs, rowNum) -> new AgentSession(
                    rs.getLong("id"),
                    rs.getString("session_key"),
                    rs.getString("workspace_root"),
                    rs.getString("summary_text"),
                    rs.getObject("summary_message_id") == null ? 0L : rs.getLong("summary_message_id")
            ), previousResponseId, auth.userId());
            if (!existingByResponse.isEmpty()) {
                return existingByResponse.get(0);
            }
        }

        String sessionKey = input.path("metadata").path("session_id").asText("");
        if (sessionKey.isBlank()) {
            sessionKey = "sess_" + UUID.randomUUID().toString().replace("-", "");
        }

        List<AgentSession> existing = jdbcTemplate.query("""
                select id, session_key, workspace_root, summary_text, summary_message_id
                from agent_sessions
                where session_key = ? and user_id = ?
                limit 1
                """, (rs, rowNum) -> new AgentSession(
                rs.getLong("id"),
                rs.getString("session_key"),
                rs.getString("workspace_root"),
                rs.getString("summary_text"),
                rs.getObject("summary_message_id") == null ? 0L : rs.getLong("summary_message_id")
        ), sessionKey, auth.userId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        jdbcTemplate.update("""
                insert into agent_sessions (session_key, user_id, api_key_id, model_code, workspace_root, created_at, updated_at)
                values (?, ?, ?, ?, ?, now(), now())
                """, sessionKey, auth.userId(), auth.id(), modelCode, workspaceRoot.toString());
        Long sessionId = jdbcTemplate.queryForObject("select id from agent_sessions where session_key = ?", Long.class, sessionKey);
        return new AgentSession(sessionId, sessionKey, workspaceRoot.toString(), null, 0L);
    }

    private AgentSession compressSessionHistoryIfNeeded(AgentSession session, AgentSseSink sink) throws Exception {
        List<StoredAgentMessage> unsummarized = jdbcTemplate.query("""
                select id, role_code, source_type, content_text, tool_name
                from agent_messages
                where session_id = ? and id > ?
                order by id asc
                """, (rs, rowNum) -> new StoredAgentMessage(
                rs.getLong("id"),
                rs.getString("role_code"),
                rs.getString("source_type"),
                rs.getString("content_text"),
                rs.getString("tool_name")
        ), session.id(), session.summaryMessageId());

        if (unsummarized.size() <= AGENT_SUMMARY_TRIGGER_MESSAGES) {
            return session;
        }

        int splitIndex = Math.max(0, unsummarized.size() - AGENT_RECENT_MESSAGE_LIMIT);
        if (splitIndex <= 0) {
            return session;
        }

        List<StoredAgentMessage> toSummarize = unsummarized.subList(0, splitIndex);
        long summaryMessageId = toSummarize.get(toSummarize.size() - 1).id();
        String summaryText = mergeSessionSummary(session.summaryText(), toSummarize);

        jdbcTemplate.update("""
                update agent_sessions
                set summary_text = ?, summary_message_id = ?, updated_at = now()
                where id = ?
                """, summaryText, summaryMessageId, session.id());

        if (sink != null) {
            sink.emit(buildAgentStatusEvent("memory.compacted", Map.of(
                    "session_id", session.sessionKey(),
                    "compressed_messages", toSummarize.size()
            )));
        }

        return new AgentSession(session.id(), session.sessionKey(), session.workspaceRoot(), summaryText, summaryMessageId);
    }

    private ArrayNode loadSessionConversation(AgentSession session) {
        List<StoredAgentMessage> messages = jdbcTemplate.query("""
                select id, role_code, source_type, content_text, tool_name
                from agent_messages
                where session_id = ? and id > ?
                order by id asc
                """, (rs, rowNum) -> new StoredAgentMessage(
                rs.getLong("id"),
                rs.getString("role_code"),
                rs.getString("source_type"),
                rs.getString("content_text"),
                rs.getString("tool_name")
        ), session.id(), session.summaryMessageId());

        ArrayNode conversation = objectMapper.createArrayNode();
        if (session.summaryText() != null && !session.summaryText().isBlank()) {
            ObjectNode summaryMessage = objectMapper.createObjectNode();
            summaryMessage.put("role", "system");
            summaryMessage.put("content", "Compressed background context:\n" + session.summaryText());
            conversation.add(summaryMessage);
        }

        int start = Math.max(0, messages.size() - AGENT_RECENT_MESSAGE_LIMIT);
        for (int i = start; i < messages.size(); i++) {
            StoredAgentMessage item = messages.get(i);
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", item.roleCode());
            message.put("content", item.contentText());
            conversation.add(message);
        }
        return conversation;
    }

    private void persistMessages(Long sessionId, String responseId, ArrayNode messages, String sourceType) {
        for (JsonNode message : messages) {
            jdbcTemplate.update("""
                    insert into agent_messages (session_id, response_id, role_code, source_type, content_text, created_at)
                    values (?, ?, ?, ?, ?, now())
                    """,
                    sessionId,
                    responseId,
                    message.path("role").asText("user"),
                    sourceType,
                    message.path("content").asText("")
            );
        }
    }

    private void persistAssistantMessage(Long sessionId, String responseId, String text) {
        jdbcTemplate.update("""
                insert into agent_messages (session_id, response_id, role_code, source_type, content_text, created_at)
                values (?, ?, 'assistant', 'response', ?, now())
                """, sessionId, responseId, text);
    }

    private void persistToolConversation(Long sessionId,
                                         String responseId,
                                         String toolName,
                                         ObjectNode arguments,
                                         ObjectNode toolResult) {
        jdbcTemplate.update("""
                insert into agent_messages (session_id, response_id, role_code, source_type, content_text, tool_name, tool_payload_json, created_at)
                values (?, ?, 'assistant', 'tool_call', ?, ?, cast(? as json), now())
                """, sessionId, responseId,
                "{\"type\":\"tool\",\"tool_name\":\"" + toolName + "\"}",
                toolName,
                normalizeJson(arguments.toString()));
        jdbcTemplate.update("""
                insert into agent_messages (session_id, response_id, role_code, source_type, content_text, tool_name, tool_payload_json, created_at)
                values (?, ?, 'user', 'tool_result', ?, ?, cast(? as json), now())
                """, sessionId, responseId,
                "TOOL_RESULT " + toolResult,
                toolName,
                normalizeJson(toolResult.toString()));
    }

    private void saveToolLog(Long sessionId,
                             String responseId,
                             int step,
                             String toolName,
                             String title,
                             ObjectNode arguments,
                             ObjectNode result,
                             ArrayNode previews) {
        jdbcTemplate.update("""
                insert into agent_tool_logs (session_id, response_id, step_no, tool_name, title, success, arguments_json, result_json, file_previews_json, created_at)
                values (?, ?, ?, ?, ?, ?, cast(? as json), cast(? as json), cast(? as json), now())
                """,
                sessionId,
                responseId,
                step,
                toolName,
                title,
                result.path("ok").asBoolean(false) ? 1 : 0,
                normalizeJson(arguments.toString()),
                normalizeJson(result.toString()),
                normalizeJson(previews.toString()));
    }

    private void updateSessionLastResponse(Long sessionId, String responseId) {
        jdbcTemplate.update("""
                update agent_sessions
                set last_response_id = ?, updated_at = now()
                where id = ?
                """, responseId, sessionId);
    }

    private void initializeAgentTables() {
        jdbcTemplate.execute("""
                create table if not exists agent_sessions (
                    id bigint primary key auto_increment,
                    session_key varchar(96) not null,
                    user_id bigint not null,
                    api_key_id bigint not null,
                    model_code varchar(64) not null,
                    workspace_root varchar(512) null,
                    summary_text longtext null,
                    summary_message_id bigint not null default 0,
                    last_response_id varchar(96) null,
                    created_at datetime not null default current_timestamp,
                    updated_at datetime not null default current_timestamp on update current_timestamp,
                    unique key uk_agent_sessions_key (session_key),
                    key idx_agent_sessions_user (user_id, updated_at)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
        ensureColumnExists("agent_sessions", "summary_text", "alter table agent_sessions add column summary_text longtext null");
        ensureColumnExists("agent_sessions", "summary_message_id", "alter table agent_sessions add column summary_message_id bigint not null default 0");
        jdbcTemplate.execute("""
                create table if not exists agent_messages (
                    id bigint primary key auto_increment,
                    session_id bigint not null,
                    response_id varchar(96) not null,
                    role_code varchar(16) not null,
                    source_type varchar(32) not null,
                    content_text longtext null,
                    tool_name varchar(64) null,
                    tool_payload_json json null,
                    created_at datetime not null default current_timestamp,
                    key idx_agent_messages_session (session_id, id),
                    key idx_agent_messages_response (response_id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
        jdbcTemplate.execute("""
                create table if not exists agent_tool_logs (
                    id bigint primary key auto_increment,
                    session_id bigint not null,
                    response_id varchar(96) not null,
                    step_no int not null,
                    tool_name varchar(64) not null,
                    title varchar(128) null,
                    success tinyint(1) not null default 1,
                    arguments_json json null,
                    result_json json null,
                    file_previews_json json null,
                    created_at datetime not null default current_timestamp,
                    key idx_agent_tool_logs_session (session_id, id),
                    key idx_agent_tool_logs_response (response_id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
    }

    private void ensureColumnExists(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private String mapAnthropicStopReason(String stopReason) {
        return switch (stopReason) {
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            default -> "stop";
        };
    }

    private String mapOpenAiStopReason(String stopReason) {
        return switch (stopReason) {
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> "end_turn";
        };
    }

    private String buildAnthropicMessageStream(JsonNode anthropicResponse) throws Exception {
        String messageId = anthropicResponse.path("id").asText("msg_" + UUID.randomUUID().toString().replace("-", ""));
        String model = anthropicResponse.path("model").asText("");
        String text = extractAnthropicText(anthropicResponse.path("content"));
        String stopReason = anthropicResponse.path("stop_reason").asText("end_turn");
        int inputTokens = anthropicResponse.path("usage").path("input_tokens").asInt(0);
        int outputTokens = anthropicResponse.path("usage").path("output_tokens").asInt(0);

        ObjectNode startMessage = objectMapper.createObjectNode();
        startMessage.put("id", messageId);
        startMessage.put("type", "message");
        startMessage.put("role", "assistant");
        startMessage.put("model", model);
        startMessage.set("content", objectMapper.createArrayNode());
        startMessage.putNull("stop_reason");
        startMessage.putNull("stop_sequence");
        ObjectNode startUsage = objectMapper.createObjectNode();
        startUsage.put("input_tokens", inputTokens);
        startUsage.put("output_tokens", 0);
        startMessage.set("usage", startUsage);

        ObjectNode messageStart = objectMapper.createObjectNode();
        messageStart.put("type", "message_start");
        messageStart.set("message", startMessage);

        ObjectNode contentBlock = objectMapper.createObjectNode();
        contentBlock.put("type", "text");
        contentBlock.put("text", "");
        ObjectNode contentBlockStart = objectMapper.createObjectNode();
        contentBlockStart.put("type", "content_block_start");
        contentBlockStart.put("index", 0);
        contentBlockStart.set("content_block", contentBlock);

        ObjectNode delta = objectMapper.createObjectNode();
        delta.put("type", "text_delta");
        delta.put("text", text);
        ObjectNode contentBlockDelta = objectMapper.createObjectNode();
        contentBlockDelta.put("type", "content_block_delta");
        contentBlockDelta.put("index", 0);
        contentBlockDelta.set("delta", delta);

        ObjectNode contentBlockStop = objectMapper.createObjectNode();
        contentBlockStop.put("type", "content_block_stop");
        contentBlockStop.put("index", 0);

        ObjectNode messageDeltaDelta = objectMapper.createObjectNode();
        messageDeltaDelta.put("stop_reason", stopReason);
        messageDeltaDelta.putNull("stop_sequence");
        ObjectNode messageDeltaUsage = objectMapper.createObjectNode();
        messageDeltaUsage.put("output_tokens", outputTokens);
        ObjectNode messageDelta = objectMapper.createObjectNode();
        messageDelta.put("type", "message_delta");
        messageDelta.set("delta", messageDeltaDelta);
        messageDelta.set("usage", messageDeltaUsage);

        ObjectNode messageStop = objectMapper.createObjectNode();
        messageStop.put("type", "message_stop");

        StringBuilder builder = new StringBuilder();
        appendSseEvent(builder, "message_start", messageStart);
        appendSseEvent(builder, "content_block_start", contentBlockStart);
        appendSseEvent(builder, "content_block_delta", contentBlockDelta);
        appendSseEvent(builder, "content_block_stop", contentBlockStop);
        appendSseEvent(builder, "message_delta", messageDelta);
        appendSseEvent(builder, "message_stop", messageStop);
        return builder.toString();
    }

    private void appendSseEvent(StringBuilder builder, String eventName, JsonNode data) throws Exception {
        builder.append("event: ").append(eventName).append("\n");
        builder.append("data: ").append(objectMapper.writeValueAsString(data)).append("\n\n");
    }

    private interface AgentSseSink {
        void emit(JsonNode event) throws Exception;

        void emitFinalResponse(ObjectNode responseJson, String finalText) throws Exception;

        String fullBody();
    }

    private final class BufferingAgentSseSink implements AgentSseSink {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void emit(JsonNode event) throws Exception {
            builder.append("data: ").append(objectMapper.writeValueAsString(event)).append("\n\n");
        }

        @Override
        public void emitFinalResponse(ObjectNode responseJson, String finalText) throws Exception {
            JsonNode message = responseJson.path("output").get(0);
            String messageId = message.path("id").asText("msg_" + UUID.randomUUID().toString().replace("-", ""));

            ObjectNode itemAdded = objectMapper.createObjectNode();
            itemAdded.put("type", "response.output_item.added");
            itemAdded.put("output_index", 0);
            ObjectNode addedItem = objectMapper.createObjectNode();
            addedItem.put("id", messageId);
            addedItem.put("type", "message");
            addedItem.put("status", "in_progress");
            addedItem.put("role", "assistant");
            addedItem.set("content", objectMapper.createArrayNode());
            itemAdded.set("item", addedItem);
            emit(itemAdded);

            ObjectNode deltaEvent = objectMapper.createObjectNode();
            deltaEvent.put("type", "response.output_text.delta");
            deltaEvent.put("item_id", messageId);
            deltaEvent.put("output_index", 0);
            deltaEvent.put("content_index", 0);
            deltaEvent.put("delta", finalText);
            emit(deltaEvent);

            ObjectNode doneEvent = objectMapper.createObjectNode();
            doneEvent.put("type", "response.output_text.done");
            doneEvent.put("item_id", messageId);
            doneEvent.put("output_index", 0);
            doneEvent.put("content_index", 0);
            doneEvent.put("text", finalText);
            emit(doneEvent);

            ObjectNode itemDone = objectMapper.createObjectNode();
            itemDone.put("type", "response.output_item.done");
            itemDone.put("output_index", 0);
            itemDone.set("item", message);
            emit(itemDone);

            ObjectNode completedEvent = objectMapper.createObjectNode();
            completedEvent.put("type", "response.completed");
            completedEvent.set("response", responseJson);
            emit(completedEvent);
            builder.append("data: [DONE]\n\n");
        }

        @Override
        public String fullBody() {
            return builder.toString();
        }
    }

    private final class StreamingAgentSseSink implements AgentSseSink {
        private final OutputStream outputStream;
        private final StringBuilder builder = new StringBuilder();

        private StreamingAgentSseSink(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void emit(JsonNode event) throws Exception {
            String chunk = "data: " + objectMapper.writeValueAsString(event) + "\n\n";
            builder.append(chunk);
            outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        @Override
        public void emitFinalResponse(ObjectNode responseJson, String finalText) throws Exception {
            JsonNode message = responseJson.path("output").get(0);
            String messageId = message.path("id").asText("msg_" + UUID.randomUUID().toString().replace("-", ""));

            ObjectNode itemAdded = objectMapper.createObjectNode();
            itemAdded.put("type", "response.output_item.added");
            itemAdded.put("output_index", 0);
            ObjectNode addedItem = objectMapper.createObjectNode();
            addedItem.put("id", messageId);
            addedItem.put("type", "message");
            addedItem.put("status", "in_progress");
            addedItem.put("role", "assistant");
            addedItem.set("content", objectMapper.createArrayNode());
            itemAdded.set("item", addedItem);
            emit(itemAdded);

            ObjectNode deltaEvent = objectMapper.createObjectNode();
            deltaEvent.put("type", "response.output_text.delta");
            deltaEvent.put("item_id", messageId);
            deltaEvent.put("output_index", 0);
            deltaEvent.put("content_index", 0);
            deltaEvent.put("delta", finalText);
            emit(deltaEvent);

            ObjectNode doneEvent = objectMapper.createObjectNode();
            doneEvent.put("type", "response.output_text.done");
            doneEvent.put("item_id", messageId);
            doneEvent.put("output_index", 0);
            doneEvent.put("content_index", 0);
            doneEvent.put("text", finalText);
            emit(doneEvent);

            ObjectNode itemDone = objectMapper.createObjectNode();
            itemDone.put("type", "response.output_item.done");
            itemDone.put("output_index", 0);
            itemDone.set("item", message);
            emit(itemDone);

            ObjectNode completedEvent = objectMapper.createObjectNode();
            completedEvent.put("type", "response.completed");
            completedEvent.set("response", responseJson);
            emit(completedEvent);
        }

        private void emitError(String message) throws Exception {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("type", "error");
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", message == null ? "Unknown agent error" : message);
            error.put("type", "server_error");
            event.set("error", error);
            emit(event);
        }

        private void finish() throws Exception {
            String done = "data: [DONE]\n\n";
            builder.append(done);
            outputStream.write(done.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        @Override
        public String fullBody() {
            return builder.toString();
        }
    }

    private boolean isAnthropicRoute(GatewayRouteService.RouteDefinition route) {
        return "ANTHROPIC".equalsIgnoreCase(route.providerType());
    }

    private String resolveAnthropicEndpoint(String baseUrl) {
        if (baseUrl.endsWith("/v1/messages")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/messages";
        }
        return baseUrl + "/v1/messages";
    }

    @Transactional
    protected void logAndCharge(ApiKeyAuthService.AuthenticatedApiKey auth,
                                GatewayRouteService.RouteDefinition route,
                                String requestId,
                                HttpServletRequest servletRequest,
                                String requestBody,
                                String responseBody,
                                int promptTokens,
                                int completionTokens,
                                int totalTokens,
                                BigDecimal userAmount,
                                BigDecimal costAmount,
                                int latencyMs,
                                int statusCode,
                                boolean success,
                                String errorMessage) {
        jdbcTemplate.update("""
                insert into request_logs (request_id, user_id, api_key_id, model_code, provider_id, provider_token_id, upstream_model,
                                          request_path, request_method, request_ip, request_body_json, response_body_json,
                                          prompt_tokens, completion_tokens, total_tokens, user_amount, cost_amount, latency_ms,
                                          success, status_code, error_message, request_date, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as json), cast(? as json), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                requestId,
                auth.userId(),
                auth.id(),
                route.modelCode(),
                route.providerId(),
                route.providerTokenId(),
                route.upstreamModel(),
                servletRequest.getRequestURI(),
                servletRequest.getMethod(),
                extractRequestIp(servletRequest),
                normalizeJson(requestBody),
                normalizeJson(responseBody),
                promptTokens,
                completionTokens,
                totalTokens,
                userAmount,
                costAmount,
                latencyMs,
                success ? 1 : 0,
                statusCode,
                errorMessage,
                LocalDate.now()
        );

        if (userAmount.compareTo(BigDecimal.ZERO) > 0) {
            jdbcTemplate.update("""
                    update wallets
                    set balance = balance - ?, total_consume = total_consume + ?, updated_at = now()
                    where user_id = ?
                    """, userAmount, userAmount, auth.userId());

            Long walletId = jdbcTemplate.queryForObject("select id from wallets where user_id = ?", Long.class, auth.userId());
            BigDecimal balanceAfter = jdbcTemplate.queryForObject("select balance from wallets where user_id = ?", BigDecimal.class, auth.userId());
            BigDecimal balanceBefore = balanceAfter == null ? BigDecimal.ZERO : balanceAfter.add(userAmount);
            jdbcTemplate.update("""
                    insert into transactions (user_id, wallet_id, order_no, transaction_type, direction, amount,
                                              balance_before, balance_after, status, description_text, transaction_date)
                    values (?, ?, ?, 'CONSUME', 'OUT', ?, ?, ?, 'SUCCESS', ?, curdate())
                    """,
                    auth.userId(),
                    walletId,
                    "C" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(),
                    userAmount,
                    balanceBefore,
                    balanceAfter,
                    "模型调用扣费: " + route.modelCode()
            );

            jdbcTemplate.update("""
                    update api_keys
                    set used_quota = used_quota + ?, updated_at = now()
                    where id = ?
                    """, userAmount, auth.id());
        }

        jdbcTemplate.update("""
                insert into usage_daily (stat_date, user_id, model_code, provider_id, request_count, success_count, total_tokens, user_amount, cost_amount)
                values (?, ?, ?, ?, 1, ?, ?, ?, ?)
                on duplicate key update
                    request_count = request_count + 1,
                    success_count = success_count + values(success_count),
                    total_tokens = total_tokens + values(total_tokens),
                    user_amount = user_amount + values(user_amount),
                    cost_amount = cost_amount + values(cost_amount),
                    updated_at = now()
                """,
                LocalDate.now(),
                auth.userId(),
                route.modelCode(),
                route.providerId(),
                success ? 1 : 0,
                totalTokens,
                userAmount,
                costAmount
        );
    }

    private BigDecimal calculateBaseCost(GatewayRouteService.RouteDefinition route, int promptTokens, int completionTokens) {
        BigDecimal promptCost = route.promptPrice() == null ? BigDecimal.ZERO :
                route.promptPrice().multiply(BigDecimal.valueOf(promptTokens)).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        BigDecimal completionCost = route.completionPrice() == null ? BigDecimal.ZERO :
                route.completionPrice().multiply(BigDecimal.valueOf(completionTokens)).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        BigDecimal requestCost = route.requestPrice() == null ? BigDecimal.ZERO : route.requestPrice();
        return promptCost.add(completionCost).add(requestCost).setScale(6, RoundingMode.HALF_UP);
    }

    private ObjectNode ensureChatStreamUsageIncluded(ObjectNode request) {
        request.put("stream", true);
        ObjectNode streamOptions = request.hasNonNull("stream_options") && request.path("stream_options").isObject()
                ? (ObjectNode) request.path("stream_options")
                : objectMapper.createObjectNode();
        streamOptions.put("include_usage", true);
        request.set("stream_options", streamOptions);
        return request;
    }

    private UsageTotals extractChatUsage(JsonNode responseJson) {
        JsonNode usage = responseJson == null ? null : responseJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("completion_tokens").asInt(0);
        int totalTokens = usage == null ? 0 : usage.path("total_tokens").asInt(promptTokens + completionTokens);
        return new UsageTotals(promptTokens, completionTokens, totalTokens);
    }

    private UsageTotals extractResponsesUsage(JsonNode responseJson) {
        JsonNode usage = responseJson == null ? null : responseJson.path("usage");
        int promptTokens = usage == null ? 0 : usage.path("input_tokens").asInt(0);
        int completionTokens = usage == null ? 0 : usage.path("output_tokens").asInt(0);
        int totalTokens = usage == null ? 0 : usage.path("total_tokens").asInt(promptTokens + completionTokens);
        return new UsageTotals(promptTokens, completionTokens, totalTokens);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(401, "缺少 API Key");
        }
        return authorization.substring(7);
    }

    private String getRequiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "缺少参数: " + fieldName);
        }
        return value;
    }

    private String buildRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String extractRequestIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private JsonNode tryReadJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            objectMapper.readTree(body);
            return body;
        } catch (Exception ex) {
            try {
                return objectMapper.writeValueAsString(body);
            } catch (Exception ignored) {
                return "\"log-body-unavailable\"";
            }
        }
    }

    private record UpstreamTextResponse(
            int statusCode,
            String body
    ) {
    }

    private record UsageTotals(
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {
    }

    private record AnthropicMessageExecution(
            int statusCode,
            String body
    ) {
    }

    private record AgentDecision(
            String type,
            String title,
            String toolName,
            ObjectNode arguments,
            String finalText,
            String analysis
    ) {
    }

    private record AgentModelTurn(
            String text,
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {
    }

    private record AgentExecutionResult(
            ObjectNode responseJson,
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {
    }

    private record AgentSession(
            Long id,
            String sessionKey,
            String workspaceRoot,
            String summaryText,
            Long summaryMessageId
    ) {
    }

    private record StoredAgentMessage(
            Long id,
            String roleCode,
            String sourceType,
            String contentText,
            String toolName
    ) {
    }

    private record AgentToolExecution(
            String toolName,
            String title,
            boolean success,
            ObjectNode modelVisibleResult,
            ArrayNode filePreviews,
            ObjectNode summaryJson
    ) {
    }

    private record HeuristicAgentOutcome(
            AgentToolExecution toolExecution,
            String finalText
    ) {
    }

    private record ProcessResult(
            int exitCode,
            String stdout,
            String stderr
    ) {
    }
}
