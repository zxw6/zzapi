package com.zxw.persistence.mapper;

import com.zxw.persistence.model.ApiKeyListView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API Key 列表查询 mapper。
 */
public interface ApiKeyQueryMapper {

    /**
     * 查询管理员视角的 API Key 列表。
     */
    List<ApiKeyListView> listAdmin();

    /**
     * 查询指定用户的 API Key 列表。
     */
    List<ApiKeyListView> listUser(@Param("userId") Long userId);
}
