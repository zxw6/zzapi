package com.zxw.persistence.mapper;

import com.zxw.persistence.model.UserListView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户列表查询 mapper。
 */
public interface UserQueryMapper {

    /**
     * 查询用户列表。
     */
    List<UserListView> selectUsers();

    /**
     * 按用户 id 查询详情。
     */
    UserListView selectUserById(@Param("userId") Long userId);
}
