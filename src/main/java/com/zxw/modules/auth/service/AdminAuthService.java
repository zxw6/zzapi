package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtTokenService;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.LoginCaptchaResponse;
import com.zxw.modules.auth.dto.PasswordResetRequest;
import com.zxw.modules.auth.dto.UserRegisterRequest;
import com.zxw.modules.auth.dto.VerificationCodeSendRequest;
import com.zxw.modules.auth.dto.VerificationCodeSendResponse;
import com.zxw.persistence.entity.UserEntity;
import com.zxw.persistence.mapper.UserMapper;
import com.zxw.persistence.mapper.WalletMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final VerificationCodeService verificationCodeService;
    private final LoginCaptchaService loginCaptchaService;

    public AdminAuthService(UserMapper userMapper,
                            WalletMapper walletMapper,
                            PasswordService passwordService,
                            JwtTokenService jwtTokenService,
                            VerificationCodeService verificationCodeService,
                            LoginCaptchaService loginCaptchaService) {
        this.userMapper = userMapper;
        this.walletMapper = walletMapper;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
        this.verificationCodeService = verificationCodeService;
        this.loginCaptchaService = loginCaptchaService;
    }

    public LoginCaptchaResponse createLoginCaptcha() {
        return loginCaptchaService.createCaptcha();
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        loginCaptchaService.verifyCaptcha(request.captchaId(), request.captchaCode());

        UserEntity user = findLoginUser(request.username());
        if (user == null || !passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        Long userId = user.getId();
        userMapper.updateLastLoginAt(userId, LocalDateTime.now());

        JwtUser jwtUser = new JwtUser(userId, user.getUsername(), user.getRoleCode());
        String token = jwtTokenService.createToken(jwtUser);
        return new AdminLoginResponse(
                token,
                userId,
                user.getUsername(),
                user.getNickname(),
                user.getRoleCode(),
                findBalance(userId)
        );
    }

    public VerificationCodeSendResponse sendRegisterCode(VerificationCodeSendRequest request) {
        return verificationCodeService.sendRegisterCode(request.email());
    }

    public VerificationCodeSendResponse sendPasswordResetCode(VerificationCodeSendRequest request) {
        String normalizedEmail = normalizeQqEmail(request.email());
        UserEntity user = requireActiveUserByEmail(normalizedEmail);
        return verificationCodeService.sendPasswordResetCode(user.getEmail());
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String normalizedEmail = normalizeQqEmail(request.email());
        UserEntity user = requireActiveUserByEmail(normalizedEmail);

        verificationCodeService.verifyPasswordResetCode(normalizedEmail, request.verificationCode());

        UserEntity updateUser = new UserEntity();
        updateUser.setPasswordHash(passwordService.encode(request.newPassword()));
        updateUser.setUpdatedAt(LocalDateTime.now());
        int updated = userMapper.updateActiveUser(user.getId(), updateUser);
        if (updated == 0) {
            throw new BusinessException(500, "密码重置失败");
        }
    }

    @Transactional
    public AdminLoginResponse register(UserRegisterRequest request) {
        String normalizedEmail = normalizeQqEmail(request.email());
        verificationCodeService.verifyRegisterCode(normalizedEmail, request.verificationCode());

        if (userMapper.existsActiveByUsername(request.username())) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (userMapper.existsActiveByEmail(normalizedEmail)) {
            throw new BusinessException(400, "QQ邮箱已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordService.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(normalizedEmail);
        user.setPhone(blankToNull(request.phone()));
        user.setRoleCode("USER");
        user.setStatus("ACTIVE");
        user.setPackageRestrictionEnabled(1);
        userMapper.insert(user);

        Long userId = user.getId();
        walletMapper.insertDefaultWallet(userId);

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

        UserEntity user = userMapper.selectActiveById(jwtUser.userId());
        if (user == null) {
            return null;
        }

        return new AdminLoginResponse(
                null,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRoleCode(),
                findBalance(user.getId())
        );
    }

    private UserEntity requireActiveUserByEmail(String email) {
        UserEntity user = userMapper.selectActiveByEmail(email);
        if (user == null) {
            throw new BusinessException(404, "QQ邮箱不存在");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return user;
    }

    private BigDecimal findBalance(Long userId) {
        BigDecimal balance = walletMapper.selectBalanceByUserId(userId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeQqEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "QQ邮箱不能为空");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!normalizedEmail.endsWith("@qq.com")) {
            throw new BusinessException(400, "请使用QQ邮箱");
        }
        return normalizedEmail;
    }
    private UserEntity findLoginUser(String loginIdentity) {
        if (loginIdentity == null || loginIdentity.isBlank()) {
            return null;
        }
        String normalizedIdentity = loginIdentity.trim();
        if (normalizedIdentity.contains("@")) {
            return userMapper.selectActiveByEmail(normalizedIdentity.toLowerCase());
        }
        return userMapper.selectActiveByUsername(normalizedIdentity);
    }
}
