package com.zxw.common.security;

import com.zxw.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
/**
 * 管理端认证拦截器。
 * 负责拦截后台请求、解析 JWT，并把当前用户写入线程上下文。
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;

    public AdminAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    /**
     * 在请求进入控制器前完成后台身份校验。
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求不做鉴权，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 从 Authorization 头中解析 Bearer Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }

        // 把登录用户信息放到上下文，方便后续业务直接读取
        JwtUser jwtUser = jwtTokenService.parseToken(token.substring(7));
        AdminContext.set(jwtUser);
        return true;
    }

    @Override
    /**
     * 请求结束后清理线程上下文。
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理线程上下文，避免串数据
        AdminContext.clear();
    }
}
