import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type {
  AccountSettings,
  ApiKeyCreateRequest,
  ApiKeyStatusUpdateRequest,
  DashboardModelStatResponse,
  DashboardOverviewResponse,
  DashboardTrendPointResponse,
  ModelAccessSummaryResponse,
  ModelBatchImportRequest,
  ModelBatchImportResponse,
  ModelCreateRequest,
  ModelGroupCreateRequest,
  ModelPackagePurchaseRecordResponse,
  ModelListItemResponse,
  ModelUpdateRequest,
  ProviderCreateRequest,
  ProviderListItemResponse,
  ProviderStatusUpdateRequest,
  PurchaseModelPackageRequest,
  RequestLogItemResponse,
  SiteSettingsResponse,
  SiteSettingsUpdateRequest,
  UpstreamModelOptionResponse,
  UserCreateRequest,
  UserKey,
  UserStatusUpdateRequest,
  UserUpdateRequest,
  WalletTransactionItemResponse,
  WalletRechargeRequest,
} from "./types";
import { apiKeysApi } from "./modules/api-keys";
import { consoleApi } from "./modules/console";
import { dashboardApi } from "./modules/dashboard";
import { modelAccessApi } from "./modules/model-access";
import { modelsApi } from "./modules/models";
import { providersApi } from "./modules/providers";
import { requestLogsApi } from "./modules/request-logs";
import { systemApi } from "./modules/system";
import { buildBillingResponseFromRequestLogs } from "./request-log-utils";
import { usersApi } from "./modules/users";

export const queryKeys = {
  apiKeys: ["admin", "api-keys"] as const,
  users: ["admin", "users"] as const,
  userDetail: (userId: string) => ["admin", "users", userId] as const,
  providers: ["admin", "providers"] as const,
  dashboardOverview: (scopeKey: string) => ["dashboard", "overview", scopeKey] as const,
  adminDashboardTrend: (days: number) => ["admin", "dashboard", "trend", days] as const,
  dashboardModelStats: (scopeKey: string) => ["dashboard", "model-stats", scopeKey] as const,
  requestLogs: (limit: number) => ["admin", "request-logs", limit] as const,
  modelAccessPurchases: ["admin", "model-access", "purchases"] as const,
  walletTransactions: ["admin", "model-access", "wallet-transactions"] as const,
  siteSettings: ["admin", "system", "site-settings"] as const,
  overview: ["console", "overview"] as const,
  keys: ["console", "keys"] as const,
  models: ["console", "models"] as const,
  modelAccess: ["console", "model-access"] as const,
  billing: (username: string, balance: number) =>
    ["console", "billing", username, balance] as const,
  account: ["console", "account"] as const,
};

export function useOverviewQuery() {
  return useQuery({
    queryKey: queryKeys.overview,
    queryFn: consoleApi.getOverview,
  });
}

export function useApiKeysQuery() {
  return useQuery({
    queryKey: queryKeys.apiKeys,
    queryFn: apiKeysApi.list,
  });
}

export function useUsersQuery() {
  return useQuery({
    queryKey: queryKeys.users,
    queryFn: usersApi.list,
  });
}

export function useProvidersQuery() {
  return useQuery({
    queryKey: queryKeys.providers,
    queryFn: (): Promise<ProviderListItemResponse[]> => providersApi.list(),
  });
}

export function useAdminDashboardTrendQuery(days: number) {
  return useQuery({
    queryKey: queryKeys.adminDashboardTrend(days),
    queryFn: (): Promise<DashboardTrendPointResponse[]> => dashboardApi.trend(days),
  });
}

export function useDashboardOverviewQuery(scopeKey = "default") {
  return useQuery({
    queryKey: queryKeys.dashboardOverview(scopeKey),
    queryFn: (): Promise<DashboardOverviewResponse> => dashboardApi.overview(),
  });
}

export function useDashboardModelStatsQuery(scopeKey = "default", enabled = true) {
  return useQuery({
    queryKey: queryKeys.dashboardModelStats(scopeKey),
    queryFn: (): Promise<DashboardModelStatResponse[]> => dashboardApi.modelStats(),
    enabled,
  });
}

export function useRequestLogsQuery(limit = 20) {
  return useQuery({
    queryKey: queryKeys.requestLogs(limit),
    queryFn: (): Promise<RequestLogItemResponse[]> => requestLogsApi.list(limit),
  });
}

export function useSiteSettingsQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.siteSettings,
    queryFn: (): Promise<SiteSettingsResponse> => systemApi.getSiteSettings(),
    enabled,
  });
}

export function useUserDetailQuery(userId: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.userDetail(userId),
    queryFn: () => usersApi.detail(userId),
    enabled: enabled && Boolean(userId),
  });
}

export function useKeysQuery() {
  return useQuery({
    queryKey: queryKeys.keys,
    queryFn: consoleApi.getKeys,
  });
}

export function useModelsQuery() {
  return useQuery({
    queryKey: queryKeys.models,
    queryFn: (): Promise<ModelListItemResponse[]> => modelsApi.list(),
  });
}

export function useModelAccessSummaryQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.modelAccess,
    queryFn: (): Promise<ModelAccessSummaryResponse> => modelAccessApi.summary(),
    enabled,
  });
}

export function useModelAccessPurchasesQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.modelAccessPurchases,
    queryFn: (): Promise<ModelPackagePurchaseRecordResponse[]> => modelAccessApi.purchases(),
    enabled,
  });
}

export function useWalletTransactionsQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.walletTransactions,
    queryFn: (): Promise<WalletTransactionItemResponse[]> => modelAccessApi.walletTransactions(),
    enabled,
  });
}

export function useBillingQuery(username?: string, balance = 0, enabled = true) {
  return useQuery({
    queryKey: queryKeys.billing(username ?? "", balance),
    queryFn: (): Promise<RequestLogItemResponse[]> => requestLogsApi.list(50),
    enabled: enabled && Boolean(username),
    select: (logs) => buildBillingResponseFromRequestLogs(logs, username, balance),
  });
}

export function useAccountQuery() {
  return useQuery({
    queryKey: queryKeys.account,
    queryFn: consoleApi.getAccount,
  });
}

export function useCreateApiKeyMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ApiKeyCreateRequest) => apiKeysApi.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.apiKeys });
    },
  });
}

export function useDeleteApiKeyMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => apiKeysApi.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.apiKeys });
      void queryClient.invalidateQueries({ queryKey: queryKeys.keys });
      void queryClient.invalidateQueries({ queryKey: queryKeys.overview });
    },
  });
}

export function useUpdateSiteSettingsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: SiteSettingsUpdateRequest) => systemApi.updateSiteSettings(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.siteSettings });
    },
  });
}

export function useUpdateApiKeyStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ApiKeyStatusUpdateRequest }) =>
      apiKeysApi.updateStatus(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.apiKeys });
    },
  });
}

export function useCreateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: UserCreateRequest) => usersApi.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}

export function useCreateProviderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ProviderCreateRequest) => providersApi.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.providers });
    },
  });
}

export function useUpdateProviderStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      providerId,
      input,
    }: {
      providerId: string;
      input: ProviderStatusUpdateRequest;
    }) => providersApi.updateStatus(providerId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.providers });
    },
  });
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, input }: { userId: string; input: UserUpdateRequest }) =>
      usersApi.update(userId, input),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.users });
      void queryClient.invalidateQueries({ queryKey: queryKeys.userDetail(variables.userId) });
    },
  });
}

export function useUpdateUserStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, input }: { userId: string; input: UserStatusUpdateRequest }) =>
      usersApi.updateStatus(userId, input),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.users });
      void queryClient.invalidateQueries({ queryKey: queryKeys.userDetail(variables.userId) });
    },
  });
}

export function useDeleteUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userId: string) => usersApi.remove(userId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}

export function useRechargeUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: WalletRechargeRequest) => usersApi.recharge(input),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.users });
      void queryClient.invalidateQueries({ queryKey: queryKeys.userDetail(variables.userId) });
    },
  });
}

export function usePurchaseModelPackageMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: PurchaseModelPackageRequest) => modelAccessApi.purchase(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.modelAccess });
      void queryClient.invalidateQueries({ queryKey: queryKeys.modelAccessPurchases });
      void queryClient.invalidateQueries({ queryKey: queryKeys.walletTransactions });
      void queryClient.invalidateQueries({ queryKey: queryKeys.users });
      void queryClient.invalidateQueries({ queryKey: queryKeys.overview });
      void queryClient.invalidateQueries({ queryKey: queryKeys.billing("", 0).slice(0, 2) });
    },
  });
}

export function useCreateModelAccessGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ModelGroupCreateRequest) => modelAccessApi.createGroup(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.modelAccess });
    },
  });
}

export function useRemoveModelAccessGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (groupId: string) => modelAccessApi.remove(groupId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.modelAccess });
    },
  });
}

export function useCreateModelMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ModelCreateRequest) => modelsApi.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.models });
    },
  });
}

export function useUpdateModelMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ModelUpdateRequest }) =>
      modelsApi.update(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.models });
    },
  });
}

export function useFetchUpstreamModelsMutation() {
  return useMutation({
    mutationFn: (providerId: string): Promise<UpstreamModelOptionResponse[]> =>
      modelsApi.upstream(providerId),
  });
}

export function useImportModelsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: ModelBatchImportRequest): Promise<ModelBatchImportResponse> =>
      modelsApi.importBatch(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.models });
    },
  });
}

export function useCreateKeyMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: consoleApi.createKey,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.keys });
    },
  });
}

export function useUpdateKeyMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ keyId, input }: { keyId: string; input: Partial<UserKey> }) =>
      consoleApi.updateKey(keyId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.keys });
    },
  });
}

export function useDeleteKeyMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keyId: string) => consoleApi.deleteKey(keyId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.keys });
    },
  });
}

export function useUpdateProfileMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (profile: AccountSettings["profile"]) => consoleApi.updateProfile(profile),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.account });
    },
  });
}

export function useUpdateNotificationsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notifications: AccountSettings["notifications"]) =>
      consoleApi.updateNotifications(notifications),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.account });
    },
  });
}
