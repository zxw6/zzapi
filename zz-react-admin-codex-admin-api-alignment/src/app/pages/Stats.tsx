import { useMemo, useState } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  LineChart,
  Line,
} from "recharts";
import {
  TrendingUp,
  TrendingDown,
  Clock,
  CheckCircle,
  DollarSign,
  Cpu,
  BarChart3,
  Calendar,
} from "lucide-react";
import {
  useAdminDashboardTrendQuery,
  useDashboardModelStatsQuery,
  useDashboardOverviewQuery,
} from "../api/queries";
import { isAppError } from "../../lib/http/error";
import {
  PageCardGridSkeleton,
  PageErrorState,
  PageHeaderSkeleton,
  PagePanelSkeleton,
} from "../components/ui/feedback";
import { useThemeMode } from "../providers/theme-context";

const colorMap: Record<string, string> = {
  blue:
    "bg-blue-50 text-blue-600 border-blue-100 dark:border-blue-500/20 dark:bg-blue-500/15 dark:text-blue-300",
  emerald:
    "bg-emerald-50 text-emerald-600 border-emerald-100 dark:border-emerald-500/20 dark:bg-emerald-500/15 dark:text-emerald-300",
  violet:
    "bg-violet-50 text-violet-600 border-violet-100 dark:border-violet-500/20 dark:bg-violet-500/15 dark:text-violet-300",
  orange:
    "bg-orange-50 text-orange-600 border-orange-100 dark:border-orange-500/20 dark:bg-orange-500/15 dark:text-orange-300",
  pink:
    "bg-pink-50 text-pink-600 border-pink-100 dark:border-pink-500/20 dark:bg-pink-500/15 dark:text-pink-300",
};

const timeRanges = ["今日", "7天", "30天", "自定义"];

function getTrendDays(range: string) {
  if (range === "今日") {
    return 1;
  }

  if (range === "30天") {
    return 30;
  }

  return 7;
}

function formatTrendDate(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
  }).format(parsed);
}

export function StatsPage() {
  const { isDark } = useThemeMode();
  const [timeRange, setTimeRange] = useState("今日");
  const trendDays = getTrendDays(timeRange);
  const {
    data: overview,
    isLoading: isOverviewLoading,
    isError: isOverviewError,
    error: overviewError,
    refetch: refetchOverview,
  } = useDashboardOverviewQuery("admin");
  const {
    data: trendData,
    isLoading: isTrendLoading,
    isError: isTrendError,
    error: trendError,
    refetch: refetchTrend,
  } = useAdminDashboardTrendQuery(trendDays);
  const {
    data: modelStats,
    isLoading: isModelStatsLoading,
    isError: isModelStatsError,
    error: modelStatsError,
    refetch: refetchModelStats,
  } = useDashboardModelStatsQuery("admin");

  const requestTrendData = useMemo(
    () =>
      (trendData ?? []).map((item) => ({
        time: formatTrendDate(item.statDate),
        success: item.successCount,
        failed: Math.max(item.requestCount - item.successCount, 0),
        total: item.requestCount,
      })),
    [trendData],
  );

  const successRateData = useMemo(
    () =>
      (trendData ?? []).map((item) => ({
        date: formatTrendDate(item.statDate),
        rate:
          item.requestCount > 0
            ? Number(((item.successCount / item.requestCount) * 100).toFixed(2))
            : 0,
      })),
    [trendData],
  );

  const latencyData = useMemo(
    () =>
      (modelStats ?? []).map((item) => ({
        model: item.modelCode,
        avgLatencyMs: Number(item.avgLatencyMs.toFixed(0)),
      })),
    [modelStats],
  );

  const tokenCostData = useMemo(
    () =>
      (modelStats ?? []).map((item) => ({
        model: item.modelCode,
        cost: item.userAmount,
        tokens: item.totalTokens,
      })),
    [modelStats],
  );

  const summaryCards = useMemo(() => {
    const totalRequests = (trendData ?? []).reduce((sum, item) => sum + item.requestCount, 0);
    const totalSuccess = (trendData ?? []).reduce((sum, item) => sum + item.successCount, 0);
    const weightedLatency = (modelStats ?? []).reduce((sum, item) => sum + item.totalLatencyMs, 0);
    const totalModelRequests = (modelStats ?? []).reduce((sum, item) => sum + item.requestCount, 0);
    const avgLatency =
      totalModelRequests > 0 ? Math.round(weightedLatency / totalModelRequests) : 0;
    const successRate =
      totalRequests > 0 ? ((totalSuccess / totalRequests) * 100).toFixed(1) : "0.0";

    return [
      {
        label: "今日总请求",
        value: overview ? overview.requestCountToday.toLocaleString() : "0",
        change: `${trendDays}天趋势`,
        trend: "up",
        icon: BarChart3,
        color: "blue",
        sub: "来自 dashboard overview",
      },
      {
        label: "实时人数",
        value: overview ? overview.onlineUserCount.toLocaleString() : "0",
        change: "最近 5 分钟活跃",
        trend: "up",
        icon: TrendingUp,
        color: "emerald",
        sub: "来自 dashboard overview",
      },
      {
        label: "今天总人数",
        value: overview ? overview.todayActiveUserCount.toLocaleString() : "0",
        change: "今日去重活跃用户",
        trend: "up",
        icon: Calendar,
        color: "pink",
        sub: "来自 dashboard overview",
      },
      {
        label: "成功率",
        value: `${successRate}%`,
        change: `${totalSuccess.toLocaleString()}/${totalRequests.toLocaleString()}`,
        trend: "up",
        icon: CheckCircle,
        color: "emerald",
        sub: "来自 dashboard trend",
      },
      {
        label: "平均延迟",
        value: `${avgLatency} ms`,
        change: `${modelStats?.length ?? 0} 个模型`,
        trend: "up",
        icon: Clock,
        color: "violet",
        sub: "来自 model-stats",
      },
      {
        label: "今日费用",
        value: `¥ ${overview ? overview.consumeAmountToday.toFixed(2) : "0.00"}`,
        change: `充值 ¥${overview ? overview.rechargeAmountToday.toFixed(2) : "0.00"}`,
        trend: "down",
        icon: DollarSign,
        color: "orange",
        sub: "来自 dashboard overview",
      },
      {
        label: "消耗 Tokens",
        value: overview ? overview.totalTokensToday.toLocaleString() : "0",
        change: `7天 ${overview ? overview.totalTokens7d.toLocaleString() : "0"}`,
        trend: "up",
        icon: Cpu,
        color: "pink",
        sub: "来自 dashboard overview",
      },
    ] as const;
  }, [modelStats, overview, trendData, trendDays]);

  const isLoading = isOverviewLoading || isTrendLoading || isModelStatsLoading;
  const pageError = overviewError ?? trendError ?? modelStatsError;
  const isError = isOverviewError || isTrendError || isModelStatsError;

  if (isLoading) {
    return (
      <div className="space-y-6 p-6">
        <PageHeaderSkeleton />
        <PageCardGridSkeleton cards={7} gridClassName="grid-cols-1 md:grid-cols-2 xl:grid-cols-7" />
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          <PagePanelSkeleton className="xl:col-span-2" lines={1} />
          <PagePanelSkeleton lines={4} />
        </div>
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          <PagePanelSkeleton className="xl:col-span-2" lines={1} />
          <PagePanelSkeleton lines={5} />
        </div>
      </div>
    );
  }

  if (isError || !overview) {
    const message = isAppError(pageError) ? pageError.message : "统计数据加载失败";
    return (
      <PageErrorState
        message={message}
        onRetry={() => {
          void refetchOverview();
          void refetchTrend();
          void refetchModelStats();
        }}
      />
    );
  }

  return (
      <div className="space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">用量统计</h1>
          <p className="text-slate-500 text-sm mt-0.5 dark:text-slate-400">
            数据来自 `overview`、`trend`、`model-stats` 统计接口
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Calendar className="w-4 h-4 text-slate-400 dark:text-slate-500" />
          <div className="flex rounded-lg bg-slate-100 p-0.5 dark:bg-slate-800">
            {timeRanges.map((rangeItem) => (
              <button
                key={rangeItem}
                onClick={() => setTimeRange(rangeItem)}
                className={`px-3 py-1.5 rounded-md text-sm transition-all ${
                  timeRange === rangeItem
                    ? "bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-slate-100"
                    : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-100"
                }`}
              >
                {rangeItem}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-7">
        {summaryCards.map(({ label, value, change, trend, icon: Icon, color, sub }) => (
          <div
            key={label}
            className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="flex items-start justify-between mb-3">
              <div
                className={`w-8 h-8 rounded-lg border flex items-center justify-center ${colorMap[color]}`}
              >
                <Icon className="w-4 h-4" />
              </div>
              <div
                className={`flex items-center gap-1 text-xs ${trend === "up" ? "text-emerald-600" : "text-orange-600"}`}
              >
                {trend === "up" ? (
                  <TrendingUp className="w-3 h-3" />
                ) : (
                  <TrendingDown className="w-3 h-3" />
                )}
                {change}
              </div>
            </div>
            <div className="text-slate-900 text-xl font-semibold dark:text-slate-100">{value}</div>
            <div className="text-slate-500 text-xs mt-0.5 dark:text-slate-400">
              {label} · {sub}
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5 xl:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-slate-900 dark:text-slate-100">请求趋势</h3>
              <p className="text-slate-400 text-xs mt-0.5 dark:text-slate-500">
                来自 `/admin/dashboard/trend?days={trendDays}` 的成功 / 失败请求分布
              </p>
            </div>
            <div className="flex items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-blue-500" />
                成功
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-red-400" />
                失败
              </span>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={requestTrendData}>
              <defs>
                <linearGradient id="successGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="failedGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#f87171" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#f87171" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke={isDark ? "#1e293b" : "#f1f5f9"} />
              <XAxis
                dataKey="time"
                tick={{ fontSize: 11, fill: isDark ? "#64748b" : "#94a3b8" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tick={{ fontSize: 11, fill: isDark ? "#64748b" : "#94a3b8" }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                contentStyle={{
                  border: `1px solid ${isDark ? "#334155" : "#e2e8f0"}`,
                  borderRadius: 8,
                  fontSize: 12,
                  backgroundColor: isDark ? "#0f172a" : "#ffffff",
                  color: isDark ? "#e2e8f0" : "#0f172a",
                }}
                formatter={(value: number, name: string) => [
                  value.toLocaleString(),
                  name === "success" ? "成功" : "失败",
                ]}
              />
              <Area
                type="monotone"
                dataKey="success"
                stroke="#3b82f6"
                fill="url(#successGrad)"
                strokeWidth={2}
                dot={false}
              />
              <Area
                type="monotone"
                dataKey="failed"
                stroke="#f87171"
                fill="url(#failedGrad)"
                strokeWidth={2}
                dot={false}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
          <div className="mb-4">
            <h3 className="text-slate-900 dark:text-slate-100">成功率趋势</h3>
            <p className="text-slate-400 text-xs mt-0.5 dark:text-slate-500">基于 `/admin/dashboard/trend` 计算</p>
          </div>
          <div className="mb-4 flex items-center gap-3 rounded-lg border border-emerald-100 bg-emerald-50 p-3 dark:border-emerald-500/20 dark:bg-emerald-500/10">
            <CheckCircle className="w-5 h-5 text-emerald-500" />
            <div>
              <div className="text-emerald-700 text-lg font-semibold dark:text-emerald-300">
                {successRateData.length > 0
                  ? `${(successRateData.reduce((sum, item) => sum + item.rate, 0) / successRateData.length).toFixed(1)}%`
                  : "0.0%"}
              </div>
              <div className="text-emerald-600 text-xs dark:text-emerald-400">平均成功率</div>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={130}>
            <LineChart data={successRateData}>
              <CartesianGrid strokeDasharray="3 3" stroke={isDark ? "#1e293b" : "#f1f5f9"} />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 10, fill: isDark ? "#64748b" : "#94a3b8" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                domain={[0, 100]}
                tick={{ fontSize: 10, fill: isDark ? "#64748b" : "#94a3b8" }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                contentStyle={{
                  border: `1px solid ${isDark ? "#334155" : "#e2e8f0"}`,
                  borderRadius: 8,
                  fontSize: 11,
                  backgroundColor: isDark ? "#0f172a" : "#ffffff",
                  color: isDark ? "#e2e8f0" : "#0f172a",
                }}
                formatter={(value: number) => [`${value}%`, "成功率"]}
              />
              <Line
                type="monotone"
                dataKey="rate"
                stroke="#10b981"
                strokeWidth={2}
                dot={{ r: 3, fill: "#10b981" }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5 xl:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-slate-900 dark:text-slate-100">模型平均延迟</h3>
              <p className="text-slate-400 text-xs mt-0.5 dark:text-slate-500">
                来自 `/admin/dashboard/model-stats` 的 `avgLatencyMs`
              </p>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={latencyData}>
              <CartesianGrid
                strokeDasharray="3 3"
                stroke={isDark ? "#1e293b" : "#f1f5f9"}
                vertical={false}
              />
              <XAxis
                dataKey="model"
                tick={{ fontSize: 10, fill: isDark ? "#64748b" : "#64748b" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tick={{ fontSize: 11, fill: isDark ? "#64748b" : "#94a3b8" }}
                axisLine={false}
                tickLine={false}
                unit="ms"
              />
              <Tooltip
                contentStyle={{
                  border: `1px solid ${isDark ? "#334155" : "#e2e8f0"}`,
                  borderRadius: 8,
                  fontSize: 12,
                  backgroundColor: isDark ? "#0f172a" : "#ffffff",
                  color: isDark ? "#e2e8f0" : "#0f172a",
                }}
                formatter={(value: number) => [`${value}ms`, "平均延迟"]}
              />
              <Bar dataKey="avgLatencyMs" fill="#8b5cf6" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 sm:p-5">
          <div className="mb-4">
            <h3 className="text-slate-900 dark:text-slate-100">Token / 费用统计</h3>
            <p className="text-slate-400 text-xs mt-0.5 dark:text-slate-500">来自 `/admin/dashboard/model-stats`</p>
          </div>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={tokenCostData} layout="vertical">
              <CartesianGrid
                strokeDasharray="3 3"
                stroke={isDark ? "#1e293b" : "#f1f5f9"}
                horizontal={false}
              />
              <XAxis
                type="number"
                tick={{ fontSize: 10, fill: isDark ? "#64748b" : "#94a3b8" }}
                axisLine={false}
                tickLine={false}
                tickFormatter={(value) => `¥${value}`}
              />
              <YAxis
                type="category"
                dataKey="model"
                tick={{ fontSize: 10, fill: isDark ? "#64748b" : "#64748b" }}
                axisLine={false}
                tickLine={false}
                width={80}
              />
              <Tooltip
                contentStyle={{
                  border: `1px solid ${isDark ? "#334155" : "#e2e8f0"}`,
                  borderRadius: 8,
                  fontSize: 11,
                  backgroundColor: isDark ? "#0f172a" : "#ffffff",
                  color: isDark ? "#e2e8f0" : "#0f172a",
                }}
                formatter={(value: number, name: string) => {
                  if (name === "cost") {
                    return [`¥${value.toFixed(2)}`, "费用"];
                  }

                  return [value.toLocaleString(), "Tokens"];
                }}
              />
              <Bar dataKey="cost" fill="#3b82f6" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
