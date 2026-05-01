package com.zxw.persistence.mapper;

import com.zxw.persistence.model.DashboardModelStatView;
import com.zxw.persistence.model.DashboardOverviewView;
import com.zxw.persistence.model.DashboardTrendView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘统计查询 mapper。
 */
public interface DashboardQueryMapper {

    /**
     * 查询普通用户概览统计。
     */
    DashboardOverviewView selectUserOverview(@Param("userId") Long userId);

    /**
     * 查询管理员概览统计。
     */
    DashboardOverviewView selectAdminOverview();

    /**
     * 查询普通用户趋势统计。
     */
    List<DashboardTrendView> selectUserTrend(@Param("userId") Long userId, @Param("daysBack") int daysBack);

    /**
     * 查询管理员趋势统计。
     */
    List<DashboardTrendView> selectAdminTrend(@Param("daysBack") int daysBack);

    /**
     * 查询普通用户模型统计。
     */
    List<DashboardModelStatView> selectUserModelStats(@Param("userId") Long userId);

    /**
     * 查询管理员模型统计。
     */
    List<DashboardModelStatView> selectAdminModelStats();
}
