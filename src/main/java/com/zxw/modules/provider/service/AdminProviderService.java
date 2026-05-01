package com.zxw.modules.provider.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.AesCryptoService;
import com.zxw.modules.model.service.AntigravityPresetService;
import com.zxw.modules.provider.dto.ProviderCreateRequest;
import com.zxw.modules.provider.dto.ProviderListItemResponse;
import com.zxw.persistence.entity.ProviderEntity;
import com.zxw.persistence.entity.ProviderTokenEntity;
import com.zxw.persistence.mapper.ProviderMapper;
import com.zxw.persistence.mapper.ProviderTokenMapper;
import com.zxw.persistence.model.ProviderListView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * 渠道管理服务。
 * 负责渠道创建、状态维护以及默认令牌初始化。
 */
public class AdminProviderService {

    private final ProviderMapper providerMapper;
    private final ProviderTokenMapper providerTokenMapper;
    private final AesCryptoService aesCryptoService;
    private final AntigravityPresetService antigravityPresetService;

    public AdminProviderService(ProviderMapper providerMapper,
                                ProviderTokenMapper providerTokenMapper,
                                AesCryptoService aesCryptoService,
                                AntigravityPresetService antigravityPresetService) {
        this.providerMapper = providerMapper;
        this.providerTokenMapper = providerTokenMapper;
        this.aesCryptoService = aesCryptoService;
        this.antigravityPresetService = antigravityPresetService;
    }

    /**
     * 查询渠道列表。
     */
    public List<ProviderListItemResponse> listProviders() {
        AdminContext.requireAdmin();
        // 查询渠道列表并转换成前端展示结构
        return providerMapper.selectProviderList().stream()
                .map(item -> new ProviderListItemResponse(
                        item.getId(),
                        item.getProviderCode(),
                        item.getProviderName(),
                        item.getBaseUrl(),
                        item.getProviderType(),
                        item.getStatus(),
                        item.getPriorityNo(),
                        item.getTimeoutMs(),
                        item.getTokenCount(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    /**
     * 创建渠道并按需初始化默认令牌。
     */
    public void create(ProviderCreateRequest request) {
        AdminContext.requireAdmin();
        // 渠道编码必须唯一
        if (providerMapper.existsActiveByCode(request.providerCode())) {
            throw new BusinessException("Provider code already exists");
        }

        // 先创建渠道主记录
        ProviderEntity provider = new ProviderEntity();
        provider.setProviderCode(request.providerCode());
        provider.setProviderName(request.providerName());
        provider.setBaseUrl(trimEndSlash(request.baseUrl()));
        provider.setProviderType(blankToDefault(request.providerType(), "OPENAI_COMPATIBLE"));
        provider.setStatus("ACTIVE");
        provider.setPriorityNo(request.priorityNo() == null ? 100 : request.priorityNo());
        provider.setTimeoutMs(request.timeoutMs() == null ? 60000 : request.timeoutMs());
        provider.setRemark(request.remark());
        providerMapper.insert(provider);

        if (request.tokenValue() != null && !request.tokenValue().isBlank()) {
            // 如果传入了默认令牌，则一并创建令牌记录
            ProviderTokenEntity token = new ProviderTokenEntity();
            token.setProviderId(provider.getId());
            token.setTokenName(blankToDefault(request.tokenName(), request.providerName() + " Default Token"));
            token.setTokenValueEncrypted(aesCryptoService.encrypt(request.tokenValue()));
            token.setStatus("ACTIVE");
            token.setWeightNo(request.weightNo() == null ? 100 : request.weightNo());
            token.setRpmLimit(request.rpmLimit() == null ? 0 : request.rpmLimit());
            token.setTpmLimit(request.tpmLimit() == null ? 0 : request.tpmLimit());
            token.setCurrentBalance(BigDecimal.ZERO);
            providerTokenMapper.insert(token);
        }

        // Antigravity 渠道创建后需要同步预置模型
        antigravityPresetService.syncForProvider(provider.getId());
    }

    /**
     * 更新渠道启用状态。
     */
    public void updateStatus(Long id, String status) {
        AdminContext.requireAdmin();
        // 更新渠道启用禁用状态
        int updated = providerMapper.updateStatus(id, status, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException("Provider not found");
        }
    }

    /**
     * 空白值回退到默认值。
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 去掉地址末尾的斜杠，避免后续路径拼接重复。
     */
    private String trimEndSlash(String value) {
        // 统一去掉结尾斜杠，避免地址拼接重复
        if (value == null) {
            return null;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
