package com.zxw.config;

import com.zxw.common.security.PasswordService;
import com.zxw.modules.access.service.UserModelAccessService;
import com.zxw.modules.system.service.AdminSiteSettingsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@ConditionalOnBean(JdbcTemplate.class)
@Component
public class BootstrapDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final UserModelAccessService userModelAccessService;
    private final AdminSiteSettingsService adminSiteSettingsService;

    public BootstrapDataInitializer(JdbcTemplate jdbcTemplate,
                                    PasswordService passwordService,
                                    UserModelAccessService userModelAccessService,
                                    AdminSiteSettingsService adminSiteSettingsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
        this.userModelAccessService = userModelAccessService;
        this.adminSiteSettingsService = adminSiteSettingsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userModelAccessService.initializeDefaults();
        adminSiteSettingsService.initializeDefaults();

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where username = 'admin' and deleted = 0",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                """
                insert into users (username, password_hash, nickname, role_code, status, remark)
                values (?, ?, ?, 'ADMIN', 'ACTIVE', ?)
                """,
                "admin",
                passwordService.encode("admin123456"),
                "System Administrator",
                "Default administrator account, please change the password after first login"
        );

        Long userId = jdbcTemplate.queryForObject(
                "select id from users where username = 'admin' and deleted = 0",
                Long.class
        );
        if (userId != null) {
            jdbcTemplate.update(
                    """
                    insert into wallets (user_id, balance, frozen_balance, total_recharge, total_consume)
                    values (?, 0, 0, 0, 0)
                    on duplicate key update user_id = values(user_id)
                    """,
                    userId
            );
        }
    }
}
