package com.zxw.common.security;

import com.zxw.common.exception.BusinessException;

/**
 * 管理端登录上下文。
 * 通过 ThreadLocal 保存当前请求对应的登录用户，方便业务层随时读取。
 */
public final class AdminContext {

    private static final ThreadLocal<JwtUser> HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    /**
     * 写入当前请求对应的登录用户。
     */
    public static void set(JwtUser jwtUser) {
        // 写入当前请求线程的用户信息
        HOLDER.set(jwtUser);
    }

    /**
     * 读取当前线程中的登录用户。
     */
    public static JwtUser get() {
        // 读取当前线程中的用户信息
        return HOLDER.get();
    }

    /**
     * 强制要求当前请求已登录。
     */
    public static JwtUser require() {
        // 强制要求当前请求必须已登录
        JwtUser jwtUser = HOLDER.get();
        if (jwtUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        return jwtUser;
    }

    /**
     * 判断当前登录用户是否为管理员。
     */
    public static boolean isAdmin() {
        // 判断当前登录用户是否为管理员
        JwtUser jwtUser = HOLDER.get();
        return jwtUser != null && "ADMIN".equalsIgnoreCase(jwtUser.roleCode());
    }

    /**
     * 强制要求当前用户具备管理员权限。
     */
    public static void requireAdmin() {
        // 要求当前用户必须具备管理员权限
        if (!isAdmin()) {
            throw new BusinessException(403, "仅管理员可操作");
        }
    }

    /**
     * 清理当前线程保存的登录上下文。
     */
    public static void clear() {
        // 请求结束后清理线程变量
        HOLDER.remove();
    }
}
