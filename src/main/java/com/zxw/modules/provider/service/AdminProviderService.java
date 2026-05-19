package com.zxw.modules.provider.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.AesCryptoService;
import com.zxw.modules.gateway.service.GatewayRouteService;
import com.zxw.modules.model.service.AntigravityPresetService;
import com.zxw.modules.provider.dto.ProviderCreateRequest;
import com.zxw.modules.provider.dto.ProviderListItemResponse;
import com.zxw.persistence.entity.ProviderEntity;
import com.zxw.persistence.entity.ProviderTokenEntity;
import com.zxw.persistence.mapper.ModelRouteMapper;
import com.zxw.persistence.mapper.ProviderMapper;
import com.zxw.persistence.mapper.ProviderTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminProviderService {

    private final ProviderMapper providerMapper;
    private final ProviderTokenMapper providerTokenMapper;
    private final ModelRouteMapper modelRouteMapper;
    private final AesCryptoService aesCryptoService;
    private final AntigravityPresetService antigravityPresetService;
    private final GatewayRouteService gatewayRouteService;

    public AdminProviderService(ProviderMapper providerMapper,
                                ProviderTokenMapper providerTokenMapper,
                                ModelRouteMapper modelRouteMapper,
                                AesCryptoService aesCryptoService,
                                AntigravityPresetService antigravityPresetService,
                                GatewayRouteService gatewayRouteService) {
        this.providerMapper = providerMapper;
        this.providerTokenMapper = providerTokenMapper;
        this.modelRouteMapper = modelRouteMapper;
        this.aesCryptoService = aesCryptoService;
        this.antigravityPresetService = antigravityPresetService;
        this.gatewayRouteService = gatewayRouteService;
    }

    public List<ProviderListItemResponse> listProviders() {
        AdminContext.requireAdmin();
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
    public void create(ProviderCreateRequest request) {
        AdminContext.requireAdmin();
        if (providerMapper.existsActiveByCode(request.providerCode())) {
            throw new BusinessException("Provider code already exists");
        }

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

        antigravityPresetService.syncForProvider(provider.getId());
        gatewayRouteService.evictRouteCache();
    }

    public void updateStatus(Long id, String status) {
        AdminContext.requireAdmin();
        int updated = providerMapper.updateStatus(id, status, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException(404, "Provider does not exist");
        }
        gatewayRouteService.evictRouteCache();
    }

    @Transactional
    public void deleteProvider(Long id) {
        AdminContext.requireAdmin();
        LocalDateTime now = LocalDateTime.now();
        if (!providerMapper.existsActiveById(id)) {
            throw new BusinessException(404, "Provider does not exist");
        }

        modelRouteMapper.deleteByProviderId(id);
        providerTokenMapper.softDeleteByProviderId(id, now);
        int deleted = providerMapper.softDelete(id, now);
        if (deleted == 0) {
            throw new BusinessException(400, "Provider delete failed");
        }
        gatewayRouteService.evictRouteCache();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String trimEndSlash(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
