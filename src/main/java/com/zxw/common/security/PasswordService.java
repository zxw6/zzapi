package com.zxw.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
/**
 * 密码服务。
 * 负责密码哈希和密码匹配校验。
 */
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行 BCrypt 加密。
     */
    public String encode(String rawPassword) {
        // 对明文密码进行 BCrypt 加密
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文密码和哈希密码是否匹配。
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        // 比较明文密码和数据库中的哈希值是否一致
        return encoder.matches(rawPassword, encodedPassword);
    }
}
