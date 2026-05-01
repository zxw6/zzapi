package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.UserEntity;

import java.time.LocalDateTime;

/**
 * 用户表 Mapper。
 */
/**
 * 用户数据访问接口。
 * 提供有效用户查询、重复校验和状态维护能力。
 */
public interface UserMapper extends BaseMapper<UserEntity> {

    default UserEntity selectActiveByUsername(String username) {
        // 按用户名查询未删除的有效用户
        return selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0)
                .last("limit 1"));
    }

    default UserEntity selectActiveById(Long userId) {
        // 按主键查询未删除的有效用户
        return selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0)
                .last("limit 1"));
    }

    default boolean existsActiveByUsername(String username) {
        // 判断用户名是否已被有效用户占用
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveByEmail(String email) {
        // 判断邮箱是否已被有效用户占用
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveByEmailExcludingId(String email, Long excludedUserId) {
        // 排除当前用户后检查邮箱是否冲突
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .ne(UserEntity::getId, excludedUserId)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveById(Long userId) {
        // 判断用户主记录是否存在且未删除
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default int updateLastLoginAt(Long userId, LocalDateTime lastLoginAt) {
        // 单独更新最后登录时间
        UserEntity updateUser = new UserEntity();
        updateUser.setLastLoginAt(lastLoginAt);
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId));
    }

    default int updateActiveUser(Long userId, UserEntity updateUser) {
        // 更新未删除用户的基础信息
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0));
    }

    default int updateActiveUserStatus(Long userId, String status, LocalDateTime updatedAt) {
        // 单独更新用户状态字段
        UserEntity updateUser = new UserEntity();
        updateUser.setStatus(status);
        updateUser.setUpdatedAt(updatedAt);
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0));
    }
}
