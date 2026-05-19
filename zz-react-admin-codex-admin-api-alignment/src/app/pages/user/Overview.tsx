import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import {
  Zap,
  TrendingUp,
  CreditCard,
  Key,
  ArrowRight,
  Copy,
  ExternalLink,
  Cpu,
  Network,
  Wallet,
  CalendarClock,
  Layers3,
} from "lucide-react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  useModelAccessSummaryQuery,
  useOverviewQuery,
  useUserDetailQuery,
} from "../../api/queries";
import { useAuth } from "../../auth/auth-context";
import { isAppError } from "../../../lib/http/error";
import {
  PageCardGridSkeleton,
  PageErrorState,
  PageHeaderSkeleton,
  PageEmptyState,
  PagePanelSkeleton,
} from "../../components/ui/feedback";
import { useRouteTransition } from "../../components/layout/route-transition-context";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import type { ModelGroupOptionResponse } from "../../api/types";

function formatTrendLabel(value: string) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function getGreetingLabel() {
  const hour = new Date().getHours();
  if (hour < 11) {
    return "早上好";
  }
  if (hour < 18) {
    return "中午好";
  }
  return "晚上好";
}

function formatQuotaValue(value: number | null | undefined) {
  if (value == null || value <= 0) {
    return "$0";
  }

  return `$${value.toLocaleString(undefined, {
    maximumFractionDigits: value % 1 === 0 ? 0 : 4,
  })}`;
}

function formatDateTimeLabel(value: string | null | undefined) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const parts = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).formatToParts(date);

  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}/${get("month")}/${get("day")} ${get("hour")}:${get("minute")}`;
}

function resolveQuotaProgress(quota: number | null | undefined, used: number | null | undefined) {
  if (!quota || quota <= 0) {
    return 0;
  }

  const ratio = ((used ?? 0) / quota) * 100;
  return Math.max(0, Math.min(100, Number.isFinite(ratio) ? ratio : 0));
}

function resolveSelectedGroupLabel(group: ModelGroupOptionResponse) {
  if (group.active) {
    return `${group.groupName}（使用中）`;
  }

  if (group.packageType === "BALANCE") {
    return `${group.groupName}（余额计费）`;
  }

  return group.groupName;
}

function isBalancePackage(group: Pick<ModelGroupOptionResponse, "packageType"> | null | undefined) {
  return group?.packageType === "BALANCE";
}

function getPackageTypeLabel(group: Pick<ModelGroupOptionResponse, "packageType" | "packageTypeText">) {
  return group.packageType === "BALANCE"
    ? group.packageTypeText || "余额套餐"
    : group.packageTypeText || "普通套餐";
}

export function OverviewPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data, isLoading, isError, refetch, error } = useOverviewQuery();
  const { data: currentUser } = useUserDetailQuery(user?.id ?? "", Boolean(user?.id));
  const { data: packageSummary } = useModelAccessSummaryQuery(Boolean(user));
  const { beginTransition } = useRouteTransition();
  const [selectedGroupId, setSelectedGroupId] = useState<string>("");

  const navigateWithTransition = (path: string) => {
    beginTransition(path);
    navigate(path);
  };

  const chartData = useMemo(
    () =>
      (data?.trend ?? []).map((item) => ({
        ...item,
        day: formatTrendLabel(item.statDate),
      })),
    [data?.trend],
  );

  const packageGroups = packageSummary?.groups ?? [];
  const ownedPackageGroups = useMemo(
    () => packageGroups.filter((group) => group.purchased),
    [packageGroups],
  );

  useEffect(() => {
    if (ownedPackageGroups.length === 0) {
      if (selectedGroupId) {
        setSelectedGroupId("");
      }
      return;
    }

    const currentExists = ownedPackageGroups.some((group) => String(group.id) === selectedGroupId);
    if (currentExists) {
      return;
    }

    const nextGroup =
      ownedPackageGroups.find((group) => group.active) ??
      ownedPackageGroups.find((group) => String(group.id) === packageSummary?.activeGroupId) ??
      ownedPackageGroups[0];

    setSelectedGroupId(String(nextGroup.id));
  }, [ownedPackageGroups, packageSummary?.activeGroupId, selectedGroupId]);

  const selectedPackageGroup = useMemo(
    () =>
      ownedPackageGroups.find((group) => String(group.id) === selectedGroupId) ??
      ownedPackageGroups[0] ??
      null,
    [ownedPackageGroups, selectedGroupId],
  );

  if (isLoading) {
    return (
      <div className="mx-auto max-w-6xl space-y-6 p-4 sm:p-6">
        <PageHeaderSkeleton />
        <PageCardGridSkeleton />
        <PagePanelSkeleton lines={7} />
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-5">
          <PagePanelSkeleton className="xl:col-span-3" lines={1} />
          <PagePanelSkeleton className="xl:col-span-2" lines={4} />
        </div>
        <PageCardGridSkeleton cards={3} />
      </div>
    );
  }

  if (isError || !data) {
    const message = isAppError(error) ? error.message : "加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  const greetingLabel = getGreetingLabel();

  const statsCards = [
    {
      label: "账户余额",
      value: `¥ ${Number(currentUser?.balance ?? user?.balance ?? 0).toFixed(2)}`,
      sub: `当前套餐 ${packageSummary?.activeGroupName || packageSummary?.packageStatusText || "未开通套餐"}`,
      icon: CreditCard,
      color: "from-blue-500 to-indigo-600",
      action: "购买套餐",
      actionPath: "/console/billing",
    },
    {
      label: "今日请求",
      value: data.stats.requestCountToday.toLocaleString(),
      sub: "今日累计调用次数",
      icon: Zap,
      color: "from-emerald-400 to-teal-500",
      action: "查看账单",
      actionPath: "/console/billing",
    },
    {
      label: "今日 Tokens",
      value: data.stats.totalTokensToday.toLocaleString(),
      sub: `近7天累计 ${data.stats.totalTokens7d.toLocaleString()}`,
      icon: TrendingUp,
      color: "from-violet-500 to-purple-600",
      action: "模型广场",
      actionPath: "/console/models",
    },
    {
      label: "活跃 Keys",
      value: `${data.stats.activeKeys} / ${data.stats.apiKeyCount}`,
      sub: "当前账号可用情况",
      icon: Key,
      color: "from-orange-400 to-rose-500",
      action: "管理",
      actionPath: "/console/keys",
    },
  ];

  return (
      <div className="mx-auto max-w-6xl space-y-6 p-4 text-slate-900 dark:text-slate-100 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">
            {greetingLabel}，{currentUser?.nickname ?? user?.nickname ?? user?.username ?? "开发者"} 👋
          </h1>
        </div>
        <button
          onClick={() => navigateWithTransition("/console/docs")}
          className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-4 py-2 text-sm text-white shadow-md shadow-blue-500/20 transition-all hover:shadow-blue-500/30 sm:w-auto"
        >
          <Zap className="w-4 h-4" />
          快速接入
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {statsCards.map(({ label, value, sub, icon: Icon, color, action, actionPath }) => (
          <div
            key={label}
            className="rounded-2xl border border-slate-100 bg-white p-5 transition-shadow hover:shadow-md hover:shadow-slate-200/60 dark:border-slate-800 dark:bg-slate-900 dark:hover:shadow-slate-950/40"
          >
            <div
              className={`w-10 h-10 rounded-xl bg-gradient-to-br ${color} flex items-center justify-center mb-3 shadow-sm`}
            >
              <Icon className="w-5 h-5 text-white" />
            </div>
            <div className="mb-0.5 text-2xl font-semibold text-slate-900 dark:text-slate-100">
              {value}
            </div>
            <div className="mb-3 text-xs text-slate-500 dark:text-slate-400">
              {label} · {sub}
            </div>
            <button
              onClick={() => navigateWithTransition(actionPath)}
              className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 transition-colors"
            >
              {action}
              <ArrowRight className="w-3 h-3" />
            </button>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-5">
        <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5 xl:col-span-3">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-slate-900 dark:text-slate-100">近 7 天请求趋势</h3>
            </div>
          </div>
          {chartData.length === 0 ? (
            <PageEmptyState
              className="border-none px-0 py-10 shadow-none dark:bg-transparent"
              description="近 7 天暂无请求趋势数据。"
            />
          ) : (
            <ResponsiveContainer width="100%" height={180}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="reqGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis
                  dataKey="day"
                  tick={{ fontSize: 11, fill: "#94a3b8" }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis tick={{ fontSize: 11, fill: "#94a3b8" }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{ border: "1px solid #e2e8f0", borderRadius: 10, fontSize: 12 }}
                  formatter={(value: number, name: string) => [
                    value.toLocaleString(),
                    name === "requestCount" ? "请求数" : "总 Tokens",
                  ]}
                />
                <Area
                  type="monotone"
                  dataKey="requestCount"
                  stroke="#6366f1"
                  fill="url(#reqGrad)"
                  strokeWidth={2.5}
                  dot={false}
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5 xl:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-slate-900 dark:text-slate-100">模型统计</h3>
            <button
              onClick={() => navigateWithTransition("/console/models")}
              className="text-xs text-blue-600 hover:text-blue-700 flex items-center gap-1"
            >
              查看模型 <ArrowRight className="w-3 h-3" />
            </button>
          </div>
          <div className="space-y-3">
            {data.modelStats.length === 0 ? (
              <PageEmptyState
                className="border-none px-0 py-8 shadow-none dark:bg-transparent"
                description="当前暂无模型统计数据。"
              />
            ) : (
              data.modelStats.slice(0, 4).map((item) => (
                <div key={item.modelCode} className="flex items-center gap-3">
                  <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-blue-50 dark:bg-blue-500/15">
                    <Cpu className="w-3.5 h-3.5 text-blue-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="truncate text-xs font-medium text-slate-800 dark:text-slate-100">
                      {item.modelCode}
                    </div>
                    <div className="text-xs text-slate-400 dark:text-slate-500">
                      {item.requestCount.toLocaleString()} 次 · {item.totalTokens.toLocaleString()}{" "}
                      Tokens
                    </div>
                  </div>
                  <div className="flex-shrink-0 text-xs text-slate-400 dark:text-slate-500">
                    {item.successRate.toFixed(1)}%
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="rounded-[28px] border border-slate-200 bg-gradient-to-br from-white via-blue-50/40 to-indigo-50/60 p-4 shadow-sm shadow-slate-200/60 dark:border-slate-800 dark:from-slate-900 dark:via-slate-900 dark:to-slate-950 dark:shadow-slate-950/30 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300">
              <Layers3 className="h-3.5 w-3.5" />
              套餐余额和套餐切换
            </div>
            <div>
              <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                当前套餐额度概览
              </h3>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                基于套餐摘要接口展示当前已购套餐额度与到期情况
              </p>
            </div>
          </div>
          <button
            onClick={() => navigateWithTransition("/console/billing?tab=package")}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white/90 px-4 py-2 text-sm text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900/80 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            查看套餐中心
            <ArrowRight className="h-4 w-4" />
          </button>
        </div>

        {selectedPackageGroup ? (
          <div className="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-[320px_minmax(0,1fr)]">
            <div className="rounded-3xl border border-slate-200 bg-white/90 p-4 shadow-sm shadow-slate-200/40 dark:border-slate-800 dark:bg-slate-900/90 dark:shadow-slate-950/20 sm:p-5">
              <div className="flex flex-col gap-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="text-xs text-slate-500 dark:text-slate-400">当前套餐</div>
                    <div className="mt-2">
                      <Select value={selectedGroupId} onValueChange={setSelectedGroupId}>
                        <SelectTrigger className="h-12 rounded-2xl border-slate-200 bg-white text-left text-base font-semibold text-slate-900 shadow-sm shadow-slate-200/30 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100">
                          <SelectValue placeholder="选择套餐" />
                        </SelectTrigger>
                        <SelectContent>
                          {ownedPackageGroups.map((group) => (
                            <SelectItem key={group.id} value={String(group.id)}>
                              {resolveSelectedGroupLabel(group)}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <div className="mt-1 flex flex-col items-end gap-2">
                    <span className="inline-flex rounded-full bg-blue-600 px-3 py-1 text-xs font-medium text-white shadow-sm shadow-blue-500/30">
                      {selectedPackageGroup.packageStatusText || "使用中"}
                    </span>
                    <span
                      className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] ${
                        isBalancePackage(selectedPackageGroup)
                          ? "border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300"
                          : "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                      }`}
                    >
                      {getPackageTypeLabel(selectedPackageGroup)}
                    </span>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-100 bg-slate-50/90 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/70">
                  <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                    <CalendarClock className="h-3.5 w-3.5" />
                    到期时间
                  </div>
                  <div className="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1">
                    <div className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                      {formatDateTimeLabel(selectedPackageGroup.expiresAt)}
                    </div>
                    <div className="text-sm font-medium text-blue-600 dark:text-blue-300">
                      {selectedPackageGroup.remainingDays != null
                        ? `(${selectedPackageGroup.remainingDays}天后)`
                        : "未设置"}
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-2xl bg-blue-50 px-4 py-3 dark:bg-blue-500/10">
                    <div className="text-xs text-blue-700/80 dark:text-blue-300/80">套餐价格</div>
                    <div className="mt-1 text-lg font-semibold text-blue-700 dark:text-blue-200">
                      {isBalancePackage(selectedPackageGroup)
                        ? `$${Number(selectedPackageGroup.salePrice ?? 0).toFixed(2)}`
                        : `¥${Number(selectedPackageGroup.salePrice ?? 0).toFixed(2)}`}
                    </div>
                  </div>
                  <div className="rounded-2xl bg-slate-100/80 px-4 py-3 dark:bg-slate-800/80">
                    <div className="text-xs text-slate-500 dark:text-slate-400">
                      {isBalancePackage(selectedPackageGroup) ? "计费方式" : "模型数量"}
                    </div>
                    <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                      {isBalancePackage(selectedPackageGroup)
                        ? "按余额实时扣费"
                        : selectedPackageGroup.modelCount}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="rounded-3xl border border-slate-200 bg-white/90 p-4 shadow-sm shadow-slate-200/40 dark:border-slate-800 dark:bg-slate-900/90 dark:shadow-slate-950/20 sm:p-5">
              {isBalancePackage(selectedPackageGroup) ? (
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-2xl border border-amber-200 bg-amber-50/80 p-4 dark:border-amber-500/20 dark:bg-amber-500/10">
                    <div className="text-sm font-medium text-amber-800 dark:text-amber-200">开通条件</div>
                    <div className="mt-2 text-sm text-amber-700 dark:text-amber-300">
                      钱包余额需大于等于 $1.00，购买时不会扣减余额，也不会写套餐购买扣费流水。
                    </div>
                  </div>
                  <div className="rounded-2xl border border-blue-200 bg-blue-50/80 p-4 dark:border-blue-500/20 dark:bg-blue-500/10">
                    <div className="text-sm font-medium text-blue-800 dark:text-blue-200">调用规则</div>
                    <div className="mt-2 text-sm text-blue-700 dark:text-blue-300">
                      创建出的 API Key 只按钱包余额实时扣费，不消耗普通套餐额度，也不会自动切换到普通套餐。
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50/90 p-4 dark:border-slate-800 dark:bg-slate-950/70">
                    <div className="text-sm font-medium text-slate-700 dark:text-slate-200">最低调用门槛</div>
                    <div className="mt-2 text-lg font-semibold text-slate-900 dark:text-slate-100">$0.50</div>
                    <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">低于该余额时将直接拒绝调用</div>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50/90 p-4 dark:border-slate-800 dark:bg-slate-950/70">
                    <div className="text-sm font-medium text-slate-700 dark:text-slate-200">当前钱包余额</div>
                    <div className="mt-2 text-lg font-semibold text-slate-900 dark:text-slate-100">
                      ¥{Number(currentUser?.balance ?? user?.balance ?? 0).toFixed(2)}
                    </div>
                    <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">每次请求完成后按实际 userAmount 扣费</div>
                  </div>
                </div>
              ) : (
                <div className="space-y-6">
                  {[
                    {
                      label: "每日额度",
                      quota: selectedPackageGroup.dailyQuota,
                      used: selectedPackageGroup.dailyUsed,
                    },
                    {
                      label: "每周额度",
                      quota: selectedPackageGroup.weeklyQuota,
                      used: selectedPackageGroup.weeklyUsed,
                    },
                    {
                      label: "每月额度",
                      quota: selectedPackageGroup.monthlyQuota,
                      used: selectedPackageGroup.monthlyUsed,
                    },
                  ].map((item) => (
                    <div key={item.label}>
                      <div className="mb-2 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div className="text-base font-medium text-slate-700 dark:text-slate-200">
                          {item.label}
                        </div>
                        <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                          {formatQuotaValue(item.used)} / {formatQuotaValue(item.quota)}
                        </div>
                      </div>
                      <div className="h-3 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-blue-500 to-indigo-500 transition-[width] duration-300"
                          style={{ width: `${resolveQuotaProgress(item.quota, item.used)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="mt-5 rounded-3xl border border-dashed border-slate-200 bg-white/90 px-6 py-12 text-center dark:border-slate-800 dark:bg-slate-900/90">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 dark:bg-slate-800">
              <Wallet className="h-6 w-6 text-slate-400 dark:text-slate-500" />
            </div>
            <div className="mt-4 text-lg font-semibold text-slate-900 dark:text-slate-100">
              没有对应的套餐
            </div>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
              当前用户还没有已购套餐，购买后这里会展示套餐余额与额度使用情况。
            </p>
            <button
              onClick={() => navigateWithTransition("/console/billing?tab=package")}
              className="mt-5 inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
            >
              前往购买套餐
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-9 h-9 rounded-xl bg-blue-50 flex items-center justify-center">
              <Key className="w-4 h-4 text-blue-600" />
            </div>
            <div>
              <h4 className="text-slate-900 dark:text-slate-100">API 接入地址</h4>
              <p className="text-xs text-slate-400 dark:text-slate-500">兼容 OpenAI 格式</p>
            </div>
          </div>
          <div className="mb-3 flex flex-col items-stretch gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 dark:border-slate-700 dark:bg-slate-800 sm:flex-row sm:items-center">
            <code className="flex-1 truncate font-mono text-xs text-slate-700 dark:text-slate-200">
              {data.endpoint}
            </code>
            <button
              onClick={() => navigator.clipboard.writeText(data.endpoint).catch(() => {})}
              className="text-slate-400 hover:text-slate-600 dark:text-slate-500 dark:hover:text-slate-200"
            >
              <Copy className="w-3.5 h-3.5" />
            </button>
          </div>
          <button
            onClick={() => navigateWithTransition("/console/docs")}
            className="w-full flex items-center justify-center gap-2 py-2 bg-blue-50 hover:bg-blue-100 text-blue-600 rounded-xl text-sm transition-colors"
          >
            查看接入文档 <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-9 h-9 rounded-xl bg-emerald-50 flex items-center justify-center">
              <Network className="w-4 h-4 text-emerald-600" />
            </div>
            <div>
              <h4 className="text-slate-900 dark:text-slate-100">平台资源</h4>
              <p className="text-xs text-slate-400 dark:text-slate-500">来自仪表盘概览</p>
            </div>
          </div>
          <div className="space-y-2 text-xs text-slate-500 dark:text-slate-400">
            <div className="flex items-center justify-between">
              <span>提供商数量</span>
              <span className="text-slate-800 dark:text-slate-100">{data.stats.providerCount}</span>
            </div>
            <div className="flex items-center justify-between">
              <span>模型数量</span>
              <span className="text-slate-800 dark:text-slate-100">{data.stats.modelCount}</span>
            </div>
            <div className="flex items-center justify-between">
              <span>全部 API Keys</span>
              <span className="text-slate-800 dark:text-slate-100">{data.stats.apiKeyCount}</span>
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl p-5 text-white">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-9 h-9 rounded-xl bg-white/15 flex items-center justify-center">
              <Wallet className="w-4 h-4 text-white" />
            </div>
            <div>
              <h4 className="text-white">今日收支</h4>
              <p className="text-indigo-200 text-xs">来自仪表盘概览</p>
            </div>
          </div>
          <div className="space-y-2 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-indigo-100">今日购买</span>
              <span>¥{data.stats.rechargeAmountToday.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-indigo-100">今日消耗</span>
              <span>¥{data.stats.consumeAmountToday.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-indigo-100">近7天 Tokens</span>
              <span>{data.stats.totalTokens7d.toLocaleString()}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
