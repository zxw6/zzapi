package com.zxw.modules.access.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.access.dto.ModelAccessSummaryResponse;
import com.zxw.modules.access.dto.ModelGroupCreateRequest;
import com.zxw.modules.access.dto.ModelGroupOptionResponse;
import com.zxw.modules.access.dto.ModelGroupUpdateRequest;
import com.zxw.modules.access.dto.ModelPackagePurchaseRecordResponse;
import com.zxw.modules.access.dto.PurchaseModelPackageRequest;
import com.zxw.modules.apikey.service.ApiKeyAuthService;
import com.zxw.modules.gateway.service.GatewayRouteService;
import com.zxw.modules.user.dto.WalletTransactionItemResponse;
import com.zxw.persistence.entity.ApiKeyEntity;
import com.zxw.persistence.entity.ModelGroupEntity;
import com.zxw.persistence.entity.ModelGroupModelEntity;
import com.zxw.persistence.entity.TransactionEntity;
import com.zxw.persistence.entity.UserEntity;
import com.zxw.persistence.entity.UserModelPackageEntity;
import com.zxw.persistence.entity.WalletEntity;
import com.zxw.persistence.mapper.ApiKeyMapper;
import com.zxw.persistence.mapper.ModelGroupMapper;
import com.zxw.persistence.mapper.ModelGroupModelMapper;
import com.zxw.persistence.mapper.TransactionMapper;
import com.zxw.persistence.mapper.UserMapper;
import com.zxw.persistence.mapper.UserModelAccessQueryMapper;
import com.zxw.persistence.mapper.UserModelPackageMapper;
import com.zxw.persistence.mapper.WalletMapper;
import com.zxw.persistence.model.ModelPackagePurchaseRecordView;
import com.zxw.persistence.model.UserModelAccessGroupView;
import com.zxw.persistence.model.UserModelAccessPackageView;
import com.zxw.persistence.model.WalletTransactionView;
import org.springframework.dao.DataAccessException;
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

    public static final String PACKAGE_TYPE_QUOTA = "QUOTA";
    public static final String PACKAGE_TYPE_BALANCE = "BALANCE";
    public static final BigDecimal BALANCE_PACKAGE_PURCHASE_MIN_BALANCE = new BigDecimal("1.00");
    public static final BigDecimal BALANCE_PACKAGE_CALL_MIN_BALANCE = new BigDecimal("0.50");

    private static final BigDecimal DEFAULT_PACKAGE_PRICE = new BigDecimal("70.0000");
    private static final int DEFAULT_PACKAGE_DAYS = 30;
    private static final BigDecimal DEFAULT_DAILY_QUOTA = new BigDecimal("60.0000");
    private static final BigDecimal DEFAULT_WEEKLY_QUOTA = new BigDecimal("420.0000");
    private static final BigDecimal DEFAULT_MONTHLY_QUOTA = new BigDecimal("1800.0000");

    private static final List<PresetGroup> PRESET_GROUPS = List.of(
            new PresetGroup("claude", "Claude 套餐", "QUOTA", "适合 Claude / Sonnet / Opus 系列"),
            new PresetGroup("codex", "Codex 套餐", "QUOTA", "适合 Codex 与编程模型"),
            new PresetGroup("gpt", "GPT 套餐", "QUOTA", "适合 GPT 系列模型")
    );

    private final UserModelAccessQueryMapper accessQueryMapper;
    private final UserModelPackageMapper userModelPackageMapper;
    private final ModelGroupMapper modelGroupMapper;
    private final ModelGroupModelMapper modelGroupModelMapper;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Object defaultInitializationLock = new Object();
    private volatile boolean defaultsInitialized;

    public UserModelAccessService(UserModelAccessQueryMapper accessQueryMapper,
                                  UserModelPackageMapper userModelPackageMapper,
                                  ModelGroupMapper modelGroupMapper,
                                  ModelGroupModelMapper modelGroupModelMapper,
                                  UserMapper userMapper,
                                  WalletMapper walletMapper,
                                  TransactionMapper transactionMapper,
                                  ApiKeyMapper apiKeyMapper,
                                  JdbcTemplate jdbcTemplate) {
        this.accessQueryMapper = accessQueryMapper;
        this.userModelPackageMapper = userModelPackageMapper;
        this.modelGroupMapper = modelGroupMapper;
        this.modelGroupModelMapper = modelGroupModelMapper;
        this.userMapper = userMapper;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
        this.apiKeyMapper = apiKeyMapper;
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
        expirePackages();
        ensurePresetGroups();
        ensureModelGroupPricingSchema();
        accessQueryMapper.refreshPackageRestrictionPolicy();
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

    public String resolveGatewayPackageType(ApiKeyAuthService.AuthenticatedApiKey auth,
                                            GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        UserPackageRow targetPackage = resolveGatewayPackage(auth, route);
        if (targetPackage != null) {
            return normalizePackageType(targetPackage.packageType());
        }
        return auth == null ? PACKAGE_TYPE_QUOTA : normalizePackageType(auth.packageType());
    }

    public Long resolveApiKeyModelGroupId(Long userId, Long requestedGroupId) {
        initializeDefaults();
        expirePackages();
        if (requestedGroupId == null) {
            throw new BusinessException(403, "请先购买套餐并在创建 API Key 时选择套餐");
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

        Long exists = modelGroupMapper.selectCount(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode));
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "套餐编码已存在");
        }

        ModelGroupEntity entity = new ModelGroupEntity();
        entity.setGroupCode(groupCode);
        entity.setGroupName(trimToLength(request.groupName(), 64));
        entity.setPackageType(normalizePackageType(request.packageType()));
        entity.setSalePrice(numberOrZero(request.salePrice()));
        entity.setPackageDays(request.packageDays() == null || request.packageDays() <= 0
                ? DEFAULT_PACKAGE_DAYS
                : request.packageDays());
        entity.setDailyQuota(resolveQuotaValue(entity.getPackageType(), request.dailyQuota()));
        entity.setWeeklyQuota(resolveQuotaValue(entity.getPackageType(), request.weeklyQuota()));
        entity.setMonthlyQuota(resolveQuotaValue(entity.getPackageType(), request.monthlyQuota()));
        entity.setStatus("ACTIVE");
        entity.setRemark(trimToLength(request.remark(), 255));
        modelGroupMapper.insert(entity);
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
        BigDecimal salePrice = numberOrZero(group.salePrice());
        String packageType = normalizePackageType(group.packageType());

        if (PACKAGE_TYPE_BALANCE.equals(packageType) && wallet.balance().compareTo(BALANCE_PACKAGE_PURCHASE_MIN_BALANCE) < 0) {
            throw new BusinessException(400, "当前余额不足 1 美元，无法购买该套餐，请先充值");
        }
        if (!PACKAGE_TYPE_BALANCE.equals(packageType) && wallet.balance().compareTo(salePrice) < 0) {
            throw new BusinessException(400, "余额不足，请先充值");
        }

        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime expiresAt = startAt.plusDays(group.packageDays());

        UserModelPackageEntity packageEntity = new UserModelPackageEntity();
        packageEntity.setUserId(userId);
        packageEntity.setGroupId(group.id());
        packageEntity.setPackageName(group.groupName());
        packageEntity.setPurchasePrice(salePrice);
        packageEntity.setStartAt(startAt);
        packageEntity.setExpiresAt(expiresAt);
        packageEntity.setStatus("ACTIVE");
        userModelPackageMapper.insert(packageEntity);

        if (PACKAGE_TYPE_BALANCE.equals(packageType)) {
            return buildSummary(userId);
        }

        BigDecimal balanceBefore = wallet.balance();
        BigDecimal balanceAfter = balanceBefore.subtract(salePrice);
        int updatedWallet = walletMapper.debitBalance(userId, salePrice);
        if (updatedWallet == 0) {
            throw new BusinessException(400, "余额不足，请先充值");
        }

        TransactionEntity transaction = new TransactionEntity();
        transaction.setUserId(userId);
        transaction.setWalletId(wallet.id());
        transaction.setOrderNo(buildOrderNo("P"));
        transaction.setTransactionType("PACKAGE_BUY");
        transaction.setDirection("OUT");
        transaction.setAmount(salePrice);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus("SUCCESS");
        transaction.setDescriptionText("购买 " + group.groupName());
        transaction.setTransactionDate(LocalDate.now());
        transactionMapper.insert(transaction);

        return buildSummary(userId);
    }

    @Transactional
    public void disableGroup(Long groupId) {
        AdminContext.requireAdmin();
        ModelGroupRow group = getGroupByIdIncludingDisabled(groupId);
        if (group == null) {
            throw new BusinessException(404, "套餐分组不存在");
        }

        ModelGroupEntity updateEntity = new ModelGroupEntity();
        updateEntity.setStatus("DISABLED");
        updateEntity.setUpdatedAt(LocalDateTime.now());
        int updated = modelGroupMapper.update(updateEntity, Wrappers.<ModelGroupEntity>lambdaUpdate()
                .eq(ModelGroupEntity::getId, groupId));
        if (updated == 0) {
            throw new BusinessException(400, "套餐分组删除失败");
        }
    }

    @Transactional
    public void disablePurchasedPackage(Long packageId) {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();

        UserModelPackageEntity updatePackage = new UserModelPackageEntity();
        updatePackage.setStatus("DELETED");
        updatePackage.setUpdatedAt(LocalDateTime.now());
        var packageUpdate = Wrappers.<UserModelPackageEntity>lambdaUpdate()
                .eq(UserModelPackageEntity::getId, packageId)
                .ne(UserModelPackageEntity::getStatus, "DELETED");
        if (!admin) {
            packageUpdate.eq(UserModelPackageEntity::getUserId, currentUser.userId());
        }
        int updated = userModelPackageMapper.update(updatePackage, packageUpdate);
        if (updated == 0) {
            throw new BusinessException(404, "Purchased package does not exist");
        }

        ApiKeyEntity updateApiKey = new ApiKeyEntity();
        updateApiKey.setStatus("DISABLED");
        updateApiKey.setUpdatedAt(LocalDateTime.now());
        var apiKeyUpdate = Wrappers.<ApiKeyEntity>lambdaUpdate()
                .eq(ApiKeyEntity::getUserPackageId, packageId)
                .eq(ApiKeyEntity::getDeleted, 0);
        if (!admin) {
            apiKeyUpdate.eq(ApiKeyEntity::getUserId, currentUser.userId());
        }
        apiKeyMapper.update(updateApiKey, apiKeyUpdate);
    }

    public void validateApiKeyCreationAccess(Long userId, Long modelGroupId) {
        initializeDefaults();
        expirePackages();
        if (modelGroupId == null) {
            throw new BusinessException(403, "请选择已购买且有效的套餐");
        }

        getGroupById(modelGroupId);
        UserPackageRow latestPackage = findLatestPackage(userId, modelGroupId);
        if (latestPackage == null) {
            throw new BusinessException(403, "所选套餐尚未购买");
        }
        if (!latestPackage.active()) {
            throw new BusinessException(403, "套餐已过期");
        }
    }

    public void validateGatewayPackageAccess(ApiKeyAuthService.AuthenticatedApiKey auth,
                                             GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        if (auth == null || route == null) {
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

        if (PACKAGE_TYPE_BALANCE.equals(normalizePackageType(targetPackage.packageType()))) {
            BigDecimal currentBalance = auth.balance() == null ? BigDecimal.ZERO : auth.balance();
            if (currentBalance.compareTo(BALANCE_PACKAGE_CALL_MIN_BALANCE) < 0) {
                throw new BusinessException(403, "当前余额不足，请先充值");
            }
            return;
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

    public List<ModelPackagePurchaseRecordResponse> listPurchaseRecords() {
        initializeDefaults();
        expirePackages();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        List<ModelPackagePurchaseRecordView> views = admin
                ? accessQueryMapper.selectPurchaseRecordsAdmin(100)
                : accessQueryMapper.selectPurchaseRecordsUser(currentUser.userId(), 100);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        return views.stream()
                .map(item -> {
                    Long packageId = item.getId();
                    LocalDateTime expiresAt = item.getExpiresAt();
                    BigDecimal dailyQuota = numberOrZero(item.getDailyQuota());
                    BigDecimal weeklyQuota = numberOrZero(item.getWeeklyQuota());
                    BigDecimal monthlyQuota = numberOrZero(item.getMonthlyQuota());
                    BigDecimal totalQuota = resolveTotalQuota(dailyQuota, monthlyQuota, item.getPackageDays());
                    boolean active = "ACTIVE".equalsIgnoreCase(item.getStatus())
                            && expiresAt != null
                            && expiresAt.isAfter(now);
                    return new ModelPackagePurchaseRecordResponse(
                            item.getId(),
                            item.getUserId(),
                            item.getUsername(),
                            item.getGroupId(),
                            item.getGroupCode(),
                            item.getGroupName(),
                            normalizePackageType(item.getPackageType()),
                            item.getModelCount(),
                            item.getPurchasePrice(),
                            item.getStartAt(),
                            item.getExpiresAt(),
                            item.getStatus(),
                            item.getCreatedAt(),
                            active,
                            dailyQuota,
                            weeklyQuota,
                            monthlyQuota,
                            totalQuota,
                            calculatePackageUsageAmount(packageId, today, today),
                            calculatePackageUsageAmount(packageId, today.minusDays(6), today),
                            calculatePackageUsageAmount(packageId, today.withDayOfMonth(1), today),
                            calculatePackageTotalUsageAmount(packageId),
                            expiresAt == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(now, expiresAt))
                    );
                })
                .toList();
    }

    public List<WalletTransactionItemResponse> listWalletTransactions() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        List<WalletTransactionView> views = admin
                ? accessQueryMapper.selectWalletTransactionsAdmin(100)
                : accessQueryMapper.selectWalletTransactionsUser(currentUser.userId(), 100);
        return views.stream()
                .map(item -> new WalletTransactionItemResponse(
                        item.getId(),
                        item.getUserId(),
                        item.getUsername(),
                        item.getWalletId(),
                        item.getOrderNo(),
                        item.getTransactionType(),
                        item.getDirection(),
                        item.getAmount(),
                        item.getBalanceBefore(),
                        item.getBalanceAfter(),
                        item.getStatus(),
                        item.getDescriptionText(),
                        item.getTransactionDate(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    public void addModelGroupBinding(Long modelId, Long groupId) {
        addModelGroupBindingWithPrices(modelId, groupId, null, null, null, null, null, null);
    }

    public void addModelGroupBindingWithPrices(Long modelId,
                                               Long groupId,
                                               String billingType,
                                               BigDecimal promptPrice,
                                               BigDecimal cachedPromptPrice,
                                               BigDecimal completionPrice,
                                               BigDecimal requestPrice,
                                               BigDecimal multiplier) {
        if (modelId == null || groupId == null) {
            return;
        }
        accessQueryMapper.insertBindingFromModel(
                groupId,
                modelId,
                billingType,
                promptPrice,
                cachedPromptPrice,
                completionPrice,
                requestPrice,
                multiplier
        );
    }

    public void clearModelGroupBindingPrices(Long modelId) {
        if (modelId == null) {
            return;
        }
        accessQueryMapper.clearBindingPricingByModelId(modelId);
    }

    public void deleteModelBindings(Long modelId) {
        if (modelId == null) {
            return;
        }
        modelGroupModelMapper.delete(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getModelId, modelId));
    }

    private ModelAccessSummaryResponse buildSummary(Long userId) {
        ModelGroupRow activeGroup = null;
        List<ModelGroupOptionResponse> groups = listGroups(userId);
        for (ModelGroupOptionResponse item : groups) {
            if (item.active()) {
                activeGroup = new ModelGroupRow(
                        item.id(),
                        item.groupCode(),
                        item.groupName(),
                        item.packageType(),
                        numberOrZero(item.salePrice()),
                        item.packageDays(),
                        numberOrZero(item.dailyQuota()),
                        numberOrZero(item.weeklyQuota()),
                        numberOrZero(item.monthlyQuota()),
                        item.modelCount(),
                        item.remark()
                );
                break;
            }
        }
        ModelGroupOptionResponse referenceGroup = groups.stream().filter(ModelGroupOptionResponse::active).findFirst().orElse(null);
        String status;
        String statusText;
        if (referenceGroup != null && referenceGroup.active()) {
            status = "ACTIVE";
            statusText = "使用中";
        } else if (groups.stream().anyMatch(ModelGroupOptionResponse::purchased)) {
            status = "EXPIRED";
            statusText = "套餐已过期";
        } else {
            status = "NOT_PURCHASED";
            statusText = "未购买套餐";
        }

        return new ModelAccessSummaryResponse(
                true,
                status,
                statusText,
                activeGroup == null ? null : activeGroup.id(),
                activeGroup == null ? null : activeGroup.groupCode(),
                activeGroup == null ? null : activeGroup.groupName(),
                activeGroup == null ? null : activeGroup.packageType(),
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
        List<UserModelAccessGroupView> groups = accessQueryMapper.selectActiveGroups();
        List<ModelGroupOptionResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        for (UserModelAccessGroupView view : groups) {
            ModelGroupRow group = toGroupRow(view);
            UserPackageRow latestPackage = findLatestPackage(userId, group.id());
            boolean purchased = latestPackage != null;
            boolean active = purchased && latestPackage.active();
            BigDecimal dailyUsed = isQuotaPackage(group.packageType()) && purchased
                    ? calculateUsageAmount(userId, group.id(), today, today)
                    : BigDecimal.ZERO;
            BigDecimal weeklyUsed = isQuotaPackage(group.packageType()) && purchased
                    ? calculateUsageAmount(userId, group.id(), today.minusDays(6), today)
                    : BigDecimal.ZERO;
            BigDecimal monthlyUsed = isQuotaPackage(group.packageType()) && purchased
                    ? calculateUsageAmount(userId, group.id(), today.withDayOfMonth(1), today)
                    : BigDecimal.ZERO;
            Long remainingDays = latestPackage == null || latestPackage.expiresAt() == null
                    ? null
                    : Math.max(0, ChronoUnit.DAYS.between(now, latestPackage.expiresAt()));
            String packageStatus = active ? "ACTIVE" : purchased ? "EXPIRED" : "NOT_PURCHASED";
            String packageStatusText = active ? "使用中" : purchased ? "套餐已过期" : "未购买套餐";

            result.add(new ModelGroupOptionResponse(
                    group.id(),
                    group.groupCode(),
                    group.groupName(),
                    group.packageType(),
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

    private UserPackageRow findPackageById(Long userId, Long packageId) {
        if (packageId == null) {
            return null;
        }
        return toPackageRow(accessQueryMapper.selectPackageById(userId, packageId));
    }

    private UserPackageRow findFirstActivePackage(Long userId, Long groupId) {
        return toPackageRow(accessQueryMapper.selectFirstActivePackage(userId, groupId));
    }

    private UserPackageRow findLatestPackage(Long userId, Long groupId) {
        return groupId == null
                ? toPackageRow(accessQueryMapper.selectLatestPackageByUser(userId))
                : toPackageRow(accessQueryMapper.selectLatestPackage(userId, groupId));
    }

    private BigDecimal calculateUsageAmount(Long userId, Long groupId, LocalDate startDate, LocalDate endDate) {
        return numberOrZero(accessQueryMapper.sumUsageByUserAndGroup(userId, groupId, startDate, endDate));
    }

    private BigDecimal calculatePackageUsageAmount(Long packageId, LocalDate startDate, LocalDate endDate) {
        if (packageId == null) {
            return BigDecimal.ZERO;
        }
        return numberOrZero(accessQueryMapper.sumUsageByPackage(packageId, startDate, endDate));
    }

    private BigDecimal calculatePackageTotalUsageAmount(Long packageId) {
        if (packageId == null) {
            return BigDecimal.ZERO;
        }
        return numberOrZero(accessQueryMapper.sumTotalUsageByPackage(packageId));
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
                    "套餐额度不足：套餐【" + displayName + "】近 7 天额度已用完，请稍后再试或切换其他套餐");
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
        Integer count = accessQueryMapper.countModelInGroup(groupId, modelId);
        return count != null && count > 0;
    }

    private WalletRow getWallet(Long userId) {
        WalletEntity wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(400, "钱包不存在");
        }
        return new WalletRow(wallet.getId(), numberOrZero(wallet.getBalance()));
    }

    private ModelGroupRow getGroupById(Long groupId) {
        ModelGroupRow group = toGroupRow(accessQueryMapper.selectActiveGroupById(groupId));
        if (group == null) {
            throw new BusinessException(404, "套餐分组不存在");
        }
        return group;
    }

    private ModelGroupRow getGroupByIdIncludingDisabled(Long groupId) {
        return toGroupRow(accessQueryMapper.selectGroupById(groupId));
    }

    @Transactional
    public ModelAccessSummaryResponse updateGroup(Long groupId, ModelGroupUpdateRequest request) {
        AdminContext.requireAdmin();
        initializeDefaults();

        ModelGroupRow existingGroup = getGroupByIdIncludingDisabled(groupId);
        if (existingGroup == null) {
            throw new BusinessException(404, "套餐分组不存在");
        }

        String groupCode = normalizeGroupCode(request.groupCode());
        if (groupCode.isBlank()) {
            throw new BusinessException(400, "套餐编码不能为空");
        }
        if (isSystemPreset(existingGroup.groupCode()) && !existingGroup.groupCode().equalsIgnoreCase(groupCode)) {
            throw new BusinessException(400, "系统预置套餐编码不允许修改");
        }

        Long exists = modelGroupMapper.selectCount(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode)
                .ne(ModelGroupEntity::getId, groupId));
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "套餐编码已存在");
        }

        ModelGroupEntity updateEntity = new ModelGroupEntity();
        updateEntity.setGroupCode(groupCode);
        updateEntity.setGroupName(trimToLength(request.groupName(), 64));
        String packageType = normalizePackageType(request.packageType());
        if (isSystemPreset(existingGroup.groupCode()) && !existingGroup.packageType().equals(packageType)) {
            throw new BusinessException(400, "系统预置套餐不允许修改套餐类型");
        }
        updateEntity.setPackageType(packageType);
        updateEntity.setSalePrice(numberOrZero(request.salePrice()));
        updateEntity.setPackageDays(request.packageDays() == null || request.packageDays() <= 0
                ? DEFAULT_PACKAGE_DAYS
                : request.packageDays());
        updateEntity.setDailyQuota(resolveQuotaValue(packageType, request.dailyQuota()));
        updateEntity.setWeeklyQuota(resolveQuotaValue(packageType, request.weeklyQuota()));
        updateEntity.setMonthlyQuota(resolveQuotaValue(packageType, request.monthlyQuota()));
        updateEntity.setRemark(trimToLength(request.remark(), 255));
        updateEntity.setUpdatedAt(LocalDateTime.now());

        int updated = modelGroupMapper.update(updateEntity, Wrappers.<ModelGroupEntity>lambdaUpdate()
                .eq(ModelGroupEntity::getId, groupId));
        if (updated == 0) {
            throw new BusinessException(400, "套餐修改失败");
        }
        return buildSummary(AdminContext.require().userId());
    }

    private void ensurePresetGroups() {
        for (PresetGroup preset : PRESET_GROUPS) {
            Long existingId = findGroupIdByCode(preset.groupCode());
            if (existingId != null) {
                ModelGroupEntity updateEntity = new ModelGroupEntity();
                updateEntity.setGroupName(preset.groupName());
                updateEntity.setPackageType(preset.packageType());
                updateEntity.setSalePrice(DEFAULT_PACKAGE_PRICE);
                updateEntity.setPackageDays(DEFAULT_PACKAGE_DAYS);
                updateEntity.setDailyQuota(DEFAULT_DAILY_QUOTA);
                updateEntity.setWeeklyQuota(DEFAULT_WEEKLY_QUOTA);
                updateEntity.setMonthlyQuota(DEFAULT_MONTHLY_QUOTA);
                updateEntity.setRemark(preset.remark());
                updateEntity.setUpdatedAt(LocalDateTime.now());
                modelGroupMapper.update(updateEntity, Wrappers.<ModelGroupEntity>lambdaUpdate()
                        .eq(ModelGroupEntity::getId, existingId));
                continue;
            }

            ModelGroupEntity entity = new ModelGroupEntity();
            entity.setGroupCode(preset.groupCode());
            entity.setGroupName(preset.groupName());
            entity.setPackageType(preset.packageType());
            entity.setSalePrice(DEFAULT_PACKAGE_PRICE);
            entity.setPackageDays(DEFAULT_PACKAGE_DAYS);
            entity.setDailyQuota(DEFAULT_DAILY_QUOTA);
            entity.setWeeklyQuota(DEFAULT_WEEKLY_QUOTA);
            entity.setMonthlyQuota(DEFAULT_MONTHLY_QUOTA);
            entity.setStatus("ACTIVE");
            entity.setRemark(preset.remark());
            modelGroupMapper.insert(entity);
        }
    }

    private void ensureModelGroupPricingSchema() {
        ensureColumn("models", "cached_prompt_price",
                "ALTER TABLE models ADD COLUMN cached_prompt_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000 AFTER prompt_price");
        ensureColumn("model_groups", "package_type",
                "ALTER TABLE model_groups ADD COLUMN package_type VARCHAR(32) NOT NULL DEFAULT 'QUOTA' AFTER group_name");
        ensureColumn("model_group_models", "billing_type",
                "ALTER TABLE model_group_models ADD COLUMN billing_type VARCHAR(32) NULL AFTER model_id");
        ensureColumn("model_group_models", "prompt_price",
                "ALTER TABLE model_group_models ADD COLUMN prompt_price DECIMAL(18, 6) NULL AFTER billing_type");
        ensureColumn("model_group_models", "cached_prompt_price",
                "ALTER TABLE model_group_models ADD COLUMN cached_prompt_price DECIMAL(18, 6) NULL AFTER prompt_price");
        ensureColumn("model_group_models", "completion_price",
                "ALTER TABLE model_group_models ADD COLUMN completion_price DECIMAL(18, 6) NULL AFTER cached_prompt_price");
        ensureColumn("model_group_models", "request_price",
                "ALTER TABLE model_group_models ADD COLUMN request_price DECIMAL(18, 6) NULL AFTER completion_price");
        ensureColumn("model_group_models", "multiplier",
                "ALTER TABLE model_group_models ADD COLUMN multiplier DECIMAL(18, 4) NULL AFTER request_price");
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        try {
            jdbcTemplate.execute(ddl);
        } catch (DataAccessException ex) {
            if (!columnExists(tableName, columnName)) {
                throw ex;
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private Long findGroupIdByCode(String groupCode) {
        ModelGroupEntity group = modelGroupMapper.selectOne(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode));
        return group == null ? null : group.getId();
    }

    private void expirePackages() {
        UserModelPackageEntity updateEntity = new UserModelPackageEntity();
        updateEntity.setStatus("EXPIRED");
        updateEntity.setUpdatedAt(LocalDateTime.now());
        userModelPackageMapper.update(updateEntity, Wrappers.<UserModelPackageEntity>lambdaUpdate()
                .eq(UserModelPackageEntity::getStatus, "ACTIVE")
                .le(UserModelPackageEntity::getExpiresAt, LocalDateTime.now()));
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

    private BigDecimal resolveQuotaValue(String packageType, BigDecimal value) {
        if (PACKAGE_TYPE_BALANCE.equals(normalizePackageType(packageType))) {
            return BigDecimal.ZERO;
        }
        return numberOrZero(value);
    }

    private boolean isSystemPreset(String groupCode) {
        return PRESET_GROUPS.stream().anyMatch(item -> item.groupCode().equalsIgnoreCase(groupCode));
    }

    private boolean isQuotaPackage(String packageType) {
        return PACKAGE_TYPE_QUOTA.equals(normalizePackageType(packageType));
    }

    private String normalizePackageType(String packageType) {
        if (packageType == null || packageType.isBlank()) {
            return PACKAGE_TYPE_QUOTA;
        }
        return PACKAGE_TYPE_BALANCE.equalsIgnoreCase(packageType) ? PACKAGE_TYPE_BALANCE : PACKAGE_TYPE_QUOTA;
    }

    private ModelGroupRow toGroupRow(UserModelAccessGroupView view) {
        if (view == null) {
            return null;
        }
        return new ModelGroupRow(
                view.getId(),
                view.getGroupCode(),
                view.getGroupName(),
                normalizePackageType(view.getPackageType()),
                numberOrZero(view.getSalePrice()),
                view.getPackageDays() == null || view.getPackageDays() <= 0 ? DEFAULT_PACKAGE_DAYS : view.getPackageDays(),
                numberOrZero(view.getDailyQuota()),
                numberOrZero(view.getWeeklyQuota()),
                numberOrZero(view.getMonthlyQuota()),
                view.getModelCount() == null ? 0 : view.getModelCount(),
                view.getRemark()
        );
    }

    private UserPackageRow toPackageRow(UserModelAccessPackageView view) {
        if (view == null) {
            return null;
        }
        return new UserPackageRow(
                view.getId(),
                view.getGroupId(),
                view.getGroupCode(),
                view.getGroupName(),
                normalizePackageType(view.getPackageType()),
                view.getExpiresAt(),
                view.getStatus(),
                view.getPackageDays() == null || view.getPackageDays() <= 0 ? DEFAULT_PACKAGE_DAYS : view.getPackageDays(),
                numberOrZero(view.getDailyQuota()),
                numberOrZero(view.getWeeklyQuota()),
                numberOrZero(view.getMonthlyQuota())
        );
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
            String packageType,
            String remark
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
            String packageType,
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
            String packageType,
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
