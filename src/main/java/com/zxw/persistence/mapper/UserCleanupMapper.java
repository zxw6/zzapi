package com.zxw.persistence.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 用户删除清理 mapper。
 * 用于在删除用户前清理其关联的 agent、日志和套餐数据。
 */
public interface UserCleanupMapper {

    /**
     * 删除用户的工具日志。
     */
    int deleteAgentToolLogsByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的会话消息。
     */
    int deleteAgentMessagesByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的 agent 会话。
     */
    int deleteAgentSessionsByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的请求日志。
     */
    int deleteRequestLogsByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的日用量汇总。
     */
    int deleteUsageDailyByUserId(@Param("userId") Long userId);

    /**
     * 删除用户已购买的套餐记录。
     */
    int deleteUserModelPackagesByUserId(@Param("userId") Long userId);
}
