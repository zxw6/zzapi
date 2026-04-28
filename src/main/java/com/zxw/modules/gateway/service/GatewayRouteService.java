package com.zxw.modules.gateway.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AesCryptoService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GatewayRouteService {

    private final JdbcTemplate jdbcTemplate;
    private final AesCryptoService aesCryptoService;

    public GatewayRouteService(JdbcTemplate jdbcTemplate, AesCryptoService aesCryptoService) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesCryptoService = aesCryptoService;
    }

    public RouteDefinition resolve(String modelCode) {
        List<RouteDefinition> routes = findRoutes(modelCode);
        if (routes.isEmpty()) {
            String aliasModelCode = resolveAliasModelCode(modelCode);
            if (aliasModelCode != null) {
                routes = findRoutes(aliasModelCode);
            }
        }

        if (routes.isEmpty()) {
            throw new BusinessException(404, "模型不存在或未配置可用路由");
        }
        return routes.get(0);
    }

    public List<ModelCard> listPublicModels() {
        return jdbcTemplate.query("""
                select model_code, model_name, model_type
                from models
                where deleted = 0 and status = 'ACTIVE' and is_public = 1
                order by id desc
                """, (rs, rowNum) -> new ModelCard(
                rs.getString("model_code"),
                rs.getString("model_name"),
                rs.getString("model_type")
        ));
    }

    public List<ModelCard> listModelsByGroup(Long groupId) {
        if (groupId == null) {
            return listPublicModels();
        }
        return jdbcTemplate.query("""
                select m.model_code, m.model_name, m.model_type, max(m.id) as sort_id
                from model_group_models mgm
                join models m on m.id = mgm.model_id
                where mgm.group_id = ?
                  and m.deleted = 0
                  and m.status = 'ACTIVE'
                group by m.model_code, m.model_name, m.model_type
                order by sort_id desc
                """, (rs, rowNum) -> new ModelCard(
                rs.getString("model_code"),
                rs.getString("model_name"),
                rs.getString("model_type")
        ), groupId);
    }

    public RouteDefinition applyGroupPricing(RouteDefinition route, Long groupId) {
        if (route == null || groupId == null) {
            return route;
        }
        List<RouteDefinition> pricedRoutes = jdbcTemplate.query("""
                select coalesce(mgm.billing_type, m.billing_type) as billing_type,
                       coalesce(mgm.prompt_price, m.prompt_price) as prompt_price,
                       coalesce(mgm.cached_prompt_price, m.cached_prompt_price) as cached_prompt_price,
                       coalesce(mgm.completion_price, m.completion_price) as completion_price,
                       coalesce(mgm.request_price, m.request_price) as request_price,
                       coalesce(mgm.multiplier, m.multiplier) as multiplier
                from model_group_models mgm
                join models m on m.id = mgm.model_id
                where mgm.group_id = ? and mgm.model_id = ?
                limit 1
                """, (rs, rowNum) -> new RouteDefinition(
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
                rs.getString("billing_type"),
                rs.getBigDecimal("prompt_price"),
                rs.getBigDecimal("cached_prompt_price"),
                rs.getBigDecimal("completion_price"),
                rs.getBigDecimal("request_price"),
                rs.getBigDecimal("multiplier"),
                route.providerToken()
        ), groupId, route.modelId());
        return pricedRoutes.isEmpty() ? route : pricedRoutes.get(0);
    }

    private List<RouteDefinition> findRoutes(String modelCode) {
        return jdbcTemplate.query("""
                select m.id as model_id, m.model_code, m.model_name, m.billing_type, m.prompt_price, m.cached_prompt_price, m.completion_price,
                       m.request_price, m.multiplier, p.id as provider_id, p.provider_name, p.base_url, p.provider_type,
                       p.timeout_ms, r.upstream_model, t.id as provider_token_id, t.token_value_encrypted
                from models m
                join model_routes r on r.model_id = m.id and r.status = 'ACTIVE'
                join providers p on p.id = r.provider_id and p.deleted = 0 and p.status = 'ACTIVE'
                join provider_tokens t on t.provider_id = p.id and t.deleted = 0 and t.status = 'ACTIVE'
                where m.model_code = ? and m.deleted = 0 and m.status = 'ACTIVE'
                order by r.priority_no asc, t.weight_no desc, t.id asc
                limit 1
                """, (rs, rowNum) -> new RouteDefinition(
                rs.getLong("model_id"),
                rs.getString("model_code"),
                rs.getString("model_name"),
                rs.getLong("provider_id"),
                rs.getLong("provider_token_id"),
                rs.getString("provider_name"),
                rs.getString("base_url"),
                rs.getString("provider_type"),
                rs.getInt("timeout_ms"),
                rs.getString("upstream_model"),
                rs.getString("billing_type"),
                rs.getBigDecimal("prompt_price"),
                rs.getBigDecimal("cached_prompt_price"),
                rs.getBigDecimal("completion_price"),
                rs.getBigDecimal("request_price"),
                rs.getBigDecimal("multiplier"),
                aesCryptoService.decrypt(rs.getString("token_value_encrypted"))
        ), modelCode);
    }

    private String resolveAliasModelCode(String modelCode) {
        return null;
    }

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

    public record ModelCard(
            String id,
            String ownedBy,
            String type
    ) {
    }
}
