package com.zxw.persistence.mapper;

import com.zxw.persistence.entity.AgentSessionEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 网关 agent 查询 mapper。
 */
public interface GatewayAgentQueryMapper {

    /**
     * 根据响应 id 和用户 id 查找所属会话。
     */
    AgentSessionEntity selectSessionByResponse(@Param("responseId") String responseId, @Param("userId") Long userId);

    /**
     * 查询指定套餐是否绑定了某个模型。
     */
    Long selectBoundPackageIdForModel(@Param("packageId") Long packageId,
                                      @Param("userId") Long userId,
                                      @Param("modelId") Long modelId);

    /**
     * 查询分组下首个可用且包含目标模型的套餐 id。
     */
    Long selectFirstActivePackageIdByGroupAndModel(@Param("userId") Long userId,
                                                   @Param("groupId") Long groupId,
                                                   @Param("modelId") Long modelId);
}
