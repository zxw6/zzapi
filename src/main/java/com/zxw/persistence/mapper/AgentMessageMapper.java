package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.AgentMessageEntity;

import java.util.List;

/**
 * Agent 消息 Mapper。
 */
/**
 * Agent 消息数据访问接口。
 * 提供会话消息查询与摘要截断后的消息读取能力。
 */
public interface AgentMessageMapper extends BaseMapper<AgentMessageEntity> {

    default List<AgentMessageEntity> selectAfterSummary(Long sessionId, Long summaryMessageId) {
        // 查询某个会话中摘要点之后的消息列表
        return selectList(Wrappers.<AgentMessageEntity>lambdaQuery()
                .eq(AgentMessageEntity::getSessionId, sessionId)
                .gt(AgentMessageEntity::getId, summaryMessageId)
                .orderByAsc(AgentMessageEntity::getId));
    }

    default List<AgentMessageEntity> selectBySessionIdAfterSummary(Long sessionId, Long summaryMessageId) {
        // 提供一个更贴近业务含义的方法名
        return selectAfterSummary(sessionId, summaryMessageId);
    }
}
