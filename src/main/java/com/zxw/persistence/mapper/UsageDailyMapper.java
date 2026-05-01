package com.zxw.persistence.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日用量汇总 Mapper。
 */
public interface UsageDailyMapper {

    /**
     * 写入或更新每日统计数据。
     */
    int upsert(@Param("statDate") LocalDate statDate,
               @Param("userId") Long userId,
               @Param("modelCode") String modelCode,
               @Param("providerId") Long providerId,
               @Param("successCount") int successCount,
               @Param("totalTokens") int totalTokens,
               @Param("userAmount") BigDecimal userAmount,
               @Param("costAmount") BigDecimal costAmount);
}
