package com.zxw.modules.gateway.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AesCryptoService;
import com.zxw.persistence.mapper.ModelGroupModelMapper;
import com.zxw.persistence.mapper.ModelMapper;
import com.zxw.persistence.mapper.ModelRouteMapper;
import com.zxw.persistence.model.ModelGroupPricingView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * 网关路由服务。
 * 负责根据模型编码解析可用路由，并在需要时应用套餐价格覆盖。
 */
public class GatewayRouteService {

    private static final long ROUTE_CACHE_TTL_MS = 300_000L;

    private final ModelMapper modelMapper;
    private final ModelRouteMapper modelRouteMapper;
    private final ModelGroupModelMapper modelGroupModelMapper;
    private final AesCryptoService aesCryptoService;
    private final ConcurrentHashMap<String, CacheEntry<List<RouteDefinition>>> routeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<RoutePricing>> pricingCache = new ConcurrentHashMap<>();

    public GatewayRouteService(ModelMapper modelMapper,
                               ModelRouteMapper modelRouteMapper,
                               ModelGroupModelMapper modelGroupModelMapper,
                               AesCryptoService aesCryptoService) {
        this.modelMapper = modelMapper;
        this.modelRouteMapper = modelRouteMapper;
        this.modelGroupModelMapper = modelGroupModelMapper;
        this.aesCryptoService = aesCryptoService;
    }

    /**
     * 按公开模型编码解析可用路由。
     */
    public RouteDefinition resolve(String modelCode) {
        List<RouteDefinition> routes = findRoutes(modelCode);
        if (routes.isEmpty()) {
            throw new BusinessException(404, "Model route not found or inactive");
        }
        return routes.get(0);
    }

    /**
     * 查询公开可用的模型列表。
     */
    public List<ModelCard> listPublicModels() {
        // 返回公开且启用的模型列表
        return modelMapper.selectPublicResolvableModels()
                .stream()
                .map(model -> new ModelCard(model.getModelCode(), model.getModelName(), model.getModelType()))
                .toList();
    }

    /**
     * 按分组查询模型列表。
     */
    public List<ModelCard> listModelsByGroup(Long groupId) {
        // 不传分组时默认返回公开模型
        if (groupId == null) {
            return listPublicModels();
        }
        return modelMapper.selectActiveModelsByGroup(groupId).stream()
                .map(model -> new ModelCard(model.getModelCode(), model.getModelName(), model.getModelType()))
                .toList();
    }

    /**
     * 按分组价格覆盖基础路由价格。
     */
    public RouteDefinition applyGroupPricing(RouteDefinition route, Long groupId) {
        // 套餐未绑定分组时，直接返回基础路由价格
        if (route == null || groupId == null) {
            return route;
        }
        RoutePricing pricing = findGroupPricing(groupId, route.modelId());
        if (pricing == null) {
            // 分组没有单独配置价格时，沿用模型默认价格
            return route;
        }
        // 用分组价格覆盖基础价格
        return new RouteDefinition(
                route.modelId(),
                route.modelCode(),
                route.modelName(),
                route.providerId(),
                route.providerTokenId(),
                route.providerName(),
                route.baseUrl(),
                route.providerType(),
                route.timeoutMs(),
                route.upstreamModel(),
                pricing.billingType(),
                pricing.promptPrice(),
                pricing.cachedPromptPrice(),
                pricing.cacheWritePromptPrice(),
                pricing.completionPrice(),
                pricing.requestPrice(),
                pricing.multiplier(),
                route.providerToken()
        );
    }

    public void evictRouteCache() {
        routeCache.clear();
        pricingCache.clear();
    }

    /**
     * 读取数据库候选路由并解密真实渠道令牌。
     */
    private List<RouteDefinition> findRoutes(String modelCode) {
        String cacheKey = modelCode == null ? "" : modelCode.trim();
        CacheEntry<List<RouteDefinition>> cached = routeCache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.value();
        }
        // 读取数据库路由并解密真实渠道令牌
        List<RouteDefinition> routes = modelRouteMapper.selectRoutesByModelCode(modelCode).stream()
                .map(route -> new RouteDefinition(
                        route.getModelId(),
                        route.getModelCode(),
                        route.getModelName(),
                        route.getProviderId(),
                        route.getProviderTokenId(),
                        route.getProviderName(),
                        route.getBaseUrl(),
                        route.getProviderType(),
                        route.getTimeoutMs(),
                        route.getUpstreamModel(),
                        route.getBillingType(),
                        route.getPromptPrice(),
                        route.getCachedPromptPrice(),
                        route.getCacheWritePromptPrice(),
                        route.getCompletionPrice(),
                        route.getRequestPrice(),
                        route.getMultiplier(),
                        aesCryptoService.decrypt(route.getTokenValueEncrypted())
                ))
                .toList();
        routeCache.put(cacheKey, new CacheEntry<>(routes, System.currentTimeMillis() + ROUTE_CACHE_TTL_MS));
        return routes;
    }

    private RoutePricing findGroupPricing(Long groupId, Long modelId) {
        String cacheKey = groupId + ":" + modelId;
        CacheEntry<RoutePricing> cached = pricingCache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.value();
        }
        ModelGroupPricingView pricing = modelGroupModelMapper.selectGroupPricing(groupId, modelId);
        RoutePricing routePricing = pricing == null ? null : new RoutePricing(
                pricing.getBillingType(),
                pricing.getPromptPrice(),
                pricing.getCachedPromptPrice(),
                pricing.getCacheWritePromptPrice(),
                pricing.getCompletionPrice(),
                pricing.getRequestPrice(),
                pricing.getMultiplier()
        );
        pricingCache.put(cacheKey, new CacheEntry<>(routePricing, System.currentTimeMillis() + ROUTE_CACHE_TTL_MS));
        return routePricing;
    }

    private record CacheEntry<T>(T value, long expiresAtMs) {
        boolean expired() {
            return System.currentTimeMillis() >= expiresAtMs;
        }
    }

    private record RoutePricing(
            String billingType,
            BigDecimal promptPrice,
            BigDecimal cachedPromptPrice,
            BigDecimal cacheWritePromptPrice,
            BigDecimal completionPrice,
            BigDecimal requestPrice,
            BigDecimal multiplier
    ) {
    }

    /**
     * 网关最终路由定义。
     * 保存模型、渠道、价格和真实令牌等下游转发所需信息。
     */
    public record RouteDefinition(
            Long modelId,
            String modelCode,
            String modelName,
            Long providerId,
            Long providerTokenId,
            String providerName,
            String baseUrl,
            String providerType,
            Integer timeoutMs,
            String upstreamModel,
            String billingType,
            BigDecimal promptPrice,
            BigDecimal cachedPromptPrice,
            BigDecimal cacheWritePromptPrice,
            BigDecimal completionPrice,
            BigDecimal requestPrice,
            BigDecimal multiplier,
            String providerToken
    ) {
    }

    /**
     * 对外公开的模型卡片信息。
     * 用于模型列表等轻量展示场景。
     */
    public record ModelCard(
            String id,
            String ownedBy,
            String type
    ) {
    }
}
