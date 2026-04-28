package com.zxw.modules.access.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.access.dto.ModelAccessSummaryResponse;
import com.zxw.modules.access.dto.ModelGroupCreateRequest;
import com.zxw.modules.access.dto.ModelGroupOptionResponse;
import com.zxw.modules.access.dto.ModelPackagePurchaseRecordResponse;
import com.zxw.modules.access.dto.PurchaseModelPackageRequest;
import com.zxw.modules.apikey.service.ApiKeyAuthService;
import com.zxw.modules.gateway.service.GatewayRouteService;
import com.zxw.modules.user.dto.WalletTransactionItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserModelAccessService {

    private static final BigDecimal DEFAULT_PACKAGE_PRICE = new BigDecimal("70.0000");
    private static final int DEFAULT_PACKAGE_DAYS = 30;
    private static final BigDecimal DEFAULT_DAILY_QUOTA = new BigDecimal("60.0000");
    private static final BigDecimal DEFAULT_WEEKLY_QUOTA = new BigDecimal("420.0000");
    private static final BigDecimal DEFAULT_MONTHLY_QUOTA = new BigDecimal("1800.0000");

    private static final List<PresetGroup> PRESET_GROUPS = List.of(
            new PresetGroup("claude", "Claude 套餐", "适合 Claude / Sonnet / Opus 系列"),
            new PresetGroup("codex", "Codex 套餐", "适合 Codex 与编程模型"),
            new PresetGroup("gpt", "GPT 分组", "适合 GPT 系列模型")
    );

    private final JdbcTemplate jdbcTemplate;
    private final Object defaultInitializationLock = new Object();
    private volatile boolean defaultsInitialized;

    public UserModelAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void initializeDefaults() {
        if (defaultsInitialized) {
            return;
        }
        synchronized (defaultInitializationLock) {
            if (defaultsInitialized) {
                return;
            }
            initializeDefaultsOnce();
            defaultsInitialized = true;
        }
    }

    private void initializeDefaultsOnce() {
        ensureTableExists("model_groups", """
                create table if not exists model_groups (
                    id bigint primary key auto_increment,
                    group_code varchar(64) not null,
                    group_name varchar(64) not null,
                    sale_price decimal(18, 4) not null default 0.0000,
                    package_days int not null default 30,
                    daily_quota decimal(18, 4) not null default 0.0000,
                    weekly_quota decimal(18, 4) not null default 0.0000,
                    monthly_quota decimal(18, 4) not null default 0.0000,
                    status varchar(32) not null default 'ACTIVE',
                    remark varchar(255) null,
                    created_at datetime not null default current_timestamp,
                    updated_at datetime not null default current_timestamp on update current_timestamp,
                    unique key uk_model_groups_code (group_code),
                    key idx_model_groups_status (status)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
        ensureTableExists("model_group_models", """
                create table if not exists model_group_models (
                    id bigint primary key auto_increment,
                    group_id bigint not null,
                    model_id bigint not null,
                    created_at datetime not null default current_timestamp,
                    unique key uk_model_group_models_unique (group_id, model_id),
                    key idx_model_group_models_group (group_id, model_id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
        ensureTableExists("user_model_packages", """
                create table if not exists user_model_packages (
                    id bigint primary key auto_increment,
                    user_id bigint not null,
                    group_id bigint not null,
                    package_name varchar(64) not null,
                    purchase_price decimal(18, 4) not null default 0.0000,
                    start_at datetime not null,
                    expires_at datetime not null,
                    status varchar(32) not null default 'ACTIVE',
                    created_at datetime not null default current_timestamp,
                    updated_at datetime not null default current_timestamp on update current_timestamp,
                    key idx_user_model_packages_user_group_status (user_id, group_id, status),
                    key idx_user_model_packages_expire (expires_at)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);

        ensureColumnExists("users", "package_restriction_enabled",
                "alter table users add column package_restriction_enabled tinyint(1) not null default 1");
        ensureColumnExists("api_keys", "model_group_id",
                "alter table api_keys add column model_group_id bigint null");
        ensureColumnExists("api_keys", "user_package_id",
                "alter table api_keys add column user_package_id bigint null");
        ensureColumnExists("api_keys", "total_quota",
                "alter table api_keys add column total_quota decimal(18, 4) not null default 0.0000");
        ensureColumnExists("api_keys", "used_quota",
                "alter table api_keys add column used_quota decimal(18, 4) not null default 0.0000");
        ensureColumnExists("request_logs", "user_package_id",
                "alter table request_logs add column user_package_id bigint null after api_key_id");
        ensureColumnExists("request_logs", "cached_prompt_tokens",
                "alter table request_logs add column cached_prompt_tokens int not null default 0 after total_tokens");
        ensureColumnExists("models", "cached_prompt_price",
                "alter table models add column cached_prompt_price decimal(18, 6) not null default 0.000000 after prompt_price");
        ensureColumnExists("model_group_models", "billing_type",
                "alter table model_group_models add column billing_type varchar(32) null after model_id");
        ensureColumnExists("model_group_models", "prompt_price",
                "alter table model_group_models add column prompt_price decimal(18, 6) null after billing_type");
        ensureColumnExists("model_group_models", "cached_prompt_price",
                "alter table model_group_models add column cached_prompt_price decimal(18, 6) null after prompt_price");
        ensureColumnExists("model_group_models", "completion_price",
                "alter table model_group_models add column completion_price decimal(18, 6) null after cached_prompt_price");
        ensureColumnExists("model_group_models", "request_price",
                "alter table model_group_models add column request_price decimal(18, 6) null after completion_price");
        ensureColumnExists("model_group_models", "multiplier",
                "alter table model_group_models add column multiplier decimal(18, 4) null after request_price");
        ensureIndexExists("api_keys", "idx_api_keys_package",
                "create index idx_api_keys_package on api_keys (user_package_id)");
        ensureIndexExists("api_keys", "idx_api_keys_group",
                "create index idx_api_keys_group on api_keys (model_group_id)");
        ensureIndexExists("request_logs", "idx_request_logs_package_date",
                "create index idx_request_logs_package_date on request_logs (user_package_id, request_date)");

        expirePackages();
        ensurePresetGroups();
        backfillModelGroupPrices();
        jdbcTemplate.update("""
                update users
                set package_restriction_enabled = case
                    when upper(role_code) = 'ADMIN' then 0
                    else 1
                end
                where deleted = 0
                """);
    }

    public ModelAccessSummaryResponse getCurrentSummary() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        expirePackages();
        return buildSummary(currentUser.userId());
    }

    public ApiKeyPackageBinding resolveApiKeyPackageBinding(Long userId, Long requestedPackageId, Long requestedGroupId) {
        initializeDefaults();
        expirePackages();
        UserPackageRow targetPackage = requestedPackageId != null
                ? findPackageById(userId, requestedPackageId)
                : requestedGroupId == null ? null : findLatestPackage(userId, requestedGroupId);
        if (targetPackage == null) {
            throw new BusinessException(403, "请选择一个已购买且有效的套餐");
        }
        if (!targetPackage.active()) {
            throw new BusinessException(403, "套餐已过期");
        }
        return new ApiKeyPackageBinding(targetPackage.id(), targetPackage.groupId(), targetPackage.groupName());
    }

    public Long resolveGatewayPackageId(ApiKeyAuthService.AuthenticatedApiKey auth,
                                        GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        UserPackageRow targetPackage = resolveGatewayPackage(auth, route);
        return targetPackage == null ? null : targetPackage.id();
    }

    public Long resolveGatewayPackageGroupId(ApiKeyAuthService.AuthenticatedApiKey auth,
                                             GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        UserPackageRow targetPackage = resolveGatewayPackage(auth, route);
        return targetPackage == null ? auth == null ? null : auth.modelGroupId() : targetPackage.groupId();
    }

    public Long resolveApiKeyModelGroupId(Long userId, Long requestedGroupId) {
        initializeDefaults();
        expirePackages();
        if (requestedGroupId == null) {
            throw new BusinessException(403, "请先购买套餐并选择已购套餐后再创建 API Key");
        }
        validateApiKeyCreationAccess(userId, requestedGroupId);
        return requestedGroupId;
    }

    @Transactional
    public ModelAccessSummaryResponse createGroup(ModelGroupCreateRequest request) {
        AdminContext.requireAdmin();
        initializeDefaults();

        String groupCode = normalizeGroupCode(request.groupCode());
        if (groupCode.isBlank()) {
            throw new BusinessException(400, "套餐编码不能为空");
        }

        Integer exists = jdbcTemplate.queryForObject("""
                select count(*)
                from model_groups
                where group_code = ?
                """, Integer.class, groupCode);
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "套餐编码已存在");
        }

        jdbcTemplate.update("""
                insert into model_groups (group_code, group_name, sale_price, package_days, daily_quota, weekly_quota, monthly_quota, status, remark)
                values (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """,
                groupCode,
                trimToLength(request.groupName(), 64),
                numberOrZero(request.salePrice()),
                request.packageDays() == null || request.packageDays() <= 0 ? DEFAULT_PACKAGE_DAYS : request.packageDays(),
                numberOrZero(request.dailyQuota()),
                numberOrZero(request.weeklyQuota()),
                numberOrZero(request.monthlyQuota()),
                trimToLength(request.remark(), 255)
        );
        return buildSummary(AdminContext.require().userId());
    }

    @Transactional
    public ModelAccessSummaryResponse purchase(PurchaseModelPackageRequest request) {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        expirePackages();

        Long userId = currentUser.userId();
        ModelGroupRow group = getGroupById(request.groupId());
        WalletRow wallet = getWallet(userId);
        if (wallet.balance().compareTo(group.salePrice()) < 0) {
            throw new BusinessException(400, "余额不足，请先充值");
        }

        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime expiresAt = startAt.plusDays(group.packageDays());

        jdbcTemplate.update("""
                insert into user_model_packages (user_id, group_id, package_name, purchase_price, start_at, expires_at, status)
                values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                """,
                userId,
                group.id(),
                group.groupName(),
                group.salePrice(),
                startAt,
                expiresAt
        );

        BigDecimal balanceBefore = wallet.balance();
        BigDecimal balanceAfter = balanceBefore.subtract(group.salePrice());
        jdbcTemplate.update("""
                update wallets
                set balance = balance - ?, total_consume = total_consume + ?, updated_at = now()
                where user_id = ?
                """, group.salePrice(), group.salePrice(), userId);

        jdbcTemplate.update("""
                insert into transactions (user_id, wallet_id, order_no, transaction_type, direction, amount,
                                          balance_before, balance_after, status, description_text, transaction_date)
                values (?, ?, ?, 'PACKAGE_BUY', 'OUT', ?, ?, ?, 'SUCCESS', ?, curdate())
                """,
                userId,
                wallet.id(),
                buildOrderNo("P"),
                group.salePrice(),
                balanceBefore,
                balanceAfter,
                "购买 " + group.groupName()
        );

        return buildSummary(userId);
    }

    @Transactional
    public void disableGroup(Long groupId) {
        AdminContext.requireAdmin();
        ModelGroupRow group = getGroupByIdIncludingDisabled(groupId);
        if (group == null) {
            throw new BusinessException(404, "套餐分组不存在");
        }
        int updated = jdbcTemplate.update("""
                update model_groups
                set status = 'DISABLED', updated_at = now()
                where id = ?
                """, groupId);
        if (updated == 0) {
            throw new BusinessException(400, "套餐分组删除失败");
        }
    }

    @Transactional
    public void disablePurchasedPackage(Long packageId) {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        int updated = jdbcTemplate.update("""
                update user_model_packages
                set status = 'DELETED', updated_at = now()
                where id = ?
                  and (? = 1 or user_id = ?)
                  and status <> 'DELETED'
                """, packageId, admin ? 1 : 0, currentUser.userId());
        if (updated == 0) {
            throw new BusinessException(404, "Purchased package does not exist");
        }
        jdbcTemplate.update("""
                update api_keys
                set status = 'DISABLED', updated_at = now()
                where user_package_id = ?
                  and (? = 1 or user_id = ?)
                  and deleted = 0
                """, packageId, admin ? 1 : 0, currentUser.userId());
    }

    public void validateApiKeyCreationAccess(Long userId, Long modelGroupId) {
        initializeDefaults();
        expirePackages();
        if (modelGroupId == null) {
            throw new BusinessException(403, "请先选择已购买且有效的套餐");
        }

        getGroupById(modelGroupId);
        UserPackageRow latestPackage = findLatestPackage(userId, modelGroupId);
        if (latestPackage == null) {
            throw new BusinessException(403, "所选套餐尚未购买");
        }
        if (!latestPackage.active()) {
            throw new BusinessException(403, "套餐过期");
        }
    }

    public void validateGatewayAccess(ApiKeyAuthService.AuthenticatedApiKey auth,
                                      GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        if (auth == null || route == null) {
            return;
        }
        if ("ADMIN".equalsIgnoreCase(auth.roleCode())) {
            return;
        }
        if (auth.modelGroupId() == null) {
            throw new BusinessException(403, "当前 API Key 未绑定套餐，请重新创建");
        }

        UserPackageRow matchedPackage = resolveGatewayPackage(auth, route);
        if (matchedPackage != null) {
            if (!matchedPackage.active()) {
                throw new BusinessException(403, "套餐已过期");
            }
            LocalDate today = LocalDate.now();
            BigDecimal usedToday = calculatePackageUsageAmount(matchedPackage.id(), today, today);
            BigDecimal usedThisWeek = calculatePackageUsageAmount(matchedPackage.id(), today.minusDays(6), today);
            BigDecimal usedThisMonth = calculatePackageUsageAmount(matchedPackage.id(), today.withDayOfMonth(1), today);
            validateQuotaLimits(matchedPackage.groupName(), matchedPackage.dailyQuota(), usedToday,
                    matchedPackage.weeklyQuota(), usedThisWeek,
                    matchedPackage.monthlyQuota(), usedThisMonth,
                    resolveTotalQuota(matchedPackage.dailyQuota(), matchedPackage.monthlyQuota(), matchedPackage.packageDays()),
                    calculatePackageTotalUsageAmount(matchedPackage.id()));
            return;
        }
        if (auth.userPackageId() != null) {
            throw new BusinessException(403,
                    "Model " + route.modelCode() + " is not available in the selected package, or the package is expired/deleted.");
        }

        Integer inGroup = jdbcTemplate.queryForObject("""
                select count(*)
                from model_group_models
                where group_id = ? and model_id = ?
                """, Integer.class, auth.modelGroupId(), route.modelId());
        if (inGroup == null || inGroup == 0) {
            String groupName = auth.modelGroupName() == null || auth.modelGroupName().isBlank()
                    ? "当前套餐"
                    : auth.modelGroupName();
            throw new BusinessException(400,
                    "模型错误：当前 API Key 仅支持套餐【" + groupName + "】内的模型，不能使用 " + route.modelCode());
        }

        UserPackageRow latestPackage = findLatestPackage(auth.userId(), auth.modelGroupId());
        if (latestPackage == null) {
            throw new BusinessException(403, "当前套餐不存在");
        }
        if (!latestPackage.active()) {
            throw new BusinessException(403, "套餐过期");
        }
        LocalDate today = LocalDate.now();
        BigDecimal usedToday = calculateUsageAmount(auth.userId(), auth.modelGroupId(), today, today);
        BigDecimal usedThisWeek = calculateUsageAmount(auth.userId(), auth.modelGroupId(), today.minusDays(6), today);
        BigDecimal usedThisMonth = calculateUsageAmount(auth.userId(), auth.modelGroupId(), today.withDayOfMonth(1), today);
        validateQuotaLimits(latestPackage.groupName(), latestPackage.dailyQuota(), usedToday,
                latestPackage.weeklyQuota(), usedThisWeek,
                latestPackage.monthlyQuota(), usedThisMonth,
                resolveTotalQuota(latestPackage.dailyQuota(), latestPackage.monthlyQuota(), latestPackage.packageDays()),
                calculatePackageTotalUsageAmount(latestPackage.id()));
    }

    public void validateGatewayPackageAccess(ApiKeyAuthService.AuthenticatedApiKey auth,
                                             GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        if (auth == null || route == null) {
            return;
        }
        if ("ADMIN".equalsIgnoreCase(auth.roleCode())
                && auth.modelGroupId() == null
                && auth.userPackageId() == null) {
            return;
        }
        if (auth.modelGroupId() == null) {
            throw new BusinessException(403, "当前 API Key 未绑定套餐，请重新创建");
        }

        UserPackageRow targetPackage = resolveGatewayPackage(auth, route);
        if (targetPackage == null) {
            String groupName = auth.userPackageName() == null || auth.userPackageName().isBlank()
                    ? auth.modelGroupName()
                    : auth.userPackageName();
            if (groupName == null || groupName.isBlank()) {
                groupName = "current package";
            }
            throw new BusinessException(403,
                    "Model " + route.modelCode() + " is not available in " + groupName
                            + ", or the selected package is expired/deleted.");
        }
        if (!targetPackage.active()) {
            throw new BusinessException(403, "套餐已过期");
        }
        LocalDate today = LocalDate.now();
        BigDecimal usedToday = calculatePackageUsageAmount(targetPackage.id(), today, today);
        BigDecimal usedThisWeek = calculatePackageUsageAmount(targetPackage.id(), today.minusDays(6), today);
        BigDecimal usedThisMonth = calculatePackageUsageAmount(targetPackage.id(), today.withDayOfMonth(1), today);
        validateQuotaLimits(targetPackage.groupName(), targetPackage.dailyQuota(), usedToday,
                targetPackage.weeklyQuota(), usedThisWeek,
                targetPackage.monthlyQuota(), usedThisMonth,
                resolveTotalQuota(targetPackage.dailyQuota(), targetPackage.monthlyQuota(), targetPackage.packageDays()),
                calculatePackageTotalUsageAmount(targetPackage.id()));
    }

    @Transactional
    public void replaceModelGroupBinding(Long modelId, Long groupId) {
        AdminContext.requireAdmin();
        initializeDefaults();
        if (modelId == null) {
            throw new BusinessException(400, "模型不存在");
        }
        getGroupById(groupId);
        jdbcTemplate.update("delete from model_group_models where model_id = ?", modelId);
        addModelGroupBinding(modelId, groupId);
    }

    @Transactional
    public void addModelGroupBinding(Long modelId, Long groupId) {
        addModelGroupBindingWithPrices(modelId, groupId, null, null, null, null, null, null);
    }

    @Transactional
    public void addModelGroupBindingWithPrices(Long modelId,
                                               Long groupId,
                                               String billingType,
                                               BigDecimal promptPrice,
                                               BigDecimal cachedPromptPrice,
                                               BigDecimal completionPrice,
                                               BigDecimal requestPrice,
                                               BigDecimal multiplier) {
        AdminContext.requireAdmin();
        initializeDefaults();
        if (modelId == null) {
            throw new BusinessException(400, "模型不存在");
        }
        getGroupById(groupId);
        Integer exists = jdbcTemplate.queryForObject("""
                select count(*)
                from model_group_models
                where group_id = ? and model_id = ?
                """, Integer.class, groupId, modelId);
        if (exists != null && exists > 0) {
            if (billingType == null && promptPrice == null && cachedPromptPrice == null
                    && completionPrice == null && requestPrice == null && multiplier == null) {
                return;
            }
            updateModelGroupBindingPrice(modelId, groupId, billingType, promptPrice, cachedPromptPrice,
                    completionPrice, requestPrice, multiplier);
            return;
        }
        jdbcTemplate.update("""
                insert into model_group_models (
                    group_id, model_id, billing_type, prompt_price, cached_prompt_price,
                    completion_price, request_price, multiplier
                )
                select ?, m.id,
                       coalesce(?, m.billing_type),
                       coalesce(?, m.prompt_price),
                       coalesce(?, m.cached_prompt_price),
                       coalesce(?, m.completion_price),
                       coalesce(?, m.request_price),
                       coalesce(?, m.multiplier)
                from models m
                where m.id = ?
                """, groupId, billingType, promptPrice, cachedPromptPrice, completionPrice, requestPrice, multiplier, modelId);
    }

    @Transactional
    public void updateModelGroupBindingPrice(Long modelId,
                                             Long groupId,
                                             String billingType,
                                             BigDecimal promptPrice,
                                             BigDecimal cachedPromptPrice,
                                             BigDecimal completionPrice,
                                             BigDecimal requestPrice,
                                             BigDecimal multiplier) {
        AdminContext.requireAdmin();
        initializeDefaults();
        int updated = jdbcTemplate.update("""
                update model_group_models
                set billing_type = ?, prompt_price = ?, cached_prompt_price = ?,
                    completion_price = ?, request_price = ?, multiplier = ?
                where model_id = ? and group_id = ?
                """,
                billingType,
                promptPrice,
                cachedPromptPrice,
                completionPrice,
                requestPrice,
                multiplier,
                modelId,
                groupId);
        if (updated == 0) {
            addModelGroupBindingWithPrices(modelId, groupId, billingType, promptPrice, cachedPromptPrice,
                    completionPrice, requestPrice, multiplier);
        }
    }

    @Transactional
    public void deleteModelBindings(Long modelId) {
        AdminContext.requireAdmin();
        jdbcTemplate.update("delete from model_group_models where model_id = ?", modelId);
    }

    public List<ModelPackagePurchaseRecordResponse> listPurchaseRecords() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        String sql = """
                select p.id, p.user_id, u.username, p.group_id, g.group_code, g.group_name,
                       (
                           select count(*)
                           from model_group_models mgm
                           join models m on m.id = mgm.model_id
                           where mgm.group_id = g.id and m.deleted = 0 and m.status = 'ACTIVE'
                       ) as model_count,
                       p.purchase_price, p.start_at, p.expires_at, p.status, p.created_at,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join users u on u.id = p.user_id and u.deleted = 0
                join model_groups g on g.id = p.group_id
                where %s and p.status <> 'DELETED'
                order by p.id desc
                limit 100
                """.formatted(admin ? "1 = 1" : "p.user_id = ?");
        Object[] args = admin ? new Object[]{} : new Object[]{currentUser.userId()};
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long packageId = rs.getLong("id");
            LocalDateTime expiresAt = rs.getTimestamp("expires_at").toLocalDateTime();
            BigDecimal dailyQuota = rs.getBigDecimal("daily_quota");
            BigDecimal weeklyQuota = rs.getBigDecimal("weekly_quota");
            BigDecimal monthlyQuota = rs.getBigDecimal("monthly_quota");
            BigDecimal totalQuota = resolveTotalQuota(dailyQuota, monthlyQuota, rs.getInt("package_days"));
            return new ModelPackagePurchaseRecordResponse(
                    packageId,
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getLong("group_id"),
                    rs.getString("group_code"),
                    rs.getString("group_name"),
                    rs.getInt("model_count"),
                    rs.getBigDecimal("purchase_price"),
                    rs.getTimestamp("start_at").toLocalDateTime(),
                    expiresAt,
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    "ACTIVE".equalsIgnoreCase(rs.getString("status")) && expiresAt.isAfter(LocalDateTime.now()),
                    dailyQuota,
                    weeklyQuota,
                    monthlyQuota,
                    totalQuota,
                    calculatePackageUsageAmount(packageId, LocalDate.now(), LocalDate.now()),
                    calculatePackageUsageAmount(packageId, LocalDate.now().minusDays(6), LocalDate.now()),
                    calculatePackageUsageAmount(packageId, LocalDate.now().withDayOfMonth(1), LocalDate.now()),
                    calculatePackageTotalUsageAmount(packageId),
                    Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt))
            );
        }, args);
    }

    public List<WalletTransactionItemResponse> listWalletTransactions() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        String sql = """
                select t.id, t.user_id, u.username, t.wallet_id, t.order_no, t.transaction_type,
                       t.direction, t.amount, t.balance_before, t.balance_after, t.status,
                       t.description_text, t.transaction_date, t.created_at
                from transactions t
                join users u on u.id = t.user_id and u.deleted = 0
                where %s
                order by t.id desc
                limit 200
                """.formatted(admin ? "1 = 1" : "t.user_id = ?");
        Object[] args = admin ? new Object[]{} : new Object[]{currentUser.userId()};
        return jdbcTemplate.query(sql, (rs, rowNum) -> new WalletTransactionItemResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getLong("wallet_id"),
                rs.getString("order_no"),
                rs.getString("transaction_type"),
                rs.getString("direction"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_before"),
                rs.getBigDecimal("balance_after"),
                rs.getString("status"),
                rs.getString("description_text"),
                rs.getDate("transaction_date").toLocalDate(),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), args);
    }

    private ModelAccessSummaryResponse buildSummary(Long userId) {
        UserPolicyRow policy = getUserPolicy(userId);
        List<ModelGroupOptionResponse> groups = listGroups(userId);

        ModelGroupOptionResponse activeGroup = groups.stream()
                .filter(ModelGroupOptionResponse::active)
                .findFirst()
                .orElse(null);
        ModelGroupOptionResponse referenceGroup = activeGroup;
        if (referenceGroup == null) {
            referenceGroup = groups.stream()
                    .filter(ModelGroupOptionResponse::purchased)
                    .findFirst()
                    .orElse(groups.isEmpty() ? null : groups.get(0));
        }

        String status;
        String statusText;
        if (activeGroup != null) {
            status = "ACTIVE";
            statusText = "使用中";
        } else if (groups.stream().anyMatch(ModelGroupOptionResponse::purchased)) {
            status = "EXPIRED";
            statusText = "套餐过期";
        } else {
            status = "NOT_PURCHASED";
            statusText = "未购买套餐";
        }

        return new ModelAccessSummaryResponse(
                policy.packageRestrictionEnabled(),
                status,
                statusText,
                activeGroup == null ? null : activeGroup.id(),
                activeGroup == null ? null : activeGroup.groupCode(),
                activeGroup == null ? referenceGroup == null ? null : referenceGroup.groupName() : activeGroup.groupName(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.salePrice(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.dailyQuota(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.weeklyQuota(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.monthlyQuota(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.dailyUsed(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.weeklyUsed(),
                referenceGroup == null ? BigDecimal.ZERO : referenceGroup.monthlyUsed(),
                referenceGroup == null ? null : referenceGroup.expiresAt(),
                referenceGroup == null ? null : referenceGroup.remainingDays(),
                groups
        );
    }

    private List<ModelGroupOptionResponse> listGroups(Long userId) {
        List<ModelGroupRow> groups = jdbcTemplate.query("""
                select g.id, g.group_code, g.group_name, g.sale_price, g.package_days,
                       g.daily_quota, g.weekly_quota, g.monthly_quota, g.remark,
                       (
                           select count(*)
                           from model_group_models mgm
                           join models m on m.id = mgm.model_id
                           where mgm.group_id = g.id and m.deleted = 0 and m.status = 'ACTIVE'
                       ) as model_count
                from model_groups g
                where g.status = 'ACTIVE'
                order by field(g.group_code, 'claude', 'codex', 'gpt'), g.id asc
                """, (rs, rowNum) -> new ModelGroupRow(
                rs.getLong("id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getBigDecimal("sale_price"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota"),
                rs.getInt("model_count"),
                rs.getString("remark")
        ));

        List<ModelGroupOptionResponse> result = new ArrayList<>();
        for (ModelGroupRow group : groups) {
            UserPackageRow latestPackage = findLatestPackage(userId, group.id());
            boolean purchased = latestPackage != null;
            boolean active = purchased && latestPackage.active();
            BigDecimal dailyUsed = purchased ? calculateUsageAmount(userId, group.id(), LocalDate.now(), LocalDate.now()) : BigDecimal.ZERO;
            BigDecimal weeklyUsed = purchased ? calculateUsageAmount(userId, group.id(), LocalDate.now().minusDays(6), LocalDate.now()) : BigDecimal.ZERO;
            BigDecimal monthlyUsed = purchased ? calculateUsageAmount(userId, group.id(), LocalDate.now().withDayOfMonth(1), LocalDate.now()) : BigDecimal.ZERO;
            Long remainingDays = latestPackage == null || latestPackage.expiresAt() == null
                    ? null
                    : Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), latestPackage.expiresAt()));
            String packageStatus = active ? "ACTIVE" : purchased ? "EXPIRED" : "NOT_PURCHASED";
            String packageStatusText = active ? "使用中" : purchased ? "套餐过期" : "未购买套餐";

            result.add(new ModelGroupOptionResponse(
                    group.id(),
                    group.groupCode(),
                    group.groupName(),
                    group.salePrice(),
                    group.packageDays(),
                    group.dailyQuota(),
                    group.weeklyQuota(),
                    group.monthlyQuota(),
                    group.modelCount(),
                    purchased,
                    active,
                    latestPackage == null ? null : latestPackage.expiresAt(),
                    remainingDays,
                    dailyUsed,
                    weeklyUsed,
                    monthlyUsed,
                    packageStatus,
                    packageStatusText,
                    group.remark(),
                    isSystemPreset(group.groupCode())
            ));
        }
        return result;
    }

    private UserPackageRow findActivePackage(Long userId, Long groupId) {
        return jdbcTemplate.query("""
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.user_id = ? and p.group_id = ? and p.status = 'ACTIVE' and p.expires_at > now()
                order by p.expires_at desc, p.id desc
                limit 1
                """, rs -> rs.next() ? new UserPackageRow(
                rs.getLong("id"),
                rs.getLong("group_id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, userId, groupId);
    }

    private UserPackageRow findPackageById(Long userId, Long packageId) {
        if (packageId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.id = ? and p.user_id = ? and p.status <> 'DELETED'
                limit 1
                """, rs -> rs.next() ? new UserPackageRow(
                rs.getLong("id"),
                rs.getLong("group_id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, packageId, userId);
    }

    private UserPackageRow findFirstActivePackage(Long userId, Long groupId) {
        return jdbcTemplate.query("""
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.user_id = ? and p.group_id = ? and p.status = 'ACTIVE' and p.expires_at > now()
                order by p.id asc
                limit 1
                """, rs -> rs.next() ? new UserPackageRow(
                rs.getLong("id"),
                rs.getLong("group_id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, userId, groupId);
    }

    private UserPackageRow findLatestPackage(Long userId, Long groupId) {
        String sql = groupId == null
                ? """
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.user_id = ? and p.status <> 'DELETED'
                order by p.expires_at desc, p.id desc
                limit 1
                """
                : """
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.user_id = ? and p.group_id = ? and p.status <> 'DELETED'
                order by p.expires_at desc, p.id desc
                limit 1
                """;
        Object[] args = groupId == null ? new Object[]{userId} : new Object[]{userId, groupId};
        return jdbcTemplate.query(sql, rs -> rs.next() ? new UserPackageRow(
                rs.getLong("id"),
                rs.getLong("group_id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, args);
    }

    private BigDecimal calculateUsageAmount(Long userId, Long groupId, LocalDate startDate, LocalDate endDate) {
        BigDecimal amount = jdbcTemplate.queryForObject("""
                select coalesce(sum(l.user_amount), 0)
                from request_logs l
                join user_model_packages p on p.id = l.user_package_id
                where l.user_id = ?
                  and p.group_id = ?
                  and l.request_date between ? and ?
                """, BigDecimal.class, userId, groupId, startDate, endDate);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal calculatePackageUsageAmount(Long packageId, LocalDate startDate, LocalDate endDate) {
        if (packageId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = jdbcTemplate.queryForObject("""
                select coalesce(sum(user_amount), 0)
                from request_logs
                where user_package_id = ?
                  and request_date between ? and ?
                """, BigDecimal.class, packageId, startDate, endDate);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal calculatePackageTotalUsageAmount(Long packageId) {
        if (packageId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = jdbcTemplate.queryForObject("""
                select coalesce(sum(user_amount), 0)
                from request_logs
                where user_package_id = ?
                """, BigDecimal.class, packageId);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal resolveTotalQuota(BigDecimal dailyQuota, BigDecimal monthlyQuota, Integer packageDays) {
        if (monthlyQuota != null && monthlyQuota.compareTo(BigDecimal.ZERO) > 0) {
            return monthlyQuota;
        }
        if (dailyQuota == null || dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int days = packageDays == null || packageDays <= 0 ? DEFAULT_PACKAGE_DAYS : packageDays;
        return dailyQuota.multiply(BigDecimal.valueOf(days)).setScale(4, RoundingMode.HALF_UP);
    }

    private void validateQuotaLimits(String packageName,
                                     BigDecimal dailyQuota,
                                     BigDecimal dailyUsed,
                                     BigDecimal weeklyQuota,
                                     BigDecimal weeklyUsed,
                                     BigDecimal monthlyQuota,
                                     BigDecimal monthlyUsed,
                                     BigDecimal totalQuota,
                                     BigDecimal totalUsed) {
        String displayName = packageName == null || packageName.isBlank() ? "当前套餐" : packageName;
        if (isQuotaExceeded(dailyQuota, dailyUsed)) {
            throw new BusinessException(403,
                    "套餐额度不足：套餐【" + displayName + "】今日额度已用完，请明天再试或切换其他套餐");
        }
        if (isQuotaExceeded(weeklyQuota, weeklyUsed)) {
            throw new BusinessException(403,
                    "套餐额度不足：套餐【" + displayName + "】近7天额度已用完，请稍后再试或切换其他套餐");
        }
        if (isQuotaExceeded(monthlyQuota, monthlyUsed)) {
            throw new BusinessException(403,
                    "套餐额度不足：套餐【" + displayName + "】本月额度已用完，请下月再试或切换其他套餐");
        }
        if (isQuotaExceeded(totalQuota, totalUsed)) {
            throw new BusinessException(403,
                    "套餐额度不足：套餐【" + displayName + "】总额度已用完，请购买新套餐后继续使用");
        }
    }

    private boolean isQuotaExceeded(BigDecimal quota, BigDecimal used) {
        return quota != null
                && quota.compareTo(BigDecimal.ZERO) > 0
                && used != null
                && used.compareTo(quota) >= 0;
    }

    private UserPackageRow resolveGatewayPackage(ApiKeyAuthService.AuthenticatedApiKey auth,
                                                 GatewayRouteService.RouteDefinition route) {
        if (auth == null) {
            return null;
        }
        Long routeModelId = route == null ? null : route.modelId();
        if (auth.userPackageId() != null) {
            UserPackageRow boundPackage = findPackageById(auth.userId(), auth.userPackageId());
            if (boundPackage != null && isModelInGroup(boundPackage.groupId(), routeModelId)) {
                return boundPackage;
            }
            return null;
        }
        if (auth.modelGroupId() != null && isModelInGroup(auth.modelGroupId(), routeModelId)) {
            UserPackageRow boundGroupPackage = findFirstActivePackage(auth.userId(), auth.modelGroupId());
            if (boundGroupPackage != null) {
                return boundGroupPackage;
            }
        }
        return null;
    }

    private boolean isModelInGroup(Long groupId, Long modelId) {
        if (groupId == null || modelId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from model_group_models
                where group_id = ? and model_id = ?
                """, Integer.class, groupId, modelId);
        return count != null && count > 0;
    }

    private UserPackageRow findLatestActivePackageByModel(Long userId, Long modelId) {
        if (userId == null || modelId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.package_days, g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                join model_group_models mgm on mgm.group_id = g.id
                where p.user_id = ?
                  and p.status = 'ACTIVE'
                  and p.expires_at > now()
                  and mgm.model_id = ?
                order by p.expires_at desc, p.id desc
                limit 1
                """, rs -> rs.next() ? new UserPackageRow(
                rs.getLong("id"),
                rs.getLong("group_id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, userId, modelId);
    }

    private WalletRow getWallet(Long userId) {
        WalletRow wallet = jdbcTemplate.query("""
                select id, balance
                from wallets
                where user_id = ?
                limit 1
                """, rs -> rs.next() ? new WalletRow(
                rs.getLong("id"),
                rs.getBigDecimal("balance")
        ) : null, userId);
        if (wallet == null) {
            throw new BusinessException(400, "钱包不存在");
        }
        return wallet;
    }

    private UserPolicyRow getUserPolicy(Long userId) {
        UserPolicyRow policy = jdbcTemplate.query("""
                select role_code, package_restriction_enabled
                from users
                where id = ? and deleted = 0
                limit 1
                """, rs -> rs.next() ? new UserPolicyRow(
                rs.getString("role_code"),
                rs.getInt("package_restriction_enabled") == 1
        ) : null, userId);
        if (policy == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return policy;
    }

    private ModelGroupRow getGroupById(Long groupId) {
        ModelGroupRow group = jdbcTemplate.query("""
                select g.id, g.group_code, g.group_name, g.sale_price, g.package_days,
                       g.daily_quota, g.weekly_quota, g.monthly_quota, g.remark,
                       (
                           select count(*)
                           from model_group_models mgm
                           join models m on m.id = mgm.model_id
                           where mgm.group_id = g.id and m.deleted = 0 and m.status = 'ACTIVE'
                       ) as model_count
                from model_groups g
                where g.id = ? and g.status = 'ACTIVE'
                limit 1
                """, rs -> rs.next() ? new ModelGroupRow(
                rs.getLong("id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getBigDecimal("sale_price"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota"),
                rs.getInt("model_count"),
                rs.getString("remark")
        ) : null, groupId);
        if (group == null) {
            throw new BusinessException(404, "套餐分组不存在");
        }
        return group;
    }

    private ModelGroupRow getGroupByIdIncludingDisabled(Long groupId) {
        return jdbcTemplate.query("""
                select g.id, g.group_code, g.group_name, g.sale_price, g.package_days,
                       g.daily_quota, g.weekly_quota, g.monthly_quota, g.remark,
                       (
                           select count(*)
                           from model_group_models mgm
                           join models m on m.id = mgm.model_id
                           where mgm.group_id = g.id and m.deleted = 0 and m.status = 'ACTIVE'
                       ) as model_count
                from model_groups g
                where g.id = ?
                limit 1
                """, rs -> rs.next() ? new ModelGroupRow(
                rs.getLong("id"),
                rs.getString("group_code"),
                rs.getString("group_name"),
                rs.getBigDecimal("sale_price"),
                rs.getInt("package_days"),
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota"),
                rs.getInt("model_count"),
                rs.getString("remark")
        ) : null, groupId);
    }

    private void ensurePresetGroups() {
        for (PresetGroup preset : PRESET_GROUPS) {
            Long existingId = findGroupIdByCode(preset.groupCode());
            if (existingId != null) {
                jdbcTemplate.update("""
                        update model_groups
                        set group_name = ?, sale_price = ?, package_days = ?,
                            daily_quota = ?, weekly_quota = ?, monthly_quota = ?,
                            remark = ?, updated_at = now()
                        where id = ?
                        """,
                        preset.groupName(),
                        DEFAULT_PACKAGE_PRICE,
                        DEFAULT_PACKAGE_DAYS,
                        DEFAULT_DAILY_QUOTA,
                        DEFAULT_WEEKLY_QUOTA,
                        DEFAULT_MONTHLY_QUOTA,
                        preset.remark(),
                        existingId
                );
                continue;
            }

            jdbcTemplate.update("""
                    insert into model_groups (group_code, group_name, sale_price, package_days, daily_quota, weekly_quota, monthly_quota, status, remark)
                    values (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                    """,
                    preset.groupCode(),
                    preset.groupName(),
                    DEFAULT_PACKAGE_PRICE,
                    DEFAULT_PACKAGE_DAYS,
                    DEFAULT_DAILY_QUOTA,
                    DEFAULT_WEEKLY_QUOTA,
                    DEFAULT_MONTHLY_QUOTA,
                    preset.remark()
            );

        }
    }

    private void backfillModelGroupPrices() {
        jdbcTemplate.update("""
                update model_group_models mgm
                join models m on m.id = mgm.model_id
                set mgm.billing_type = coalesce(mgm.billing_type, m.billing_type),
                    mgm.prompt_price = coalesce(mgm.prompt_price, m.prompt_price),
                    mgm.cached_prompt_price = coalesce(mgm.cached_prompt_price, m.cached_prompt_price),
                    mgm.completion_price = coalesce(mgm.completion_price, m.completion_price),
                    mgm.request_price = coalesce(mgm.request_price, m.request_price),
                    mgm.multiplier = coalesce(mgm.multiplier, m.multiplier)
                where mgm.billing_type is null
                   or mgm.prompt_price is null
                   or mgm.cached_prompt_price is null
                   or mgm.completion_price is null
                   or mgm.request_price is null
                   or mgm.multiplier is null
                """);
    }

    private Long findGroupIdByCode(String groupCode) {
        return jdbcTemplate.query("""
                select id
                from model_groups
                where group_code = ?
                limit 1
                """, rs -> rs.next() ? rs.getLong("id") : null, groupCode);
    }

    private void expirePackages() {
        jdbcTemplate.update("""
                update user_model_packages
                set status = 'EXPIRED', updated_at = now()
                where status = 'ACTIVE' and expires_at <= now()
                """);
    }

    private void ensureTableExists(String tableName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """, Integer.class, tableName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureColumnExists(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureIndexExists(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = ?
                  and index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private String buildOrderNo(String prefix) {
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String normalizeGroupCode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? "" : trimToLength(normalized, 64);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private BigDecimal numberOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isSystemPreset(String groupCode) {
        return PRESET_GROUPS.stream().anyMatch(item -> item.groupCode().equalsIgnoreCase(groupCode));
    }

    public record ApiKeyPackageBinding(
            Long packageId,
            Long modelGroupId,
            String packageName
    ) {
    }

    private record PresetGroup(
            String groupCode,
            String groupName,
            String remark
    ) {
    }

    private record UserPolicyRow(
            String roleCode,
            boolean packageRestrictionEnabled
    ) {
    }

    private record WalletRow(
            Long id,
            BigDecimal balance
    ) {
    }

    private record ModelGroupRow(
            Long id,
            String groupCode,
            String groupName,
            BigDecimal salePrice,
            Integer packageDays,
            BigDecimal dailyQuota,
            BigDecimal weeklyQuota,
            BigDecimal monthlyQuota,
            Integer modelCount,
            String remark
    ) {
    }

    private record UserPackageRow(
            Long id,
            Long groupId,
            String groupCode,
            String groupName,
            LocalDateTime expiresAt,
            String status,
            Integer packageDays,
            BigDecimal dailyQuota,
            BigDecimal weeklyQuota,
            BigDecimal monthlyQuota
    ) {
        private boolean active() {
            return "ACTIVE".equalsIgnoreCase(status) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
        }
    }
}
