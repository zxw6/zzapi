import { useMemo, useState } from "react";
import {
  Activity,
  AlertCircle,
  CheckCircle,
  ChevronRight,
  Copy,
  Filter,
  RefreshCw,
  Search,
  X,
  XCircle,
} from "lucide-react";
import { useRequestLogsQuery } from "../api/queries";
import { mapRequestLogToDisplayItem, type RequestLogDisplayItem } from "../api/request-log-utils";
import { isAppError } from "../../lib/http/error";
import { PageErrorState, PagePanelSkeleton, PageTableSkeleton } from "../components/ui/feedback";

const statusConfig: Record<
  number,
  { label: string; icon: typeof CheckCircle; color: string; bg: string; accent: string }
> = {
  200: {
    label: "成功",
    icon: CheckCircle,
    color: "text-emerald-600 dark:text-emerald-300",
    bg: "bg-emerald-50 border-emerald-200 dark:bg-emerald-500/10 dark:border-emerald-500/20",
    accent: "from-emerald-500/15 to-transparent dark:from-emerald-400/20 dark:to-transparent",
  },
  429: {
    label: "限流",
    icon: AlertCircle,
    color: "text-orange-600 dark:text-orange-300",
    bg: "bg-orange-50 border-orange-200 dark:bg-orange-500/10 dark:border-orange-500/20",
    accent: "from-orange-500/15 to-transparent dark:from-orange-400/20 dark:to-transparent",
  },
  500: {
    label: "错误",
    icon: XCircle,
    color: "text-red-600 dark:text-red-300",
    bg: "bg-red-50 border-red-200 dark:bg-red-500/10 dark:border-red-500/20",
    accent: "from-red-500/15 to-transparent dark:from-red-400/20 dark:to-transparent",
  },
  401: {
    label: "未授权",
    icon: XCircle,
    color: "text-rose-600 dark:text-rose-300",
    bg: "bg-rose-50 border-rose-200 dark:bg-rose-500/10 dark:border-rose-500/20",
    accent: "from-rose-500/15 to-transparent dark:from-rose-400/20 dark:to-transparent",
  },
};

const statusFilters = ["全部", "200", "429", "500", "401"];

function formatTokenValue(value: number) {
  return value.toLocaleString();
}

function formatCurrency(value: number) {
  return `¥${value.toFixed(4)}`;
}

function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

function formatRequestId(value: string) {
  if (value.length <= 14) {
    return value;
  }

  return `${value.slice(0, 10)}...${value.slice(-4)}`;
}

function buildSummary(logs: RequestLogDisplayItem[]) {
  const total = logs.length;
  const successCount = logs.filter((item) => item.success).length;
  const totalTokens = logs.reduce((sum, item) => sum + item.totalTokens, 0);
  const cachedInputTokens = logs.reduce((sum, item) => sum + item.cachedInputTokens, 0);
  const totalLatency = logs.reduce((sum, item) => sum + item.latency, 0);

  return {
    total,
    successRate: total > 0 ? (successCount / total) * 100 : 0,
    totalTokens,
    cachedInputTokens,
    avgLatency: total > 0 ? Math.round(totalLatency / total) : 0,
  };
}

export function LogsPage() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("全部");
  const [selectedLog, setSelectedLog] = useState<RequestLogDisplayItem | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const { data, isLoading, isError, error, refetch, isFetching } = useRequestLogsQuery(100);

  const logs = useMemo(() => {
    if (!Array.isArray(data)) {
      return [];
    }

    return data.map(mapRequestLogToDisplayItem);
  }, [data]);

  const filteredLogs = useMemo(() => {
    return logs.filter((log) => {
      const keyword = search.trim().toLowerCase();
      const matchSearch =
        !keyword ||
        log.model.toLowerCase().includes(keyword) ||
        log.upstreamModel.toLowerCase().includes(keyword) ||
        log.username.toLowerCase().includes(keyword) ||
        log.requestId.toLowerCase().includes(keyword);
      const matchStatus = statusFilter === "全部" || log.status.toString() === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [logs, search, statusFilter]);

  const summary = useMemo(() => buildSummary(filteredLogs), [filteredLogs]);
  const activeSelectedLog = useMemo(
    () =>
      selectedLog && filteredLogs.some((item) => item.id === selectedLog.id)
        ? selectedLog
        : null,
    [filteredLogs, selectedLog],
  );

  const handleCopy = (text: string, id: string, e?: React.MouseEvent) => {
    e?.stopPropagation();
    navigator.clipboard.writeText(text).catch(() => {});
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const cfg = (status: number) => statusConfig[status] ?? statusConfig[500];

  if (isLoading) {
    return (
      <div className="flex h-full gap-0">
        <div className="flex-1 space-y-0">
          <div className="border-b border-slate-200 bg-white px-6 py-5 dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex items-start justify-between gap-4">
              <div className="space-y-2">
                <div className="h-7 w-32 rounded-xl bg-slate-200/80 dark:bg-slate-800" />
                <div className="h-4 w-72 rounded-lg bg-slate-200/70 dark:bg-slate-800/80" />
              </div>
              <div className="h-9 w-20 rounded-xl bg-slate-200/80 dark:bg-slate-800" />
            </div>
            <div className="grid gap-3 xl:grid-cols-4">
              {Array.from({ length: 4 }).map((_, index) => (
                <div
                  key={index}
                  className="h-24 rounded-2xl border border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-950"
                />
              ))}
            </div>
          </div>
          <PageTableSkeleton
            className="rounded-none border-x-0 border-b-0 shadow-none"
            columns={7}
            rows={8}
          />
        </div>
        <div className="hidden w-80 border-l border-slate-200 bg-white xl:block dark:border-slate-800 dark:bg-slate-900">
          <PagePanelSkeleton className="h-full rounded-none border-0 shadow-none" lines={9} />
        </div>
      </div>
    );
  }

  if (isError) {
    const message = isAppError(error) ? error.message : "请求日志加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="flex h-full bg-slate-50/40 dark:bg-slate-950">
      <div className={`flex min-w-0 flex-col ${activeSelectedLog ? "flex-1" : "w-full"} transition-all`}>
        <div className="border-b border-slate-200 bg-white px-4 py-5 dark:border-slate-800 dark:bg-slate-900 sm:px-6">
          <div className="mb-5 flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div>
              <h1 className="text-slate-900 dark:text-slate-100">请求日志</h1>
              <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
                重点查看 Token 用量、缓存命中、耗时与计费明细
              </p>
            </div>
            <button
              onClick={() => void refetch()}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              <RefreshCw className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`} />
              刷新
            </button>
          </div>

          <div className="mb-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            {[
              {
                label: "请求总数",
                value: summary.total.toLocaleString(),
                helper: "当前筛选结果",
                accent: "from-blue-500/15 to-cyan-500/5 dark:from-blue-400/20 dark:to-cyan-400/5",
              },
              {
                label: "成功率",
                value: formatPercent(summary.successRate),
                helper: "成功 / 全部",
                accent:
                  "from-emerald-500/15 to-lime-500/5 dark:from-emerald-400/20 dark:to-lime-400/5",
              },
              {
                label: "Token 总量",
                value: formatTokenValue(summary.totalTokens),
                helper: "输入 + 输出",
                accent:
                  "from-violet-500/15 to-fuchsia-500/5 dark:from-violet-400/20 dark:to-fuchsia-400/5",
              },
              {
                label: "缓存输入 / 平均耗时",
                value: `${formatTokenValue(summary.cachedInputTokens)} / ${summary.avgLatency.toLocaleString()} ms`,
                helper: "缓存命中输入 / 平均响应",
                accent:
                  "from-amber-500/15 to-orange-500/5 dark:from-amber-400/20 dark:to-orange-400/5",
              },
            ].map((item) => (
              <div
                key={item.label}
                className={`relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-950`}
              >
                <div className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${item.accent}`} />
                <div className="relative">
                  <div className="text-xs uppercase tracking-[0.18em] text-slate-400 dark:text-slate-500">
                    {item.label}
                  </div>
                  <div className="mt-3 text-xl font-semibold text-slate-900 dark:text-slate-100">
                    {item.value}
                  </div>
                  <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">{item.helper}</div>
                </div>
              </div>
            ))}
          </div>

          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative max-w-md flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 dark:text-slate-500" />
              <input
                type="text"
                placeholder="搜索用户、模型、上游模型、请求 ID..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-4 text-sm text-slate-700 placeholder:text-slate-400 focus:border-blue-300 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-sky-500"
              />
            </div>
            <div className="flex flex-wrap items-center gap-1 rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
              {statusFilters.map((filterItem) => (
                <button
                  key={filterItem}
                  onClick={() => setStatusFilter(filterItem)}
                  className={`rounded-lg px-3 py-1.5 text-xs transition-all ${
                    statusFilter === filterItem
                      ? "bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-slate-100"
                      : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-100"
                  }`}
                >
                  {filterItem === "全部" ? "全部状态" : filterItem}
                </button>
              ))}
            </div>
            <div className="ml-auto flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400">
              <Filter className="h-3.5 w-3.5" />
              共 {filteredLogs.length} 条记录
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-auto">
          {filteredLogs.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">
              暂无符合条件的请求日志
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-[1180px] w-full text-sm">
                <thead className="sticky top-0 z-10 border-b border-slate-200 bg-slate-50/95 backdrop-blur dark:border-slate-800 dark:bg-slate-950/95">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      时间
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      用户 / 模型
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      Token 用量(输入/输出)
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      缓存输入
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      状态 / 延迟
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      金额
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 whitespace-nowrap">
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {filteredLogs.map((log) => {
                    const statusItem = cfg(log.status);
                    const StatusIcon = statusItem.icon;
                    const cacheRate = log.inputTokens > 0 ? (log.cachedInputTokens / log.inputTokens) * 100 : 0;

                    return (
                      <tr
                        key={log.id}
                        onClick={() => setSelectedLog(activeSelectedLog?.id === log.id ? null : log)}
                        className={`cursor-pointer align-top transition-all ${
                          activeSelectedLog?.id === log.id
                            ? "bg-blue-50/70 dark:bg-blue-500/10"
                            : "bg-white hover:bg-slate-50 dark:bg-slate-900 dark:hover:bg-slate-900/70"
                        }`}
                      >
                        <td className="px-4 py-4">
                          <div className="font-mono text-[12px] text-slate-700 dark:text-slate-200">
                            {log.timestamp}
                          </div>
                          <div className="mt-1 flex items-center gap-2 text-[11px] text-slate-400 dark:text-slate-500">
                            <span>ID</span>
                            <span className="font-mono">{formatRequestId(log.requestId)}</span>
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <div className="mb-2 inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-[11px] font-medium text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200">
                            {log.username}
                          </div>
                          <div className="text-sm font-semibold text-slate-900 dark:text-slate-100">{log.model}</div>
                          <div className="mt-1 text-[12px] text-slate-500 dark:text-slate-400">
                            上游模型：{log.upstreamModel}
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <div className="rounded-2xl border border-violet-100 bg-violet-50/80 p-3 dark:border-violet-500/20 dark:bg-violet-500/10">
                            <div className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                              {formatTokenValue(log.totalTokens)}
                            </div>
                            <div className="mt-1 text-[12px] text-slate-500 dark:text-slate-400">
                              总 Token
                            </div>
                            <div className="mt-3 grid grid-cols-2 gap-2 text-[12px]">
                              <div className="rounded-xl bg-white/80 px-2.5 py-2 dark:bg-slate-900/70">
                                <div className="text-slate-400 dark:text-slate-500">输入</div>
                                <div className="mt-1 font-medium text-slate-800 dark:text-slate-100">
                                  {formatTokenValue(log.inputTokens)}
                                </div>
                              </div>
                              <div className="rounded-xl bg-white/80 px-2.5 py-2 dark:bg-slate-900/70">
                                <div className="text-slate-400 dark:text-slate-500">输出</div>
                                <div className="mt-1 font-medium text-slate-800 dark:text-slate-100">
                                  {formatTokenValue(log.outputTokens)}
                                </div>
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <div className="rounded-2xl border border-amber-100 bg-amber-50/80 p-3 dark:border-amber-500/20 dark:bg-amber-500/10">
                            <div className="flex items-center gap-2 text-[12px] text-amber-700 dark:text-amber-300">
                              <Activity className="h-3.5 w-3.5" />
                              缓存命中输入
                            </div>
                            <div className="mt-2 text-base font-semibold text-slate-900 dark:text-slate-100">
                              {formatTokenValue(log.cachedInputTokens)}
                            </div>
                            <div className="mt-1 text-[12px] text-slate-500 dark:text-slate-400">
                              占输入 {formatPercent(cacheRate)}
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <div className={`mb-2 inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[12px] ${statusItem.bg} ${statusItem.color}`}>
                            <StatusIcon className="h-3.5 w-3.5" />
                            {log.status} {statusItem.label}
                          </div>
                          <div className={`relative overflow-hidden rounded-2xl border border-slate-200 bg-white px-3 py-3 dark:border-slate-700 dark:bg-slate-950`}>
                            <div className={`pointer-events-none absolute inset-0 bg-gradient-to-r ${statusItem.accent}`} />
                            <div className="relative">
                              <div className="text-[12px] text-slate-400 dark:text-slate-500">响应耗时</div>
                              <div
                                className={`mt-1 text-base font-semibold ${
                                  log.latency > 3000
                                    ? "text-orange-600 dark:text-orange-300"
                                    : "text-slate-900 dark:text-slate-100"
                                }`}
                              >
                                {log.latency.toLocaleString()} ms
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/80">
                            <div className="text-[12px] text-slate-400 dark:text-slate-500">用户扣费</div>
                            <div className="mt-1 text-base font-semibold text-slate-900 dark:text-slate-100">
                              {formatCurrency(log.userAmount)}
                            </div>
                            <div className="mt-3 text-[12px] text-slate-400 dark:text-slate-500">平台成本</div>
                            <div className="mt-1 font-medium text-slate-700 dark:text-slate-200">
                              {formatCurrency(log.costAmount)}
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={(e) => handleCopy(log.requestId, `copy-${log.id}`, e)}
                              title="复制请求 ID"
                              className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:border-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-200"
                            >
                              {copiedId === `copy-${log.id}` ? (
                                <CheckCircle className="h-4 w-4 text-emerald-500" />
                              ) : (
                                <Copy className="h-4 w-4" />
                              )}
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setSelectedLog(log);
                              }}
                              title="查看详情"
                              className="flex h-9 w-9 items-center justify-center rounded-xl border border-blue-200 bg-blue-50 text-blue-600 transition-colors hover:bg-blue-100 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300 dark:hover:bg-blue-500/20"
                            >
                              <ChevronRight className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {activeSelectedLog ? (
        <div className="fixed inset-x-0 bottom-0 z-30 flex max-h-[75vh] flex-col overflow-hidden rounded-t-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-slate-900 md:static md:max-h-none md:w-[22rem] md:flex-shrink-0 md:rounded-none md:border-l md:border-t-0 md:shadow-none">
          <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
            <h3 className="text-slate-900 dark:text-slate-100">请求详情</h3>
            <button
              onClick={() => setSelectedLog(null)}
              className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="flex-1 space-y-5 overflow-auto p-5">
            {(() => {
              const statusItem = cfg(activeSelectedLog.status);
              const StatusIcon = statusItem.icon;
              return (
                <div className={`rounded-2xl border p-4 ${statusItem.bg}`}>
                  <div className="flex items-center gap-3">
                    <StatusIcon className={`h-5 w-5 ${statusItem.color}`} />
                    <div>
                      <div className={`text-sm font-medium ${statusItem.color}`}>
                        {activeSelectedLog.status} {statusItem.label}
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">
                        耗时 {activeSelectedLog.latency.toLocaleString()} ms
                      </div>
                    </div>
                  </div>
                </div>
              );
            })()}

            <div className="space-y-3">
              <h4 className="text-xs uppercase tracking-wider text-slate-700 dark:text-slate-300">
                基本信息
              </h4>
              {[
                { label: "请求 ID", value: activeSelectedLog.requestId, mono: true, copy: true },
                { label: "时间", value: activeSelectedLog.timestamp, mono: true, copy: false },
                { label: "用户", value: activeSelectedLog.username, mono: false, copy: false },
                { label: "是否成功", value: activeSelectedLog.success ? "是" : "否", mono: false, copy: false },
              ].map(({ label, value, mono, copy }) => (
                <div key={label} className="flex items-start justify-between gap-2">
                  <span className="flex-shrink-0 text-xs text-slate-500 dark:text-slate-400">{label}</span>
                  <div className="flex items-center gap-1">
                    <span
                      className={`break-all text-right text-xs text-slate-800 dark:text-slate-100 ${mono ? "font-mono" : ""}`}
                    >
                      {value}
                    </span>
                    {copy ? (
                      <button
                        onClick={() => handleCopy(value, `detail-${label}`)}
                        className="flex-shrink-0 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                      >
                        <Copy className="h-3 w-3" />
                      </button>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>

            <div className="space-y-3">
              <h4 className="text-xs uppercase tracking-wider text-slate-700 dark:text-slate-300">
                模型信息
              </h4>
              {[
                { label: "模型", value: activeSelectedLog.model },
                { label: "上游模型", value: activeSelectedLog.upstreamModel },
                { label: "状态码", value: activeSelectedLog.status.toString() },
                { label: "响应耗时", value: `${activeSelectedLog.latency.toLocaleString()} ms` },
              ].map(({ label, value }) => (
                <div key={label} className="flex items-center justify-between gap-2">
                  <span className="text-xs text-slate-500 dark:text-slate-400">{label}</span>
                  <span className="text-right text-xs font-medium text-slate-800 dark:text-slate-100">
                    {value}
                  </span>
                </div>
              ))}
            </div>

            <div className="space-y-3">
              <h4 className="text-xs uppercase tracking-wider text-slate-700 dark:text-slate-300">
                Token 统计
              </h4>
              {[
                { label: "输入 Token", value: formatTokenValue(activeSelectedLog.inputTokens) },
                { label: "缓存输入", value: formatTokenValue(activeSelectedLog.cachedInputTokens) },
                { label: "输出 Token", value: formatTokenValue(activeSelectedLog.outputTokens) },
                { label: "总 Token", value: formatTokenValue(activeSelectedLog.totalTokens) },
              ].map(({ label, value }) => (
                <div key={label} className="flex items-center justify-between gap-2">
                  <span className="text-xs text-slate-500 dark:text-slate-400">{label}</span>
                  <span className="text-right text-xs font-medium text-slate-800 dark:text-slate-100">
                    {value}
                  </span>
                </div>
              ))}
            </div>

            <div className="space-y-2 rounded-2xl bg-slate-50 p-4 dark:bg-slate-800">
              <h4 className="text-xs uppercase tracking-wider text-slate-700 dark:text-slate-300">
                费用统计
              </h4>
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500 dark:text-slate-400">用户扣费</span>
                <span className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {formatCurrency(activeSelectedLog.userAmount)}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500 dark:text-slate-400">平台成本</span>
                <span className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {formatCurrency(activeSelectedLog.costAmount)}
                </span>
              </div>
            </div>

            <button
              onClick={() =>
                handleCopy(
                  JSON.stringify(activeSelectedLog, null, 2),
                  `payload-${activeSelectedLog.id}`,
                )
              }
              className="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-200 py-2.5 text-sm text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              <Copy className="h-4 w-4" />
              复制请求数据
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
