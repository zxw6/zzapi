package com.zxw.modules.access.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.access.dto.ModelAccessSummaryResponse;
import com.zxw.modules.access.dto.ModelGroupOptionResponse;
import com.zxw.modules.access.dto.PurchaseModelPackageRequest;
import com.zxw.modules.apikey.service.ApiKeyAuthService;
import com.zxw.modules.gateway.service.GatewayRouteService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
            new PresetGroup("gpt", "GPT 套餐", "适合 GPT 系列模型")
    );

    private final JdbcTemplate jdbcTemplate;

    public UserModelAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void initializeDefaults() {
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
        ensureColumnExists("api_keys", "total_quota",
                "alter table api_keys add column total_quota decimal(18, 4) not null default 0.0000");
        ensureColumnExists("api_keys", "used_quota",
                "alter table api_keys add column used_quota decimal(18, 4) not null default 0.0000");
        ensureIndexExists("api_keys", "idx_api_keys_group",
                "create index idx_api_keys_group on api_keys (model_group_id)");

        expirePackages();
        ensurePresetGroups();
        syncAllPresetModels();
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

        UserPackageRow activePackage = findActivePackage(userId, group.id());
        if (activePackage != null) {
            throw new BusinessException(400, "当前分组套餐仍在有效期内");
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

    public void validateApiKeyCreationAccess(Long userId, Long modelGroupId) {
        initializeDefaults();
        expirePackages();
        if (modelGroupId == null) {
            throw new BusinessException(403, "请先选择已购买且有效的套餐");
        }

        ModelGroupRow group = getGroupById(modelGroupId);
        UserPackageRow latestPackage = findLatestPackage(userId, group.id());
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

        Integer inGroup = jdbcTemplate.queryForObject("""
                select count(*)
                from model_group_models
                where group_id = ? and model_id = ?
                """, Integer.class, auth.modelGroupId(), route.modelId());
        if (inGroup == null || inGroup == 0) {
            throw new BusinessException(403, "当前套餐分组不可使用该模型");
        }

        UserPackageRow latestPackage = findLatestPackage(auth.userId(), auth.modelGroupId());
        if (latestPackage == null) {
            throw new BusinessException(403, "当前套餐不存在");
        }
        if (!latestPackage.active()) {
            throw new BusinessException(403, "套餐过期");
        }
        if (latestPackage.dailyQuota().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal usedToday = calculateUsageAmount(auth.userId(), auth.modelGroupId(), LocalDate.now(), LocalDate.now());
        if (usedToday.compareTo(latestPackage.dailyQuota()) >= 0) {
            throw new BusinessException(403, "今日余额已用完，请明天再来");
        }
    }

    @Transactional
    public void syncPresetGroupsForModel(Long modelId, String modelCode, String upstreamModel) {
        if (modelId == null) {
            return;
        }
        for (PresetGroup preset : PRESET_GROUPS) {
            Long groupId = findGroupIdByCode(preset.groupCode());
            if (groupId == null) {
                continue;
            }
            boolean matches = matchesPreset(preset.groupCode(), modelCode, upstreamModel);
            if (matches) {
                jdbcTemplate.update("""
                        insert ignore into model_group_models (group_id, model_id)
                        values (?, ?)
                        """, groupId, modelId);
            } else {
                jdbcTemplate.update("""
                        delete from model_group_models
                        where group_id = ? and model_id = ?
                        """, groupId, modelId);
            }
        }
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
            String packageStatusText = active ? "使用中" : purchased ? "套餐过期" : "未购买";

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
                    group.remark()
            ));
        }
        return result;
    }

    private UserPackageRow findActivePackage(Long userId, Long groupId) {
        return jdbcTemplate.query("""
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.daily_quota, g.weekly_quota, g.monthly_quota
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
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, userId, groupId);
    }

    private UserPackageRow findLatestPackage(Long userId, Long groupId) {
        String sql = groupId == null
                ? """
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.user_id = ?
                order by p.expires_at desc, p.id desc
                limit 1
                """
                : """
                select p.id, p.group_id, g.group_code, g.group_name, p.expires_at, p.status,
                       g.daily_quota, g.weekly_quota, g.monthly_quota
                from user_model_packages p
                join model_groups g on g.id = p.group_id
                where p.user_id = ? and p.group_id = ?
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
                rs.getBigDecimal("daily_quota"),
                rs.getBigDecimal("weekly_quota"),
                rs.getBigDecimal("monthly_quota")
        ) : null, args);
    }

    private BigDecimal calculateUsageAmount(Long userId, Long groupId, LocalDate startDate, LocalDate endDate) {
        BigDecimal amount = jdbcTemplate.queryForObject("""
                select coalesce(sum(l.user_amount), 0)
                from request_logs l
                join models m on m.model_code = l.model_code and m.deleted = 0
                join model_group_models mgm on mgm.model_id = m.id
                where l.user_id = ?
                  and mgm.group_id = ?
                  and l.request_date between ? and ?
                """, BigDecimal.class, userId, groupId, startDate, endDate);
        return amount == null ? BigDecimal.ZERO : amount;
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

    private void syncAllPresetModels() {
        for (PresetGroup preset : PRESET_GROUPS) {
            Long groupId = findGroupIdByCode(preset.groupCode());
            if (groupId == null) {
                continue;
            }
            String matchSql = buildPresetMatchSql(preset.groupCode(), "m", "r");
            jdbcTemplate.update("""
                    insert ignore into model_group_models (group_id, model_id)
                    select distinct ?, m.id
                    from models m
                    left join model_routes r on r.model_id = m.id and r.status = 'ACTIVE'
                    where m.deleted = 0 and m.status = 'ACTIVE' and (%s)
                    """.formatted(matchSql), groupId);

            jdbcTemplate.update("""
                    delete mgm
                    from model_group_models mgm
                    join models m on m.id = mgm.model_id
                    left join model_routes r on r.model_id = m.id and r.status = 'ACTIVE'
                    where mgm.group_id = ?
                      and (m.deleted <> 0 or m.status <> 'ACTIVE' or not (%s))
                    """.formatted(matchSql), groupId);
        }
    }

    private Long findGroupIdByCode(String groupCode) {
        return jdbcTemplate.query("""
                select id
                from model_groups
                where group_code = ?
                limit 1
                """, rs -> rs.next() ? rs.getLong("id") : null, groupCode);
    }

    private String buildPresetMatchSql(String groupCode, String modelAlias, String routeAlias) {
        String modelCode = "lower(coalesce(%s.model_code, ''))".formatted(modelAlias);
        String modelName = "lower(coalesce(%s.model_name, ''))".formatted(modelAlias);
        String upstreamModel = "lower(coalesce(%s.upstream_model, ''))".formatted(routeAlias);
        return switch (groupCode) {
            case "claude" -> "%s like 'claude%%' or %s like 'claude%%' or %s like 'claude%%'".formatted(modelCode, modelName, upstreamModel);
            case "codex" -> "%s like 'codex%%' or %s like 'codex%%' or %s like 'codex%%' or %s like '%%codex%%' or %s like '%%codex%%' or %s like '%%codex%%'"
                    .formatted(modelCode, modelName, upstreamModel, modelCode, modelName, upstreamModel);
            case "gpt" -> "(%s like 'gpt%%' or %s like 'gpt%%' or %s like 'gpt%%') and %s not like '%%codex%%' and %s not like '%%codex%%' and %s not like '%%codex%%'"
                    .formatted(modelCode, modelName, upstreamModel, modelCode, modelName, upstreamModel);
            default -> "1 = 0";
        };
    }

    private boolean matchesPreset(String groupCode, String modelCode, String upstreamModel) {
        String normalizedModelCode = normalize(modelCode);
        String normalizedUpstreamModel = normalize(upstreamModel);
        return switch (groupCode) {
            case "claude" -> normalizedModelCode.startsWith("claude") || normalizedUpstreamModel.startsWith("claude");
            case "codex" -> normalizedModelCode.startsWith("codex")
                    || normalizedUpstreamModel.startsWith("codex")
                    || normalizedModelCode.contains("codex")
                    || normalizedUpstreamModel.contains("codex");
            case "gpt" -> (normalizedModelCode.startsWith("gpt") || normalizedUpstreamModel.startsWith("gpt"))
                    && !normalizedModelCode.contains("codex")
                    && !normalizedUpstreamModel.contains("codex");
            default -> false;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
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
            BigDecimal dailyQuota,
            BigDecimal weeklyQuota,
            BigDecimal monthlyQuota
    ) {
        private boolean active() {
            return "ACTIVE".equalsIgnoreCase(status) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
        }
    }
}
