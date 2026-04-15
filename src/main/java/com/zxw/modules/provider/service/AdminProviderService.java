package com.zxw.modules.provider.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.AesCryptoService;
import com.zxw.modules.provider.dto.ProviderCreateRequest;
import com.zxw.modules.provider.dto.ProviderListItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminProviderService {

    private final JdbcTemplate jdbcTemplate;
    private final AesCryptoService aesCryptoService;

    public AdminProviderService(JdbcTemplate jdbcTemplate, AesCryptoService aesCryptoService) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesCryptoService = aesCryptoService;
    }

    public List<ProviderListItemResponse> listProviders() {
        AdminContext.requireAdmin();
        return jdbcTemplate.query("""
                select p.id, p.provider_code, p.provider_name, p.base_url, p.provider_type,
                       p.status, p.priority_no, p.timeout_ms, p.created_at,
                       (select count(*) from provider_tokens t where t.provider_id = p.id and t.deleted = 0) as token_count
                from providers p
                where p.deleted = 0
                order by p.priority_no asc, p.id desc
                """, (rs, rowNum) -> new ProviderListItemResponse(
                rs.getLong("id"),
                rs.getString("provider_code"),
                rs.getString("provider_name"),
                rs.getString("base_url"),
                rs.getString("provider_type"),
                rs.getString("status"),
                rs.getInt("priority_no"),
                rs.getInt("timeout_ms"),
                rs.getInt("token_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    @Transactional
    public void create(ProviderCreateRequest request) {
        AdminContext.requireAdmin();
        Integer exists = jdbcTemplate.queryForObject("select count(*) from providers where provider_code = ? and deleted = 0",
                Integer.class, request.providerCode());
        if (exists != null && exists > 0) {
            throw new BusinessException("渠道编码已存在");
        }

        jdbcTemplate.update("""
                insert into providers (provider_code, provider_name, base_url, provider_type, status, priority_no, timeout_ms, remark)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?, ?)
                """,
                request.providerCode(),
                request.providerName(),
                trimEndSlash(request.baseUrl()),
                blankToDefault(request.providerType(), "OPENAI_COMPATIBLE"),
                request.priorityNo() == null ? 100 : request.priorityNo(),
                request.timeoutMs() == null ? 60000 : request.timeoutMs(),
                request.remark()
        );

        if (request.tokenValue() != null && !request.tokenValue().isBlank()) {
            Long providerId = jdbcTemplate.queryForObject("select id from providers where provider_code = ?", Long.class, request.providerCode());
            jdbcTemplate.update("""
                    insert into provider_tokens (provider_id, token_name, token_value_encrypted, status, weight_no, rpm_limit, tpm_limit, current_balance)
                    values (?, ?, ?, 'ACTIVE', ?, ?, ?, 0)
                    """,
                    providerId,
                    blankToDefault(request.tokenName(), request.providerName() + " 默认 Token"),
                    aesCryptoService.encrypt(request.tokenValue()),
                    request.weightNo() == null ? 100 : request.weightNo(),
                    request.rpmLimit() == null ? 0 : request.rpmLimit(),
                    request.tpmLimit() == null ? 0 : request.tpmLimit()
            );
        }
    }

    public void updateStatus(Long id, String status) {
        AdminContext.requireAdmin();
        int updated = jdbcTemplate.update("""
                update providers
                set status = ?, updated_at = now()
                where id = ? and deleted = 0
                """, status, id);
        if (updated == 0) {
            throw new BusinessException("渠道不存在");
        }
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
