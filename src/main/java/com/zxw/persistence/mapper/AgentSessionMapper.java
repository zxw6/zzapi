package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.AgentSessionEntity;

import java.time.LocalDateTime;

/**
 * Agent 会话 Mapper。
 */
/**
 * Agent 会话数据访问接口。
 * 负责会话查询、摘要更新和响应追踪。
 */
public interface AgentSessionMapper extends BaseMapper<AgentSessionEntity> {

    default AgentSessionEntity selectBySessionKeyAndUserId(String sessionKey, Long userId) {
        // 按会话标识和用户ID查询 Agent 会话
        return selectOne(Wrappers.<AgentSessionEntity>lambdaQuery()
                .eq(AgentSessionEntity::getSessionKey, sessionKey)
                .eq(AgentSessionEntity::getUserId, userId)
                .last("limit 1"));
    }

    default int updateSummary(Long sessionId, String summaryText, Long summaryMessageId, LocalDateTime updatedAt) {
        // 更新会话压缩后的摘要信息
        AgentSessionEntity updateEntity = new AgentSessionEntity();
        updateEntity.setSummaryText(summaryText);
        updateEntity.setSummaryMessageId(summaryMessageId);
        updateEntity.setUpdatedAt(updatedAt);
        return update(updateEntity, Wrappers.<AgentSessionEntity>lambdaUpdate()
                .eq(AgentSessionEntity::getId, sessionId));
    }

    default int updateLastResponse(Long sessionId, String responseId, LocalDateTime updatedAt) {
        // 更新会话最后一次响应ID
        AgentSessionEntity updateEntity = new AgentSessionEntity();
        updateEntity.setLastResponseId(responseId);
        updateEntity.setUpdatedAt(updatedAt);
        return update(updateEntity, Wrappers.<AgentSessionEntity>lambdaUpdate()
                .eq(AgentSessionEntity::getId, sessionId));
    }

    default AgentSessionEntity selectBySessionKeyAndUserIdOrNull(String sessionKey, Long userId) {
        // 语义化别名，便于调用方表达“允许为空”
        return selectBySessionKeyAndUserId(sessionKey, userId);
    }
}
