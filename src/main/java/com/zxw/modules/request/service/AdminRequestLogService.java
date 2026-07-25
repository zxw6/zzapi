package com.zxw.modules.request.service;

import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.request.dto.RequestLogItemResponse;
import com.zxw.modules.request.dto.RequestLogPageResponse;
import com.zxw.persistence.mapper.RequestLogMapper;
import com.zxw.persistence.mapper.RequestLogQueryMapper;
import com.zxw.persistence.model.RequestLogListView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminRequestLogService {

    private final RequestLogQueryMapper requestLogQueryMapper;
    private final RequestLogMapper requestLogMapper;

    public AdminRequestLogService(RequestLogQueryMapper requestLogQueryMapper,
                                  RequestLogMapper requestLogMapper) {
        this.requestLogQueryMapper = requestLogQueryMapper;
        this.requestLogMapper = requestLogMapper;
    }

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
                .map(item -> new RequestLogItemResponse(
                        item.getRequestId(),
                        item.getUsername(),
                        item.getModelCode(),
                        item.getUpstreamModel(),
                        item.getPackageName(),
                        defaultBigDecimal(item.getMultiplier()),
                        item.getStatusCode(),
                        item.getLatencyMs(),
                        item.getFirstTokenLatencyMs(),
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

    @Transactional
    public int clear(Long userId) {
        JwtUser currentUser = AdminContext.require();
        if (AdminContext.isAdmin()) {
            return userId == null ? requestLogMapper.deleteAllLogs() : requestLogMapper.deleteByUserId(userId);
        }
        return requestLogMapper.deleteByUserId(currentUser.userId());
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }
}
