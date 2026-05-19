import type { BillingResponse, RequestLogItemResponse } from "./types";

function parseDate(value: string) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function roundCurrency(value: number) {
  return Number(value.toFixed(4));
}

function formatMonthDay(value: string) {
  const parsed = parseDate(value);
  if (!parsed) {
    return value;
  }

  return `${parsed.getMonth() + 1}/${parsed.getDate()}`;
}

function formatDateTime(value: string) {
  const parsed = parseDate(value);
  if (!parsed) {
    return value;
  }

  const pad = (input: number) => String(input).padStart(2, "0");
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())} ${pad(parsed.getHours())}:${pad(parsed.getMinutes())}:${pad(parsed.getSeconds())}`;
}

function isCurrentMonth(value: string) {
  const parsed = parseDate(value);
  if (!parsed) {
    return false;
  }

  const now = new Date();
  return parsed.getFullYear() === now.getFullYear() && parsed.getMonth() === now.getMonth();
}

export type RequestLogDisplayItem = {
  id: string;
  timestamp: string;
  username: string;
  model: string;
  upstreamModel: string;
  inputTokens: number;
  cachedInputTokens: number;
  outputTokens: number;
  totalTokens: number;
  latency: number;
  status: number;
  userAmount: number;
  costAmount: number;
  requestId: string;
  success: boolean;
};

export function mapRequestLogToDisplayItem(item: RequestLogItemResponse): RequestLogDisplayItem {
  return {
    id: item.requestId,
    timestamp: formatDateTime(item.createdAt),
    username: item.username,
    model: item.modelCode,
    upstreamModel: item.upstreamModel ?? "—",
    inputTokens: item.promptTokens,
    cachedInputTokens: item.cachedPromptTokens ?? 0,
    outputTokens: item.completionTokens,
    totalTokens: item.totalTokens,
    latency: item.latencyMs,
    status: item.statusCode,
    userAmount: item.userAmount,
    costAmount: item.costAmount,
    requestId: item.requestId,
    success: item.success === 1,
  };
}

export function buildBillingResponseFromRequestLogs(
  logs: RequestLogItemResponse[],
  username?: string,
  balance = 0,
): BillingResponse {
  const scopedLogs = username
    ? logs.filter((item) => item.username === username)
    : logs;

  const sortedLogs = [...scopedLogs].sort((left, right) => {
    return (parseDate(right.createdAt)?.getTime() ?? 0) - (parseDate(left.createdAt)?.getTime() ?? 0);
  });

  const monthLogs = sortedLogs.filter((item) => isCurrentMonth(item.createdAt));
  const usageMap = new Map<string, { date: string; cost: number; tokens: number }>();

  for (const item of [...monthLogs].reverse()) {
    const key = formatMonthDay(item.createdAt);
    const existing = usageMap.get(key);
    usageMap.set(key, {
      date: key,
      cost: roundCurrency((existing?.cost ?? 0) + item.userAmount),
      tokens: (existing?.tokens ?? 0) + item.totalTokens,
    });
  }

  const modelMap = new Map<string, number>();
  for (const item of monthLogs) {
    modelMap.set(item.modelCode, roundCurrency((modelMap.get(item.modelCode) ?? 0) + item.userAmount));
  }

  const monthlySpend = roundCurrency(monthLogs.reduce((sum, item) => sum + item.userAmount, 0));
  const totalModelCost = [...modelMap.values()].reduce((sum, item) => sum + item, 0);
  const monthlyBudget = roundCurrency(Math.max(balance + monthlySpend, 0));

  return {
    balance: roundCurrency(balance),
    monthlySpend,
    monthlyBudget,
    usageData: [...usageMap.values()],
    modelUsage: [...modelMap.entries()]
      .sort((left, right) => right[1] - left[1])
      .map(([model, cost]) => ({
        model,
        cost,
        pct: totalModelCost > 0 ? Number(((cost / totalModelCost) * 100).toFixed(1)) : 0,
      })),
    billingHistory: sortedLogs.map((item) => ({
      id: item.requestId,
      type: "消费",
      amount: item.userAmount > 0 ? `-¥${item.userAmount.toFixed(4)}` : "¥0.0000",
      method: item.upstreamModel || item.modelCode,
      time: formatDateTime(item.createdAt),
      status: item.success === 1 ? "成功" : `失败(${item.statusCode})`,
    })),
    rechargeAmounts: [50, 100, 200, 500],
  };
}
