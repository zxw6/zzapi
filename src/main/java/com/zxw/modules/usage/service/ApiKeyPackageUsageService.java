package com.zxw.modules.usage.service;

import com.zxw.common.exception.BusinessException;
import com.zxw.modules.apikey.service.ApiKeyAuthService;
import com.zxw.modules.usage.dto.ApiKeyPackageUsageResponse;
import com.zxw.persistence.mapper.UserModelAccessQueryMapper;
import com.zxw.persistence.model.UserModelAccessPackageView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
public class ApiKeyPackageUsageService {

    private static final String PACKAGE_TYPE_BALANCE = "BALANCE";
    private static final String PACKAGE_TYPE_QUOTA = "QUOTA";
    private static final int DEFAULT_PACKAGE_DAYS = 30;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private final ApiKeyAuthService apiKeyAuthService;
    private final UserModelAccessQueryMapper accessQueryMapper;

    public ApiKeyPackageUsageService(ApiKeyAuthService apiKeyAuthService,
                                     UserModelAccessQueryMapper accessQueryMapper) {
        this.apiKeyAuthService = apiKeyAuthService;
        this.accessQueryMapper = accessQueryMapper;
    }

    public ApiKeyPackageUsageResponse getPackageUsage(String apiKey) {
        ApiKeyAuthService.AuthenticatedApiKey auth = apiKeyAuthService.authenticate(apiKey);
        UserModelAccessPackageView packageView = resolvePackage(auth);
        if (packageView == null) {
            throw new BusinessException(404, "API key has no available package binding");
        }

        LocalDate today = currentDate();
        BigDecimal dailyQuota = numberOrZero(packageView.getDailyQuota());
        BigDecimal weeklyQuota = numberOrZero(packageView.getWeeklyQuota());
        BigDecimal monthlyQuota = numberOrZero(packageView.getMonthlyQuota());
        BigDecimal totalQuota = resolveTotalQuota(dailyQuota, monthlyQuota, packageView.getPackageDays());
        BigDecimal dailyUsed = sumPackageUsage(packageView.getId(), today, today);
        BigDecimal weeklyUsed = sumPackageUsage(packageView.getId(), today.minusDays(6), today);
        BigDecimal monthlyUsed = sumPackageUsage(packageView.getId(), today.withDayOfMonth(1), today);
        BigDecimal totalUsed = numberOrZero(accessQueryMapper.sumTotalUsageByPackage(packageView.getId()));
        LocalDateTime now = currentDateTime();
        LocalDateTime expiresAt = packageView.getExpiresAt();

        return new ApiKeyPackageUsageResponse(
                packageView.getGroupName(),
                normalizePackageType(packageView.getPackageType()),
                packageView.getStatus(),
                today,
                expiresAt,
                expiresAt == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(now, expiresAt)),
                packageView.getPackageDays() == null || packageView.getPackageDays() <= 0
                        ? DEFAULT_PACKAGE_DAYS
                        : packageView.getPackageDays(),
                dailyQuota,
                dailyUsed,
                remaining(dailyQuota, dailyUsed),
                weeklyQuota,
                weeklyUsed,
                remaining(weeklyQuota, weeklyUsed),
                monthlyQuota,
                monthlyUsed,
                remaining(monthlyQuota, monthlyUsed),
                totalQuota,
                totalUsed,
                remaining(totalQuota, totalUsed)
        );
    }

    private UserModelAccessPackageView resolvePackage(ApiKeyAuthService.AuthenticatedApiKey auth) {
        if (auth.userPackageId() != null) {
            return accessQueryMapper.selectPackageById(auth.userId(), auth.userPackageId());
        }
        if (auth.modelGroupId() != null) {
            return accessQueryMapper.selectFirstActivePackage(auth.userId(), auth.modelGroupId());
        }
        return null;
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

    private BigDecimal remaining(BigDecimal quota, BigDecimal used) {
        if (quota == null || quota.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = quota.subtract(numberOrZero(used));
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal sumPackageUsage(Long packageId, LocalDate startDate, LocalDate endDate) {
        return numberOrZero(accessQueryMapper.sumUsageByPackage(packageId, startDate, endDate));
    }

    private BigDecimal numberOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate currentDate() {
        return LocalDate.now(APP_ZONE);
    }

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(APP_ZONE);
    }

    private String normalizePackageType(String packageType) {
        if (packageType == null || packageType.isBlank()) {
            return PACKAGE_TYPE_QUOTA;
        }
        return PACKAGE_TYPE_BALANCE.equalsIgnoreCase(packageType) ? PACKAGE_TYPE_BALANCE : PACKAGE_TYPE_QUOTA;
    }
}
