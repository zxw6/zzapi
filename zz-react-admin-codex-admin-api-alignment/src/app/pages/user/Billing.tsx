import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { CheckCircle, Download, Package, TrendingUp, Zap } from "lucide-react";
import { toast } from "sonner";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  useBillingQuery,
  useDashboardModelStatsQuery,
  useModelAccessSummaryQuery,
  usePurchaseModelPackageMutation,
} from "../../api/queries";
import type { BillingResponse, ModelGroupOptionResponse } from "../../api/types";
import { isAppError } from "../../../lib/http/error";
import { useAuth } from "../../auth/auth-context";
import {
  ButtonLoadingContent,
  PageCardGridSkeleton,
  PageEmptyState,
  PageErrorState,
  PageHeaderSkeleton,
  PagePanelSkeleton,
} from "../../components/ui/feedback";

const modelColors = ["#6366f1", "#3b82f6", "#f97316", "#8b5cf6", "#94a3b8"];
const EMPTY_BILLING_RESPONSE: BillingResponse = {
  balance: 0,
  monthlySpend: 0,
  monthlyBudget: 0,
  usageData: [],
  modelUsage: [],
  billingHistory: [],
  rechargeAmounts: [],
};

function formatQuota(value: number | null | undefined) {
  if (value == null || value <= 0) {
    return "$0";
  }

  return `$${value.toLocaleString()}`;
}

function resolvePackageName(activeGroupName: string | null | undefined, packageStatusText: string) {
  return activeGroupName || packageStatusText || "未开通套餐";
}

function isBalancePackage(group: Pick<ModelGroupOptionResponse, "packageType"> | null | undefined) {
  return group?.packageType === "BALANCE";
}

function getPackageTypeLabel(group: Pick<ModelGroupOptionResponse, "packageType" | "packageTypeText">) {
  return group.packageType === "BALANCE"
    ? group.packageTypeText || "余额套餐"
    : group.packageTypeText || "普通套餐";
}

export function BillingPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get("tab");
  const initialTab =
    requestedTab === "billing" || requestedTab === "package" || requestedTab === "usage"
      ? requestedTab
      : "usage";
  const [activeTab, setActiveTab] = useState<"usage" | "billing" | "package">(
    initialTab,
  );
  const {
    data: packageSummary,
    isLoading: packageLoading,
    isError: packageError,
    refetch: refetchPackage,
    error: packageQueryError,
  } = useModelAccessSummaryQuery(Boolean(user));
  const billingEnabled = activeTab !== "package";
  const {
    data,
    isLoading,
    isError,
    refetch,
    error,
  } = useBillingQuery(
    user?.username,
    Number(user?.balance ?? 0),
    billingEnabled,
  );
  const {
    data: modelStats = [],
  } = useDashboardModelStatsQuery(user?.username ?? "user", activeTab === "usage");
  const purchaseMutation = usePurchaseModelPackageMutation();

  useEffect(() => {
    if (activeTab !== initialTab) {
      setActiveTab(initialTab);
    }
  }, [activeTab, initialTab]);

  const changeTab = (nextTab: "usage" | "billing" | "package") => {
    setActiveTab(nextTab);
    const nextSearchParams = new URLSearchParams(searchParams);
    if (nextTab === "usage") {
      nextSearchParams.delete("tab");
    } else {
      nextSearchParams.set("tab", nextTab);
    }
    setSearchParams(nextSearchParams, { replace: true });
  };

  if (!user) {
    return (
      <div className="mx-auto max-w-5xl space-y-6 p-4 sm:p-6">
        <PageHeaderSkeleton showAction={false} />
        <PageCardGridSkeleton />
        <PagePanelSkeleton lines={5} />
      </div>
    );
  }

  if (packageLoading || (billingEnabled && isLoading)) {
    return (
      <div className="mx-auto max-w-5xl space-y-6 p-4 sm:p-6">
        <PageHeaderSkeleton showAction={false} />
        <PageCardGridSkeleton />
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-5">
          <PagePanelSkeleton className="xl:col-span-3" lines={1} />
          <PagePanelSkeleton className="xl:col-span-2" lines={5} />
        </div>
        <PagePanelSkeleton lines={6} />
      </div>
    );
  }

  if ((billingEnabled && isError) || packageError) {
    const message = isAppError(error)
      ? error.message
      : isAppError(packageQueryError)
        ? packageQueryError.message
        : "加载失败";

    return (
      <PageErrorState
        message={message}
        onRetry={() => {
          void refetch();
          void refetchPackage();
        }}
      />
    );
  }

  if (!packageSummary || (billingEnabled && !data)) {
    if (!billingEnabled) {
      return (
        <PageErrorState
          message="套餐数据缺失，请刷新后重试"
          onRetry={() => {
            void refetchPackage();
          }}
        />
      );
    }

    return (
      <PageErrorState
        message="账单或套餐数据缺失，请刷新后重试"
        onRetry={() => {
          void refetch();
          void refetchPackage();
        }}
      />
    );
  }

  const billingData = billingEnabled ? (data ?? EMPTY_BILLING_RESPONSE) : EMPTY_BILLING_RESPONSE;
  const packageName = resolvePackageName(
    packageSummary.activeGroupName,
    packageSummary.packageStatusText,
  );
  const balance = Number(user?.balance ?? 0);

  const handlePurchase = async (groupId: string) => {
    try {
      const targetGroup = packageSummary.groups.find((item) => item.id === groupId);
      await purchaseMutation.mutateAsync({ groupId });
      toast.success(targetGroup?.packageType === "BALANCE" ? "余额套餐开通成功" : "套餐购买成功");
    } catch (purchaseError) {
      if (isAppError(purchaseError)) {
        toast.error(purchaseError.message);
      } else {
        toast.error("套餐购买失败，请稍后重试");
      }
    }
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 text-slate-900 dark:text-slate-100 sm:p-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">用量 & 账单</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            基于请求日志统计消费情况，并展示当前套餐与额度使用
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
        <div className="rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 p-5 text-white xl:col-span-1">
          <div className="text-xs text-blue-200">当前余额</div>
          <div className="mb-1 text-3xl font-bold text-white">¥{balance.toFixed(2)}</div>
          <div className="text-xs text-blue-100">当前套餐：{packageName}</div>
          <div className="mt-1 text-xs text-blue-200">
            {packageSummary.remainingDays != null
              ? `剩余 ${packageSummary.remainingDays} 天`
              : packageSummary.packageStatusText}
          </div>
          <button
            onClick={() => changeTab("package")}
            className="mt-4 w-full rounded-xl bg-white/20 py-2 text-sm transition-colors hover:bg-white/30"
          >
            购买套餐
          </button>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:col-span-3 xl:grid-cols-3">
          <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-2 flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <TrendingUp className="h-4 w-4" />
              本月消费
            </div>
            <div className="text-2xl font-semibold text-slate-900 dark:text-slate-100">
              ¥{billingData.monthlySpend.toFixed(2)}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-2 flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <Zap className="h-4 w-4" />
              计费模型数
            </div>
            <div className="text-2xl font-semibold text-slate-900 dark:text-slate-100">
              {billingData.modelUsage.length}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-2 flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <Package className="h-4 w-4" />
              套餐状态
            </div>
            <div className="text-2xl font-semibold text-slate-900 dark:text-slate-100">
              {packageName}
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {[
          { id: "usage", label: "用量分析" },
          { id: "billing", label: "账单记录" },
          { id: "package", label: "套餐购买" },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => changeTab(tab.id as typeof activeTab)}
            className={`rounded-lg px-3 py-2 text-sm ${
              activeTab === tab.id
                ? "bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-300"
                : "text-slate-500 hover:bg-slate-50 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "usage" ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4 xl:grid-cols-5">
            <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5 xl:col-span-3">
              <div className="mb-3 flex items-center justify-between">
                <h3 className="text-slate-900 dark:text-slate-100">周期消费趋势</h3>
              </div>
              {billingData.usageData.length === 0 ? (
                <PageEmptyState
                  className="border-none px-0 py-10 shadow-none dark:bg-transparent"
                  description="当前暂无用量趋势数据。"
                />
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart data={billingData.usageData}>
                    <defs>
                      <linearGradient id="costGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.2} />
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke="#f1f5f9" strokeDasharray="3 3" />
                    <XAxis
                      dataKey="date"
                      tick={{ fontSize: 11, fill: "#94a3b8" }}
                      axisLine={false}
                      tickLine={false}
                    />
                    <YAxis
                      tick={{ fontSize: 11, fill: "#94a3b8" }}
                      axisLine={false}
                      tickLine={false}
                    />
                    <Tooltip formatter={(value: number) => [`¥${value.toFixed(2)}`, "费用"]} />
                    <Area
                      type="monotone"
                      dataKey="cost"
                      stroke="#6366f1"
                      fill="url(#costGrad)"
                      strokeWidth={2.5}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </div>

            <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5 xl:col-span-2">
              <div className="mb-3 flex items-center justify-between">
                <h3 className="text-slate-900 dark:text-slate-100">模型成本占比</h3>
              </div>
              {billingData.modelUsage.length === 0 ? (
                <PageEmptyState
                  className="border-none px-0 py-10 shadow-none dark:bg-transparent"
                  description="当前暂无模型费用占比数据。"
                />
              ) : (
                <div className="space-y-3">
                  {billingData.modelUsage.map((item, index) => (
                    <div key={item.model}>
                      <div className="mb-1 flex justify-between text-xs">
                        <span className="text-slate-600 dark:text-slate-300">{item.model}</span>
                        <span className="text-slate-500 dark:text-slate-400">
                          ¥{item.cost.toFixed(2)} · {item.pct}%
                        </span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                        <div
                          className="h-full"
                          style={{
                            width: `${item.pct}%`,
                            backgroundColor: modelColors[index % modelColors.length],
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
            <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h3 className="text-slate-900 dark:text-slate-100">对外模型统计</h3>
              </div>
            </div>
            {modelStats.length === 0 ? (
              <PageEmptyState description="当前暂无模型统计数据。" />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="border-b border-slate-100 dark:border-slate-800">
                    <tr className="text-left text-xs text-slate-500 dark:text-slate-400">
                      <th className="py-2 pr-4 font-medium">模型</th>
                      <th className="py-2 pr-4 font-medium">上游模型</th>
                      <th className="py-2 pr-4 font-medium">调用次数</th>
                      <th className="py-2 pr-4 font-medium">总 Tokens</th>
                      <th className="py-2 pr-4 font-medium">平均耗时</th>
                      <th className="py-2 pr-4 font-medium">成功率</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {modelStats.map((item) => (
                      <tr key={item.modelCode}>
                        <td className="py-3 pr-4 text-slate-800 dark:text-slate-100">{item.modelCode}</td>
                        <td className="py-3 pr-4 text-slate-500 dark:text-slate-400">{item.upstreamModels}</td>
                        <td className="py-3 pr-4 text-slate-600 dark:text-slate-300">
                          {item.requestCount.toLocaleString()}
                        </td>
                        <td className="py-3 pr-4 text-slate-600 dark:text-slate-300">
                          {item.totalTokens.toLocaleString()}
                        </td>
                        <td className="py-3 pr-4 text-slate-600 dark:text-slate-300">
                          {item.avgLatencyMs.toFixed(0)} ms
                        </td>
                        <td className="py-3 pr-4 text-slate-600 dark:text-slate-300">{item.successRate.toFixed(1)}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      ) : null}

      {activeTab === "billing" ? (
        <div className="rounded-2xl border border-slate-100 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-slate-900 dark:text-slate-100">账单明细</h3>
            <button className="inline-flex items-center gap-2 text-xs text-blue-600 hover:text-blue-700">
              <Download className="h-3.5 w-3.5" />
              导出
            </button>
          </div>
          {billingData.billingHistory.length === 0 ? (
            <PageEmptyState description="当前暂无账单记录。" />
          ) : (
            <div className="space-y-2">
              {billingData.billingHistory.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center justify-between rounded-xl border border-slate-100 px-4 py-3 text-sm dark:border-slate-800 dark:bg-slate-950/40"
                >
                  <div>
                    <div className="text-slate-800 dark:text-slate-100">
                      {item.type} · {item.method}
                    </div>
                    <div className="text-xs text-slate-400 dark:text-slate-500">
                      {item.time} · {item.id}
                    </div>
                  </div>
                  <div className="text-right">
                    <div
                      className={
                        item.amount.startsWith("+")
                          ? "text-emerald-600"
                          : "text-slate-700 dark:text-slate-200"
                      }
                    >
                      {item.amount}
                    </div>
                    <div className="text-xs text-slate-400 dark:text-slate-500">{item.status}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {activeTab === "package" ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
              <div className="text-xs text-slate-500 dark:text-slate-400">当前套餐</div>
              <div className="mt-2 text-xl font-semibold text-slate-900 dark:text-slate-100">{packageName}</div>
              <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{packageSummary.packageStatusText}</div>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
              <div className="text-xs text-slate-500 dark:text-slate-400">月度额度</div>
              <div className="mt-2 text-xl font-semibold text-slate-900 dark:text-slate-100">
                {formatQuota(packageSummary.monthlyUsed)} /{" "}
                {formatQuota(packageSummary.monthlyQuota)}
              </div>
              <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">已用 / 总额</div>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
              <div className="text-xs text-slate-500 dark:text-slate-400">到期时间</div>
              <div className="mt-2 text-xl font-semibold text-slate-900 dark:text-slate-100">
                {packageSummary.expiresAt
                  ? new Date(packageSummary.expiresAt).toLocaleDateString("zh-CN")
                  : "—"}
              </div>
              <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                {packageSummary.remainingDays != null
                  ? `剩余 ${packageSummary.remainingDays} 天`
                  : "未开通"}
              </div>
            </div>
          </div>

          {!packageSummary.packageRestrictionEnabled ? (
            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              当前系统未开启套餐限制，用户可直接访问公开模型。
            </div>
          ) : null}

          {packageSummary.groups.length === 0 ? (
            <PageEmptyState description="当前暂无可购买套餐。" />
          ) : (
            <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
              {packageSummary.groups.map((group) => {
                const isCurrent = Boolean(group.active);
                const isOwned = Boolean(group.purchased);
                const isOwnedBalancePackage = isBalancePackage(group) && isOwned;

                return (
                  <div
                    key={group.id}
                    className={`rounded-2xl border p-5 ${
                      isCurrent
                        ? "border-blue-200 bg-blue-50/60 dark:border-blue-500/40 dark:bg-blue-500/10"
                        : "border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-slate-900 dark:text-slate-100">{group.groupName}</h3>
                          <span
                            className={`rounded-full border px-2 py-0.5 text-xs ${
                              isBalancePackage(group)
                                ? "border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300"
                                : "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            }`}
                          >
                            {getPackageTypeLabel(group)}
                          </span>
                          {isCurrent ? (
                            <span className="rounded-full border border-blue-200 bg-blue-100 px-2 py-0.5 text-xs text-blue-700">
                              当前套餐
                            </span>
                          ) : null}
                          {isOwned && !isCurrent ? (
                            <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-xs text-emerald-700">
                              已购买
                            </span>
                          ) : null}
                        </div>
                        <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                          {group.packageStatusText}
                          {group.remark ? ` · ${group.remark}` : ""}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-2xl font-semibold text-slate-900 dark:text-slate-100">
                          {isBalancePackage(group)
                            ? `$${Number(group.salePrice ?? 0).toFixed(2)}`
                            : `¥${Number(group.salePrice ?? 0).toFixed(2)}`}
                        </div>
                        <div className="text-xs text-slate-400 dark:text-slate-500">
                          {isBalancePackage(group) ? "按余额实时扣费" : `${group.packageDays} 天有效期`}
                        </div>
                      </div>
                    </div>

                    <div className="mt-4 grid grid-cols-1 gap-3 text-sm text-slate-600 dark:text-slate-300 sm:grid-cols-2">
                      <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                        模型数量：{group.modelCount}
                      </div>
                      {isBalancePackage(group) ? (
                        <>
                          <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                            开通门槛：余额 &gt;= $1.00
                          </div>
                          <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                            调用门槛：余额 &gt;= $0.50
                          </div>
                          <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                            调用时直接扣钱包余额
                          </div>
                        </>
                      ) : (
                        <>
                          <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                            日额度：{formatQuota(group.dailyQuota)}
                          </div>
                          <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                            周额度：{formatQuota(group.weeklyQuota)}
                          </div>
                          <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                            月额度：{formatQuota(group.monthlyQuota)}
                          </div>
                        </>
                      )}
                    </div>

                    <div className="mt-4 flex items-center justify-between">
                      <div className="text-xs text-slate-400 dark:text-slate-500">
                        {group.expiresAt
                          ? `到期：${new Date(group.expiresAt).toLocaleDateString("zh-CN")}`
                          : "尚未开通"}
                      </div>

                      <button
                        onClick={() => void handlePurchase(group.id)}
                        disabled={purchaseMutation.isPending || isCurrent || isOwnedBalancePackage}
                        className={`rounded-xl px-4 py-2 text-sm ${
                          isCurrent || isOwnedBalancePackage
                            ? "cursor-not-allowed bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500"
                            : "bg-blue-600 text-white hover:bg-blue-700"
                        } disabled:opacity-70`}
                      >
                        <ButtonLoadingContent
                          loading={
                            purchaseMutation.isPending &&
                            purchaseMutation.variables?.groupId === group.id
                          }
                          loadingText="购买中..."
                        >
                          {isCurrent
                            ? "已生效"
                            : isOwnedBalancePackage
                              ? "已开通"
                              : isBalancePackage(group)
                                ? "开通余额套餐"
                                : "购买套餐"}
                        </ButtonLoadingContent>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {purchaseMutation.isSuccess ? (
            <div className="inline-flex items-center gap-2 text-sm text-emerald-600">
              <CheckCircle className="h-4 w-4" />
              套餐状态已刷新
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
