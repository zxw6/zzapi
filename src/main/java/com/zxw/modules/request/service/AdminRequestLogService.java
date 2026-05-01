package com.zxw.modules.request.service;

import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.request.dto.RequestLogItemResponse;
import com.zxw.modules.request.dto.RequestLogPageResponse;
import com.zxw.persistence.mapper.RequestLogQueryMapper;
import com.zxw.persistence.model.RequestLogListView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
/**
 * 管理端请求日志服务。
 * 用于按权限范围分页查询最近请求记录，并转换成前端响应结构。
 */
public class AdminRequestLogService {

    // 查询层 Mapper，负责按权限范围拉取请求日志视图数据
    private final RequestLogQueryMapper requestLogQueryMapper;

    public AdminRequestLogService(RequestLogQueryMapper requestLogQueryMapper) {
        this.requestLogQueryMapper = requestLogQueryMapper;
    }

    /**
     * 分页查询最近的请求日志。
     */
    public RequestLogPageResponse<RequestLogItemResponse> page(int page, int pageSize) {
        JwtUser currentUser = AdminContext.require();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;

        long total = AdminContext.isAdmin()
                ? requestLogQueryMapper.countAdmin()
                : requestLogQueryMapper.countUser(currentUser.userId());
        List<RequestLogListView> rows = AdminContext.isAdmin()
                ? requestLogQueryMapper.pageAdmin(offset, safePageSize)
                : requestLogQueryMapper.pageUser(currentUser.userId(), offset, safePageSize);

        List<RequestLogItemResponse> records = rows.stream()
                // 统一补齐默认倍率字段，避免前端拿到空值。
                .map(item -> new RequestLogItemResponse(
                        item.getRequestId(),
                        item.getUsername(),
                        item.getModelCode(),
                        item.getUpstreamModel(),
                        item.getPackageName(),
                        defaultBigDecimal(item.getMultiplier()),
                        item.getStatusCode(),
                        item.getLatencyMs(),
                        item.getPromptTokens(),
                        item.getCompletionTokens(),
                        item.getTotalTokens(),
                        item.getCachedPromptTokens(),
                        item.getUserAmount(),
                        item.getCostAmount(),
                        item.getSuccess(),
                        item.getCreatedAt()
                ))
                .toList();

        long totalPages = total <= 0 ? 0 : (total + safePageSize - 1) / safePageSize;
        return new RequestLogPageResponse<>(
                safePage,
                safePageSize,
                total,
                totalPages,
                safePage > 1,
                totalPages > safePage,
                records
        );
    }

    /**
     * 把空倍率值兜底为默认 1 倍。
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }
}
