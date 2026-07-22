import { useMemo, useState } from "react";
import { CalendarRange, Package, ReceiptText, Search, ShieldCheck, TimerReset } from "lucide-react";
import { useModelAccessPurchasesQuery } from "../api/queries";
import { isAppError } from "../../lib/http/error";
import { PageEmptyState, PageErrorState, PageTableSkeleton } from "../components/ui/feedback";

const statusFilters = [
  { id: "all", label: "全部记录" },
  { id: "ACTIVE", label: "生效中" },
  { id: "EXPIRED", label: "已过期" },
  { id: "DELETED", label: "已删除" },
] as const;

function formatCurrency(value: number | null | undefined) {
  return `¥ ${Number(value ?? 0).toFixed(2)}`;
}

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString();
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "—";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  const pad = (input: number) => String(input).padStart(2, "0");
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())} ${pad(parsed.getHours())}:${pad(parsed.getMinutes())}:${pad(parsed.getSeconds())}`;
}

function getStatusTone(status: string) {
  switch (status) {
    case "ACTIVE":
      return {
        label: "生效中",
        className:
          "border-emerald-200 bg-emerald-50 text-emerald-600 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-300",
      };
    case "EXPIRED":
      return {
        label: "已过期",
        className:
          "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300",
      };
    case "DELETED":
      return {
        label: "已删除",
        className:
          "border-rose-200 bg-rose-50 text-rose-600 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-300",
      };
    default:
      return {
        label: status || "未知",
        className:
          "border-amber-200 bg-amber-50 text-amber-600 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300",
      };
  }
}

function getPackageTypeLabel(type?: string, text?: string) {
  if (type === "BALANCE") {
    return text || "余额套餐";
  }

  return text || "普通套餐";
}

export function PackagePurchasesPage() {
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<(typeof statusFilters)[number]["id"]>("all");
  const { data, isLoading, isError, error, refetch } = useModelAccessPurchasesQuery();

  const records = useMemo(() => data ?? [], [data]);

  const filteredRecords = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    return records.filter((item) => {
      const matchKeyword =
        !normalizedKeyword ||
        item.username.toLowerCase().includes(normalizedKeyword) ||
        item.groupName.toLowerCase().includes(normalizedKeyword) ||
        item.groupCode.toLowerCase().includes(normalizedKeyword) ||
        String(item.id).toLowerCase().includes(normalizedKeyword);
      const matchStatus = statusFilter === "all" || item.status === statusFilter;
      return matchKeyword && matchStatus;
    });
  }, [keyword, records, statusFilter]);

  const summary = useMemo(() => {
    const activeRecords = records.filter((item) => item.status === "ACTIVE");
    const expiringSoonRecords = activeRecords.filter((item) => (item.remainingDays ?? 9999) <= 7);

    return {
      totalAmount: records.reduce((sum, item) => sum + Number(item.purchasePrice ?? 0), 0),
      activeCount: activeRecords.length,
      expiringSoonCount: expiringSoonRecords.length,
      totalUsed: records.reduce((sum, item) => sum + Number(item.totalUsed ?? 0), 0),
    };
  }, [records]);

  if (isLoading) {
    return (
      <div className="space-y-6 p-4 sm:p-6">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div
              key={index}
              className="h-28 rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
        <PageTableSkeleton columns={6} rows={6} />
      </div>
    );
  }

  if (isError) {
    const message = isAppError(error) ? error.message : "套餐购买记录加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">套餐购买记录</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            已切换到 `/admin/model-access/purchases`，展示用户套餐订购、生效与额度使用情况
          </p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs text-slate-500 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-400">
          实时接口字段：价格 / 生效时间 / 到期时间 / 配额使用
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">累计购买金额</div>
            <ReceiptText className="h-4 w-4 text-blue-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">
            {formatCurrency(summary.totalAmount)}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">所有套餐购买记录累计</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">当前生效套餐</div>
            <ShieldCheck className="h-4 w-4 text-emerald-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-emerald-600">{summary.activeCount}</div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">状态为 ACTIVE 的套餐</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">7 天内到期</div>
            <TimerReset className="h-4 w-4 text-amber-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-amber-600">
            {summary.expiringSoonCount}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">便于管理员跟进续费</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">累计已用额度</div>
            <Package className="h-4 w-4 text-violet-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-violet-600">
            {formatNumber(summary.totalUsed)}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">按记录汇总 totalUsed</div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          <div className="relative w-full max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 dark:text-slate-500" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索记录 ID、用户名、套餐编码、套餐名称"
              className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-4 text-sm text-slate-700 placeholder:text-slate-400 focus:border-blue-300 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-sky-500"
            />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {statusFilters.map((item) => (
              <button
                key={item.id}
                onClick={() => setStatusFilter(item.id)}
                className={`rounded-lg px-3 py-1.5 text-sm transition-all ${
                  statusFilter === item.id
                    ? "bg-slate-900 text-white shadow-sm dark:bg-slate-700"
                    : "bg-slate-100 text-slate-500 hover:text-slate-700 dark:bg-slate-800 dark:text-slate-400 dark:hover:text-slate-100"
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {filteredRecords.length === 0 ? (
        <PageEmptyState
          title="暂无套餐购买记录"
          description="当前筛选条件下没有匹配到套餐购买记录。"
        />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-5 py-3 dark:border-slate-800 dark:bg-slate-950">
            <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <CalendarRange className="h-4 w-4" />
              共 {filteredRecords.length} 条套餐购买记录
            </div>
            <div className="text-xs text-slate-400 dark:text-slate-500">来源：/admin/model-access/purchases</div>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-[1120px] w-full text-sm">
              <thead className="border-b border-slate-100 bg-white dark:border-slate-800 dark:bg-slate-900">
                <tr className="text-left text-xs text-slate-500 dark:text-slate-400">
                  <th className="px-5 py-3 font-medium">用户 / 记录</th>
                  <th className="px-4 py-3 font-medium">套餐信息</th>
                  <th className="px-4 py-3 font-medium">价格 / 模型数</th>
                  <th className="px-4 py-3 font-medium">额度</th>
                  <th className="px-4 py-3 font-medium">使用情况</th>
                  <th className="px-4 py-3 font-medium">生效周期</th>
                  <th className="px-4 py-3 font-medium">状态</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredRecords.map((item) => {
                  const statusTone = getStatusTone(item.status);

                  return (
                    <tr key={String(item.id)}>
                      <td className="px-5 py-4">
                        <div className="text-sm font-medium text-slate-800 dark:text-slate-100">{item.username}</div>
                        <div className="mt-1 font-mono text-[11px] text-slate-400 dark:text-slate-500">
                          记录 ID：{item.id}
                        </div>
                      </td>
                      <td className="px-4 py-4">
                        <div className="inline-flex items-center gap-2 text-sm font-medium text-slate-800 dark:text-slate-100">
                          <Package className="h-4 w-4 text-blue-500" />
                          {item.groupName}
                        </div>
                        <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                          {item.groupCode} · {getPackageTypeLabel(item.packageType, item.packageTypeText)}
                        </div>
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        <div className="text-sm font-semibold text-slate-800 dark:text-slate-100">
                          {formatCurrency(item.purchasePrice)}
                        </div>
                        <div className="mt-1">
                          {item.packageType === "BALANCE" ? "开通后按余额实时扣费" : `模型数：${item.modelCount}`}
                        </div>
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        {item.packageType === "BALANCE" ? (
                          <>
                            <div>购买门槛：余额 &gt;= $1.00</div>
                            <div className="mt-1">调用门槛：余额 &gt;= $0.50</div>
                            <div className="mt-1">不写购买扣费流水</div>
                          </>
                        ) : (
                          <>
                            <div>日额度：{formatNumber(item.dailyQuota)}</div>
                            <div className="mt-1">周额度：{formatNumber(item.weeklyQuota)}</div>
                            <div className="mt-1">月额度：{formatNumber(item.monthlyQuota)}</div>
                          </>
                        )}
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        <div>当日已用：{formatNumber(item.dailyUsed)}</div>
                        <div className="mt-1">当月已用：{formatNumber(item.monthlyUsed)}</div>
                        <div className="mt-1">累计已用：{formatNumber(item.totalUsed)}</div>
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        <div>创建：{formatDateTime(item.createdAt)}</div>
                        <div className="mt-1">生效：{formatDateTime(item.startAt)}</div>
                        <div className="mt-1">到期：{formatDateTime(item.expiresAt)}</div>
                      </td>
                      <td className="px-4 py-4">
                        <span
                          className={`inline-flex rounded-full border px-2 py-1 text-xs ${statusTone.className}`}
                        >
                          {statusTone.label}
                        </span>
                        <div className="mt-2 text-xs text-slate-400 dark:text-slate-500">
                          剩余 {item.remainingDays ?? 0} 天
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
