import { env } from "../../../lib/config/env";
import { apiKeysApi } from "./api-keys";
import { dashboardApi } from "./dashboard";
import { request } from "../../../lib/http/client";
import type {
  AccountSettings,
  BillingResponse,
  OverviewResponse,
  UserKey,
} from "../types";
import { CONSOLE_API_ENDPOINTS } from "../endpoints";

export const consoleApi = {
  getOverview: async (): Promise<OverviewResponse> => {
    const [overview, trend, modelStats, apiKeys] = await Promise.all([
      dashboardApi.overview(),
      dashboardApi.trend(7),
      dashboardApi.modelStats(),
      apiKeysApi.list(),
    ]);

    const activeKeys = apiKeys.filter((item) => {
      const status = item.status.trim().toUpperCase();
      return status === "ACTIVE" || status === "ENABLED";
    }).length;

    return {
      stats: {
        balance: overview.walletBalanceTotal,
        requestCountToday: overview.requestCountToday,
        totalTokensToday: overview.totalTokensToday,
        totalTokens7d: overview.totalTokens7d,
        rechargeAmountToday: overview.rechargeAmountToday,
        consumeAmountToday: overview.consumeAmountToday,
        activeKeys,
        apiKeyCount: apiKeys.length,
        providerCount: overview.providerCount,
        modelCount: overview.modelCount,
      },
      trend,
      modelStats,
      endpoint: env.apiBaseUrl.replace(/\/$/, ""),
    };
  },

  getKeys: () => request<UserKey[]>(CONSOLE_API_ENDPOINTS.keys),

  createKey: (input: { name: string; rateLimit?: number; budget?: number | null }) =>
    request<UserKey>(CONSOLE_API_ENDPOINTS.keys, {
      method: "POST",
      body: input,
    }),

  updateKey: (keyId: string, input: Partial<UserKey>) =>
    request<UserKey>(CONSOLE_API_ENDPOINTS.keyDetail(keyId), {
      method: "PATCH",
      body: input,
    }),

  deleteKey: (keyId: string) =>
    request<{ ok: boolean }>(CONSOLE_API_ENDPOINTS.keyDetail(keyId), {
      method: "DELETE",
    }),

  getBilling: () => request<BillingResponse>(CONSOLE_API_ENDPOINTS.billing),

  getAccount: () => request<AccountSettings>(CONSOLE_API_ENDPOINTS.account),

  updateProfile: (profile: AccountSettings["profile"]) =>
    request<AccountSettings>(CONSOLE_API_ENDPOINTS.accountProfile, {
      method: "PUT",
      body: profile,
    }),

  updateNotifications: (notifications: AccountSettings["notifications"]) =>
    request<AccountSettings>(CONSOLE_API_ENDPOINTS.accountNotifications, {
      method: "PUT",
      body: notifications,
    }),
};
