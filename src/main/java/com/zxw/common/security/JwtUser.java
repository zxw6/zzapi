package com.zxw.common.security;

/**
 * JWT 中保存的用户身份快照。
 * 只保留当前请求处理所需的最核心字段。
 */
/**
 * 当前请求的登录用户快照。
 * 仅承载鉴权和权限判断所需的最小用户信息。
 */
public record JwtUser(
        Long userId,
        String username,
        String roleCode
) {
}
