package com.zxw.common.security;

import com.zxw.common.exception.BusinessException;
import com.zxw.config.GatewaySecurityProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
/**
 * AES 加解密服务。
 * 主要用于对上游渠道令牌等敏感信息进行加密存储和解密读取。
 */
public class AesCryptoService {

    private final SecretKeySpec secretKeySpec;

    public AesCryptoService(GatewaySecurityProperties properties) {
        // 读取配置中的 AES 密钥，并校验长度是否符合要求
        String secret = properties.cryptoSecret();
        if (secret == null || secret.length() != 16) {
            throw new IllegalArgumentException("gateway.security.crypto-secret 必须是 16 位字符串");
        }
        this.secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
    }

    /**
     * 加密敏感明文内容。
     */
    public String encrypt(String plainText) {
        // 空值统一返回空串，避免数据库里出现无意义密文
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        try {
            // 使用 AES/ECB/PKCS5Padding 对明文做对称加密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(500, "上游密钥加密失败");
        }
    }

    /**
     * 解密数据库中存储的密文内容。
     */
    public String decrypt(String encryptedText) {
        // 空值统一返回空串
        if (encryptedText == null || encryptedText.isBlank()) {
            return "";
        }
        try {
            // 先做 Base64 解码，再执行 AES 解密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(500, "上游密钥解密失败");
        }
    }
}
