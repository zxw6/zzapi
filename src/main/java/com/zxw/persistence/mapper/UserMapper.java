package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.UserEntity;

import java.time.LocalDateTime;

public interface UserMapper extends BaseMapper<UserEntity> {

    default UserEntity selectActiveByUsername(String username) {
        return selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0)
                .last("limit 1"));
    }

    default UserEntity selectActiveById(Long userId) {
        return selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0)
                .last("limit 1"));
    }

    default boolean existsActiveByUsername(String username) {
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveByEmail(String email) {
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveByEmailExcludingId(String email, Long excludedUserId) {
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .ne(UserEntity::getId, excludedUserId)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default boolean existsActiveById(Long userId) {
        return selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    default int updateLastLoginAt(Long userId, LocalDateTime lastLoginAt) {
        UserEntity updateUser = new UserEntity();
        updateUser.setLastLoginAt(lastLoginAt);
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId));
    }

    default int updateLastActiveAt(Long userId, LocalDateTime lastActiveAt) {
        UserEntity updateUser = new UserEntity();
        updateUser.setLastActiveAt(lastActiveAt);
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0));
    }

    default int updateActiveUser(Long userId, UserEntity updateUser) {
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0));
    }

    default int updateActiveUserStatus(Long userId, String status, LocalDateTime updatedAt) {
        UserEntity updateUser = new UserEntity();
        updateUser.setStatus(status);
        updateUser.setUpdatedAt(updatedAt);
        return update(updateUser, Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0));
    }
}
