package com.zxw.modules.model.service;

import com.zxw.modules.access.service.UserModelAccessService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class AntigravityPresetService {

    private static final String GROUP_CODE = "antigravity";
    private static final String GROUP_NAME = "Antigravity";
    private static final BigDecimal REQUEST_PRICE = new BigDecimal("0.200000");
    private static final BigDecimal GROUP_SALE_PRICE = new BigDecimal("100.0000");
    private static final int PACKAGE_DAYS = 30;
    private static final BigDecimal DAILY_QUOTA = new BigDecimal("60.0000");
    private static final BigDecimal WEEKLY_QUOTA = new BigDecimal("420.0000");
    private static final BigDecimal MONTHLY_QUOTA = new BigDecimal("1800.0000");

    private static final List<AntigravityModelPreset> PRESET_MODELS = List.of(
            new AntigravityModelPreset("antigravity-claude-gpt", "Claude/GPT", "Claude/GPT", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-1-pro-series", "Gemini 3.1 Pro Series", "Gemini 3.1 Pro Series", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-2-5-flash", "Gemini 2.5 Flash", "Gemini 2.5 Flash", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-2-5-flash-lite", "Gemini 2.5 Flash Lite", "Gemini 2.5 Flash Lite", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-flash", "Gemini 3 Flash", "Gemini 3 Flash", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-1-flash-image", "Gemini 3.1 Flash Image", "Gemini 3.1 Flash Image", "IMAGE")
    );

    private final JdbcTemplate jdbcTemplate;
    private final UserModelAccessService userModelAccessService;

    public AntigravityPresetService(JdbcTemplate jdbcTemplate,
                                    UserModelAccessService userModelAccessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userModelAccessService = userModelAccessService;
    }

    @Transactional
    public void syncForExistingProviders() {
        List<Long> providerIds = jdbcTemplate.query("""
                select id
                from providers
                where deleted = 0
                  and (
                        lower(coalesce(provider_code, '')) like '%antigravity%'
                     or lower(coalesce(provider_name, '')) like '%antigravity%'
                  )
                order by id asc
                """, (rs, rowNum) -> rs.getLong("id"));
        for (Long providerId : providerIds) {
            syncForProvider(providerId);
        }
    }

    @Transactional
    public void syncForProvider(Long providerId) {
        if (providerId == null || !isAntigravityProvider(providerId)) {
            return;
        }

        Long groupId = ensureAntigravityGroup();
        for (AntigravityModelPreset preset : PRESET_MODELS) {
            Long modelId = findModelIdByProviderRoute(providerId, preset.upstreamModel());
            if (modelId == null) {
                modelId = findModelIdByCode(preset.modelCode());
            }

            if (modelId == null) {
                jdbcTemplate.update("""
                        insert into models (
                            model_code, model_name, model_type, billing_type,
                            prompt_price, completion_price, request_price, image_price,
                            multiplier, is_public, status, remark
                        )
                        values (?, ?, ?, 'REQUEST', 0, 0, ?, 0, 1, 1, 'ACTIVE', ?)
                        """,
                        preset.modelCode(),
                        preset.modelName(),
                        preset.modelType(),
                        REQUEST_PRICE,
                        "Antigravity preset model"
                );
                modelId = findModelIdByCode(preset.modelCode());
            } else {
                jdbcTemplate.update("""
                        update models
                        set model_code = ?, model_name = ?, model_type = ?, billing_type = 'REQUEST',
                            prompt_price = 0, completion_price = 0, request_price = ?, image_price = 0,
                            multiplier = 1, is_public = 1, status = 'ACTIVE',
                            remark = ?, updated_at = now()
                        where id = ?
                        """,
                        preset.modelCode(),
                        preset.modelName(),
                        preset.modelType(),
                        REQUEST_PRICE,
                        "Antigravity preset model",
                        modelId
                );
            }

            jdbcTemplate.update("""
                    delete from model_routes
                    where model_id = ? and provider_id = ?
                    """, modelId, providerId);

            jdbcTemplate.update("""
                    insert into model_routes (model_id, provider_id, provider_token_id, upstream_model, route_type, priority_no, status)
                    values (?, ?, null, ?, 'PRIMARY', 100, 'ACTIVE')
                    """,
                    modelId,
                    providerId,
                    preset.upstreamModel()
            );

            jdbcTemplate.update("""
                    insert ignore into model_group_models (group_id, model_id)
                    values (?, ?)
                    """, groupId, modelId);

            userModelAccessService.syncPresetGroupsForModel(modelId, preset.modelCode(), preset.upstreamModel());
        }
    }

    private Long ensureAntigravityGroup() {
        Long groupId = jdbcTemplate.query("""
                select id
                from model_groups
                where group_code = ?
                limit 1
                """, rs -> rs.next() ? rs.getLong("id") : null, GROUP_CODE);
        if (groupId == null) {
            jdbcTemplate.update("""
                    insert into model_groups (
                        group_code, group_name, sale_price, package_days,
                        daily_quota, weekly_quota, monthly_quota, status, remark
                    )
                    values (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                    """,
                    GROUP_CODE,
                    GROUP_NAME,
                    GROUP_SALE_PRICE,
                    PACKAGE_DAYS,
                    DAILY_QUOTA,
                    WEEKLY_QUOTA,
                    MONTHLY_QUOTA,
                    "Antigravity preset group"
            );
            groupId = jdbcTemplate.query("""
                    select id
                    from model_groups
                    where group_code = ?
                    limit 1
                    """, rs -> rs.next() ? rs.getLong("id") : null, GROUP_CODE);
        } else {
            jdbcTemplate.update("""
                    update model_groups
                    set group_name = ?, sale_price = ?, package_days = ?,
                        daily_quota = ?, weekly_quota = ?, monthly_quota = ?,
                        status = 'ACTIVE', remark = ?, updated_at = now()
                    where id = ?
                    """,
                    GROUP_NAME,
                    GROUP_SALE_PRICE,
                    PACKAGE_DAYS,
                    DAILY_QUOTA,
                    WEEKLY_QUOTA,
                    MONTHLY_QUOTA,
                    "Antigravity preset group",
                    groupId
            );
        }
        return groupId;
    }

    private boolean isAntigravityProvider(Long providerId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from providers
                where id = ?
                  and deleted = 0
                  and (
                        lower(coalesce(provider_code, '')) like '%antigravity%'
                     or lower(coalesce(provider_name, '')) like '%antigravity%'
                  )
                """, Integer.class, providerId);
        return count != null && count > 0;
    }

    private Long findModelIdByProviderRoute(Long providerId, String upstreamModel) {
        return jdbcTemplate.query("""
                select m.id
                from model_routes r
                join models m on m.id = r.model_id
                where r.provider_id = ?
                  and lower(r.upstream_model) = ?
                order by m.id asc
                limit 1
                """, rs -> rs.next() ? rs.getLong("id") : null, providerId, upstreamModel.toLowerCase(Locale.ROOT));
    }

    private Long findModelIdByCode(String modelCode) {
        return jdbcTemplate.query("""
                select id
                from models
                where model_code = ?
                limit 1
                """, rs -> rs.next() ? rs.getLong("id") : null, modelCode);
    }

    private record AntigravityModelPreset(
            String modelCode,
            String modelName,
            String upstreamModel,
            String modelType
    ) {
    }
}
