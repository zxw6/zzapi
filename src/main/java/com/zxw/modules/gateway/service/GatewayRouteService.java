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

@Service
/**
 * 网关路由服务。
 * 负责根据模型编码解析可用路由，并在需要时应用套餐价格覆盖。
 */
public class GatewayRouteService {

    private final ModelMapper modelMapper;
    private final ModelRouteMapper modelRouteMapper;
    private final ModelGroupModelMapper modelGroupModelMapper;
    private final AesCryptoService aesCryptoService;

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
        // 先按原始模型编码查路由
        List<RouteDefinition> routes = findRoutes(modelCode);
        if (routes.isEmpty()) {
            // 如果主编码查不到，再尝试别名映射
            String aliasModelCode = resolveAliasModelCode(modelCode);
            if (aliasModelCode != null) {
                routes = findRoutes(aliasModelCode);
            }
        }

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
        return modelMapper.selectPublicActiveModels()
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
        ModelGroupPricingView pricing = modelGroupModelMapper.selectGroupPricing(groupId, route.modelId());
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
                pricing.getBillingType(),
                pricing.getPromptPrice(),
                pricing.getCachedPromptPrice(),
                pricing.getCompletionPrice(),
                pricing.getRequestPrice(),
                pricing.getMultiplier(),
                route.providerToken()
        );
    }

    /**
     * 读取数据库候选路由并解密真实渠道令牌。
     */
    private List<RouteDefinition> findRoutes(String modelCode) {
        // 读取数据库路由并解密真实渠道令牌
        return modelRouteMapper.selectRoutesByModelCode(modelCode).stream()
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
                        route.getCompletionPrice(),
                        route.getRequestPrice(),
                        route.getMultiplier(),
                        aesCryptoService.decrypt(route.getTokenValueEncrypted())
                ))
                .toList();
    }

    /**
     * 解析模型别名。
     * 当前预留扩展点，后续如有别名表可在这里接入。
     */
    private String resolveAliasModelCode(String modelCode) {
        // 预留模型别名映射能力，当前未启用
        return null;
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
