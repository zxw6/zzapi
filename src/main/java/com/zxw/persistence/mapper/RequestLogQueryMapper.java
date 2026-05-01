package com.zxw.persistence.mapper;

import com.zxw.persistence.model.RequestLogListView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 请求日志查询 mapper。
 */
public interface RequestLogQueryMapper {

    /**
     * 统计管理员可见的请求日志总数。
     */
    long countAdmin();

    /**
     * 统计指定用户可见的请求日志总数。
     */
    long countUser(@Param("userId") Long userId);

    /**
     * 分页查询管理员可见的请求日志。
     */
    List<RequestLogListView> pageAdmin(@Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    /**
     * 分页查询指定用户可见的请求日志。
     */
    List<RequestLogListView> pageUser(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);
}
