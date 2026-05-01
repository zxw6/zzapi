package com.zxw.modules.auth.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtTokenService;
import com.zxw.common.security.JwtUser;
import com.zxw.common.security.PasswordService;
import com.zxw.modules.auth.dto.AdminLoginRequest;
import com.zxw.modules.auth.dto.AdminLoginResponse;
import com.zxw.modules.auth.dto.LoginCaptchaResponse;
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
/**
 * 管理员认证服务。
 * 负责登录、注册以及当前用户信息查询。
 */
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

    /**
     * 生成登录图形验证码。
     */
    public LoginCaptchaResponse createLoginCaptcha() {
        // 登录前先获取一张新的图形验证码
        return loginCaptchaService.createCaptcha();
    }

    /**
     * 使用用户名和密码完成登录。
     */
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 登录前先校验图形验证码，避免账号密码被批量撞库。
        loginCaptchaService.verifyCaptcha(request.captchaId(), request.captchaCode());

        // 按用户名查询有效用户，并校验密码。
        UserEntity user = userMapper.selectActiveByUsername(request.username());

        if (user == null || !passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }

        Long userId = user.getId();
        // 登录成功后刷新最后登录时间。
        userMapper.updateLastLoginAt(userId, LocalDateTime.now());

        // 生成登录令牌并返回当前用户信息。
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

    /**
     * 发送注册验证码。
     */
    public VerificationCodeSendResponse sendRegisterCode(VerificationCodeSendRequest request) {
        // 发送注册验证码到指定邮箱。
        return verificationCodeService.sendRegisterCode(request.email());
    }

    @Transactional
    /**
     * 完成用户注册并自动登录。
     */
    public AdminLoginResponse register(UserRegisterRequest request) {
        // 先做邮箱格式与业务限制校验。
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessException(400, "QQ 邮箱不能为空");
        }
        if (!request.email().trim().toLowerCase().endsWith("@qq.com")) {
            throw new BusinessException(400, "请使用 QQ 邮箱");
        }

        // 校验注册验证码是否正确。
        verificationCodeService.verifyRegisterCode(request.email(), request.verificationCode());

        // 避免用户名和邮箱重复。
        if (userMapper.existsActiveByUsername(request.username())) {
            throw new BusinessException(400, "用户名已存在");
        }

        if (userMapper.existsActiveByEmail(request.email())) {
            throw new BusinessException(400, "QQ 邮箱已存在");
        }

        // 创建普通用户账号。
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordService.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setRoleCode("USER");
        user.setStatus("ACTIVE");
        user.setPackageRestrictionEnabled(1);
        userMapper.insert(user);

        Long userId = user.getId();
        // 注册成功后自动创建钱包。
        walletMapper.insertDefaultWallet(userId);

        // 注册后直接签发登录令牌。
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

    /**
     * 查询当前登录用户信息。
     */
    public AdminLoginResponse me() {
        // 从上下文中读取当前登录用户。
        JwtUser jwtUser = AdminContext.get();
        if (jwtUser == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 查询当前登录用户的最新资料。
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

    /**
     * 查询用户钱包余额，不存在时返回 0。
     */
    private BigDecimal findBalance(Long userId) {
        // 钱包不存在时按 0 余额处理。
        BigDecimal balance = walletMapper.selectBalanceByUserId(userId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    /**
     * 空白字符串转成 null。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
