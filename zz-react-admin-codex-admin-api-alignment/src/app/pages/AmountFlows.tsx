import { useMemo, useState } from "react";
import { ArrowDownRight, ArrowUpRight, Landmark, Receipt, Search, Wallet } from "lucide-react";
import { useWalletTransactionsQuery } from "../api/queries";
import type { WalletTransactionItemResponse } from "../api/types";
import { isAppError } from "../../lib/http/error";
import { PageEmptyState, PageErrorState, PageTableSkeleton } from "../components/ui/feedback";

const flowFilters = [
  { id: "all", label: "全部流水" },
  { id: "RECHARGE", label: "充值入账" },
  { id: "PACKAGE_BUY", label: "套餐购买" },
  { id: "CONSUME", label: "模型消耗" },
] as const;

function formatAmount(value: number, direction: string) {
  const normalized = Number(value ?? 0);
  const prefix = direction === "IN" ? "+" : "-";
  return `${prefix}¥ ${normalized.toFixed(2)}`;
}

function formatBalance(value: number | null | undefined) {
  return `¥ ${Number(value ?? 0).toFixed(2)}`;
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

function getFlowTone(type: string) {
  switch (type) {
    case "RECHARGE":
      return {
        label: "充值入账",
        className:
          "border-emerald-200 bg-emerald-50 text-emerald-600 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-300",
        icon: ArrowUpRight,
      };
    case "PACKAGE_BUY":
      return {
        label: "套餐购买",
        className:
          "border-violet-200 bg-violet-50 text-violet-600 dark:border-violet-500/20 dark:bg-violet-500/10 dark:text-violet-300",
        icon: Wallet,
      };
    case "CONSUME":
      return {
        label: "模型消耗",
        className:
          "border-blue-200 bg-blue-50 text-blue-600 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300",
        icon: ArrowDownRight,
      };
    default:
      return {
        label: type || "未知类型",
        className:
          "border-amber-200 bg-amber-50 text-amber-600 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300",
        icon: Landmark,
      };
  }
}

export function AmountFlowsPage() {
  const [keyword, setKeyword] = useState("");
  const [flowFilter, setFlowFilter] = useState<(typeof flowFilters)[number]["id"]>("all");
  const { data, isLoading, isError, error, refetch } = useWalletTransactionsQuery();

  const records = useMemo(() => data ?? [], [data]);

  const filteredRecords = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    return records.filter((item) => {
      const matchKeyword =
        !normalizedKeyword ||
        item.username.toLowerCase().includes(normalizedKeyword) ||
        item.orderNo.toLowerCase().includes(normalizedKeyword) ||
        item.descriptionText.toLowerCase().includes(normalizedKeyword) ||
        item.transactionType.toLowerCase().includes(normalizedKeyword);
      const matchType = flowFilter === "all" || item.transactionType === flowFilter;
      return matchKeyword && matchType;
    });
  }, [flowFilter, keyword, records]);

  const summary = useMemo(() => {
    return {
      inflow: records
        .filter((item) => item.direction === "IN")
        .reduce((sum, item) => sum + Number(item.amount ?? 0), 0),
      outflow: records
        .filter((item) => item.direction === "OUT")
        .reduce((sum, item) => sum + Number(item.amount ?? 0), 0),
      purchaseCount: records.filter((item) => item.transactionType === "PACKAGE_BUY").length,
      consumeCount: records.filter((item) => item.transactionType === "CONSUME").length,
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
    const message = isAppError(error) ? error.message : "余额流水加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">金额流水</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            已切换到 `/admin/model-access/wallet-transactions`，展示充值、套餐购买与模型扣费流水
          </p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs text-slate-500 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-400">
          实时接口字段：方向 / 金额 / 变动前后余额 / 订单号
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">累计入账</div>
            <Wallet className="h-4 w-4 text-emerald-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-emerald-600">
            {formatBalance(summary.inflow)}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">direction = IN</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">累计出账</div>
            <Receipt className="h-4 w-4 text-blue-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-blue-600">
            {formatBalance(summary.outflow)}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">direction = OUT</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">套餐购买笔数</div>
            <ArrowDownRight className="h-4 w-4 text-violet-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-violet-600">
            {summary.purchaseCount}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">transactionType = PACKAGE_BUY</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between">
            <div className="text-xs text-slate-500 dark:text-slate-400">模型扣费笔数</div>
            <Landmark className="h-4 w-4 text-amber-500" />
          </div>
          <div className="mt-2 text-2xl font-semibold text-amber-600">
            {summary.consumeCount}
          </div>
          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">transactionType = CONSUME</div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          <div className="relative w-full max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 dark:text-slate-500" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索用户名、订单号、类型、描述"
              className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2 pl-9 pr-4 text-sm text-slate-700 placeholder:text-slate-400 focus:border-blue-300 focus:bg-white focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-sky-500"
            />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {flowFilters.map((item) => (
              <button
                key={item.id}
                onClick={() => setFlowFilter(item.id)}
                className={`rounded-lg px-3 py-1.5 text-sm transition-all ${
                  flowFilter === item.id
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
          title="暂无余额流水"
          description="当前筛选条件下没有匹配到钱包流水记录。"
        />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-5 py-3 dark:border-slate-800 dark:bg-slate-950">
            <div className="text-xs text-slate-500 dark:text-slate-400">共 {filteredRecords.length} 条金额流水</div>
            <div className="text-xs text-slate-400 dark:text-slate-500">来源：/admin/model-access/wallet-transactions</div>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full text-sm">
              <thead className="border-b border-slate-100 bg-white dark:border-slate-800 dark:bg-slate-900">
                <tr className="text-left text-xs text-slate-500 dark:text-slate-400">
                  <th className="px-5 py-3 font-medium">用户 / 钱包</th>
                  <th className="px-4 py-3 font-medium">类型 / 方向</th>
                  <th className="px-4 py-3 font-medium">金额</th>
                  <th className="px-4 py-3 font-medium">变动前后</th>
                  <th className="px-4 py-3 font-medium">订单 / 状态</th>
                  <th className="px-4 py-3 font-medium">时间 / 描述</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredRecords.map((item: WalletTransactionItemResponse) => {
                  const tone = getFlowTone(item.transactionType);
                  const ToneIcon = tone.icon;
                  const isInflow = item.direction === "IN";

                  return (
                    <tr key={String(item.id)}>
                      <td className="px-5 py-4">
                        <div className="text-sm font-medium text-slate-800 dark:text-slate-100">{item.username}</div>
                        <div className="mt-1 font-mono text-[11px] text-slate-400 dark:text-slate-500">
                          walletId: {item.walletId}
                        </div>
                      </td>
                      <td className="px-4 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs ${tone.className}`}
                        >
                          <ToneIcon className="h-3 w-3" />
                          {tone.label}
                        </span>
                        <div className="mt-2 text-xs text-slate-400 dark:text-slate-500">
                          方向：{item.direction}
                        </div>
                      </td>
                      <td
                        className={`px-4 py-4 text-sm font-semibold ${
                          isInflow ? "text-emerald-600" : "text-slate-800 dark:text-slate-100"
                        }`}
                      >
                        {formatAmount(item.amount, item.direction)}
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        <div>变动前：{formatBalance(item.balanceBefore)}</div>
                        <div className="mt-1">变动后：{formatBalance(item.balanceAfter)}</div>
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        <div className="font-mono text-[11px] text-slate-500 dark:text-slate-400">
                          {item.orderNo}
                        </div>
                        <div className="mt-1">状态：{item.status}</div>
                      </td>
                      <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                        <div>{formatDateTime(item.createdAt)}</div>
                        <div className="mt-1 text-slate-400 dark:text-slate-500">{item.descriptionText}</div>
                        <div className="mt-1 text-slate-400 dark:text-slate-500">
                          交易日：{item.transactionDate}
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
