package com.zxw.modules.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.AesCryptoService;
import com.zxw.modules.model.dto.ModelBatchImportRequest;
import com.zxw.modules.model.dto.ModelBatchImportResponse;
import com.zxw.modules.model.dto.ModelCreateRequest;
import com.zxw.modules.model.dto.ModelListItemResponse;
import com.zxw.modules.model.dto.ModelUpdateRequest;
import com.zxw.modules.model.dto.UpstreamModelOptionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminModelService {

    private final JdbcTemplate jdbcTemplate;
    private final AesCryptoService aesCryptoService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AdminModelService(JdbcTemplate jdbcTemplate,
                             AesCryptoService aesCryptoService,
                             ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesCryptoService = aesCryptoService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public List<ModelListItemResponse> listModels() {
        if (!AdminContext.isAdmin()) {
            return jdbcTemplate.query("""
                    select m.id, m.model_code, m.model_name, m.model_type, m.billing_type, m.prompt_price,
                           m.completion_price, m.multiplier, m.is_public, m.status, m.created_at,
                           p.id as provider_id, p.provider_name, p.provider_type, r.upstream_model
                    from models m
                    left join model_routes r on r.model_id = m.id and r.status = 'ACTIVE'
                    left join providers p on p.id = r.provider_id
                    where m.deleted = 0 and m.status = 'ACTIVE' and m.is_public = 1
                    order by m.id desc
                    """, (rs, rowNum) -> new ModelListItemResponse(
                    rs.getLong("id"),
                    rs.getString("model_code"),
                    rs.getString("model_name"),
                    rs.getString("model_type"),
                    rs.getString("billing_type"),
                    rs.getBigDecimal("prompt_price"),
                    rs.getBigDecimal("completion_price"),
                    rs.getBigDecimal("multiplier"),
                    rs.getInt("is_public"),
                    rs.getString("status"),
                    rs.getObject("provider_id") == null ? null : rs.getLong("provider_id"),
                    rs.getString("provider_name"),
                    rs.getString("provider_type"),
                    rs.getString("upstream_model"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            ));
        }

        return jdbcTemplate.query("""
                select m.id, m.model_code, m.model_name, m.model_type, m.billing_type, m.prompt_price,
                       m.completion_price, m.multiplier, m.is_public, m.status, m.created_at,
                       p.id as provider_id, p.provider_name, p.provider_type, r.upstream_model
                from models m
                left join model_routes r on r.model_id = m.id and r.status = 'ACTIVE'
                left join providers p on p.id = r.provider_id
                where m.deleted = 0
                order by m.id desc
                """, (rs, rowNum) -> new ModelListItemResponse(
                rs.getLong("id"),
                rs.getString("model_code"),
                rs.getString("model_name"),
                rs.getString("model_type"),
                rs.getString("billing_type"),
                rs.getBigDecimal("prompt_price"),
                rs.getBigDecimal("completion_price"),
                rs.getBigDecimal("multiplier"),
                rs.getInt("is_public"),
                rs.getString("status"),
                rs.getObject("provider_id") == null ? null : rs.getLong("provider_id"),
                rs.getString("provider_name"),
                rs.getString("provider_type"),
                rs.getString("upstream_model"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public List<UpstreamModelOptionResponse> fetchUpstreamModels(Long providerId) {
        AdminContext.requireAdmin();
        ProviderAccess provider = loadProviderAccess(providerId);
        HttpRequest request = buildModelListRequest(provider);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(400, "鎷夊彇涓婃父妯″瀷澶辫触: " + extractErrorMessage(response.body(), response.statusCode()));
            }
            return parseUpstreamModels(response.body(), provider);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(500, "鎷夊彇涓婃父妯″瀷澶辫触: " + ex.getMessage());
        }
    }

    @Transactional
    public void create(ModelCreateRequest request) {
        AdminContext.requireAdmin();
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from models where model_code = ?",
                Integer.class,
                request.modelCode()
        );
        if (exists != null && exists > 0) {
            throw new BusinessException("Model code already exists");
        }

        insertModelWithRoute(
                request.modelCode(),
                request.modelName(),
                blankToDefault(request.modelType(), "CHAT"),
                blankToDefault(request.billingType(), "TOKEN"),
                numberOrZero(request.promptPrice()),
                numberOrZero(request.completionPrice()),
                numberOrZero(request.requestPrice()),
                numberOrZero(request.imagePrice()),
                request.multiplier() == null ? BigDecimal.ONE : request.multiplier(),
                Boolean.TRUE.equals(request.isPublic()),
                request.providerId(),
                request.upstreamModel()
        );
    }

    @Transactional
    public void update(Long id, ModelUpdateRequest request) {
        AdminContext.requireAdmin();
        int updated = jdbcTemplate.update("""
                update models
                set model_name = ?, model_type = ?, billing_type = ?, prompt_price = ?, completion_price = ?,
                    multiplier = ?, is_public = ?, updated_at = now()
                where id = ? and deleted = 0
                """,
                trimToLength(request.modelName(), 64),
                blankToDefault(request.modelType(), "CHAT"),
                blankToDefault(request.billingType(), "TOKEN"),
                numberOrZero(request.promptPrice()),
                numberOrZero(request.completionPrice()),
                request.multiplier() == null ? BigDecimal.ONE : request.multiplier(),
                Boolean.TRUE.equals(request.isPublic()) ? 1 : 0,
                id
        );
        if (updated == 0) {
            throw new BusinessException("Model does not exist");
        }

        replaceModelRoutes(id, request.providerId(), request.upstreamModel());
    }

    @Transactional
    public ModelBatchImportResponse batchImport(ModelBatchImportRequest request) {
        AdminContext.requireAdmin();
        loadProviderAccess(request.providerId());

        Set<String> upstreamModels = new LinkedHashSet<>();
        for (String item : request.upstreamModels()) {
            if (item != null && !item.isBlank()) {
                upstreamModels.add(item.trim());
            }
        }
        if (upstreamModels.isEmpty()) {
            throw new BusinessException("Please select at least one upstream model");
        }

        List<String> importedModels = new ArrayList<>();
        List<String> skippedModels = new ArrayList<>();

        for (String upstreamModel : upstreamModels) {
            if (routeExists(request.providerId(), upstreamModel)) {
                skippedModels.add(upstreamModel);
                continue;
            }

            String modelCode = buildUniqueModelCode(normalizeModelCode(upstreamModel));
            String modelName = trimToLength(upstreamModel, 64);
            insertModelWithRoute(
                    modelCode,
                    modelName,
                    "CHAT",
                    "TOKEN",
                    numberOrZero(request.promptPrice()),
                    numberOrZero(request.completionPrice()),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    request.multiplier() == null ? BigDecimal.ONE : request.multiplier(),
                    request.isPublic() == null || request.isPublic(),
                    request.providerId(),
                    upstreamModel
            );
            importedModels.add(modelCode);
        }

        return new ModelBatchImportResponse(
                importedModels.size(),
                skippedModels.size(),
                importedModels,
                skippedModels
        );
    }

    public void updateStatus(Long id, String status) {
        AdminContext.requireAdmin();
        int updated = jdbcTemplate.update("""
                update models
                set status = ?, updated_at = now()
                where id = ? and deleted = 0
                """, status, id);
        if (updated == 0) {
            throw new BusinessException("Model does not exist");
        }
    }

    @Transactional
    public void delete(Long id) {
        AdminContext.requireAdmin();
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from models where id = ? and deleted = 0",
                Integer.class,
                id
        );
        if (exists == null || exists == 0) {
            throw new BusinessException("Model does not exist");
        }

        jdbcTemplate.update("""
                delete from model_routes
                where model_id = ?
                """, id);
        jdbcTemplate.update("""
                delete from models
                where id = ?
                """, id);
    }

    private void insertModelWithRoute(String modelCode,
                                      String modelName,
                                      String modelType,
                                      String billingType,
                                      BigDecimal promptPrice,
                                      BigDecimal completionPrice,
                                      BigDecimal requestPrice,
                                      BigDecimal imagePrice,
                                      BigDecimal multiplier,
                                      boolean isPublic,
                                      Long providerId,
                                      String upstreamModel) {
        try {
            jdbcTemplate.update("""
                    insert into models (model_code, model_name, model_type, billing_type, prompt_price, completion_price,
                                        request_price, image_price, multiplier, is_public, status)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """,
                    trimToLength(modelCode, 64),
                    trimToLength(modelName, 64),
                    modelType,
                    billingType,
                    promptPrice,
                    completionPrice,
                    requestPrice,
                    imagePrice,
                    multiplier,
                    isPublic ? 1 : 0
            );
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("Model code already exists");
        }

        Long modelId = jdbcTemplate.queryForObject(
                "select id from models where model_code = ?",
                Long.class,
                trimToLength(modelCode, 64)
        );
        jdbcTemplate.update("""
                insert into model_routes (model_id, provider_id, provider_token_id, upstream_model, route_type, priority_no, status)
                values (?, ?, null, ?, 'PRIMARY', 100, 'ACTIVE')
                """, modelId, providerId, trimToLength(upstreamModel, 128));
    }

    private void replaceModelRoutes(Long modelId, Long providerId, String upstreamModel) {
        jdbcTemplate.update("""
                delete from model_routes
                where model_id = ?
                """, modelId);
        jdbcTemplate.update("""
                insert into model_routes (model_id, provider_id, provider_token_id, upstream_model, route_type, priority_no, status)
                values (?, ?, null, ?, 'PRIMARY', 100, 'ACTIVE')
                """, modelId, providerId, trimToLength(upstreamModel, 128));
    }

    private ProviderAccess loadProviderAccess(Long providerId) {
        List<ProviderAccess> providers = jdbcTemplate.query("""
                select p.id, p.provider_name, p.base_url, p.provider_type, p.timeout_ms, t.token_value_encrypted
                from providers p
                join provider_tokens t on t.provider_id = p.id and t.deleted = 0 and t.status = 'ACTIVE'
                where p.id = ? and p.deleted = 0 and p.status = 'ACTIVE'
                order by t.weight_no desc, t.id asc
                limit 1
                """, (rs, rowNum) -> new ProviderAccess(
                rs.getLong("id"),
                rs.getString("provider_name"),
                rs.getString("base_url"),
                rs.getString("provider_type"),
                rs.getInt("timeout_ms"),
                aesCryptoService.decrypt(rs.getString("token_value_encrypted"))
        ), providerId);

        if (providers.isEmpty()) {
            throw new BusinessException("娓犻亾涓嶅瓨鍦紝鎴栬娓犻亾杩樻病鏈夊彲鐢?Token");
        }
        return providers.get(0);
    }

    private HttpRequest buildModelListRequest(ProviderAccess provider) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofMillis(provider.timeoutMs()))
                .GET()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if ("ANTHROPIC".equalsIgnoreCase(provider.providerType())) {
            return builder
                    .uri(URI.create(resolveAnthropicModelsEndpoint(provider.baseUrl())))
                    .header("x-api-key", provider.token())
                    .header("anthropic-version", "2023-06-01")
                    .build();
        }

        return builder
                .uri(URI.create(resolveOpenAiModelsEndpoint(provider.baseUrl())))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.token())
                .build();
    }

    private List<UpstreamModelOptionResponse> parseUpstreamModels(String body, ProviderAccess provider) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode items = root.path("data");
        if (!items.isArray()) {
            items = root.path("models");
        }
        if (!items.isArray()) {
            throw new BusinessException(400, "涓婃父娌℃湁杩斿洖鍙瘑鍒殑妯″瀷鍒楄〃");
        }

        List<UpstreamModelOptionResponse> result = new ArrayList<>();
        for (JsonNode item : items) {
            String id = text(item, "id");
            if (id.isBlank()) {
                id = text(item, "name");
            }
            if (id.isBlank()) {
                continue;
            }
            String displayName = firstNonBlank(
                    text(item, "display_name"),
                    text(item, "name"),
                    id
            );
            result.add(new UpstreamModelOptionResponse(
                    id,
                    displayName,
                    firstNonBlank(text(item, "owned_by"), provider.providerName()),
                    provider.providerType()
            ));
        }

        result.sort(Comparator.comparing(UpstreamModelOptionResponse::id, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private boolean routeExists(Long providerId, String upstreamModel) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from model_routes r
                join models m on m.id = r.model_id
                where r.provider_id = ? and r.upstream_model = ? and r.status = 'ACTIVE'
                  and m.deleted = 0
                """, Integer.class, providerId, trimToLength(upstreamModel, 128));
        return count != null && count > 0;
    }

    private String buildUniqueModelCode(String baseCode) {
        String normalized = trimToLength(baseCode.isBlank() ? "model" : baseCode, 64);
        if (!modelCodeExists(normalized)) {
            return normalized;
        }

        for (int i = 2; i < 1000; i++) {
            String suffix = "-" + i;
            String candidate = trimToLength(normalized, 64 - suffix.length()) + suffix;
            if (!modelCodeExists(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("鑷姩鐢熸垚妯″瀷缂栫爜澶辫触锛岃绋嶅悗閲嶈瘯");
    }

    private boolean modelCodeExists(String modelCode) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from models where model_code = ?",
                Integer.class,
                modelCode
        );
        return count != null && count > 0;
    }

    private String normalizeModelCode(String upstreamModel) {
        String normalized = upstreamModel.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? "model" : normalized;
    }

    private String resolveOpenAiModelsEndpoint(String baseUrl) {
        if (baseUrl.endsWith("/models")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/models";
        }
        return baseUrl + "/v1/models";
    }

    private String resolveAnthropicModelsEndpoint(String baseUrl) {
        if (baseUrl.endsWith("/v1/models")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/models";
        }
        if (baseUrl.endsWith("/v1/messages")) {
            return baseUrl.substring(0, baseUrl.length() - "/messages".length()) + "/models";
        }
        return baseUrl + "/v1/models";
    }

    private String extractErrorMessage(String body, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String errorMessage = firstNonBlank(
                    text(root.path("error"), "message"),
                    text(root, "message"),
                    text(root, "error")
            );
            if (!errorMessage.isBlank()) {
                return errorMessage;
            }
        } catch (Exception ignored) {
        }
        return "HTTP " + statusCode;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private BigDecimal numberOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record ProviderAccess(
            Long id,
            String providerName,
            String baseUrl,
            String providerType,
            Integer timeoutMs,
            String token
    ) {
    }
}

