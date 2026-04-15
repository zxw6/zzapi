package com.zxw.modules.gateway.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AesCryptoService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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

    private List<RouteDefinition> findRoutes(String modelCode) {
        return jdbcTemplate.query("""
                select m.id as model_id, m.model_code, m.model_name, m.prompt_price, m.completion_price,
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
                rs.getBigDecimal("prompt_price"),
                rs.getBigDecimal("completion_price"),
                rs.getBigDecimal("request_price"),
                rs.getBigDecimal("multiplier"),
                aesCryptoService.decrypt(rs.getString("token_value_encrypted"))
        ), modelCode);
    }

    private String resolveAliasModelCode(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return null;
        }
        return findLatestSuffixedModelCode(modelCode);
    }

    private String findLatestSuffixedModelCode(String modelCode) {
        List<String> candidates = jdbcTemplate.query("""
                select model_code
                from models
                where deleted = 0
                  and status = 'ACTIVE'
                  and model_code like concat(?, '-%%')
                order by id desc
                limit 1
                """, (rs, rowNum) -> rs.getString("model_code"), modelCode);
        return candidates.isEmpty() ? null : candidates.get(0);
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
            BigDecimal promptPrice,
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
