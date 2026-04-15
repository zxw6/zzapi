package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtTokenService;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.UserRegisterRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;

    public AdminAuthService(JdbcTemplate jdbcTemplate,
                            PasswordService passwordService,
                            JwtTokenService jwtTokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUserRow user = jdbcTemplate.query("""
                select id, username, password_hash, nickname, role_code, status
                from users
                where username = ? and deleted = 0
                limit 1
                """, rs -> rs.next() ? new AdminUserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getString("role_code"),
                rs.getString("status")
        ) : null, request.username());

        if (user == null || !passwordService.matches(request.password(), user.passwordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        Long userId = user.id();
        jdbcTemplate.update("update users set last_login_at = now() where id = ?", userId);
        JwtUser jwtUser = new JwtUser(userId, user.username(), user.roleCode());
        String token = jwtTokenService.createToken(jwtUser);
        return new AdminLoginResponse(
                token,
                userId,
                user.username(),
                user.nickname(),
                user.roleCode(),
                findBalance(userId)
        );
    }

    @Transactional
    public AdminLoginResponse register(UserRegisterRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessException(400, "QQ邮箱不能为空");
        }
        if (!request.email().toLowerCase().endsWith("@qq.com")) {
            throw new BusinessException(400, "请使用QQ邮箱注册");
        }

        Integer usernameExists = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ? and deleted = 0",
                Integer.class,
                request.username()
        );
        if (usernameExists != null && usernameExists > 0) {
            throw new BusinessException(400, "Username already exists");
        }

        Integer emailExists = jdbcTemplate.queryForObject(
                "select count(*) from users where email = ? and deleted = 0",
                Integer.class,
                request.email()
        );
        if (emailExists != null && emailExists > 0) {
            throw new BusinessException(400, "QQ邮箱已存在");
        }

        jdbcTemplate.update("""
                insert into users (username, password_hash, nickname, email, phone, role_code, status)
                values (?, ?, ?, ?, ?, 'USER', 'ACTIVE')
                """,
                request.username(),
                passwordService.encode(request.password()),
                request.nickname(),
                blankToNull(request.email()),
                blankToNull(request.phone())
        );

        Long userId = jdbcTemplate.queryForObject(
                "select id from users where username = ? and deleted = 0",
                Long.class,
                request.username()
        );
        jdbcTemplate.update("""
                insert into wallets (user_id, balance, frozen_balance, total_recharge, total_consume)
                values (?, 0, 0, 0, 0)
                on duplicate key update user_id = values(user_id)
                """, userId);

        JwtUser jwtUser = new JwtUser(userId, request.username(), "USER");
        String token = jwtTokenService.createToken(jwtUser);
        return new AdminLoginResponse(
                token,
                userId,
                request.username(),
                request.nickname(),
                "USER",
                findBalance(userId)
        );
    }

    public AdminLoginResponse me() {
        JwtUser jwtUser = AdminContext.get();
        if (jwtUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        return jdbcTemplate.query("""
                select u.id, u.username, u.nickname, u.role_code, coalesce(w.balance, 0) as balance
                from users
                left join wallets w on w.user_id = u.id
                where u.id = ? and u.deleted = 0
                """, rs -> rs.next() ? new AdminLoginResponse(
                null,
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("role_code"),
                rs.getBigDecimal("balance")
        ) : null, jwtUser.userId());
    }

    private java.math.BigDecimal findBalance(Long userId) {
        return jdbcTemplate.queryForObject(
                "select coalesce(balance, 0) from wallets where user_id = ?",
                java.math.BigDecimal.class,
                userId
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record AdminUserRow(
            Long id,
            String username,
            String passwordHash,
            String nickname,
            String roleCode,
            String status
    ) {
    }
}
