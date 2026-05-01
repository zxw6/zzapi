package com.zxw.modules.access.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.common.exception.BusinessException;
import com.zxw.common.security.AdminContext;
import com.zxw.common.security.JwtUser;
import com.zxw.modules.access.dto.ModelAccessSummaryResponse;
import com.zxw.modules.access.dto.ModelGroupCreateRequest;
import com.zxw.modules.access.dto.ModelGroupUpdateRequest;
import com.zxw.modules.access.dto.ModelGroupOptionResponse;
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
/**
 * 用户模型套餐访问服务。
 * 统一处理套餐初始化、购买、鉴权、配额校验、模型分组绑定以及购买记录汇总等逻辑。
 */
public class UserModelAccessService {

    // 系统内置套餐的默认价格与额度配置。
    private static final BigDecimal DEFAULT_PACKAGE_PRICE = new BigDecimal("70.0000");
    private static final int DEFAULT_PACKAGE_DAYS = 30;
    private static final BigDecimal DEFAULT_DAILY_QUOTA = new BigDecimal("60.0000");
    private static final BigDecimal DEFAULT_WEEKLY_QUOTA = new BigDecimal("420.0000");
    private static final BigDecimal DEFAULT_MONTHLY_QUOTA = new BigDecimal("1800.0000");

    // 初始化时自动补齐的预置套餐分组。
    private static final List<PresetGroup> PRESET_GROUPS = List.of(
            new PresetGroup("claude", "Claude 套餐", "适合 Claude / Sonnet / Opus 系列"),
            new PresetGroup("codex", "Codex 套餐", "适合 Codex 与编程模型"),
            new PresetGroup("gpt", "GPT 套餐", "适合 GPT 系列模型")
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

    /**
     * 初始化套餐默认数据。
     * 这里做了双重校验，避免应用运行过程中重复执行初始化逻辑。
     */
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

    /**
     * 真正执行一次性的默认数据初始化。
     */
    private void initializeDefaultsOnce() {
        // 先处理已过期套餐，避免初始化后查询状态不一致。
        expirePackages();
        // 确保内置套餐分组和价格字段结构始终存在。
        ensurePresetGroups();
        ensureModelGroupPricingSchema();
        backfillModelGroupPrices();
        accessQueryMapper.refreshPackageRestrictionPolicy();
    }

    /**
     * 获取当前登录用户的套餐总览信息。
     */
    public ModelAccessSummaryResponse getCurrentSummary() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        expirePackages();
        return buildSummary(currentUser.userId());
    }

    /**
     * 解析 API Key 绑定的具体套餐。
     * 可以按套餐 id 指定，也可以按套餐分组取用户最近一次购买的有效套餐。
     */
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

    /**
     * 根据网关请求与 API Key 信息，解析当前计费应归属到哪个套餐。
     */
    public Long resolveGatewayPackageId(ApiKeyAuthService.AuthenticatedApiKey auth,
                                        GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        UserPackageRow targetPackage = resolveGatewayPackage(auth, route);
        return targetPackage == null ? null : targetPackage.id();
    }

    /**
     * 根据网关请求与 API Key 信息，解析当前请求实际命中的套餐分组。
     */
    public Long resolveGatewayPackageGroupId(ApiKeyAuthService.AuthenticatedApiKey auth,
                                             GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        UserPackageRow targetPackage = resolveGatewayPackage(auth, route);
        return targetPackage == null ? auth == null ? null : auth.modelGroupId() : targetPackage.groupId();
    }

    /**
     * 校验创建 API Key 时传入的模型分组是否合法，并返回最终分组 id。
     */
    public Long resolveApiKeyModelGroupId(Long userId, Long requestedGroupId) {
        initializeDefaults();
        expirePackages();
        if (requestedGroupId == null) {
            throw new BusinessException(403, "请先购买套餐并在创建 API Key 时选择套餐");
        }
        validateApiKeyCreationAccess(userId, requestedGroupId);
        return requestedGroupId;
    }

    /**
     * 管理员新增一个套餐分组。
     */
    @Transactional
    public ModelAccessSummaryResponse createGroup(ModelGroupCreateRequest request) {
        AdminContext.requireAdmin();
        initializeDefaults();

        // 分组编码会被规范化，避免出现重复、大小写不一致等情况。
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
        entity.setSalePrice(numberOrZero(request.salePrice()));
        entity.setPackageDays(request.packageDays() == null || request.packageDays() <= 0
                ? DEFAULT_PACKAGE_DAYS
                : request.packageDays());
        entity.setDailyQuota(numberOrZero(request.dailyQuota()));
        entity.setWeeklyQuota(numberOrZero(request.weeklyQuota()));
        entity.setMonthlyQuota(numberOrZero(request.monthlyQuota()));
        entity.setStatus("ACTIVE");
        entity.setRemark(trimToLength(request.remark(), 255));
        modelGroupMapper.insert(entity);
        return buildSummary(AdminContext.require().userId());
    }

    /**
     * 当前用户购买一个模型套餐。
     */
    @Transactional
    public ModelAccessSummaryResponse purchase(PurchaseModelPackageRequest request) {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        expirePackages();

        Long userId = currentUser.userId();
        ModelGroupRow group = getGroupById(request.groupId());
        WalletRow wallet = getWallet(userId);
        BigDecimal salePrice = numberOrZero(group.salePrice());
        if (wallet.balance().compareTo(salePrice) < 0) {
            throw new BusinessException(400, "余额不足，请先充值");
        }

        // 先生成购买记录，再扣减钱包余额并写入交易流水。
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

        BigDecimal balanceBefore = wallet.balance();
        BigDecimal balanceAfter = balanceBefore.subtract(salePrice);
        int updatedWallet = walletMapper.debitBalance(userId, salePrice);
        if (updatedWallet == 0) {
            throw new BusinessException(400, "钱包不存在");
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

    /**
     * 管理员禁用某个套餐分组。
     */
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

    /**
     * 停用已购买的套餐，同时同步禁用关联的 API Key。
     */
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

    /**
     * 校验用户是否有权限基于某个套餐分组创建 API Key。
     */
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
            throw new BusinessException(403, "套餐已过期");
        }
    }

    /**
     * 校验网关请求是否允许访问目标模型。
     * 这个方法兼容旧逻辑：既支持按套餐分组校验，也支持按具体购买记录校验。
     */
    public void validateGatewayAccess(ApiKeyAuthService.AuthenticatedApiKey auth,
                                      GatewayRouteService.RouteDefinition route) {
        initializeDefaults();
        expirePackages();
        if (auth == null || route == null) {
            return;
        }
        if (auth.modelGroupId() == null) {
            throw new BusinessException(403, "当前 API Key 未绑定套餐，请重新创建");
        }

        UserPackageRow matchedPackage = resolveGatewayPackage(auth, route);
        // 命中具体套餐时，优先按照套餐购买记录做额度校验。
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

        // 回退到分组维度校验，兼容还未绑定具体购买记录的旧 API Key。
        if (!isModelInGroup(auth.modelGroupId(), route.modelId())) {
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
            throw new BusinessException(403, "套餐已过期");
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

    /**
     * 严格按照套餐购买记录校验网关访问权限。
     * 新逻辑优先使用这个方法，避免同一分组下多次购买时出现歧义。
     */
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

    /**
     * 直接替换模型所属套餐分组。
     */
    @Transactional
    public void replaceModelGroupBinding(Long modelId, Long groupId) {
        AdminContext.requireAdmin();
        initializeDefaults();
        if (modelId == null) {
            throw new BusinessException(400, "模型不存在");
        }
        getGroupById(groupId);
        // 一个模型只保留当前指定的套餐绑定关系。
        modelGroupModelMapper.delete(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getModelId, modelId));
        addModelGroupBinding(modelId, groupId);
    }

    /**
     * 给模型追加一个基础的套餐分组绑定。
     */
    @Transactional
    public void addModelGroupBinding(Long modelId, Long groupId) {
        addModelGroupBindingWithPrices(modelId, groupId, null, null, null, null, null, null);
    }

    /**
     * 给模型分组绑定单独配置计费价格。
     */
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
        Long exists = modelGroupModelMapper.selectCount(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getGroupId, groupId)
                .eq(ModelGroupModelEntity::getModelId, modelId));
        if (exists != null && exists > 0) {
            if (billingType == null && promptPrice == null && cachedPromptPrice == null
                    && completionPrice == null && requestPrice == null && multiplier == null) {
                return;
            }
            // 已存在绑定时，自动转成价格更新，避免重复插入。
            updateModelGroupBindingPrice(modelId, groupId, billingType, promptPrice, cachedPromptPrice,
                    completionPrice, requestPrice, multiplier);
            return;
        }
        accessQueryMapper.insertBindingFromModel(groupId, modelId, billingType, promptPrice, cachedPromptPrice,
                completionPrice, requestPrice, multiplier);
    }

    /**
     * 更新模型在某个套餐分组下的计费配置。
     */
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
        int updated = accessQueryMapper.updateBindingPricing(modelId, groupId, billingType, promptPrice,
                cachedPromptPrice, completionPrice, requestPrice, multiplier);
        if (updated == 0) {
            addModelGroupBindingWithPrices(modelId, groupId, billingType, promptPrice, cachedPromptPrice,
                    completionPrice, requestPrice, multiplier);
        }
    }

    /**
     * 清空模型的分组级别价格覆盖，回退为模型默认价格。
     */
    @Transactional
    public void clearModelGroupBindingPrices(Long modelId) {
        AdminContext.requireAdmin();
        initializeDefaults();
        if (modelId == null) {
            return;
        }
        accessQueryMapper.clearBindingPricingByModelId(modelId);
    }

    /**
     * 删除模型与所有套餐分组的绑定关系。
     */
    @Transactional
    public void deleteModelBindings(Long modelId) {
        AdminContext.requireAdmin();
        modelGroupModelMapper.delete(Wrappers.<ModelGroupModelEntity>lambdaQuery()
                .eq(ModelGroupModelEntity::getModelId, modelId));
    }

    /**
     * 查询套餐购买记录列表。
     * 管理员看全量，普通用户只看自己的记录。
     */
    public List<ModelPackagePurchaseRecordResponse> listPurchaseRecords() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        List<ModelPackagePurchaseRecordView> views = admin
                ? accessQueryMapper.selectPurchaseRecordsAdmin(100)
                : accessQueryMapper.selectPurchaseRecordsUser(currentUser.userId(), 100);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        return views.stream()
                .map(item -> {
                    // 这里把数据库视图结果补齐成前端展示所需的额度、有效期和使用量信息。
                    Long packageId = item.getId();
                    LocalDateTime expiresAt = item.getExpiresAt();
                    BigDecimal dailyQuota = numberOrZero(item.getDailyQuota());
                    BigDecimal weeklyQuota = numberOrZero(item.getWeeklyQuota());
                    BigDecimal monthlyQuota = numberOrZero(item.getMonthlyQuota());
                    BigDecimal totalQuota = resolveTotalQuota(dailyQuota, monthlyQuota, item.getPackageDays());
                    return new ModelPackagePurchaseRecordResponse(
                            packageId,
                            item.getUserId(),
                            item.getUsername(),
                            item.getGroupId(),
                            item.getGroupCode(),
                            item.getGroupName(),
                            item.getModelCount(),
                            numberOrZero(item.getPurchasePrice()),
                            item.getStartAt(),
                            expiresAt,
                            item.getStatus(),
                            item.getCreatedAt(),
                            "ACTIVE".equalsIgnoreCase(item.getStatus()) && expiresAt != null && expiresAt.isAfter(now),
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

    /**
     * 查询钱包交易流水。
     */
    public List<WalletTransactionItemResponse> listWalletTransactions() {
        initializeDefaults();
        JwtUser currentUser = AdminContext.require();
        boolean admin = AdminContext.isAdmin();
        List<WalletTransactionView> views = admin
                ? accessQueryMapper.selectWalletTransactionsAdmin(200)
                : accessQueryMapper.selectWalletTransactionsUser(currentUser.userId(), 200);
        return views.stream()
                .map(item -> new WalletTransactionItemResponse(
                        item.getId(),
                        item.getUserId(),
                        item.getUsername(),
                        item.getWalletId(),
                        item.getOrderNo(),
                        item.getTransactionType(),
                        item.getDirection(),
                        numberOrZero(item.getAmount()),
                        numberOrZero(item.getBalanceBefore()),
                        numberOrZero(item.getBalanceAfter()),
                        item.getStatus(),
                        item.getDescriptionText(),
                        item.getTransactionDate(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 组装当前用户的套餐总览信息。
     */
    private ModelAccessSummaryResponse buildSummary(Long userId) {
        UserPolicyRow policy = getUserPolicy(userId);
        List<ModelGroupOptionResponse> groups = listGroups(userId);

        // 优先取当前可用套餐作为主展示对象，没有可用套餐时再回退到已购买或第一个套餐。
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
            statusText = "套餐已过期";
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
                activeGroup == null ? null : activeGroup.groupName(),
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

    /**
     * 构造所有套餐分组的可选项列表，并附带当前用户的购买/额度状态。
     */
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
            // 每个分组都会把当前用户在日/周/月三个维度的消耗实时统计出来。
            BigDecimal dailyUsed = purchased ? calculateUsageAmount(userId, group.id(), today, today) : BigDecimal.ZERO;
            BigDecimal weeklyUsed = purchased ? calculateUsageAmount(userId, group.id(), today.minusDays(6), today) : BigDecimal.ZERO;
            BigDecimal monthlyUsed = purchased ? calculateUsageAmount(userId, group.id(), today.withDayOfMonth(1), today) : BigDecimal.ZERO;
            Long remainingDays = latestPackage == null || latestPackage.expiresAt() == null
                    ? null
                    : Math.max(0, ChronoUnit.DAYS.between(now, latestPackage.expiresAt()));
            String packageStatus = active ? "ACTIVE" : purchased ? "EXPIRED" : "NOT_PURCHASED";
            String packageStatusText = active ? "使用中" : purchased ? "套餐已过期" : "未购买套餐";

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
        return toPackageRow(accessQueryMapper.selectActivePackage(userId, groupId));
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

    /**
     * 为本次网关调用解析最合适的套餐记录。
     * 优先使用 API Key 显式绑定的购买记录，其次回退到分组下当前有效的套餐。
     */
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

    private UserPackageRow findLatestActivePackageByModel(Long userId, Long modelId) {
        if (userId == null || modelId == null) {
            return null;
        }
        return toPackageRow(accessQueryMapper.selectLatestActivePackageByModel(userId, modelId));
    }

    private WalletRow getWallet(Long userId) {
        WalletEntity wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(400, "钱包不存在");
        }
        return new WalletRow(wallet.getId(), numberOrZero(wallet.getBalance()));
    }

    private UserPolicyRow getUserPolicy(Long userId) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return new UserPolicyRow(user.getRoleCode(), user.getPackageRestrictionEnabled() != null && user.getPackageRestrictionEnabled() == 1);
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

    /**
     * 管理员修改一个套餐分组。
     */
    @Transactional
    public ModelAccessSummaryResponse updateGroup(Long groupId, ModelGroupUpdateRequest request) {
        AdminContext.requireAdmin();
        initializeDefaults();

        // 先确认套餐分组存在，避免更新不存在的数据。
        ModelGroupRow existingGroup = getGroupByIdIncludingDisabled(groupId);
        if (existingGroup == null) {
            throw new BusinessException(404, "套餐分组不存在");
        }

        String groupCode = normalizeGroupCode(request.groupCode());
        if (groupCode.isBlank()) {
            throw new BusinessException(400, "套餐编码不能为空");
        }

        // 系统预置套餐编码会参与自动初始化，禁止改成别的编码，避免系统再次补回默认分组。
        if (isSystemPreset(existingGroup.groupCode()) && !existingGroup.groupCode().equalsIgnoreCase(groupCode)) {
            throw new BusinessException(400, "系统预置套餐编码不允许修改");
        }

        Long exists = modelGroupMapper.selectCount(Wrappers.<ModelGroupEntity>lambdaQuery()
                .eq(ModelGroupEntity::getGroupCode, groupCode)
                .ne(ModelGroupEntity::getId, groupId));
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "套餐编码已存在");
        }

        // 这里只更新套餐模板本身，历史购买记录仍保留原有快照，便于后续核对。
        ModelGroupEntity updateEntity = new ModelGroupEntity();
        updateEntity.setGroupCode(groupCode);
        updateEntity.setGroupName(trimToLength(request.groupName(), 64));
        updateEntity.setSalePrice(numberOrZero(request.salePrice()));
        updateEntity.setPackageDays(request.packageDays() == null || request.packageDays() <= 0
                ? DEFAULT_PACKAGE_DAYS
                : request.packageDays());
        updateEntity.setDailyQuota(numberOrZero(request.dailyQuota()));
        updateEntity.setWeeklyQuota(numberOrZero(request.weeklyQuota()));
        updateEntity.setMonthlyQuota(numberOrZero(request.monthlyQuota()));
        updateEntity.setRemark(trimToLength(request.remark(), 255));
        updateEntity.setUpdatedAt(LocalDateTime.now());

        int updated = modelGroupMapper.update(updateEntity, Wrappers.<ModelGroupEntity>lambdaUpdate()
                .eq(ModelGroupEntity::getId, groupId));
        if (updated == 0) {
            throw new BusinessException(400, "套餐修改失败");
        }
        return buildSummary(AdminContext.require().userId());
    }

    /**
     * 初始化系统预置套餐。
     * 如果已存在则做补齐更新，避免历史数据缺字段或默认值不一致。
     */
    private void ensurePresetGroups() {
        for (PresetGroup preset : PRESET_GROUPS) {
            Long existingId = findGroupIdByCode(preset.groupCode());
            if (existingId != null) {
                ModelGroupEntity updateEntity = new ModelGroupEntity();
                updateEntity.setGroupName(preset.groupName());
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

    private void backfillModelGroupPrices() {
        accessQueryMapper.backfillModelGroupPrices();
    }

    /**
     * 确保模型分组价格扩展字段存在。
     * 这里做的是运行期兜底，避免旧库结构升级不完整时直接报错。
     */
    private void ensureModelGroupPricingSchema() {
        ensureColumn("models", "cached_prompt_price",
                "ALTER TABLE models ADD COLUMN cached_prompt_price DECIMAL(18, 6) NOT NULL DEFAULT 0.000000 AFTER prompt_price");
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

    /**
     * 把已到期但仍然是 ACTIVE 的套餐批量改为 EXPIRED。
     */
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

    private boolean isSystemPreset(String groupCode) {
        return PRESET_GROUPS.stream().anyMatch(item -> item.groupCode().equalsIgnoreCase(groupCode));
    }

    private ModelGroupRow toGroupRow(UserModelAccessGroupView view) {
        if (view == null) {
            return null;
        }
        return new ModelGroupRow(
                view.getId(),
                view.getGroupCode(),
                view.getGroupName(),
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
                view.getExpiresAt(),
                view.getStatus(),
                view.getPackageDays() == null || view.getPackageDays() <= 0 ? DEFAULT_PACKAGE_DAYS : view.getPackageDays(),
                numberOrZero(view.getDailyQuota()),
                numberOrZero(view.getWeeklyQuota()),
                numberOrZero(view.getMonthlyQuota())
        );
    }

    /**
     * API Key 绑定结果。
     */
    public record ApiKeyPackageBinding(
            Long packageId,
            Long modelGroupId,
            String packageName
    ) {
    }

    /**
     * 系统预置套餐定义。
     */
    private record PresetGroup(
            String groupCode,
            String groupName,
            String remark
    ) {
    }

    /**
     * 用户套餐策略快照。
     */
    private record UserPolicyRow(
            String roleCode,
            boolean packageRestrictionEnabled
    ) {
    }

    /**
     * 钱包余额快照。
     */
    private record WalletRow(
            Long id,
            BigDecimal balance
    ) {
    }

    /**
     * 套餐分组视图对象。
     */
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

    /**
     * 用户已购买套餐的简化视图。
     */
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
        // 只有状态为 ACTIVE 且还未过期，才视为真正可用的套餐。
        private boolean active() {
            return "ACTIVE".equalsIgnoreCase(status) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
        }
    }
}
