export type UserProfile = {
  id: string;
  username: string;
  nickname: string;
  email?: string;
  phone?: string;
  company?: string;
  bio?: string;
  role: "user" | "admin";
  roleCode: "USER" | "ADMIN";
  balance?: number;
};

export type AuthSession = {
  accessToken: string;
  user: UserProfile;
};

export type LoginInput = {
  username: string;
  password: string;
  captchaId: string;
  captchaCode: string;
};

export type LoginCaptchaResponse = {
  captchaId: string;
  imageBase64: string;
  expireSeconds: number;
};

export type SendRegisterCodeRequest = {
  email: string;
};

export type VerificationCodeSendResponse = {
  email: string;
  expireSeconds: number;
};

export type PasswordResetCodeRequest = {
  email: string;
};

export type PasswordResetRequest = {
  email: string;
  verificationCode: string;
  newPassword: string;
};

export type RegisterInput = {
  username: string;
  email: string;
  password: string;
  verificationCode: string;
  nickname?: string;
  phone?: string;
};

export type ApiKeyListItemResponse = {
  id: string;
  userId: string;
  username: string;
  name: string;
  accessKey: string;
  status: string;
  modelGroupId: string | null;
  modelGroupName: string | null;
  totalQuota: number | null;
  usedQuota: number | null;
  expiresAt: string | null;
  lastUsedAt: string | null;
  createdAt: string;
};

export type ApiKeyCreateRequest = {
  userId: string;
  name: string;
  modelGroupId?: string;
  expiresAt?: string;
  remark?: string;
};

export type ApiKeyCreateResponse = {
  id: string;
  plainTextKey: string;
};

export type ApiKeyStatusUpdateRequest = {
  status: string;
};

export type UpstreamModelOptionResponse = {
  id: string;
  displayName: string;
  ownedBy: string | null;
  providerType: string | null;
};

export type UserListItemResponse = {
  id: string;
  username: string;
  nickname: string;
  roleCode: string;
  status: string;
  email: string | null;
  phone: string | null;
  balance: number | null;
  lastLoginAt: string | null;
  createdAt: string;
};

export type UserCreateRequest = {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  phone?: string;
  roleCode?: string;
  initialBalance?: number;
};

export type UserUpdateRequest = {
  nickname?: string;
  email?: string;
  phone?: string;
  roleCode?: string;
  status?: string;
  password?: string;
};

export type UserStatusUpdateRequest = {
  status: string;
};

export type WalletRechargeRequest = {
  userId: string;
  amount: number;
  remark?: string;
};

export type ProviderListItemResponse = {
  id: string;
  providerCode: string;
  providerName: string;
  baseUrl: string;
  providerType: string | null;
  status: string;
  priorityNo: number | null;
  timeoutMs: number | null;
  tokenCount: number | null;
  createdAt: string | null;
};

export type ProviderCreateRequest = {
  providerCode: string;
  providerName: string;
  baseUrl: string;
  providerType?: string;
  priorityNo?: number;
  timeoutMs?: number;
  remark?: string;
  tokenName?: string;
  tokenValue?: string;
  weightNo?: number;
  rpmLimit?: number;
  tpmLimit?: number;
};

export type ProviderStatusUpdateRequest = {
  status: string;
};

export type ModelListItemResponse = {
  id: string;
  bindingId?: string | null;
  modelCode: string;
  modelName: string;
  modelType: string | null;
  billingType: string | null;
  promptPrice: number | null;
  cachedPromptPrice: number | null;
  completionPrice: number | null;
  requestPrice: number | null;
  multiplier: number | null;
  isPublic: number | boolean | null;
  status: string;
  groupId?: string | null;
  groupCode?: string | null;
  groupName?: string | null;
  groups?: Array<{
    groupId?: string | null;
    groupCode?: string | null;
    groupName?: string | null;
  }>;
  providerId: string | null;
  providerName: string | null;
  providerType: string | null;
  upstreamModel: string | null;
  createdAt: string | null;
};

export type ModelCreateRequest = {
  modelCode: string;
  modelName: string;
  modelType?: string;
  billingType?: string;
  promptPrice?: number;
  cachedPromptPrice?: number;
  completionPrice?: number;
  requestPrice?: number;
  imagePrice?: number;
  multiplier?: number;
  isPublic?: boolean;
  groupId: string;
  providerId: string;
  upstreamModel: string;
};

export type ModelUpdateRequest = {
  bindingId?: string;
  modelName: string;
  modelType?: string;
  billingType?: string;
  promptPrice?: number;
  cachedPromptPrice?: number;
  completionPrice?: number;
  requestPrice?: number;
  multiplier?: number;
  isPublic?: boolean;
  groupId: string;
  providerId: string;
  upstreamModel: string;
};

export type ModelBatchImportRequest = {
  groupId: string;
  providerId: string;
  upstreamModels: string[];
  promptPrice?: number;
  cachedPromptPrice?: number;
  completionPrice?: number;
  multiplier?: number;
  isPublic?: boolean;
};

export type ModelBatchImportResponse = {
  importedCount: number;
  skippedCount: number;
  importedModels: string[];
  skippedModels: string[];
};

export type OverviewTrendPoint = {
  day: string;
  requests: number;
  tokens: number;
};

export type RecentRequest = {
  id: string;
  model: string;
  tokens: number;
  latency: number;
  status: number;
  time: string;
};

export type ServiceStatus = {
  name: string;
  status: string;
  latency: string;
};

export type OverviewStats = {
  balance: number;
  requestCountToday: number;
  totalTokensToday: number;
  totalTokens7d: number;
  rechargeAmountToday: number;
  consumeAmountToday: number;
  activeKeys: number;
  apiKeyCount: number;
  providerCount: number;
  modelCount: number;
};

export type DashboardOverviewResponse = {
  userCount: number;
  apiKeyCount: number;
  providerCount: number;
  modelCount: number;
  requestCountToday: number;
  totalTokensToday: number;
  totalTokens7d: number;
  onlineUserCount: number;
  todayActiveUserCount: number;
  rechargeAmountToday: number;
  consumeAmountToday: number;
  walletBalanceTotal: number;
};

export type DashboardTrendPointResponse = {
  statDate: string;
  requestCount: number;
  successCount: number;
  totalTokens: number;
  userAmount: number;
  costAmount: number;
};

export type DashboardModelStatResponse = {
  modelCode: string;
  upstreamModels: string;
  requestCount: number;
  totalTokens: number;
  avgLatencyMs: number;
  totalLatencyMs: number;
  successRate: number;
  userAmount: number;
};

export type RequestLogItemResponse = {
  requestId: string;
  username: string;
  modelCode: string;
  upstreamModel: string | null;
  statusCode: number;
  latencyMs: number;
  promptTokens: number;
  cachedPromptTokens?: number | null;
  completionTokens: number;
  totalTokens: number;
  userAmount: number;
  costAmount: number;
  success: number;
  createdAt: string;
};

export type SiteSettingsResponse = {
  id: string;
  siteName: string;
  adminEmail: string;
  siteDescription: string | null;
  baseUrl: string;
  footerText: string | null;
  themeMode: "LIGHT" | "DARK";
  updatedAt: string;
};

export type SiteSettingsUpdateRequest = {
  siteName: string;
  adminEmail: string;
  siteDescription?: string;
  baseUrl: string;
  footerText?: string;
  themeMode: "LIGHT" | "DARK";
};

export type OverviewResponse = {
  stats: OverviewStats;
  trend: DashboardTrendPointResponse[];
  modelStats: DashboardModelStatResponse[];
  endpoint: string;
};

export type UserKey = {
  id: string;
  name: string;
  prefix: string;
  full: string;
  createdAt: string;
  lastUsed: string;
  requests: number;
  tokens: number;
  spent: number;
  status: "active" | "disabled";
  rateLimit: number;
  budget: number | null;
};

export type ModelItem = {
  id: string;
  name: string;
  provider: string;
  category: string[];
  contextLength: number;
  inputPrice: number;
  cachedInputPrice: number;
  outputPrice: number;
  latency: string;
  available: boolean;
  tags: string[];
  desc: string;
  hot?: boolean;
  isNew?: boolean;
};

export type BillingUsagePoint = {
  date: string;
  cost: number;
  tokens: number;
};

export type ModelUsagePoint = {
  model: string;
  cost: number;
  pct: number;
};

export type BillingRecord = {
  id: string;
  type: string;
  amount: string;
  method: string;
  time: string;
  status: string;
};

export type BillingResponse = {
  balance: number;
  monthlySpend: number;
  monthlyBudget: number;
  usageData: BillingUsagePoint[];
  modelUsage: ModelUsagePoint[];
  billingHistory: BillingRecord[];
  rechargeAmounts: number[];
};

export type ModelGroupOptionResponse = {
  id: string;
  groupCode: string;
  groupName: string;
  packageType?: "QUOTA" | "BALANCE";
  packageTypeText?: string;
  salePrice: number;
  packageDays: number;
  dailyQuota: number;
  weeklyQuota: number;
  monthlyQuota: number;
  modelCount: number;
  purchased: boolean;
  active: boolean;
  expiresAt: string | null;
  remainingDays: number | null;
  dailyUsed: number;
  weeklyUsed: number;
  monthlyUsed: number;
  packageStatus: string;
  packageStatusText: string;
  remark: string | null;
  systemPreset?: boolean;
};

export type ModelAccessSummaryResponse = {
  packageRestrictionEnabled: boolean;
  packageStatus: string;
  packageStatusText: string;
  activeGroupId: string | null;
  activeGroupCode: string | null;
  activeGroupName: string | null;
  packagePrice: number | null;
  dailyQuota: number | null;
  weeklyQuota: number | null;
  monthlyQuota: number | null;
  dailyUsed: number | null;
  weeklyUsed: number | null;
  monthlyUsed: number | null;
  expiresAt: string | null;
  remainingDays: number | null;
  groups: ModelGroupOptionResponse[];
};

export type PurchaseModelPackageRequest = {
  groupId: string;
};

export type ModelGroupCreateRequest = {
  groupCode: string;
  groupName: string;
  packageType?: "QUOTA" | "BALANCE";
  salePrice: number;
  packageDays: number;
  dailyQuota: number;
  weeklyQuota: number;
  monthlyQuota: number;
  remark?: string;
};

export type ModelPackagePurchaseRecordResponse = {
  id: string | number;
  userId: string | number;
  username: string;
  groupId: string | number;
  groupCode: string;
  groupName: string;
  packageType?: "QUOTA" | "BALANCE";
  packageTypeText?: string;
  modelCount: number;
  purchasePrice: number;
  startAt: string | null;
  expiresAt: string | null;
  status: string;
  createdAt: string;
  active: boolean;
  dailyQuota: number | null;
  weeklyQuota: number | null;
  monthlyQuota: number | null;
  totalQuota: number | null;
  dailyUsed: number | null;
  weeklyUsed: number | null;
  monthlyUsed: number | null;
  totalUsed: number | null;
  remainingDays: number | null;
};

export type WalletTransactionItemResponse = {
  id: string | number;
  userId: string | number;
  username: string;
  walletId: string | number;
  orderNo: string;
  transactionType: string;
  direction: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  status: string;
  descriptionText: string;
  transactionDate: string;
  createdAt: string;
};

export type AccountSettings = {
  profile: {
    nickname: string;
    email: string;
    phone: string;
    company: string;
    bio: string;
  };
  notifications: {
    balanceLow: boolean;
    keyUsage: boolean;
    monthlyReport: boolean;
    newModel: boolean;
    marketing: boolean;
    email: boolean;
    wechat: boolean;
  };
};
