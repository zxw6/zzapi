package com.zxw.common.security;

import com.zxw.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;
    private final AdminActivityService adminActivityService;

    public AdminAuthInterceptor(JwtTokenService jwtTokenService,
                                AdminActivityService adminActivityService) {
        this.jwtTokenService = jwtTokenService;
        this.adminActivityService = adminActivityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }

        JwtUser jwtUser = jwtTokenService.parseToken(token.substring(7));
        AdminContext.set(jwtUser);
        adminActivityService.markActive(jwtUser.userId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }
}
