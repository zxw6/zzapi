package com.zxw.config;

import com.zxw.common.security.PasswordService;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.modules.model.service.AntigravityPresetService;
import com.zxw.modules.system.service.AdminSiteSettingsService;
import com.zxw.persistence.entity.UserEntity;
import com.zxw.persistence.mapper.UserMapper;
import com.zxw.persistence.mapper.WalletMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@ConditionalOnBean(UserMapper.class)
@Component
/**
 * 项目启动后的基础数据初始化器。
 * 主要负责补齐默认管理员、默认站点配置、默认套餐以及预置模型。
 */
public class BootstrapDataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final PasswordService passwordService;
    private final UserModelAccessService userModelAccessService;
    private final AdminSiteSettingsService adminSiteSettingsService;
    private final AntigravityPresetService antigravityPresetService;

    public BootstrapDataInitializer(UserMapper userMapper,
                                    WalletMapper walletMapper,
                                    PasswordService passwordService,
                                    UserModelAccessService userModelAccessService,
                                    AdminSiteSettingsService adminSiteSettingsService,
                                    AntigravityPresetService antigravityPresetService) {
        this.userMapper = userMapper;
        this.walletMapper = walletMapper;
        this.passwordService = passwordService;
        this.userModelAccessService = userModelAccessService;
        this.adminSiteSettingsService = adminSiteSettingsService;
        this.antigravityPresetService = antigravityPresetService;
    }

    @Override
    /**
     * 应用启动后初始化默认数据。
     */
    public void run(ApplicationArguments args) {
        // 先补齐系统运行依赖的默认数据
        userModelAccessService.initializeDefaults();
        adminSiteSettingsService.initializeDefaults();
        antigravityPresetService.syncForExistingProviders();

        // 如果默认管理员已存在，则不再重复创建
        if (userMapper.existsActiveByUsername("admin")) {
            return;
        }

        // 初始化默认管理员账号
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setPasswordHash(passwordService.encode("admin123456"));
        user.setNickname("System Administrator");
        user.setRoleCode("ADMIN");
        user.setStatus("ACTIVE");
        user.setRemark("Default administrator account, please change the password after first login");
        userMapper.insert(user);

        if (user.getId() != null) {
            // 同步为管理员创建默认钱包
            walletMapper.insertDefaultWallet(user.getId());
        }
    }
}
