package com.zxw.modules.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.AesCryptoService;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.modules.gateway.service.GatewayRouteService;
import com.zxw.modules.model.dto.ModelBatchImportRequest;
import com.zxw.modules.model.dto.ModelBatchImportResponse;
import com.zxw.modules.model.dto.ModelGroupItemResponse;
import com.zxw.modules.model.dto.ModelCreateRequest;
import com.zxw.modules.model.dto.ModelListItemResponse;
import com.zxw.modules.model.dto.ModelRouteRepairResponse;
import com.zxw.modules.model.dto.ModelUpdateRequest;
import com.zxw.modules.model.dto.UpstreamModelOptionResponse;
import com.zxw.persistence.entity.ModelEntity;
import com.zxw.persistence.entity.ModelGroupModelEntity;
import com.zxw.persistence.entity.ModelRouteEntity;
import com.zxw.persistence.mapper.ModelAdminQueryMapper;
import com.zxw.persistence.mapper.ModelGroupModelMapper;
import com.zxw.persistence.mapper.ModelMapper;
import com.zxw.persistence.mapper.ModelRouteMapper;
import com.zxw.persistence.model.ExistingRouteModelView;
import com.zxw.persistence.model.ModelAdminListView;
import com.zxw.persistence.model.ProviderAccessView;
import com.zxw.persistence.model.GatewayRouteRow;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
/**
 * 模型管理服务。
 * 负责模型创建、更新、删除、批量导入以及上游模型拉取。
 */
public class AdminModelService {

    private static final BigDecimal DEFAULT_REQUEST_PRICE = new BigDecimal("0.200000");

    private final ModelMapper modelMapper;
    private final ModelRouteMapper modelRouteMapper;
    private final ModelGroupModelMapper modelGroupModelMapper;
    private final ModelAdminQueryMapper modelAdminQueryMapper;
    private final AesCryptoService aesCryptoService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final UserModelAccessService userModelAccessService;
    private final GatewayRouteService gatewayRouteService;

    public AdminModelService(ModelMapper modelMapper,
                             ModelRouteMapper modelRouteMapper,
                             ModelGroupModelMapper modelGroupModelMapper,
                             ModelAdminQueryMapper modelAdminQueryMapper,
                             AesCryptoService aesCryptoService,
                             ObjectMapper objectMapper,
                             UserModelAccessService userModelAccessService,
                             GatewayRouteService gatewayRouteService) {
        this.modelMapper = modelMapper;
        this.modelRouteMapper = modelRouteMapper;
        this.modelGroupModelMapper = modelGroupModelMapper;
        this.modelAdminQueryMapper = modelAdminQueryMapper;
        this.aesCryptoService = aesCryptoService;
        this.objectMapper = objectMapper;
        this.userModelAccessService = userModelAccessService;
        this.gatewayRouteService = gatewayRouteService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    /**
     * 查询模型列表。
     */
    public List<ModelListItemResponse> listModels() {
        // 查询模型列表并聚合同一模型关联到的多个套餐组
        Map<Long, ModelAdminListView> uniqueModels = new LinkedHashMap<>();
        Map<Long, List<ModelGroupItemResponse>> groupsByModelId = new LinkedHashMap<>();
        Map<Long, Set<Long>> seenGroupIdsByModelId = new LinkedHashMap<>();
        for (ModelAdminListView item : modelAdminQueryMapper.listModels(AdminContext.isAdmin())) {
            uniqueModels.putIfAbsent(item.getId(), item);
            if (item.getGroupId() == null) {
                continue;
            }
            Set<Long> seenGroupIds = seenGroupIdsByModelId.computeIfAbsent(item.getId(), key -> new LinkedHashSet<>());
            if (seenGroupIds.add(item.getGroupId())) {
                groupsByModelId.computeIfAbsent(item.getId(), key -> new ArrayList<>())
                        .add(new ModelGroupItemResponse(item.getGroupId(), item.getGroupCode(), item.getGroupName()));
            }
        }
        return uniqueModels.values().stream()
                .map(item -> toModelListItemResponse(item, groupsByModelId.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    /**
     * 从上游渠道拉取可用模型列表。
     */
    public List<UpstreamModelOptionResponse> fetchUpstreamModels(Long providerId) {
        AdminContext.requireAdmin();
        // 读取渠道访问凭证并构造远程请求
        ProviderAccess provider = loadProviderAccess(providerId);
        HttpRequest request = buildModelListRequest(provider);

        try {
            // 调用上游接口拉取模型列表
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(400, "Failed to fetch upstream models: " + extractErrorMessage(response.body(), response.statusCode()));
            }
            return parseUpstreamModels(response.body(), provider);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(500, "Failed to fetch upstream models: " + ex.getMessage());
        }
    }

    @Transactional
    /**
     * 创建模型、默认路由和分组绑定。
     */
    public void create(ModelCreateRequest request) {
        AdminContext.requireAdmin();
        // 新建前先校验模型编码唯一性
        if (modelMapper.existsByModelCode(request.modelCode())) {
            throw new BusinessException("Model code already exists");
        }

        String canonicalUpstreamModel = resolveCanonicalUpstreamModel(request.providerId(), request.upstreamModel());

        // 同时创建模型主记录、主路由以及默认分组绑定
        insertModelWithRoute(
                request.modelCode(),
                request.modelName(),
                blankToDefault(request.modelType(), "CHAT"),
                blankToDefault(request.billingType(), "TOKEN"),
                numberOrZero(request.promptPrice()),
                numberOrZero(request.cachedPromptPrice()),
                numberOrZero(request.completionPrice()),
                request.requestPrice() == null ? DEFAULT_REQUEST_PRICE : numberOrZero(request.requestPrice()),
                numberOrZero(request.imagePrice()),
                request.multiplier() == null ? BigDecimal.ONE : request.multiplier(),
                Boolean.TRUE.equals(request.isPublic()),
                request.groupId(),
                request.providerId(),
                canonicalUpstreamModel
        );
        gatewayRouteService.evictRouteCache();
    }

    @Transactional
    /**
     * 更新模型配置、路由和分组绑定。
     */
    public void update(Long id, ModelUpdateRequest request) {
        AdminContext.requireAdmin();
        String canonicalUpstreamModel = resolveCanonicalUpstreamModel(request.providerId(), request.upstreamModel());
        // 先更新模型自身配置
        ModelEntity updateModel = new ModelEntity();
        updateModel.setModelName(trimToLength(request.modelName(), 64));
        updateModel.setModelType(blankToDefault(request.modelType(), "CHAT"));
        updateModel.setBillingType(blankToDefault(request.billingType(), "TOKEN"));
        updateModel.setPromptPrice(numberOrZero(request.promptPrice()));
        updateModel.setCachedPromptPrice(numberOrZero(request.cachedPromptPrice()));
        updateModel.setCompletionPrice(numberOrZero(request.completionPrice()));
        updateModel.setRequestPrice(request.requestPrice() == null ? DEFAULT_REQUEST_PRICE : numberOrZero(request.requestPrice()));
        updateModel.setMultiplier(request.multiplier() == null ? BigDecimal.ONE : request.multiplier());
        updateModel.setIsPublic(Boolean.TRUE.equals(request.isPublic()) ? 1 : 0);
        int updated = modelMapper.updateActiveById(id, updateModel);
        if (updated == 0) {
            throw new BusinessException("Model does not exist");
        }

        // 再同步路由、分组绑定以及分组价格继承关系
        replaceModelRoutes(id, request.providerId(), canonicalUpstreamModel);
        syncModelGroupBinding(id, request.bindingId(), request.groupId());
        userModelAccessService.clearModelGroupBindingPrices(id);
        gatewayRouteService.evictRouteCache();
    }

    @Transactional
    /**
     * 批量导入上游模型并建立分组绑定。
     */
    public ModelBatchImportResponse batchImport(ModelBatchImportRequest request) {
        AdminContext.requireAdmin();
        // 仅校验渠道可访问，不在这里直接发拉取请求
        loadProviderAccess(request.providerId());
        Map<String, String> canonicalUpstreamModels = loadCanonicalUpstreamModelMap(request.providerId());

        // 去重并过滤空模型名
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

        // 逐个处理：能复用就复用，不能复用就新建
        for (String upstreamModel : upstreamModels) {
            String canonicalUpstreamModel = findCanonicalUpstreamModel(canonicalUpstreamModels, upstreamModel);
            if (canonicalUpstreamModel == null) {
                skippedModels.add(upstreamModel);
                continue;
            }

            ExistingRouteModel existingRouteModel = findExistingRouteModel(request.providerId(), canonicalUpstreamModel);
            if (existingRouteModel != null) {
                if (isModelBoundToGroup(existingRouteModel.modelId(), request.groupId())) {
                    skippedModels.add(canonicalUpstreamModel);
                    continue;
                }
                userModelAccessService.addModelGroupBinding(existingRouteModel.modelId(), request.groupId());
                importedModels.add(existingRouteModel.modelCode());
                continue;
            }

            String modelCode = normalizeModelCode(canonicalUpstreamModel);
            Long existingModelId = findModelIdByCode(modelCode);
            if (existingModelId != null) {
                ensurePrimaryRoute(existingModelId, request.providerId(), canonicalUpstreamModel);
                if (isModelBoundToGroup(existingModelId, request.groupId())) {
                    skippedModels.add(canonicalUpstreamModel);
                    continue;
                }
                userModelAccessService.addModelGroupBinding(existingModelId, request.groupId());
                importedModels.add(modelCode);
                continue;
            }

            String modelName = trimToLength(upstreamModel, 64);
            insertModelWithRoute(
                    modelCode,
                    modelName,
                    "CHAT",
                    "TOKEN",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    DEFAULT_REQUEST_PRICE,
                    BigDecimal.ZERO,
                    BigDecimal.ONE,
                    request.isPublic() == null || request.isPublic(),
                    request.groupId(),
                    request.providerId(),
                    canonicalUpstreamModel
            );
            importedModels.add(modelCode);
        }

        gatewayRouteService.evictRouteCache();
        return new ModelBatchImportResponse(
                importedModels.size(),
                skippedModels.size(),
                importedModels,
                skippedModels
        );
    }

    @Transactional
    public ModelRouteRepairResponse repairProviderRoutes(Long providerId) {
        AdminContext.requireAdmin();
        loadProviderAccess(providerId);

        Map<String, String> canonicalByLowerCaseId = loadCanonicalUpstreamModelMap(providerId);

        List<String> repairedModels = new ArrayList<>();
        List<String> skippedModels = new ArrayList<>();

        for (GatewayRouteRow route : modelRouteMapper.selectActiveRoutesByProviderId(providerId)) {
            String current = trimToLength(route.getUpstreamModel(), 128);
            if (current == null || current.isBlank()) {
                skippedModels.add(route.getModelCode() + ":blank");
                continue;
            }
            String canonical = canonicalByLowerCaseId.get(current.toLowerCase(Locale.ROOT));
            if (canonical == null) {
                skippedModels.add(route.getModelCode() + ":" + current);
                continue;
            }
            if (canonical.equals(current)) {
                continue;
            }
            int updated = modelRouteMapper.updateUpstreamModel(route.getModelId(), providerId, current, canonical);
            if (updated > 0) {
                repairedModels.add(route.getModelCode() + ":" + current + "->" + canonical);
            }
        }

        gatewayRouteService.evictRouteCache();
        return new ModelRouteRepairResponse(repairedModels.size(), skippedModels.size(), repairedModels, skippedModels);
    }

    /**
     * 单独更新模型状态。
     */
    public void updateStatus(Long id, String status) {
        AdminContext.requireAdmin();
        // 单独维护模型启用状态
        ModelEntity updateModel = new ModelEntity();
        updateModel.setStatus(status);
        int updated = modelMapper.updateActiveById(id, updateModel);
        if (updated == 0) {
            throw new BusinessException("Model does not exist");
        }
        gatewayRouteService.evictRouteCache();
    }

    @Transactional
    /**
     * 删除模型并清理路由和分组绑定。
     */
    public void delete(Long id) {
        AdminContext.requireAdmin();
        // 删除模型前先确认模型存在
        if (!modelMapper.existsActiveById(id)) {
            throw new BusinessException("Model does not exist");
        }

        // 顺序清理路由、分组绑定，再删主表
        modelRouteMapper.deleteByModelId(id);
        userModelAccessService.deleteModelBindings(id);
        modelMapper.deleteById(id);
        gatewayRouteService.evictRouteCache();
    }

    /**
     * 插入模型主记录、主路由以及默认分组绑定。
     */
    private Long insertModelWithRoute(String modelCode,
                                      String modelName,
                                      String modelType,
                                      String billingType,
                                      BigDecimal promptPrice,
                                      BigDecimal cachedPromptPrice,
                                      BigDecimal completionPrice,
                                      BigDecimal requestPrice,
                                      BigDecimal imagePrice,
                                      BigDecimal multiplier,
                                      boolean isPublic,
                                      Long groupId,
                                      Long providerId,
                                      String upstreamModel) {
        // 插入模型主记录
        ModelEntity model = new ModelEntity();
        model.setModelCode(trimToLength(modelCode, 64));
        model.setModelName(trimToLength(modelName, 64));
        model.setModelType(modelType);
        model.setBillingType(billingType);
        model.setPromptPrice(promptPrice);
        model.setCachedPromptPrice(cachedPromptPrice);
        model.setCompletionPrice(completionPrice);
        model.setRequestPrice(requestPrice);
        model.setImagePrice(imagePrice);
        model.setMultiplier(multiplier);
        model.setIsPublic(isPublic ? 1 : 0);
        model.setStatus("ACTIVE");
        modelMapper.insert(model);

        // 为模型创建默认主路由
        ModelRouteEntity route = new ModelRouteEntity();
        route.setModelId(model.getId());
        route.setProviderId(providerId);
        route.setProviderTokenId(null);
        route.setUpstreamModel(trimToLength(upstreamModel, 128));
        route.setRouteType("PRIMARY");
        route.setPriorityNo(100);
        route.setStatus("ACTIVE");
        modelRouteMapper.insert(route);

        // 补齐模型与分组之间的默认绑定关系
        userModelAccessService.addModelGroupBindingWithPrices(
                model.getId(),
                groupId,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return model.getId();
    }

    private ModelListItemResponse toModelListItemResponse(ModelAdminListView item, List<ModelGroupItemResponse> groups) {
        return new ModelListItemResponse(
                item.getId(),
                item.getBindingId(),
                item.getModelCode(),
                item.getModelName(),
                item.getModelType(),
                item.getBillingType(),
                item.getPromptPrice(),
                item.getCachedPromptPrice(),
                item.getCompletionPrice(),
                item.getRequestPrice(),
                item.getMultiplier(),
                item.getIsPublic(),
                item.getStatus(),
                item.getGroupId(),
                item.getGroupCode(),
                item.getGroupName(),
                groups,
                item.getProviderId(),
                item.getProviderName(),
                item.getProviderType(),
                item.getUpstreamModel(),
                item.getCreatedAt()
        );
    }

    /**
     * 同步模型和分组之间的绑定关系。
     */
    private void syncModelGroupBinding(Long modelId, Long bindingId, Long groupId) {
        // 没有模型或分组时无需处理绑定关系
        if (modelId == null || groupId == null) {
            return;
        }
        if (bindingId == null) {
            // 没传绑定ID时，直接补一条新绑定
            userModelAccessService.addModelGroupBinding(modelId, groupId);
            return;
        }

        // 如果绑定记录不存在或已不匹配，则重新创建绑定
        ModelGroupModelEntity binding = modelGroupModelMapper.selectById(bindingId);
        if (binding == null || !modelId.equals(binding.getModelId())) {
            userModelAccessService.addModelGroupBinding(modelId, groupId);
            return;
        }
        if (groupId.equals(binding.getGroupId())) {
            // 分组未变化时无需更新
            return;
        }

        if (modelGroupModelMapper.existsBinding(modelId, groupId)) {
            // 目标分组已经有绑定时，删除旧绑定避免重复
            modelGroupModelMapper.deleteByIdValue(bindingId);
            return;
        }

        // 否则直接把原绑定迁移到新的分组
        ModelGroupModelEntity updateBinding = new ModelGroupModelEntity();
        updateBinding.setId(bindingId);
        updateBinding.setGroupId(groupId);
        modelGroupModelMapper.updateById(updateBinding);
    }

    /**
     * 替换模型的主路由配置。
     */
    private void replaceModelRoutes(Long modelId, Long providerId, String upstreamModel) {
        // 更新模型时统一重建主路由，避免旧路由残留
        modelRouteMapper.deleteByModelId(modelId);

        ModelRouteEntity route = new ModelRouteEntity();
        route.setModelId(modelId);
        route.setProviderId(providerId);
        route.setProviderTokenId(null);
        route.setUpstreamModel(trimToLength(upstreamModel, 128));
        route.setRouteType("PRIMARY");
        route.setPriorityNo(100);
        route.setStatus("ACTIVE");
        modelRouteMapper.insert(route);
    }

    /**
     * 读取渠道访问凭证，供远程拉取模型时使用。
     */
    private void ensurePrimaryRoute(Long modelId, Long providerId, String upstreamModel) {
        String trimmedUpstreamModel = trimToLength(upstreamModel, 128);
        if (modelId == null || providerId == null || trimmedUpstreamModel == null || trimmedUpstreamModel.isBlank()) {
            return;
        }
        if (modelRouteMapper.existsActiveRoute(modelId, providerId, trimmedUpstreamModel)) {
            return;
        }

        ModelRouteEntity route = new ModelRouteEntity();
        route.setModelId(modelId);
        route.setProviderId(providerId);
        route.setProviderTokenId(null);
        route.setUpstreamModel(trimmedUpstreamModel);
        route.setRouteType("PRIMARY");
        route.setPriorityNo(100);
        route.setStatus("ACTIVE");
        modelRouteMapper.insert(route);
    }

    private ProviderAccess loadProviderAccess(Long providerId) {
        // 从联表查询结果里读取渠道和令牌信息
        ProviderAccessView provider = modelAdminQueryMapper.selectProviderAccess(providerId);
        if (provider == null) {
            throw new BusinessException("Provider is missing or has no active token");
        }
        return new ProviderAccess(
                provider.getId(),
                provider.getProviderName(),
                provider.getBaseUrl(),
                provider.getProviderType(),
                provider.getTimeoutMs(),
                aesCryptoService.decrypt(provider.getTokenValueEncrypted())
        );
    }

    private String resolveCanonicalUpstreamModel(Long providerId, String upstreamModel) {
        return resolveCanonicalUpstreamModel(loadCanonicalUpstreamModelMap(providerId), upstreamModel);
    }

    private String resolveCanonicalUpstreamModel(Map<String, String> canonicalByLowerCaseId, String upstreamModel) {
        String normalizedUpstreamModel = trimToLength(upstreamModel, 128);
        if (canonicalByLowerCaseId == null || normalizedUpstreamModel == null || normalizedUpstreamModel.isBlank()) {
            throw new BusinessException("Upstream model cannot be blank");
        }
        String canonical = canonicalByLowerCaseId.get(normalizedUpstreamModel.toLowerCase(Locale.ROOT));
        if (canonical != null) {
            return canonical;
        }
        throw new BusinessException(400, "Upstream model not found in provider model list: " + normalizedUpstreamModel);
    }

    private String findCanonicalUpstreamModel(Map<String, String> canonicalByLowerCaseId, String upstreamModel) {
        if (canonicalByLowerCaseId == null) {
            return null;
        }
        String normalizedUpstreamModel = trimToLength(upstreamModel, 128);
        if (normalizedUpstreamModel == null || normalizedUpstreamModel.isBlank()) {
            return null;
        }
        return canonicalByLowerCaseId.get(normalizedUpstreamModel.toLowerCase(Locale.ROOT));
    }

    private Map<String, String> loadCanonicalUpstreamModelMap(Long providerId) {
        Map<String, String> canonicalByLowerCaseId = new LinkedHashMap<>();
        for (UpstreamModelOptionResponse option : fetchUpstreamModels(providerId)) {
            canonicalByLowerCaseId.put(option.id().toLowerCase(Locale.ROOT), option.id());
        }
        return canonicalByLowerCaseId;
    }

    /**
     * 根据渠道协议构造模型列表请求。
     */
    private HttpRequest buildModelListRequest(ProviderAccess provider) {
        // 按不同供应商协议拼接模型列表接口
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

    /**
     * 兼容不同上游返回结构，提取模型选项。
     */
    private List<UpstreamModelOptionResponse> parseUpstreamModels(String body, ProviderAccess provider) throws Exception {
        // 兼容不同上游返回结构，统一提取模型列表
        JsonNode root = objectMapper.readTree(body);
        JsonNode items = root.path("data");
        if (!items.isArray()) {
            items = root.path("models");
        }
        if (!items.isArray()) {
            throw new BusinessException(400, "Unexpected upstream model list response");
        }

        List<UpstreamModelOptionResponse> result = new ArrayList<>();
        for (JsonNode item : items) {
            // 尽量优先取标准 id，没有时回退到 name
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

    /**
     * 检查当前渠道下是否已经存在同一个上游模型绑定。
     */
    private ExistingRouteModel findExistingRouteModel(Long providerId, String upstreamModel) {
        ExistingRouteModelView model = modelAdminQueryMapper.selectExistingRouteModel(
                providerId,
                trimToLength(upstreamModel, 128)
        );
        return model == null ? null : new ExistingRouteModel(model.getModelId(), model.getModelCode());
    }

    /**
     * 判断模型是否已经绑定到指定分组。
     */
    private boolean isModelBoundToGroup(Long modelId, Long groupId) {
        return modelGroupModelMapper.existsBinding(modelId, groupId);
    }

    /**
     * 按模型编码查询已存在的模型主键。
     */
    private Long findModelIdByCode(String modelCode) {
        return modelMapper.selectActiveIdByCode(trimToLength(modelCode, 64));
    }

    private String normalizeModelCode(String upstreamModel) {
        // 把上游模型名归一化成平台内部使用的模型编码
        String normalized = upstreamModel.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? "model" : normalized;
    }

    /**
     * 推导 OpenAI 兼容协议的模型列表地址。
     */
    private String resolveOpenAiModelsEndpoint(String baseUrl) {
        if (baseUrl.endsWith("/models")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/models";
        }
        return baseUrl + "/v1/models";
    }

    /**
     * 推导 Anthropic 协议的模型列表地址。
     */
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
        // 尽量从上游错误响应中提取更友好的报错信息
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

    /**
     * 渠道访问凭证快照。
     * 封装拉取上游模型时需要的基础连接信息。
     */
    private record ProviderAccess(
            Long id,
            String providerName,
            String baseUrl,
            String providerType,
            Integer timeoutMs,
            String token
    ) {
    }

    /**
     * 已存在的模型与路由绑定信息。
     * 用于批量导入时判断是否可以复用旧模型。
     */
    private record ExistingRouteModel(Long modelId, String modelCode) {
    }
}
