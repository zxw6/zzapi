package com.zxw.modules.model.service;

import com.zxw.persistence.entity.ModelEntity;
import com.zxw.persistence.entity.ModelGroupEntity;
import com.zxw.persistence.entity.ModelRouteEntity;
import com.zxw.persistence.mapper.AntigravityQueryMapper;
import com.zxw.persistence.mapper.ModelGroupMapper;
import com.zxw.persistence.mapper.ModelGroupModelMapper;
import com.zxw.persistence.mapper.ModelMapper;
import com.zxw.persistence.mapper.ModelRouteMapper;
import com.zxw.persistence.model.ExistingRouteModelView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
/**
 * Antigravity 预置模型同步服务。
 * 当检测到 Antigravity 供应商后，自动补齐预置套餐分组、模型与路由关系。
 */
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
            new AntigravityModelPreset("antigravity-claude-sonnet-4-6", "claude-sonnet-4-6", "claude-sonnet-4-6", "CHAT"),
            new AntigravityModelPreset("antigravity-claude-opus-4-6-thinking", "claude-opus-4-6-thinking", "claude-opus-4-6-thinking", "CHAT"),
            new AntigravityModelPreset("antigravity-gpt-oss-120b-medium", "gpt-oss-120b-medium", "gpt-oss-120b-medium", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-2-5-flash", "gemini-2.5-flash", "gemini-2.5-flash", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-2-5-flash-lite", "gemini-2.5-flash-lite", "gemini-2.5-flash-lite", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-flash", "gemini-3-flash", "gemini-3-flash", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-1-pro-high", "gemini-3.1-pro-high", "gemini-3.1-pro-high", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-1-pro-low", "gemini-3.1-pro-low", "gemini-3.1-pro-low", "CHAT"),
            new AntigravityModelPreset("antigravity-gemini-3-1-flash-image", "gemini-3.1-flash-image", "gemini-3.1-flash-image", "IMAGE")
    );

    private final AntigravityQueryMapper antigravityQueryMapper;
    private final ModelGroupMapper modelGroupMapper;
    private final ModelMapper modelMapper;
    private final ModelRouteMapper modelRouteMapper;
    private final ModelGroupModelMapper modelGroupModelMapper;

    public AntigravityPresetService(AntigravityQueryMapper antigravityQueryMapper,
                                    ModelGroupMapper modelGroupMapper,
                                    ModelMapper modelMapper,
                                    ModelRouteMapper modelRouteMapper,
                                    ModelGroupModelMapper modelGroupModelMapper) {
        this.antigravityQueryMapper = antigravityQueryMapper;
        this.modelGroupMapper = modelGroupMapper;
        this.modelMapper = modelMapper;
        this.modelRouteMapper = modelRouteMapper;
        this.modelGroupModelMapper = modelGroupModelMapper;
    }

    /**
     * 为已存在的 Antigravity 供应商批量同步预置模型。
     */
    @Transactional
    public void syncForExistingProviders() {
        for (Long providerId : antigravityQueryMapper.selectAntigravityProviderIds()) {
            syncForProvider(providerId);
        }
    }

    /**
     * 为指定供应商同步 Antigravity 预置模型和路由。
     */
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
                // 模型不存在时直接创建一套标准预置配置。
                ModelEntity model = new ModelEntity();
                model.setModelCode(preset.modelCode());
                model.setModelName(preset.modelName());
                model.setModelType(preset.modelType());
                model.setBillingType("REQUEST");
                model.setPromptPrice(BigDecimal.ZERO);
                model.setCachedPromptPrice(BigDecimal.ZERO);
                model.setCompletionPrice(BigDecimal.ZERO);
                model.setRequestPrice(REQUEST_PRICE);
                model.setImagePrice(BigDecimal.ZERO);
                model.setMultiplier(BigDecimal.ONE);
                model.setIsPublic(1);
                model.setStatus("ACTIVE");
                model.setRemark("Antigravity preset model");
                modelMapper.insert(model);
                modelId = model.getId();
            } else {
                // 模型已存在时，按预置规则覆盖成标准配置。
                ModelEntity updateModel = new ModelEntity();
                updateModel.setModelCode(preset.modelCode());
                updateModel.setModelName(preset.modelName());
                updateModel.setModelType(preset.modelType());
                updateModel.setBillingType("REQUEST");
                updateModel.setPromptPrice(BigDecimal.ZERO);
                updateModel.setCachedPromptPrice(BigDecimal.ZERO);
                updateModel.setCompletionPrice(BigDecimal.ZERO);
                updateModel.setRequestPrice(REQUEST_PRICE);
                updateModel.setImagePrice(BigDecimal.ZERO);
                updateModel.setMultiplier(BigDecimal.ONE);
                updateModel.setIsPublic(1);
                updateModel.setStatus("ACTIVE");
                updateModel.setRemark("Antigravity preset model");
                modelMapper.updateByIdValue(modelId, updateModel);
            }

            modelRouteMapper.deleteByModelIdAndProviderId(modelId, providerId);

            // 每次同步都会重建当前供应商的主路由，保证上游模型映射最新。
            ModelRouteEntity route = new ModelRouteEntity();
            route.setModelId(modelId);
            route.setProviderId(providerId);
            route.setProviderTokenId(null);
            route.setUpstreamModel(preset.upstreamModel());
            route.setRouteType("PRIMARY");
            route.setPriorityNo(100);
            route.setStatus("ACTIVE");
            modelRouteMapper.insert(route);

            modelGroupModelMapper.insertIgnoreBinding(groupId, modelId);
        }
    }

    /**
     * 确保 Antigravity 套餐分组存在且配置正确。
     */
    private Long ensureAntigravityGroup() {
        ModelGroupEntity group = modelGroupMapper.selectByGroupCode(GROUP_CODE);
        if (group == null) {
            group = new ModelGroupEntity();
            group.setGroupCode(GROUP_CODE);
            group.setGroupName(GROUP_NAME);
            group.setSalePrice(GROUP_SALE_PRICE);
            group.setPackageDays(PACKAGE_DAYS);
            group.setDailyQuota(DAILY_QUOTA);
            group.setWeeklyQuota(WEEKLY_QUOTA);
            group.setMonthlyQuota(MONTHLY_QUOTA);
            group.setStatus("ACTIVE");
            group.setRemark("Antigravity preset group");
            modelGroupMapper.insert(group);
            return group.getId();
        }

        ModelGroupEntity updateGroup = new ModelGroupEntity();
        updateGroup.setGroupName(GROUP_NAME);
        updateGroup.setSalePrice(GROUP_SALE_PRICE);
        updateGroup.setPackageDays(PACKAGE_DAYS);
        updateGroup.setDailyQuota(DAILY_QUOTA);
        updateGroup.setWeeklyQuota(WEEKLY_QUOTA);
        updateGroup.setMonthlyQuota(MONTHLY_QUOTA);
        updateGroup.setStatus("ACTIVE");
        updateGroup.setRemark("Antigravity preset group");
        modelGroupMapper.updateByIdValue(group.getId(), updateGroup);
        return group.getId();
    }

    private boolean isAntigravityProvider(Long providerId) {
        Integer count = antigravityQueryMapper.countAntigravityProviders(providerId);
        return count != null && count > 0;
    }

    private Long findModelIdByProviderRoute(Long providerId, String upstreamModel) {
        ExistingRouteModelView model = antigravityQueryMapper.selectModelByProviderRoute(
                providerId,
                upstreamModel.toLowerCase(Locale.ROOT)
        );
        return model == null ? null : model.getModelId();
    }

    private Long findModelIdByCode(String modelCode) {
        return modelMapper.selectIdByCode(modelCode);
    }

    private record AntigravityModelPreset(
            String modelCode,
            String modelName,
            String upstreamModel,
            String modelType
    ) {
    }
}
