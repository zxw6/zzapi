package com.zxw.common.security;

import com.zxw.common.exception.BusinessException;

public final class AdminContext {

    private static final ThreadLocal<JwtUser> HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(JwtUser jwtUser) {
        HOLDER.set(jwtUser);
    }

    public static JwtUser get() {
        return HOLDER.get();
    }

    public static JwtUser require() {
        JwtUser jwtUser = HOLDER.get();
        if (jwtUser == null) {
            throw new BusinessException(401, "Please login first");
        }
        return jwtUser;
    }

    public static boolean isAdmin() {
        JwtUser jwtUser = HOLDER.get();
        return jwtUser != null && "ADMIN".equalsIgnoreCase(jwtUser.roleCode());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(403, "Admin only");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
