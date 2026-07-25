package com.zxw.persistence.mapper;

import com.zxw.persistence.model.ModelPackagePurchaseRecordView;
import com.zxw.persistence.model.PackageUsageSummaryView;
import com.zxw.persistence.model.UserModelAccessGroupView;
import com.zxw.persistence.model.UserModelAccessPackageView;
import com.zxw.persistence.model.WalletTransactionView;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户模型访问与套餐聚合查询 Mapper。
 */
public interface UserModelAccessQueryMapper {

    /**
     * 刷新套餐限制策略。
     */
    int refreshPackageRestrictionPolicy();

    /**
     * 查询当前所有启用中的分组。
     */
    List<UserModelAccessGroupView> selectActiveGroups();

    /**
     * 查询启用中的分组详情。
     */
    UserModelAccessGroupView selectActiveGroupById(@Param("groupId") Long groupId);

    /**
     * 查询分组详情，包含已停用记录。
     */
    UserModelAccessGroupView selectGroupById(@Param("groupId") Long groupId);

    /**
     * 查询用户在指定分组下当前有效的套餐。
     */
    UserModelAccessPackageView selectActivePackage(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 按套餐记录 ID 查询套餐。
     */
    UserModelAccessPackageView selectPackageById(@Param("userId") Long userId, @Param("packageId") Long packageId);

    /**
     * 查询最早生效的有效套餐。
     */
    UserModelAccessPackageView selectFirstActivePackage(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 查询最近一条套餐记录。
     */
    UserModelAccessPackageView selectLatestPackage(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 查询用户最近购买的套餐。
     */
    UserModelAccessPackageView selectLatestPackageByUser(@Param("userId") Long userId);

    /**
     * 统计用户在指定时间段和分组下的消费金额。
     */
    BigDecimal sumUsageByUserAndGroup(@Param("userId") Long userId,
                                      @Param("groupId") Long groupId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    BigDecimal sumTotalUsageByUserAndGroup(@Param("userId") Long userId,
                                           @Param("groupId") Long groupId);

    /**
     * 统计套餐在指定时间段内的消费金额。
     */
    BigDecimal sumUsageByPackage(@Param("packageId") Long packageId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    /**
     * 统计套餐累计总消费金额。
     */
    BigDecimal sumTotalUsageByPackage(@Param("packageId") Long packageId);

    PackageUsageSummaryView selectPackageUsageSummary(@Param("packageId") Long packageId,
                                                       @Param("today") LocalDate today,
                                                       @Param("weekStart") LocalDate weekStart,
                                                       @Param("monthStart") LocalDate monthStart);

    /**
     * 统计某个模型是否属于指定分组。
     */
    Integer countModelInGroup(@Param("groupId") Long groupId, @Param("modelId") Long modelId);

    /**
     * 按模型查询最近仍有效的套餐。
     */
    UserModelAccessPackageView selectLatestActivePackageByModel(@Param("userId") Long userId, @Param("modelId") Long modelId);

    /**
     * 复制模型默认价格生成分组绑定。
     */
    int insertBindingFromModel(@Param("groupId") Long groupId,
                               @Param("modelId") Long modelId,
                               @Param("billingType") String billingType,
                               @Param("promptPrice") BigDecimal promptPrice,
                               @Param("cachedPromptPrice") BigDecimal cachedPromptPrice,
                               @Param("cacheWritePromptPrice") BigDecimal cacheWritePromptPrice,
                               @Param("completionPrice") BigDecimal completionPrice,
                               @Param("requestPrice") BigDecimal requestPrice,
                               @Param("multiplier") BigDecimal multiplier);

    /**
     * 更新分组绑定价格配置。
     */
    int updateBindingPricing(@Param("modelId") Long modelId,
                             @Param("groupId") Long groupId,
                             @Param("billingType") String billingType,
                             @Param("promptPrice") BigDecimal promptPrice,
                             @Param("cachedPromptPrice") BigDecimal cachedPromptPrice,
                             @Param("cacheWritePromptPrice") BigDecimal cacheWritePromptPrice,
                             @Param("completionPrice") BigDecimal completionPrice,
                             @Param("requestPrice") BigDecimal requestPrice,
                             @Param("multiplier") BigDecimal multiplier);

    /**
     * 清空模型的分组价格配置。
     */
    int clearBindingPricingByModelId(@Param("modelId") Long modelId);

    /**
     * 回填历史分组价格。
     */
    int backfillModelGroupPrices();

    /**
     * 查询管理员视角的套餐购买记录。
     */
    List<ModelPackagePurchaseRecordView> selectPurchaseRecordsAdmin(@Param("limit") int limit);

    /**
     * 查询普通用户自己的套餐购买记录。
     */
    List<ModelPackagePurchaseRecordView> selectPurchaseRecordsUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询管理员视角的钱包流水。
     */
    List<WalletTransactionView> selectWalletTransactionsAdmin(@Param("limit") int limit);

    /**
     * 查询普通用户自己的钱包流水。
     */
    List<WalletTransactionView> selectWalletTransactionsUser(@Param("userId") Long userId, @Param("limit") int limit);
}
