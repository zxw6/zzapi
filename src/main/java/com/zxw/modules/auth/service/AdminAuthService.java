package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtTokenService;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.UserRegisterRequest;
import com.zxw.modules.auth.dto.VerificationCodeSendRequest;
import com.zxw.modules.auth.dto.VerificationCodeSendResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AdminAuthService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final VerificationCodeService verificationCodeService;

    public AdminAuthService(JdbcTemplate jdbcTemplate,
                            PasswordService passwordService,
                            JwtTokenService jwtTokenService,
                            VerificationCodeService verificationCodeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
        this.verificationCodeService = verificationCodeService;
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
            throw new BusinessException(401, "Invalid username or password");
        }
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException(403, "Account is disabled");
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

    public VerificationCodeSendResponse sendRegisterCode(VerificationCodeSendRequest request) {
        return verificationCodeService.sendRegisterCode(request.email());
    }

    @Transactional
    public AdminLoginResponse register(UserRegisterRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessException(400, "QQ email cannot be blank");
        }
        if (!request.email().trim().toLowerCase().endsWith("@qq.com")) {
            throw new BusinessException(400, "Please use a QQ email");
        }

        verificationCodeService.verifyRegisterCode(request.email(), request.verificationCode());

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
            throw new BusinessException(400, "QQ email already exists");
        }

        jdbcTemplate.update("""
                insert into users (username, password_hash, nickname, email, phone, role_code, status, package_restriction_enabled)
                values (?, ?, ?, ?, ?, 'USER', 'ACTIVE', 1)
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
            throw new BusinessException(401, "Please login first");
        }
        return jdbcTemplate.query("""
                select u.id, u.username, u.nickname, u.role_code, coalesce(w.balance, 0) as balance
                from users u
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

    private BigDecimal findBalance(Long userId) {
        return jdbcTemplate.queryForObject(
                "select coalesce(balance, 0) from wallets where user_id = ?",
                BigDecimal.class,
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
