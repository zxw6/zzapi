import type {
  AccountSettings,
  ApiKeyCreateRequest,
  ApiKeyCreateResponse,
  ApiKeyListItemResponse,
  ApiKeyStatusUpdateRequest,
  BillingResponse,
  DashboardModelStatResponse,
  DashboardOverviewResponse,
  DashboardTrendPointResponse,
  LoginInput,
  PasswordResetCodeRequest,
  PasswordResetRequest,
  ModelAccessSummaryResponse,
  ModelBatchImportRequest,
  ModelBatchImportResponse,
  ModelCreateRequest,
  ModelUpdateRequest,
  ModelGroupCreateRequest,
  ModelPackagePurchaseRecordResponse,
  ModelItem,
  ModelListItemResponse,
  ModelGroupOptionResponse,
  OverviewResponse,
  PurchaseModelPackageRequest,
  RequestLogItemResponse,
  RegisterInput,
  SendRegisterCodeRequest,
  SiteSettingsResponse,
  SiteSettingsUpdateRequest,
  UpstreamModelOptionResponse,
  UserKey,
  UserProfile,
  VerificationCodeSendResponse,
  WalletTransactionItemResponse,
} from "../app/api/types";
import type { ApiFailure, ApiResponse, ApiSuccess } from "../lib/http/types";

const ACCESS_TTL_MS = 1000 * 60 * 15;
const API_PREFIX = "/api";

const users = new Map<string, UserProfile>([
  [
    "101",
    {
      id: "101",
      username: "demo",
      nickname: "张开发者",
      email: "10001@qq.com",
      phone: "138****8888",
      company: "某科技有限公司",
      bio: "AI 应用开发者",
      role: "user",
      roleCode: "USER",
      balance: 128.5,
    },
  ],
  [
    "1",
    {
      id: "1",
      username: "admin",
      nickname: "平台管理员",
      email: "10002@qq.com",
      phone: "400-800-9000",
      company: "API Hub",
      bio: "负责渠道、风控与运营配置。",
      role: "admin",
      roleCode: "ADMIN",
      balance: 9999,
    },
  ],
]);

const accessTokenStore = new Map<string, { userId: string; expiresAt: number }>();
const registerCodeStore = new Map<string, { code: string; expiresAt: number }>();
const passwordResetCodeStore = new Map<string, { code: string; expiresAt: number }>();
const loginCaptchaStore = new Map<string, { code: string; expiresAt: number }>();
const passwordStore = new Map<string, string>([
  ["demo", "demo123456"],
  ["admin", "admin123456"],
]);

const keysStore = new Map<string, UserKey[]>([
  [
    "101",
    [
      {
        id: "1",
        name: "生产环境 Key",
        prefix: "sk-hub-Ax2K...8fJm",
        full: "sk-hub-Ax2K9mNpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUvWx8fJm",
        createdAt: "2026-01-15",
        lastUsed: "2分钟前",
        requests: 12420,
        tokens: 6840000,
        spent: 34.2,
        status: "active",
        rateLimit: 500,
        budget: 200,
      },
      {
        id: "2",
        name: "开发测试 Key",
        prefix: "sk-hub-Bm7N...3hQp",
        full: "sk-hub-Bm7N1kLmNoPqRsTuVwXyZaBcDeFgHiJkLmNoPqRsT3hQp",
        createdAt: "2026-03-01",
        lastUsed: "1小时前",
        requests: 3210,
        tokens: 1520000,
        spent: 7.6,
        status: "active",
        rateLimit: 100,
        budget: 50,
      },
      {
        id: "3",
        name: "实验项目 Key",
        prefix: "sk-hub-Cn5P...7gRs",
        full: "sk-hub-Cn5P2lMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUv7gRs",
        createdAt: "2026-04-01",
        lastUsed: "昨天",
        requests: 480,
        tokens: 240000,
        spent: 1.2,
        status: "active",
        rateLimit: 50,
        budget: null,
      },
    ],
  ],
]);

const modelsData: ModelItem[] = [
  {
    id: "gpt-4o",
    name: "GPT-4o",
    provider: "OpenAI",
    category: ["对话", "多模态", "代码"],
    contextLength: 128000,
    inputPrice: 5,
    cachedInputPrice: 2.5,
    outputPrice: 15,
    latency: "~1.2s",
    available: true,
    tags: ["多模态", "旗舰"],
    desc: "OpenAI 最强旗舰模型，支持文本、图像多模态输入。",
    hot: true,
  },
  {
    id: "gpt-4o-mini",
    name: "GPT-4o mini",
    provider: "OpenAI",
    category: ["对话", "代码"],
    contextLength: 128000,
    inputPrice: 0.15,
    cachedInputPrice: 0.075,
    outputPrice: 0.6,
    latency: "~0.5s",
    available: true,
    tags: ["性价比", "快速"],
    desc: "高频低成本场景首选。",
    hot: true,
  },
  {
    id: "claude-3-5-sonnet",
    name: "Claude 3.5 Sonnet",
    provider: "Anthropic",
    category: ["对话", "代码", "推理"],
    contextLength: 200000,
    inputPrice: 3,
    cachedInputPrice: 0.3,
    outputPrice: 15,
    latency: "~2.1s",
    available: true,
    tags: ["长上下文", "代码"],
    desc: "代码和分析能力突出。",
  },
  {
    id: "deepseek-r1",
    name: "DeepSeek-R1",
    provider: "DeepSeek",
    category: ["推理", "代码"],
    contextLength: 64000,
    inputPrice: 0.55,
    cachedInputPrice: 0.14,
    outputPrice: 2.19,
    latency: "~4.5s",
    available: true,
    tags: ["推理"],
    desc: "数学和逻辑推理能力强。",
    isNew: true,
  },
];

let adminModelsData: ModelListItemResponse[] = [
  {
    id: "201",
    bindingId: "901",
    modelCode: "gpt-4o",
    modelName: "GPT-4o",
    modelType: "chat multimodal",
    billingType: "TOKEN",
    promptPrice: 5,
    cachedPromptPrice: 2.5,
    completionPrice: 15,
    requestPrice: null,
    multiplier: 1,
    isPublic: 1,
    status: "ACTIVE",
    groupId: "301",
    groupCode: "starter",
    groupName: "入门套餐",
    providerId: "11",
    providerName: "OpenAI",
    providerType: "OpenAI",
    upstreamModel: "gpt-4o",
    createdAt: "2026-04-01T10:00:00Z",
  },
  {
    id: "202",
    bindingId: "902",
    modelCode: "gpt-4o-mini",
    modelName: "GPT-4o mini",
    modelType: "chat code",
    billingType: "TOKEN",
    promptPrice: 0.15,
    cachedPromptPrice: 0.075,
    completionPrice: 0.6,
    requestPrice: null,
    multiplier: 1,
    isPublic: 1,
    status: "ACTIVE",
    groupId: "301",
    groupCode: "starter",
    groupName: "入门套餐",
    providerId: "11",
    providerName: "OpenAI",
    providerType: "OpenAI",
    upstreamModel: "gpt-4o-mini",
    createdAt: "2026-04-02T10:00:00Z",
  },
  {
    id: "203",
    bindingId: "903",
    modelCode: "claude-3-5-sonnet",
    modelName: "Claude 3.5 Sonnet",
    modelType: "chat code",
    billingType: "TOKEN",
    promptPrice: 3,
    cachedPromptPrice: 0.3,
    completionPrice: 15,
    requestPrice: null,
    multiplier: 1,
    isPublic: 1,
    status: "ACTIVE",
    groupId: "302",
    groupCode: "pro",
    groupName: "进阶套餐",
    providerId: "12",
    providerName: "Anthropic",
    providerType: "Anthropic",
    upstreamModel: "claude-3-5-sonnet",
    createdAt: "2026-04-03T10:00:00Z",
  },
  {
    id: "204",
    bindingId: "904",
    modelCode: "deepseek-r1",
    modelName: "DeepSeek-R1",
    modelType: "reason code",
    billingType: "TOKEN",
    promptPrice: 0.55,
    cachedPromptPrice: 0.14,
    completionPrice: 2.19,
    requestPrice: null,
    multiplier: 1,
    isPublic: 1,
    status: "ACTIVE",
    groupId: "302",
    groupCode: "pro",
    groupName: "进阶套餐",
    providerId: "13",
    providerName: "DeepSeek",
    providerType: "DeepSeek",
    upstreamModel: "deepseek-r1",
    createdAt: "2026-04-04T10:00:00Z",
  },
  {
    id: "205",
    bindingId: "905",
    modelCode: "internal-test-model",
    modelName: "Internal Test Model",
    modelType: "chat",
    billingType: "TOKEN",
    promptPrice: 1,
    cachedPromptPrice: 0.2,
    completionPrice: 1,
    requestPrice: null,
    multiplier: 1,
    isPublic: 0,
    status: "DISABLED",
    groupId: "303",
    groupCode: "enterprise",
    groupName: "企业套餐",
    providerId: "99",
    providerName: "Internal",
    providerType: "Custom",
    upstreamModel: "internal-test-model",
    createdAt: "2026-04-05T10:00:00Z",
  },
];

const dashboardOverviewData: DashboardOverviewResponse = {
  userCount: 128,
  apiKeyCount: 36,
  providerCount: 7,
  modelCount: 18,
  requestCountToday: 8452,
  totalTokensToday: 4567821,
  totalTokens7d: 28456782,
  onlineUserCount: 31,
  todayActiveUserCount: 96,
  rechargeAmountToday: 1280,
  consumeAmountToday: 932.4,
  walletBalanceTotal: 18653.2,
};

const userDashboardOverviewData: DashboardOverviewResponse = {
  userCount: 1,
  apiKeyCount: 3,
  providerCount: 4,
  modelCount: 4,
  requestCountToday: 3,
  totalTokensToday: 6838,
  totalTokens7d: 21788,
  onlineUserCount: 1,
  todayActiveUserCount: 1,
  rechargeAmountToday: 0,
  consumeAmountToday: 0.0700,
  walletBalanceTotal: 128.5,
};

const dashboardTrendData: DashboardTrendPointResponse[] = [
  { statDate: "2026-04-15", requestCount: 6420, successCount: 6310, totalTokens: 3580000, userAmount: 612.3, costAmount: 488.2 },
  { statDate: "2026-04-16", requestCount: 6880, successCount: 6765, totalTokens: 3760000, userAmount: 645.1, costAmount: 506.8 },
  { statDate: "2026-04-17", requestCount: 7310, successCount: 7190, totalTokens: 4010000, userAmount: 702.4, costAmount: 548.5 },
  { statDate: "2026-04-18", requestCount: 7590, successCount: 7444, totalTokens: 4180000, userAmount: 758.6, costAmount: 586.2 },
  { statDate: "2026-04-19", requestCount: 7920, successCount: 7801, totalTokens: 4290000, userAmount: 801.9, costAmount: 621.4 },
  { statDate: "2026-04-20", requestCount: 8210, successCount: 8098, totalTokens: 4410000, userAmount: 856.7, costAmount: 668.1 },
  { statDate: "2026-04-21", requestCount: 8452, successCount: 8334, totalTokens: 4567821, userAmount: 932.4, costAmount: 724.6 },
];

const userDashboardTrendData: DashboardTrendPointResponse[] = [
  { statDate: "2026-04-15", requestCount: 1, successCount: 1, totalTokens: 1620, userAmount: 0.0028, costAmount: 0.0021 },
  { statDate: "2026-04-16", requestCount: 0, successCount: 0, totalTokens: 0, userAmount: 0, costAmount: 0 },
  { statDate: "2026-04-17", requestCount: 1, successCount: 1, totalTokens: 4200, userAmount: 0.0072, costAmount: 0.0058 },
  { statDate: "2026-04-18", requestCount: 1, successCount: 1, totalTokens: 1320, userAmount: 0.0164, costAmount: 0.0131 },
  { statDate: "2026-04-19", requestCount: 2, successCount: 1, totalTokens: 7820, userAmount: 0.0126, costAmount: 0.0094 },
  { statDate: "2026-04-20", requestCount: 2, successCount: 1, totalTokens: 5918, userAmount: 0.0482, costAmount: 0.0361 },
  { statDate: "2026-04-21", requestCount: 3, successCount: 2, totalTokens: 6838, userAmount: 0.0700, costAmount: 0.0535 },
];

const dashboardModelStatsData: DashboardModelStatResponse[] = [
  { modelCode: "gpt-4o", upstreamModels: "gpt-4o", requestCount: 3180, totalTokens: 1860000, avgLatencyMs: 1240, totalLatencyMs: 3943200, successRate: 99.2, userAmount: 428.5 },
  { modelCode: "gpt-4o-mini", upstreamModels: "gpt-4o-mini", requestCount: 2260, totalTokens: 1240000, avgLatencyMs: 540, totalLatencyMs: 1220400, successRate: 99.6, userAmount: 96.2 },
  { modelCode: "claude-3-5-sonnet", upstreamModels: "claude-3-5-sonnet", requestCount: 1810, totalTokens: 892000, avgLatencyMs: 2110, totalLatencyMs: 3819100, successRate: 98.8, userAmount: 312.4 },
  { modelCode: "deepseek-r1", upstreamModels: "deepseek-r1", requestCount: 1202, totalTokens: 575821, avgLatencyMs: 4320, totalLatencyMs: 5192640, successRate: 97.3, userAmount: 95.3 },
];

const userDashboardModelStatsData: DashboardModelStatResponse[] = [
  { modelCode: "gpt-4o", upstreamModels: "gpt-4o", requestCount: 2, totalTokens: 3416, avgLatencyMs: 1481, totalLatencyMs: 2962, successRate: 100, userAmount: 0.0382 },
  { modelCode: "claude-3-5-sonnet", upstreamModels: "claude-3-5-sonnet", requestCount: 1, totalTokens: 3440, avgLatencyMs: 2341, totalLatencyMs: 2341, successRate: 100, userAmount: 0.0482 },
  { modelCode: "deepseek-r1", upstreamModels: "deepseek-r1", requestCount: 1, totalTokens: 5340, avgLatencyMs: 4521, totalLatencyMs: 4521, successRate: 100, userAmount: 0.0126 },
  { modelCode: "qwen-max", upstreamModels: "qwen-max", requestCount: 1, totalTokens: 4200, avgLatencyMs: 892, totalLatencyMs: 892, successRate: 100, userAmount: 0.0072 },
  { modelCode: "gemini-1.5-pro", upstreamModels: "gemini-1.5-pro", requestCount: 1, totalTokens: 2480, avgLatencyMs: 1234, totalLatencyMs: 1234, successRate: 0, userAmount: 0 },
  { modelCode: "gemini-1.5-flash", upstreamModels: "gemini-1.5-flash", requestCount: 1, totalTokens: 1620, avgLatencyMs: 820, totalLatencyMs: 820, successRate: 100, userAmount: 0.0028 },
  { modelCode: "gpt-4o-mini", upstreamModels: "gpt-4o-mini", requestCount: 1, totalTokens: 1302, avgLatencyMs: 654, totalLatencyMs: 654, successRate: 0, userAmount: 0 },
];

const requestLogsData: RequestLogItemResponse[] = [
  { requestId: "req_a1b2c3d4e5", username: "demo", modelCode: "gpt-4o", upstreamModel: "gpt-4o", statusCode: 200, latencyMs: 1842, promptTokens: 1240, cachedPromptTokens: 320, completionTokens: 856, totalTokens: 2096, userAmount: 0.0218, costAmount: 0.0174, success: 1, createdAt: "2026-04-21T14:32:18+08:00" },
  { requestId: "req_b2c3d4e5f6", username: "demo", modelCode: "claude-3-5-sonnet", upstreamModel: "claude-3-5-sonnet", statusCode: 200, latencyMs: 2341, promptTokens: 2100, cachedPromptTokens: 960, completionTokens: 1340, totalTokens: 3440, userAmount: 0.0482, costAmount: 0.0361, success: 1, createdAt: "2026-04-21T14:31:55+08:00" },
  { requestId: "req_c3d4e5f6g7", username: "demo", modelCode: "gpt-4o-mini", upstreamModel: "gpt-4o-mini", statusCode: 429, latencyMs: 654, promptTokens: 890, cachedPromptTokens: 128, completionTokens: 412, totalTokens: 1302, userAmount: 0, costAmount: 0, success: 0, createdAt: "2026-04-21T14:31:20+08:00" },
  { requestId: "req_d4e5f6g7h8", username: "demo", modelCode: "deepseek-r1", upstreamModel: "deepseek-r1", statusCode: 200, latencyMs: 4521, promptTokens: 3200, cachedPromptTokens: 0, completionTokens: 2140, totalTokens: 5340, userAmount: 0.0126, costAmount: 0.0094, success: 1, createdAt: "2026-04-20T14:30:44+08:00" },
  { requestId: "req_e5f6g7h8i9", username: "demo", modelCode: "gemini-1.5-pro", upstreamModel: "gemini-1.5-pro", statusCode: 500, latencyMs: 1234, promptTokens: 1560, cachedPromptTokens: 410, completionTokens: 920, totalTokens: 2480, userAmount: 0, costAmount: 0, success: 0, createdAt: "2026-04-20T14:30:12+08:00" },
  { requestId: "req_f6g7h8i9j0", username: "demo", modelCode: "gpt-4o", upstreamModel: "gpt-4o", statusCode: 200, latencyMs: 1120, promptTokens: 780, cachedPromptTokens: 220, completionTokens: 540, totalTokens: 1320, userAmount: 0.0164, costAmount: 0.0131, success: 1, createdAt: "2026-04-19T14:29:38+08:00" },
  { requestId: "req_g7h8i9j0k1", username: "demo", modelCode: "qwen-max", upstreamModel: "qwen-max", statusCode: 200, latencyMs: 892, promptTokens: 2400, cachedPromptTokens: 1080, completionTokens: 1800, totalTokens: 4200, userAmount: 0.0072, costAmount: 0.0058, success: 1, createdAt: "2026-04-18T14:29:01+08:00" },
  { requestId: "req_h8i9j0k1l2", username: "admin", modelCode: "claude-3-5-sonnet", upstreamModel: "claude-3-5-sonnet", statusCode: 401, latencyMs: 2180, promptTokens: 1890, cachedPromptTokens: 640, completionTokens: 1240, totalTokens: 3130, userAmount: 0, costAmount: 0, success: 0, createdAt: "2026-04-18T14:28:25+08:00" },
  { requestId: "req_i9j0k1l2m3", username: "admin", modelCode: "gpt-4o-mini", upstreamModel: "gpt-4o-mini", statusCode: 200, latencyMs: 542, promptTokens: 650, cachedPromptTokens: 190, completionTokens: 380, totalTokens: 1030, userAmount: 0.0031, costAmount: 0.0024, success: 1, createdAt: "2026-04-17T14:27:50+08:00" },
  { requestId: "req_j0k1l2m3n4", username: "admin", modelCode: "deepseek-r1", upstreamModel: "deepseek-r1", statusCode: 200, latencyMs: 5892, promptTokens: 4100, cachedPromptTokens: 0, completionTokens: 3200, totalTokens: 7300, userAmount: 0.0186, costAmount: 0.0148, success: 1, createdAt: "2026-04-17T14:27:14+08:00" },
  { requestId: "req_k1l2m3n4o5", username: "admin", modelCode: "gpt-4o", upstreamModel: "gpt-4o", statusCode: 200, latencyMs: 1580, promptTokens: 1100, cachedPromptTokens: 260, completionTokens: 720, totalTokens: 1820, userAmount: 0.0186, costAmount: 0.0144, success: 1, createdAt: "2026-04-16T14:26:38+08:00" },
  { requestId: "req_l2m3n4o5p6", username: "demo", modelCode: "gemini-1.5-flash", upstreamModel: "gemini-1.5-flash", statusCode: 200, latencyMs: 820, promptTokens: 980, cachedPromptTokens: 140, completionTokens: 640, totalTokens: 1620, userAmount: 0.0028, costAmount: 0.0021, success: 1, createdAt: "2026-04-16T14:26:00+08:00" },
];

let siteSettingsData: SiteSettingsResponse = {
  id: "1",
  siteName: "API Hub 中转站",
  adminEmail: "admin@apihub.io",
  siteDescription: "企业级 AI API 中转管理平台",
  baseUrl: "https://api.yourdomain.com",
  footerText: "Powered by API Hub",
  themeMode: "LIGHT",
  updatedAt: "2026-04-22T21:00:00",
};

let defaultPackageGroups: ModelGroupOptionResponse[] = [
  {
    id: "301",
    groupCode: "starter",
    groupName: "入门套餐",
    packageType: "QUOTA",
    packageTypeText: "普通套餐",
    salePrice: 29.9,
    packageDays: 30,
    dailyQuota: 200000,
    weeklyQuota: 1200000,
    monthlyQuota: 5000000,
    modelCount: 8,
    purchased: true,
    active: true,
    expiresAt: "2026-05-20T23:59:59+08:00",
    remainingDays: 23,
    dailyUsed: 6838,
    weeklyUsed: 21788,
    monthlyUsed: 68420,
    packageStatus: "ACTIVE",
    packageStatusText: "生效中",
    remark: "适合日常调试和轻量生产调用",
    systemPreset: true,
  },
  {
    id: "302",
    groupCode: "pro",
    groupName: "专业套餐",
    packageType: "QUOTA",
    packageTypeText: "普通套餐",
    salePrice: 99,
    packageDays: 30,
    dailyQuota: 800000,
    weeklyQuota: 5000000,
    monthlyQuota: 20000000,
    modelCount: 16,
    purchased: false,
    active: false,
    expiresAt: null,
    remainingDays: null,
    dailyUsed: 0,
    weeklyUsed: 0,
    monthlyUsed: 0,
    packageStatus: "NOT_PURCHASED",
    packageStatusText: "未购买",
    remark: "覆盖更多高阶模型和更高额度",
    systemPreset: true,
  },
  {
    id: "303",
    groupCode: "wallet-balance",
    groupName: "余额套餐",
    packageType: "BALANCE",
    packageTypeText: "余额套餐",
    salePrice: 0,
    packageDays: 0,
    dailyQuota: 0,
    weeklyQuota: 0,
    monthlyQuota: 0,
    modelCount: 28,
    purchased: false,
    active: false,
    expiresAt: null,
    remainingDays: null,
    dailyUsed: 0,
    weeklyUsed: 0,
    monthlyUsed: 0,
    packageStatus: "NOT_PURCHASED",
    packageStatusText: "未开通余额计费",
    remark: "0 美元开通，调用时按钱包余额实时扣费",
    systemPreset: true,
  },
];

const upstreamModelsByProvider = new Map<string, UpstreamModelOptionResponse[]>([
  [
    "11",
    [
      { id: "gpt-4.1", displayName: "GPT-4.1", ownedBy: "openai", providerType: "OpenAI" },
      { id: "gpt-4.1-mini", displayName: "GPT-4.1 mini", ownedBy: "openai", providerType: "OpenAI" },
      { id: "o4-mini", displayName: "o4-mini", ownedBy: "openai", providerType: "OpenAI" },
    ],
  ],
  [
    "12",
    [
      { id: "claude-3-7-sonnet", displayName: "Claude 3.7 Sonnet", ownedBy: "anthropic", providerType: "Anthropic" },
      { id: "claude-3-5-haiku", displayName: "Claude 3.5 Haiku", ownedBy: "anthropic", providerType: "Anthropic" },
    ],
  ],
  [
    "13",
    [
      { id: "deepseek-v3", displayName: "DeepSeek V3", ownedBy: "deepseek", providerType: "DeepSeek" },
      { id: "deepseek-r1", displayName: "DeepSeek R1", ownedBy: "deepseek", providerType: "DeepSeek" },
    ],
  ],
]);

const modelAccessSummaryStore = new Map<string, ModelAccessSummaryResponse>([
  [
    "101",
    {
      packageRestrictionEnabled: true,
      packageStatus: "ACTIVE",
      packageStatusText: "当前套餐生效中",
      activeGroupId: "301",
      activeGroupCode: "starter",
      activeGroupName: "入门套餐",
      packagePrice: 29.9,
      dailyQuota: 200000,
      weeklyQuota: 1200000,
      monthlyQuota: 5000000,
      dailyUsed: 6838,
      weeklyUsed: 21788,
      monthlyUsed: 68420,
      expiresAt: "2026-05-20T23:59:59+08:00",
      remainingDays: 23,
      groups: defaultPackageGroups.map((group) => ({ ...group })),
    },
  ],
]);

let modelAccessPurchaseRecords: ModelPackagePurchaseRecordResponse[] = [
  {
    id: "7001",
    userId: "101",
    username: "demo",
    groupId: "301",
    groupCode: "starter",
    groupName: "入门套餐",
    packageType: "QUOTA",
    packageTypeText: "普通套餐",
    modelCount: 8,
    purchasePrice: 29.9,
    startAt: "2026-04-20T00:00:00+08:00",
    expiresAt: "2026-05-20T23:59:59+08:00",
    status: "ACTIVE",
    createdAt: "2026-04-20T09:20:18+08:00",
    active: true,
    dailyQuota: 200000,
    weeklyQuota: 1200000,
    monthlyQuota: 5000000,
    totalQuota: 5000000,
    dailyUsed: 6838,
    weeklyUsed: 21788,
    monthlyUsed: 68420,
    totalUsed: 68420,
    remainingDays: 21,
  },
  {
    id: "7002",
    userId: "1",
    username: "admin",
    groupId: "302",
    groupCode: "pro",
    groupName: "专业套餐",
    packageType: "QUOTA",
    packageTypeText: "普通套餐",
    modelCount: 16,
    purchasePrice: 99,
    startAt: "2026-03-15T10:15:00+08:00",
    expiresAt: "2026-04-14T23:59:59+08:00",
    status: "EXPIRED",
    createdAt: "2026-03-15T10:14:22+08:00",
    active: false,
    dailyQuota: 800000,
    weeklyQuota: 5000000,
    monthlyQuota: 20000000,
    totalQuota: 20000000,
    dailyUsed: 0,
    weeklyUsed: 0,
    monthlyUsed: 0,
    totalUsed: 19843320,
    remainingDays: 0,
  },
  {
    id: "7003",
    userId: "102",
    username: "nova-team",
    groupId: "302",
    groupCode: "pro",
    groupName: "专业套餐",
    packageType: "QUOTA",
    packageTypeText: "普通套餐",
    modelCount: 16,
    purchasePrice: 99,
    startAt: "2026-04-25T00:00:00+08:00",
    expiresAt: "2026-05-24T23:59:59+08:00",
    status: "ACTIVE",
    createdAt: "2026-04-24T19:42:03+08:00",
    active: true,
    dailyQuota: 800000,
    weeklyQuota: 5000000,
    monthlyQuota: 20000000,
    totalQuota: 20000000,
    dailyUsed: 156220,
    weeklyUsed: 845660,
    monthlyUsed: 1845660,
    totalUsed: 1845660,
    remainingDays: 25,
  },
];

let walletTransactionsData: WalletTransactionItemResponse[] = [
  {
    id: "9001",
    userId: "101",
    username: "demo",
    walletId: "5001",
    orderNo: "TX202604240001",
    transactionType: "PACKAGE_BUY",
    direction: "OUT",
    amount: 29.9,
    balanceBefore: 158.4,
    balanceAfter: 128.5,
    status: "SUCCESS",
    descriptionText: "购买 入门套餐",
    transactionDate: "2026-04-20",
    createdAt: "2026-04-20T09:20:18+08:00",
  },
  {
    id: "9002",
    userId: "101",
    username: "demo",
    walletId: "5001",
    orderNo: "TX202604230014",
    transactionType: "CONSUME",
    direction: "OUT",
    amount: 12.46,
    balanceBefore: 170.86,
    balanceAfter: 158.4,
    status: "SUCCESS",
    descriptionText: "模型调用扣费: claude-3-5-sonnet",
    transactionDate: "2026-04-23",
    createdAt: "2026-04-23T18:12:44+08:00",
  },
  {
    id: "9003",
    userId: "1",
    username: "admin",
    walletId: "5002",
    orderNo: "TX202604180009",
    transactionType: "RECHARGE",
    direction: "IN",
    amount: 500,
    balanceBefore: 9499,
    balanceAfter: 9999,
    status: "SUCCESS",
    descriptionText: "后台人工充值",
    transactionDate: "2026-04-18",
    createdAt: "2026-04-18T11:08:10+08:00",
  },
  {
    id: "9004",
    userId: "102",
    username: "nova-team",
    walletId: "5003",
    orderNo: "TX202604240018",
    transactionType: "PACKAGE_BUY",
    direction: "OUT",
    amount: 99,
    balanceBefore: 268.2,
    balanceAfter: 169.2,
    status: "SUCCESS",
    descriptionText: "购买 专业套餐",
    transactionDate: "2026-04-24",
    createdAt: "2026-04-24T19:42:03+08:00",
  },
  {
    id: "9005",
    userId: "102",
    username: "nova-team",
    walletId: "5003",
    orderNo: "TX202604240019",
    transactionType: "CONSUME",
    direction: "OUT",
    amount: 21.38,
    balanceBefore: 169.2,
    balanceAfter: 147.82,
    status: "SUCCESS",
    descriptionText: "模型调用扣费: gpt-4o",
    transactionDate: "2026-04-24",
    createdAt: "2026-04-24T22:01:28+08:00",
  },
];

const apiKeyPackageBindings = new Map<
  string,
  {
    modelGroupId: string | null;
    modelGroupName: string | null;
    totalQuota: number | null;
    expiresAt: string | null;
  }
>([
  [
    "1",
    {
      modelGroupId: "301",
      modelGroupName: "入门套餐",
      totalQuota: 5000000,
      expiresAt: "2026-05-20T23:59:59+08:00",
    },
  ],
  [
    "2",
    {
      modelGroupId: "301",
      modelGroupName: "入门套餐",
      totalQuota: 5000000,
      expiresAt: "2026-05-20T23:59:59+08:00",
    },
  ],
  [
    "3",
    {
      modelGroupId: "301",
      modelGroupName: "入门套餐",
      totalQuota: 5000000,
      expiresAt: "2026-05-20T23:59:59+08:00",
    },
  ],
]);

const overviewDataByUser = new Map<string, unknown>([
  [
    "101",
    {
      stats: {
        balance: 128.5,
        monthlySpend: 43.2,
        requestsToday: 640,
        requestsDeltaPct: 18.5,
        tokensToday: 345000,
        tokensCost: 1.72,
        activeKeys: 3,
        maxKeys: 5,
      },
      trend: [
        { day: "4/5", requests: 320, tokens: 180000 },
        { day: "4/6", requests: 480, tokens: 265000 },
        { day: "4/7", requests: 290, tokens: 158000 },
        { day: "4/8", requests: 640, tokens: 348000 },
        { day: "4/9", requests: 520, tokens: 281000 },
        { day: "4/10", requests: 780, tokens: 420000 },
        { day: "4/11", requests: 640, tokens: 345000 },
      ],
      recentRequests: [
        { id: "req_aB1c2D3", model: "gpt-4o", tokens: 1842, latency: 1230, status: 200, time: "刚刚" },
        { id: "req_eF4g5H6", model: "claude-3-5-sonnet", tokens: 3241, latency: 2180, status: 200, time: "1分钟前" },
        { id: "req_iJ7k8L9", model: "gpt-4o-mini", tokens: 654, latency: 498, status: 429, time: "2分钟前" },
        { id: "req_mN0p1Q2", model: "deepseek-r1", tokens: 5120, latency: 4320, status: 200, time: "5分钟前" },
      ],
      serviceStatus: [
        { name: "GPT-4o 系列", status: "正常", latency: "1.2s" },
        { name: "Claude 系列", status: "正常", latency: "2.1s" },
        { name: "Gemini 系列", status: "降级", latency: "—" },
      ],
      endpoint: "https://api.apihub.io/v1",
      inviteLink: "https://apihub.io/ref/abc123",
    },
  ],
]);

const billingByUser = new Map<string, BillingResponse>([
  [
    "101",
    {
      balance: 128.5,
      monthlySpend: 43.2,
      monthlyBudget: 100,
      usageData: [
        { date: "3/12", cost: 1.2, tokens: 640000 },
        { date: "3/19", cost: 2.8, tokens: 1480000 },
        { date: "3/26", cost: 1.9, tokens: 980000 },
        { date: "4/2", cost: 4.2, tokens: 2200000 },
        { date: "4/9", cost: 3.6, tokens: 1900000 },
        { date: "4/11", cost: 1.7, tokens: 900000 },
      ],
      modelUsage: [
        { model: "GPT-4o", cost: 18.4, pct: 42 },
        { model: "GPT-4o-mini", cost: 8.2, pct: 19 },
        { model: "Claude 3.5", cost: 9.6, pct: 22 },
        { model: "DeepSeek-R1", cost: 4.2, pct: 10 },
        { model: "其他", cost: 2.8, pct: 7 },
      ],
      billingHistory: [
        { id: "txn_001", type: "充值", amount: "+¥200.00", method: "支付宝", time: "2026-04-08 14:32", status: "成功" },
        { id: "txn_002", type: "消费", amount: "-¥43.20", method: "API 调用", time: "2026-04-01 00:00", status: "成功" },
      ],
      rechargeAmounts: [50, 100, 200, 500],
    },
  ],
]);

const accountStore = new Map<string, AccountSettings>([
  [
    "101",
    {
      profile: {
        nickname: "张开发者",
        email: "dev@example.com",
        phone: "138****8888",
        company: "某科技有限公司",
        bio: "AI 应用开发者",
      },
      notifications: {
        balanceLow: true,
        keyUsage: true,
        monthlyReport: true,
        newModel: false,
        marketing: false,
        email: true,
        wechat: false,
      },
    },
  ],
]);

function nowIso() {
  return new Date().toISOString();
}

function randomId(prefix: string) {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
}

function success<T>(data: T, message = "OK"): ApiSuccess<T> {
  return {
    success: true,
    message,
    data,
    requestId: randomId("req"),
    timestamp: nowIso(),
  };
}

function failure(code: string, message: string, details?: unknown): ApiFailure {
  return {
    success: false,
    error: {
      code,
      message,
      details,
    },
    requestId: randomId("req"),
    timestamp: nowIso(),
  };
}

function toResponse<T>(status: number, body: ApiResponse<T>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "X-Mock-Api": "1",
    },
  });
}

async function readJson<T>(init?: RequestInit): Promise<T | null> {
  if (!init?.body) {
    return null;
  }

  if (typeof init.body === "string") {
    return JSON.parse(init.body) as T;
  }

  return null;
}

function extractPath(input: RequestInfo | URL): string {
  const raw = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
  const url = new URL(raw, "http://localhost");
  return url.pathname;
}

function normalizePath(pathname: string): string {
  if (pathname.startsWith(API_PREFIX)) {
    const next = pathname.slice(API_PREFIX.length);
    return next.length === 0 ? "/" : next;
  }
  return pathname;
}

function headerValue(headers: HeadersInit | undefined, key: string): string | null {
  if (!headers) {
    return null;
  }

  if (headers instanceof Headers) {
    return headers.get(key);
  }

  if (Array.isArray(headers)) {
    const found = headers.find(([k]) => k.toLowerCase() === key.toLowerCase());
    return found?.[1] ?? null;
  }

  return headers[key] ?? headers[key.toLowerCase()] ?? null;
}

function getTokenFromHeaders(headers: HeadersInit | undefined): string | null {
  const authorization = headerValue(headers, "Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return null;
  }

  return authorization.slice("Bearer ".length);
}

function toAdminAuthResponse(user: UserProfile, token: string) {
  return {
    token,
    userId: Number(user.id),
    username: user.username,
    nickname: user.nickname,
    roleCode: user.roleCode,
    balance: user.balance ?? 0,
  };
}

function issueSession(user: UserProfile) {
  const accessToken = randomId("atk");

  accessTokenStore.set(accessToken, {
    userId: user.id,
    expiresAt: Date.now() + ACCESS_TTL_MS,
  });

  return toAdminAuthResponse(user, accessToken);
}

function authorize(headers: HeadersInit | undefined): { ok: true; user: UserProfile } | { ok: false; response: Response } {
  const token = getTokenFromHeaders(headers);
  if (!token) {
    return {
      ok: false,
      response: toResponse(401, failure("UNAUTHORIZED", "缺少认证令牌")),
    };
  }

  const access = accessTokenStore.get(token);
  if (!access) {
    return {
      ok: false,
      response: toResponse(401, failure("UNAUTHORIZED", "认证令牌无效")),
    };
  }

  if (access.expiresAt <= Date.now()) {
    accessTokenStore.delete(token);
    return {
      ok: false,
      response: toResponse(401, failure("TOKEN_EXPIRED", "认证令牌已过期")),
    };
  }

  const user = users.get(access.userId);
  if (!user) {
    return {
      ok: false,
      response: toResponse(401, failure("UNAUTHORIZED", "用户不存在")),
    };
  }

  return { ok: true, user };
}

function updateUserProfile(userId: string, profile: AccountSettings["profile"]) {
  const existed = users.get(userId);
  if (!existed) {
    return;
  }

  users.set(userId, {
    ...existed,
    nickname: profile.nickname,
    email: profile.email,
    phone: profile.phone,
    company: profile.company,
    bio: profile.bio,
  });
}

function getAllApiKeys(): ApiKeyListItemResponse[] {
  return [...keysStore.entries()].flatMap(([userId, keys]) => {
    const user = users.get(userId);
    if (!user) {
      return [];
    }

    return keys.map((key) => {
      const binding = apiKeyPackageBindings.get(key.id);

      return {
        id: key.id,
        userId,
        username: user.username,
        name: key.name,
        accessKey: key.prefix,
        status: key.status.toUpperCase(),
        modelGroupId: binding?.modelGroupId ?? null,
        modelGroupName: binding?.modelGroupName ?? null,
        totalQuota: binding?.totalQuota ?? key.budget,
        usedQuota: key.spent,
        expiresAt: binding?.expiresAt ?? null,
        lastUsedAt: key.lastUsed === "未使用" ? null : key.lastUsed,
        createdAt: key.createdAt,
      };
    });
  });
}

function updateUserBalance(userId: string, balance: number) {
  const existed = users.get(userId);
  if (!existed) {
    return;
  }

  users.set(userId, {
    ...existed,
    balance,
  });
}

function getUserModelAccessSummary(userId: string): ModelAccessSummaryResponse {
  const stored = modelAccessSummaryStore.get(userId);
  if (stored) {
    return stored;
  }

  return {
    packageRestrictionEnabled: true,
    packageStatus: "NOT_PURCHASED",
    packageStatusText: "未开通套餐",
    activeGroupId: null,
    activeGroupCode: null,
    activeGroupName: null,
    packagePrice: null,
    dailyQuota: null,
    weeklyQuota: null,
    monthlyQuota: null,
    dailyUsed: null,
    weeklyUsed: null,
    monthlyUsed: null,
    expiresAt: null,
    remainingDays: null,
    groups: defaultPackageGroups.map((group) => ({
      ...group,
      purchased: false,
      active: false,
      expiresAt: null,
      remainingDays: null,
      dailyUsed: 0,
      weeklyUsed: 0,
      monthlyUsed: 0,
      packageStatus: "NOT_PURCHASED",
      packageStatusText: "未购买",
    })),
  };
}

function saveUserModelAccessSummary(userId: string, summary: ModelAccessSummaryResponse) {
  modelAccessSummaryStore.set(userId, {
    ...summary,
    groups: summary.groups.map((group) => ({ ...group })),
  });
}

function buildTopLevelSummary(groups: ModelGroupOptionResponse[]) {
  const activeGroup = groups.find((group) => group.active) ?? null;

  if (!activeGroup) {
    return {
      packageStatus: "NOT_PURCHASED",
      packageStatusText: "未开通套餐",
      activeGroupId: null,
      activeGroupCode: null,
      activeGroupName: null,
      packagePrice: null,
      dailyQuota: null,
      weeklyQuota: null,
      monthlyQuota: null,
      dailyUsed: null,
      weeklyUsed: null,
      monthlyUsed: null,
      expiresAt: null,
      remainingDays: null,
    };
  }

  return {
    packageStatus: activeGroup.packageStatus || "ACTIVE",
    packageStatusText: activeGroup.packageStatusText || "生效中",
    activeGroupId: activeGroup.id,
    activeGroupCode: activeGroup.groupCode,
    activeGroupName: activeGroup.groupName,
    packagePrice: activeGroup.salePrice,
    dailyQuota: activeGroup.dailyQuota,
    weeklyQuota: activeGroup.weeklyQuota,
    monthlyQuota: activeGroup.monthlyQuota,
    dailyUsed: activeGroup.dailyUsed,
    weeklyUsed: activeGroup.weeklyUsed,
    monthlyUsed: activeGroup.monthlyUsed,
    expiresAt: activeGroup.expiresAt,
    remainingDays: activeGroup.remainingDays,
  };
}

function appendPackageGroupToAllSummaries(group: ModelGroupOptionResponse) {
  for (const [userId, summary] of modelAccessSummaryStore.entries()) {
    saveUserModelAccessSummary(userId, {
      ...summary,
      groups: [...summary.groups.map((item) => ({ ...item })), { ...group }],
    });
  }
}

function removePackageGroupFromAllSummaries(groupId: string) {
  for (const [userId, summary] of modelAccessSummaryStore.entries()) {
    const nextGroups = summary.groups.filter((group) => group.id !== groupId).map((group) => ({ ...group }));
    const topLevel = buildTopLevelSummary(nextGroups);
    saveUserModelAccessSummary(userId, {
      ...summary,
      ...topLevel,
      groups: nextGroups,
    });
  }
}

function normalizeEmail(email: string) {
  return email.trim().toLowerCase();
}

function issueRegisterCode(email: string) {
  const normalizedEmail = normalizeEmail(email);
  const code = "123456";

  registerCodeStore.set(normalizedEmail, {
    code,
    expiresAt: Date.now() + 5 * 60 * 1000,
  });

  return code;
}

function issuePasswordResetCode(email: string) {
  const normalizedEmail = normalizeEmail(email);
  const code = "123456";

  passwordResetCodeStore.set(normalizedEmail, {
    code,
    expiresAt: Date.now() + 5 * 60 * 1000,
  });

  return code;
}

function generateLoginCaptchaCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from({ length: 4 }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join("");
}

function buildCaptchaImageBase64(code: string) {
  const noise = Array.from({ length: 6 })
    .map(
      (_, index) =>
        `<line x1="${8 + index * 18}" y1="${10 + (index % 3) * 6}" x2="${22 + index * 18}" y2="${26 - (index % 2) * 4}" stroke="rgba(59,130,246,0.18)" stroke-width="2" />`,
    )
    .join("");
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="132" height="42" viewBox="0 0 132 42">
      <rect width="132" height="42" rx="12" fill="#f8fafc" />
      <rect x="1" y="1" width="130" height="40" rx="11" fill="none" stroke="#cbd5e1" />
      ${noise}
      <text x="66" y="28" text-anchor="middle" font-family="Arial, sans-serif" font-size="22" font-weight="700" fill="#0f172a" letter-spacing="4">${code}</text>
    </svg>
  `.trim();

  return `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`;
}

function issueLoginCaptcha() {
  const captchaId = randomId("captcha");
  const code = generateLoginCaptchaCode();
  loginCaptchaStore.set(captchaId, {
    code,
    expiresAt: Date.now() + 2 * 60 * 1000,
  });

  return {
    captchaId,
    imageBase64: buildCaptchaImageBase64(code),
    expireSeconds: 120,
  };
}

function findUserByNumericId(userId: string): UserProfile | null {
  return users.get(userId) ?? null;
}

function findProviderById(providerId: string) {
  const matchedModel = adminModelsData.find((item) => item.providerId === providerId);
  if (matchedModel) {
    return {
      id: providerId,
      providerName: matchedModel.providerName ?? "未命名渠道",
      providerType: matchedModel.providerType ?? "未知类型",
    };
  }

  const matchedUpstream = upstreamModelsByProvider.get(providerId)?.[0];
  if (matchedUpstream) {
    return {
      id: providerId,
      providerName: matchedUpstream.providerType ?? providerId,
      providerType: matchedUpstream.providerType ?? "未知类型",
    };
  }

  return null;
}

function findGroupById(groupId: string): ModelGroupOptionResponse | null {
  return defaultPackageGroups.find((item) => item.id === groupId) ?? null;
}

async function simulateLatency() {
  const delay = 200 + Math.floor(Math.random() * 300);
  await new Promise((resolve) => setTimeout(resolve, delay));
}

export async function mockFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  await simulateLatency();

  const method = init?.method?.toUpperCase() ?? "GET";
  const url = new URL(typeof input === "string" ? input : input.toString(), "http://localhost");
  const path = normalizePath(extractPath(input));

  if ((path === "/admin/auth/login/captcha" || path === "/auth/login/captcha") && method === "GET") {
    return toResponse(200, success(issueLoginCaptcha(), "获取图形验证码成功"));
  }

  if ((path === "/admin/auth/login" || path === "/auth/login") && method === "POST") {
    const body = (await readJson<LoginInput>(init)) ?? {
      username: "",
      password: "",
      captchaId: "",
      captchaCode: "",
    };

    if (!body.username || !body.password || !body.captchaId || !body.captchaCode) {
      return toResponse(
        400,
        failure("BAD_REQUEST", "username、password、captchaId、captchaCode 不能为空"),
      );
    }

    const account = body.username.trim().toLowerCase();
    const user =
      [...users.values()].find(
        (it) => it.username.toLowerCase() === account || it.email?.toLowerCase() === account,
      ) ?? null;
    if (!user) {
      return toResponse(401, failure("LOGIN_FAILED", "账号或密码错误"));
    }

    const currentPassword = passwordStore.get(user.username);
    if (!currentPassword || currentPassword !== body.password) {
      return toResponse(401, failure("LOGIN_FAILED", "账号或密码错误"));
    }

    const captchaRecord = loginCaptchaStore.get(body.captchaId.trim());
    if (!captchaRecord || captchaRecord.expiresAt < Date.now()) {
      return toResponse(400, failure("CAPTCHA_EXPIRED", "图形验证码已过期，请刷新后重试"));
    }

    if (captchaRecord.code !== body.captchaCode.trim().toUpperCase()) {
      return toResponse(400, failure("CAPTCHA_INVALID", "图形验证码不正确"));
    }

    loginCaptchaStore.delete(body.captchaId.trim());
    return toResponse(200, success(issueSession(user)));
  }

  if (path === "/admin/auth/register/code" && method === "POST") {
    const body = (await readJson<SendRegisterCodeRequest>(init)) ?? { email: "" };
    const email = body.email?.trim() ?? "";

    if (!email) {
      return toResponse(400, failure("BAD_REQUEST", "QQ 邮箱不能为空"));
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return toResponse(400, failure("BAD_REQUEST", "邮箱格式不正确"));
    }

    if (!/^[1-9]\d{4,12}@qq\.com$/i.test(email)) {
      return toResponse(400, failure("BAD_REQUEST", "请使用 QQ 邮箱"));
    }

    issueRegisterCode(email);
    const response: VerificationCodeSendResponse = {
      email,
      expireSeconds: 300,
    };
    return toResponse(200, success(response, "验证码发送成功"));
  }

  if (path === "/admin/auth/password/reset/code" && method === "POST") {
    const body = (await readJson<PasswordResetCodeRequest>(init)) ?? { email: "" };
    const email = body.email?.trim() ?? "";

    if (!email) {
      return toResponse(400, failure("BAD_REQUEST", "QQ邮箱不能为空"));
    }

    if (!/^[1-9]\d{4,12}@qq\.com$/i.test(email)) {
      return toResponse(400, failure("BAD_REQUEST", "请使用QQ邮箱"));
    }

    const normalizedEmail = normalizeEmail(email);
    const user =
      [...users.values()].find((it) => normalizeEmail(it.email ?? "") === normalizedEmail) ?? null;

    if (!user) {
      return toResponse(404, failure("EMAIL_NOT_FOUND", "QQ邮箱不存在"));
    }

    issuePasswordResetCode(email);
    const response: VerificationCodeSendResponse = {
      email,
      expireSeconds: 300,
    };
    return toResponse(200, success(response, "重置密码验证码发送成功"));
  }

  if (path === "/admin/auth/password/reset" && method === "POST") {
    const body = (await readJson<PasswordResetRequest>(init)) ?? {
      email: "",
      verificationCode: "",
      newPassword: "",
    };

    const email = body.email?.trim() ?? "";
    const verificationCode = body.verificationCode?.trim() ?? "";
    const newPassword = body.newPassword ?? "";

    if (!email) {
      return toResponse(400, failure("BAD_REQUEST", "QQ邮箱不能为空"));
    }

    if (!/^[1-9]\d{4,12}@qq\.com$/i.test(email)) {
      return toResponse(400, failure("BAD_REQUEST", "请使用QQ邮箱"));
    }

    if (!verificationCode) {
      return toResponse(400, failure("BAD_REQUEST", "验证码不能为空"));
    }

    if (!newPassword) {
      return toResponse(400, failure("BAD_REQUEST", "新密码不能为空"));
    }

    if (newPassword.length < 6 || newPassword.length > 72) {
      return toResponse(400, failure("BAD_REQUEST", "新密码长度必须为6-72位"));
    }

    const normalizedEmail = normalizeEmail(email);
    const user =
      [...users.values()].find((it) => normalizeEmail(it.email ?? "") === normalizedEmail) ?? null;

    if (!user) {
      return toResponse(404, failure("EMAIL_NOT_FOUND", "QQ邮箱不存在"));
    }

    const codeRecord = passwordResetCodeStore.get(normalizedEmail);
    if (!codeRecord || codeRecord.expiresAt < Date.now()) {
      return toResponse(400, failure("CODE_EXPIRED", "验证码已过期"));
    }

    if (codeRecord.code !== verificationCode) {
      return toResponse(400, failure("CODE_INVALID", "验证码不正确"));
    }

    passwordStore.set(user.username, newPassword);
    passwordResetCodeStore.delete(normalizedEmail);

    return toResponse(200, success(null, "密码重置成功"));
  }

  if ((path === "/admin/auth/register" || path === "/auth/register") && method === "POST") {
    const body = (await readJson<RegisterInput>(init)) ?? {
      username: "",
      email: "",
      password: "",
      verificationCode: "",
      nickname: "",
    };

    if (!body.username || !body.email || !body.password || !body.verificationCode) {
      return toResponse(400, failure("BAD_REQUEST", "用户名、邮箱、验证码和密码不能为空"));
    }

    const normalizedEmail = normalizeEmail(body.email);
    if (!/^[1-9]\d{4,12}@qq\.com$/i.test(normalizedEmail)) {
      return toResponse(400, failure("BAD_REQUEST", "请使用 QQ 邮箱"));
    }

    const codeRecord = registerCodeStore.get(normalizedEmail);
    if (!codeRecord || codeRecord.expiresAt < Date.now()) {
      return toResponse(400, failure("CODE_EXPIRED", "验证码已过期"));
    }

    if (codeRecord.code !== body.verificationCode.trim()) {
      return toResponse(400, failure("CODE_INVALID", "验证码不正确"));
    }

    const exists = [...users.values()].some(
      (it) => normalizeEmail(it.email ?? "") === normalizedEmail || it.username === body.username,
    );
    if (exists) {
      const emailExists = [...users.values()].some(
        (it) => normalizeEmail(it.email ?? "") === normalizedEmail,
      );
      return toResponse(
        409,
        failure(
          "ACCOUNT_EXISTS",
          emailExists ? "QQ 邮箱已存在" : "用户名已存在",
        ),
      );
    }

    const user: UserProfile = {
      id: String(Date.now()),
      username: body.username,
      nickname: body.nickname || normalizedEmail.split("@")[0],
      email: normalizedEmail,
      role: "user",
      roleCode: "USER",
      balance: 0,
      phone: "",
      company: "",
      bio: "",
    };

    users.set(user.id, user);
    passwordStore.set(user.username, body.password);
    registerCodeStore.delete(normalizedEmail);
    keysStore.set(user.id, []);
    overviewDataByUser.set(user.id, overviewDataByUser.get("101"));
    billingByUser.set(user.id, billingByUser.get("101") as BillingResponse);
    saveUserModelAccessSummary(user.id, getUserModelAccessSummary(user.id));
    accountStore.set(user.id, {
      profile: {
        nickname: user.nickname,
        email: user.email ?? "",
        phone: "",
        company: "",
        bio: "",
      },
      notifications: {
        balanceLow: true,
        keyUsage: true,
        monthlyReport: true,
        newModel: false,
        marketing: false,
        email: true,
        wechat: false,
      },
    });
    registerCodeStore.delete(normalizedEmail);

    return toResponse(201, success(issueSession(user)));
  }

  if ((path === "/admin/auth/me" || path === "/auth/me") && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const token = getTokenFromHeaders(init?.headers);
    return toResponse(200, success(toAdminAuthResponse(auth.user, token ?? "")));
  }

  if ((path === "/admin/auth/logout" || path === "/auth/logout") && method === "POST") {
    return toResponse(200, success({ ok: true }));
  }

  if (path === "/admin/dashboard/overview" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    return toResponse(200, success(auth.user.role === "admin" ? dashboardOverviewData : userDashboardOverviewData));
  }

  if (path === "/admin/dashboard/trend" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const source = auth.user.role === "admin" ? dashboardTrendData : userDashboardTrendData;
    const daysValue = Number(url.searchParams.get("days") ?? "7");
    const days = Number.isFinite(daysValue) ? Math.min(Math.max(Math.trunc(daysValue), 1), 30) : 7;
    return toResponse(200, success(source.slice(-days)));
  }

  if (path === "/admin/dashboard/model-stats" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    return toResponse(200, success(auth.user.role === "admin" ? dashboardModelStatsData : userDashboardModelStatsData));
  }

  if (path === "/admin/models" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    return toResponse(200, success(adminModelsData));
  }

  if (path === "/admin/models/upstream" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const providerId = url.searchParams.get("providerId") ?? "";
    if (!providerId) {
      return toResponse(400, failure("BAD_REQUEST", "providerId 不能为空"));
    }

    return toResponse(200, success(upstreamModelsByProvider.get(providerId) ?? []));
  }

  if (path === "/admin/models" && method === "POST") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const body = (await readJson<ModelCreateRequest>(init)) ?? {
      modelCode: "",
      modelName: "",
      groupId: "",
      providerId: "",
      upstreamModel: "",
    };

    if (
      !body.modelCode?.trim() ||
      !body.modelName?.trim() ||
      !body.groupId ||
      !body.providerId ||
      !body.upstreamModel?.trim()
    ) {
      return toResponse(
        400,
        failure("BAD_REQUEST", "modelCode、modelName、groupId、providerId、upstreamModel 不能为空"),
      );
    }

    if (adminModelsData.some((item) => item.modelCode === body.modelCode.trim())) {
      return toResponse(400, failure("BAD_REQUEST", "模型编码已存在"));
    }

    const provider = findProviderById(body.providerId);
    if (!provider) {
      return toResponse(404, failure("NOT_FOUND", "渠道不存在"));
    }

    const group = findGroupById(body.groupId);
    if (!group) {
      return toResponse(404, failure("NOT_FOUND", "套餐分组不存在"));
    }

    adminModelsData = [
      {
        id: String(Date.now()),
        bindingId: String(Date.now() + 1000),
        modelCode: body.modelCode.trim(),
        modelName: body.modelName.trim(),
        modelType: body.modelType?.trim() || null,
        billingType: body.billingType?.trim() || null,
        promptPrice: body.promptPrice ?? null,
        cachedPromptPrice: body.cachedPromptPrice ?? null,
        completionPrice: body.completionPrice ?? null,
        requestPrice: body.requestPrice ?? null,
        multiplier: body.multiplier ?? null,
        isPublic: body.isPublic ? 1 : 0,
        status: "ACTIVE",
        groupId: group.id,
        groupCode: group.groupCode,
        groupName: group.groupName,
        providerId: provider.id,
        providerName: provider.providerName,
        providerType: provider.providerType,
        upstreamModel: body.upstreamModel.trim(),
        createdAt: new Date().toISOString(),
      },
      ...adminModelsData,
    ];

    return toResponse(200, success(null));
  }

  if (path.startsWith("/admin/models/") && method === "PUT") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const modelId = path.split("/")[3];
    const body = (await readJson<ModelUpdateRequest>(init)) ?? {
      modelName: "",
      groupId: "",
      providerId: "",
      upstreamModel: "",
    };

    if (
      !body.modelName?.trim() ||
      !body.groupId ||
      !body.providerId ||
      !body.upstreamModel?.trim()
    ) {
      return toResponse(
        400,
        failure("BAD_REQUEST", "modelName、groupId、providerId、upstreamModel 不能为空"),
      );
    }

    const provider = findProviderById(body.providerId);
    if (!provider) {
      return toResponse(404, failure("NOT_FOUND", "渠道不存在"));
    }

    const group = findGroupById(body.groupId);
    if (!group) {
      return toResponse(404, failure("NOT_FOUND", "套餐分组不存在"));
    }

    const currentModel = adminModelsData.find((item) => item.id === modelId);
    if (!currentModel) {
      return toResponse(404, failure("NOT_FOUND", "模型不存在"));
    }

    adminModelsData = adminModelsData.map((item) =>
      item.id === modelId
        ? {
            ...item,
            bindingId: body.bindingId ?? item.bindingId ?? null,
            modelName: body.modelName.trim(),
            modelType: body.modelType?.trim() || null,
            billingType: body.billingType?.trim() || null,
            promptPrice: body.promptPrice ?? null,
            cachedPromptPrice: body.cachedPromptPrice ?? null,
            completionPrice: body.completionPrice ?? null,
            requestPrice: body.requestPrice ?? null,
            multiplier: body.multiplier ?? null,
            isPublic: body.isPublic ? 1 : 0,
            groupId: group.id,
            groupCode: group.groupCode,
            groupName: group.groupName,
            providerId: provider.id,
            providerName: provider.providerName,
            providerType: provider.providerType,
            upstreamModel: body.upstreamModel.trim(),
          }
        : item,
    );

    return toResponse(200, success(null));
  }

  if (path === "/admin/models/import" && method === "POST") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const body = (await readJson<ModelBatchImportRequest>(init)) ?? {
      groupId: "",
      providerId: "",
      upstreamModels: [],
    };

    if (!body.groupId || !body.providerId || !Array.isArray(body.upstreamModels) || body.upstreamModels.length === 0) {
      return toResponse(400, failure("BAD_REQUEST", "groupId、providerId、upstreamModels 不能为空"));
    }

    const provider = findProviderById(body.providerId);
    if (!provider) {
      return toResponse(404, failure("NOT_FOUND", "渠道不存在"));
    }

    const group = findGroupById(body.groupId);
    if (!group) {
      return toResponse(404, failure("NOT_FOUND", "套餐分组不存在"));
    }

    const existingCodes = new Set(adminModelsData.map((item) => item.modelCode));
    const importedModels: string[] = [];
    const skippedModels: string[] = [];

    for (const upstreamModel of body.upstreamModels) {
      const normalizedModel = upstreamModel.trim();
      if (!normalizedModel) {
        continue;
      }

      if (existingCodes.has(normalizedModel)) {
        skippedModels.push(normalizedModel);
        continue;
      }

      existingCodes.add(normalizedModel);
      importedModels.push(normalizedModel);
      adminModelsData = [
        {
          id: String(Date.now() + importedModels.length),
          bindingId: String(Date.now() + 2000 + importedModels.length),
          modelCode: normalizedModel,
          modelName: normalizedModel,
          modelType: "chat",
          billingType: "TOKEN",
          promptPrice: body.promptPrice ?? null,
          cachedPromptPrice: body.cachedPromptPrice ?? null,
          completionPrice: body.completionPrice ?? null,
          requestPrice: null,
          multiplier: body.multiplier ?? null,
          isPublic: body.isPublic ? 1 : 0,
          status: "ACTIVE",
          groupId: group.id,
          groupCode: group.groupCode,
          groupName: group.groupName,
          providerId: provider.id,
          providerName: provider.providerName,
          providerType: provider.providerType,
          upstreamModel: normalizedModel,
          createdAt: new Date().toISOString(),
        },
        ...adminModelsData,
      ];
    }

    const response: ModelBatchImportResponse = {
      importedCount: importedModels.length,
      skippedCount: skippedModels.length,
      importedModels,
      skippedModels,
    };

    return toResponse(200, success(response));
  }

  if (path === "/admin/request-logs" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const limitValue = Number(url.searchParams.get("limit") ?? "20");
    const limit = Number.isFinite(limitValue) ? Math.min(Math.max(Math.trunc(limitValue), 1), 100) : 20;
    const scopedLogs = auth.user.role === "admin"
      ? requestLogsData
      : requestLogsData.filter((item) => item.username === auth.user.username);

    return toResponse(200, success(scopedLogs.slice(0, limit)));
  }

  if (path === "/admin/system/site-settings" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    if (auth.user.role !== "admin") {
      return toResponse(403, failure("FORBIDDEN", "当前账号没有系统设置权限"));
    }

    return toResponse(200, success(siteSettingsData));
  }

  if (path === "/admin/system/site-settings" && method === "PUT") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    if (auth.user.role !== "admin") {
      return toResponse(403, failure("FORBIDDEN", "当前账号没有系统设置权限"));
    }

    const body = (await readJson<SiteSettingsUpdateRequest>(init)) ?? {
      siteName: "",
      adminEmail: "",
      baseUrl: "",
      themeMode: "LIGHT",
    };

    const siteName = body.siteName?.trim() ?? "";
    const adminEmail = body.adminEmail?.trim() ?? "";
    const siteDescription = body.siteDescription?.trim() ?? "";
    const baseUrl = (body.baseUrl?.trim() ?? "").replace(/\/+$/, "");
    const footerText = body.footerText?.trim() ?? "";
    const themeMode = (body.themeMode?.trim().toUpperCase() ?? "") as SiteSettingsUpdateRequest["themeMode"];

    if (!siteName) {
      return toResponse(400, failure("BAD_REQUEST", "siteName 不能为空"));
    }
    if (siteName.length > 128) {
      return toResponse(400, failure("BAD_REQUEST", "siteName 不能超过 128 个字符"));
    }
    if (!adminEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(adminEmail)) {
      return toResponse(400, failure("BAD_REQUEST", "adminEmail 格式不合法"));
    }
    if (siteDescription.length > 255) {
      return toResponse(400, failure("BAD_REQUEST", "siteDescription 不能超过 255 个字符"));
    }
    if (!/^https?:\/\//.test(baseUrl)) {
      return toResponse(400, failure("BAD_REQUEST", "baseUrl 必须以 http:// 或 https:// 开头"));
    }
    if (footerText.length > 255) {
      return toResponse(400, failure("BAD_REQUEST", "footerText 不能超过 255 个字符"));
    }
    if (themeMode !== "LIGHT" && themeMode !== "DARK") {
      return toResponse(400, failure("BAD_REQUEST", "themeMode 仅支持 LIGHT 或 DARK"));
    }

    siteSettingsData = {
      ...siteSettingsData,
      siteName,
      adminEmail,
      siteDescription: siteDescription || null,
      baseUrl,
      footerText: footerText || null,
      themeMode,
      updatedAt: new Date().toISOString(),
    };

    return toResponse(200, success(null, "站点信息保存成功"));
  }

  if (path === "/admin/model-access/summary" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    return toResponse(200, success(getUserModelAccessSummary(auth.user.id)));
  }

  if (path === "/admin/model-access/groups" && method === "POST") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const body = (await readJson<ModelGroupCreateRequest>(init)) ?? {
      groupCode: "",
      groupName: "",
      salePrice: 0,
      packageDays: 0,
      dailyQuota: 0,
      weeklyQuota: 0,
      monthlyQuota: 0,
    };

    const groupCode = body.groupCode?.trim() ?? "";
    const groupName = body.groupName?.trim() ?? "";

    if (!groupCode || !groupName) {
      return toResponse(400, failure("BAD_REQUEST", "groupCode 和 groupName 不能为空"));
    }

    if (defaultPackageGroups.some((group) => group.groupCode === groupCode)) {
      return toResponse(400, failure("BAD_REQUEST", "套餐编码已存在"));
    }

    const nextGroup: ModelGroupOptionResponse = {
      id: String(Date.now()),
      groupCode,
      groupName,
      packageType: body.packageType === "BALANCE" ? "BALANCE" : "QUOTA",
      packageTypeText: body.packageType === "BALANCE" ? "余额套餐" : "普通套餐",
      salePrice: body.packageType === "BALANCE" ? 0 : Number(body.salePrice ?? 0),
      packageDays: body.packageType === "BALANCE" ? 0 : Number(body.packageDays ?? 0),
      dailyQuota: body.packageType === "BALANCE" ? 0 : Number(body.dailyQuota ?? 0),
      weeklyQuota: body.packageType === "BALANCE" ? 0 : Number(body.weeklyQuota ?? 0),
      monthlyQuota: body.packageType === "BALANCE" ? 0 : Number(body.monthlyQuota ?? 0),
      modelCount: 0,
      purchased: false,
      active: false,
      expiresAt: null,
      remainingDays: null,
      dailyUsed: 0,
      weeklyUsed: 0,
      monthlyUsed: 0,
      packageStatus: "NOT_PURCHASED",
      packageStatusText: body.packageType === "BALANCE" ? "未开通余额计费" : "未购买",
      remark: body.remark?.trim() || null,
      systemPreset: false,
    };

    defaultPackageGroups = [...defaultPackageGroups, nextGroup];
    appendPackageGroupToAllSummaries(nextGroup);

    return toResponse(200, success(getUserModelAccessSummary(auth.user.id)));
  }

  if (path === "/admin/model-access/purchases" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const scopedRecords =
      auth.user.role === "admin"
        ? modelAccessPurchaseRecords
        : modelAccessPurchaseRecords.filter((item) => String(item.userId) === auth.user.id);

    const records = [...scopedRecords].sort((left, right) => {
      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });

    return toResponse(200, success(records));
  }

  if (path === "/admin/model-access/wallet-transactions" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const scopedRecords =
      auth.user.role === "admin"
        ? walletTransactionsData
        : walletTransactionsData.filter((item) => String(item.userId) === auth.user.id);

    const records = [...scopedRecords].sort((left, right) => {
      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });

    return toResponse(200, success(records));
  }

  if (path === "/admin/model-access/purchase" && method === "POST") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const body = (await readJson<PurchaseModelPackageRequest>(init)) ?? { groupId: "" };
    if (!body.groupId) {
      return toResponse(400, failure("BAD_REQUEST", "groupId 不能为空"));
    }

    const currentSummary = getUserModelAccessSummary(auth.user.id);
    const targetGroup = currentSummary.groups.find((group) => group.id === body.groupId);
    if (!targetGroup) {
      return toResponse(404, failure("NOT_FOUND", "套餐不存在"));
    }

    if (targetGroup.purchased && targetGroup.packageType === "BALANCE") {
      return toResponse(400, failure("BAD_REQUEST", "余额套餐已开通，无需重复购买"));
    }

    const userBalance = auth.user.balance ?? 0;
    const isBalancePackage = targetGroup.packageType === "BALANCE";

    if (isBalancePackage && userBalance < 1) {
      return toResponse(
        400,
        failure("BALANCE_NOT_ENOUGH", "当前余额不足 1 美元，无法购买该套餐，请先充值"),
      );
    }

    const nextGroups = currentSummary.groups.map((group) => {
      if (group.id === body.groupId) {
        if (isBalancePackage) {
          return {
            ...group,
            purchased: true,
            active: false,
            expiresAt: null,
            remainingDays: null,
            dailyUsed: 0,
            weeklyUsed: 0,
            monthlyUsed: 0,
            packageStatus: "ACTIVE",
            packageStatusText: "已开通余额计费",
          };
        }

        return {
          ...group,
          purchased: true,
          active: true,
          expiresAt: "2026-05-27T23:59:59+08:00",
          remainingDays: group.packageDays,
          dailyUsed: 0,
          weeklyUsed: 0,
          monthlyUsed: 0,
          packageStatus: "ACTIVE",
          packageStatusText: "生效中",
        };
      }

      return {
        ...group,
        active: isBalancePackage ? group.active : false,
        packageStatus:
          isBalancePackage
            ? group.packageStatus
            : group.purchased
              ? "INACTIVE"
              : "NOT_PURCHASED",
        packageStatusText:
          isBalancePackage
            ? group.packageStatusText
            : group.purchased
              ? "已购买，未生效"
              : "未购买",
      };
    });

    const activated = nextGroups.find((group) => group.id === body.groupId)!;
    const nextBalance = isBalancePackage
      ? userBalance
      : Number((userBalance - activated.salePrice).toFixed(2));
    const shouldExposeBalanceAsCurrent = isBalancePackage && !currentSummary.activeGroupId;
    const nextSummary: ModelAccessSummaryResponse = {
      packageRestrictionEnabled: true,
      packageStatus:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? "ACTIVE"
            : currentSummary.packageStatus
          : "ACTIVE",
      packageStatusText:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? "已开通余额套餐"
            : currentSummary.packageStatusText
          : "当前套餐生效中",
      activeGroupId:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.id
            : currentSummary.activeGroupId
          : activated.id,
      activeGroupCode:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.groupCode
            : currentSummary.activeGroupCode
          : activated.groupCode,
      activeGroupName:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.groupName
            : currentSummary.activeGroupName
          : activated.groupName,
      packagePrice:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.salePrice
            : currentSummary.packagePrice
          : activated.salePrice,
      dailyQuota:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.dailyQuota
            : currentSummary.dailyQuota
          : activated.dailyQuota,
      weeklyQuota:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.weeklyQuota
            : currentSummary.weeklyQuota
          : activated.weeklyQuota,
      monthlyQuota:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.monthlyQuota
            : currentSummary.monthlyQuota
          : activated.monthlyQuota,
      dailyUsed:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.dailyUsed
            : currentSummary.dailyUsed
          : activated.dailyUsed,
      weeklyUsed:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.weeklyUsed
            : currentSummary.weeklyUsed
          : activated.weeklyUsed,
      monthlyUsed:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.monthlyUsed
            : currentSummary.monthlyUsed
          : activated.monthlyUsed,
      expiresAt:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.expiresAt
            : currentSummary.expiresAt
          : activated.expiresAt,
      remainingDays:
        isBalancePackage
          ? shouldExposeBalanceAsCurrent
            ? activated.remainingDays
            : currentSummary.remainingDays
          : activated.remainingDays,
      groups: nextGroups,
    };

    saveUserModelAccessSummary(auth.user.id, nextSummary);
    if (!isBalancePackage) {
      updateUserBalance(auth.user.id, nextBalance);
    }

    const recordId = String(Date.now());
    const walletId = auth.user.id === "1" ? "5002" : "5001";
    const createdAt = new Date().toISOString();
    const transactionDate = createdAt.slice(0, 10);
    const purchaseRecord: ModelPackagePurchaseRecordResponse = {
      id: recordId,
      userId: auth.user.id,
      username: auth.user.username,
      groupId: activated.id,
      groupCode: activated.groupCode,
      groupName: activated.groupName,
      packageType: activated.packageType,
      packageTypeText: activated.packageTypeText,
      modelCount: activated.modelCount,
      purchasePrice: activated.salePrice,
      startAt: isBalancePackage ? createdAt : createdAt,
      expiresAt: activated.expiresAt,
      status: "ACTIVE",
      createdAt,
      active: !isBalancePackage,
      dailyQuota: activated.dailyQuota,
      weeklyQuota: activated.weeklyQuota,
      monthlyQuota: activated.monthlyQuota,
      totalQuota: activated.monthlyQuota,
      dailyUsed: activated.dailyUsed,
      weeklyUsed: activated.weeklyUsed,
      monthlyUsed: activated.monthlyUsed,
      totalUsed: activated.monthlyUsed,
      remainingDays: activated.remainingDays,
    };
    modelAccessPurchaseRecords = [purchaseRecord, ...modelAccessPurchaseRecords];

    if (!isBalancePackage) {
      const walletTransaction: WalletTransactionItemResponse = {
        id: `tx_${recordId}`,
        userId: auth.user.id,
        username: auth.user.username,
        walletId,
        orderNo: `PKG${createdAt.replace(/[-:TZ.]/g, "").slice(0, 14)}`,
        transactionType: "PACKAGE_BUY",
        direction: "OUT",
        amount: activated.salePrice,
        balanceBefore: userBalance,
        balanceAfter: nextBalance,
        status: "SUCCESS",
        descriptionText: `购买 ${activated.groupName}`,
        transactionDate,
        createdAt,
      };
      walletTransactionsData = [walletTransaction, ...walletTransactionsData];
    }

    return toResponse(200, success(nextSummary));
  }

  if (path.startsWith("/admin/model-access/") && method === "DELETE") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const pathParts = path.split("/");
    if (pathParts.length === 4) {
      const groupId = pathParts[3];
      const exists = defaultPackageGroups.some((group) => group.id === groupId);

      if (!exists) {
        return toResponse(404, failure("NOT_FOUND", "套餐分组不存在"));
      }

      defaultPackageGroups = defaultPackageGroups.filter((group) => group.id !== groupId);
      removePackageGroupFromAllSummaries(groupId);

      return toResponse(200, success(null));
    }
  }

  if (path === "/admin/api-keys" && method === "GET") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const allKeys = getAllApiKeys();
    const result = auth.user.role === "admin"
      ? allKeys
      : allKeys.filter((item) => item.userId === auth.user.id);

    return toResponse(200, success(result));
  }

  if (path === "/admin/api-keys" && method === "POST") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const body = (await readJson<ApiKeyCreateRequest>(init)) ?? { userId: "", name: "" };

    if (!body.userId || !body.name) {
      return toResponse(400, failure("BAD_REQUEST", "userId 和 name 不能为空"));
    }

    if (auth.user.role !== "admin" && body.userId !== auth.user.id) {
      return toResponse(403, failure("FORBIDDEN", "普通用户只能为自己创建 API Key"));
    }

    const targetUser = findUserByNumericId(body.userId);
    if (!targetUser) {
      return toResponse(404, failure("NOT_FOUND", "目标用户不存在"));
    }

    let selectedGroup: ModelGroupOptionResponse | null = null;
    if (auth.user.role !== "admin") {
      const summary = getUserModelAccessSummary(body.userId);
      const purchasedGroups = summary.groups.filter((group) => group.purchased);

      if (purchasedGroups.length === 0) {
        return toResponse(400, failure("PACKAGE_REQUIRED", "当前账号尚未开通套餐，无法创建 API Key"));
      }

      if (!body.modelGroupId) {
        return toResponse(400, failure("MODEL_GROUP_REQUIRED", "请选择一个已购套餐"));
      }

      selectedGroup = purchasedGroups.find((group) => group.id === body.modelGroupId) ?? null;
      if (!selectedGroup) {
        return toResponse(400, failure("MODEL_GROUP_INVALID", "所选套餐不可用，请重新选择"));
      }
    } else if (body.modelGroupId) {
      const summary = getUserModelAccessSummary(body.userId);
      selectedGroup = summary.groups.find((group) => group.id === body.modelGroupId) ?? null;
    }

    const plainTextKey = `sk-hub-${Math.random().toString(36).slice(2, 10)}${Math.random().toString(36).slice(2, 6)}`;
    const maskedKey = `${plainTextKey.slice(0, 12)}...${plainTextKey.slice(-4)}`;
    const nextKey: UserKey = {
      id: String(Date.now()),
      name: body.name,
      prefix: maskedKey,
      full: plainTextKey,
      createdAt: new Date().toISOString(),
      lastUsed: "未使用",
      requests: 0,
      tokens: 0,
      spent: 0,
      status: "active",
      rateLimit: 0,
      budget: null,
    };

    const current = keysStore.get(body.userId) ?? [];
    keysStore.set(body.userId, [nextKey, ...current]);
    apiKeyPackageBindings.set(nextKey.id, {
      modelGroupId: selectedGroup?.id ?? body.modelGroupId ?? null,
      modelGroupName: selectedGroup?.groupName ?? null,
      totalQuota: selectedGroup?.monthlyQuota ?? null,
      expiresAt: body.expiresAt ?? selectedGroup?.expiresAt ?? null,
    });

    const response: ApiKeyCreateResponse = {
      id: nextKey.id,
      plainTextKey,
    };

    return toResponse(200, success(response));
  }

  if (path.startsWith("/admin/api-keys/") && path.endsWith("/status") && method === "PUT") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const body = (await readJson<ApiKeyStatusUpdateRequest>(init)) ?? { status: "" };
    if (!body.status) {
      return toResponse(400, failure("BAD_REQUEST", "status 不能为空"));
    }

    const keyId = path.split("/")[3];
    let updated = false;

    for (const [userId, keys] of keysStore.entries()) {
      const targetKey = keys.find((item) => item.id === keyId);
      if (!targetKey) {
        continue;
      }

      if (auth.user.role !== "admin" && userId !== auth.user.id) {
        return toResponse(403, failure("FORBIDDEN", "普通用户只能修改自己的 API Key"));
      }

      keysStore.set(
        userId,
        keys.map((item) =>
          item.id === keyId ? { ...item, status: body.status.toLowerCase() as UserKey["status"] } : item,
        ),
      );
      updated = true;
      break;
    }

    if (!updated) {
      return toResponse(404, failure("NOT_FOUND", "API Key 不存在"));
    }

    return toResponse(200, success(null));
  }

  if (path.startsWith("/admin/api-keys/") && method === "DELETE") {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const keyId = path.split("/")[3];
    let removed = false;

    for (const [userId, keys] of keysStore.entries()) {
      const exists = keys.some((item) => item.id === keyId);
      if (!exists) {
        continue;
      }

      if (auth.user.role !== "admin" && userId !== auth.user.id) {
        return toResponse(403, failure("FORBIDDEN", "普通用户只能删除自己的 API Key"));
      }

      keysStore.set(
        userId,
        keys.filter((item) => item.id !== keyId),
      );
      apiKeyPackageBindings.delete(keyId);
      removed = true;
      break;
    }

    if (!removed) {
      return toResponse(404, failure("NOT_FOUND", "API Key 不存在"));
    }

    return toResponse(200, success(null));
  }

  if (path.startsWith("/console")) {
    const auth = authorize(init?.headers);
    if (!auth.ok) {
      return auth.response;
    }

    const userId = auth.user.id;

    if (path === "/console/overview" && method === "GET") {
      return toResponse(200, success(overviewDataByUser.get(userId) ?? overviewDataByUser.get("101")));
    }

    if (path === "/console/keys" && method === "GET") {
      return toResponse(200, success(keysStore.get(userId) ?? []));
    }

    if (path === "/console/keys" && method === "POST") {
      const body = (await readJson<{ name: string; rateLimit?: number; budget?: number | null }>(init)) ?? { name: "" };

      if (!body.name) {
        return toResponse(400, failure("BAD_REQUEST", "Key 名称不能为空"));
      }

      const full = `sk-hub-${Math.random().toString(36).slice(2, 8)}...${Math.random().toString(36).slice(2, 6)}`;
      const key: UserKey = {
        id: randomId("key"),
        name: body.name,
        prefix: full,
        full,
        createdAt: new Date().toISOString().slice(0, 10),
        lastUsed: "未使用",
        requests: 0,
        tokens: 0,
        spent: 0,
        status: "active",
        rateLimit: body.rateLimit ?? 100,
        budget: body.budget ?? null,
      };

      const current = keysStore.get(userId) ?? [];
      keysStore.set(userId, [key, ...current]);
      return toResponse(201, success(key));
    }

    if (path.startsWith("/console/keys/") && method === "PATCH") {
      const keyId = path.split("/").pop();
      const body = (await readJson<Partial<UserKey>>(init)) ?? {};
      const current = keysStore.get(userId) ?? [];
      const updated = current.map((it) => (it.id === keyId ? { ...it, ...body } : it));
      const target = updated.find((it) => it.id === keyId);
      if (!target) {
        return toResponse(404, failure("NOT_FOUND", "Key 不存在"));
      }
      keysStore.set(userId, updated);
      return toResponse(200, success(target));
    }

    if (path.startsWith("/console/keys/") && method === "DELETE") {
      const keyId = path.split("/").pop();
      const current = keysStore.get(userId) ?? [];
      keysStore.set(
        userId,
        current.filter((it) => it.id !== keyId),
      );
      return toResponse(200, success({ ok: true }));
    }

    if (path === "/console/models" && method === "GET") {
      return toResponse(200, success(modelsData));
    }

    if (path === "/console/billing" && method === "GET") {
      return toResponse(200, success(billingByUser.get(userId) ?? billingByUser.get("101")));
    }

    if (path === "/console/account" && method === "GET") {
      return toResponse(200, success(accountStore.get(userId) ?? accountStore.get("101")));
    }

    if (path === "/console/account/profile" && method === "PUT") {
      const body = (await readJson<AccountSettings["profile"]>(init)) as AccountSettings["profile"] | null;
      if (!body) {
        return toResponse(400, failure("BAD_REQUEST", "资料不能为空"));
      }
      const current = accountStore.get(userId) ?? accountStore.get("101");
      if (!current) {
        return toResponse(404, failure("NOT_FOUND", "账户信息不存在"));
      }
      const next: AccountSettings = {
        ...current,
        profile: body,
      };
      accountStore.set(userId, next);
      updateUserProfile(userId, body);
      return toResponse(200, success(next));
    }

    if (path === "/console/account/notifications" && method === "PUT") {
      const body = (await readJson<AccountSettings["notifications"]>(init)) as AccountSettings["notifications"] | null;
      if (!body) {
        return toResponse(400, failure("BAD_REQUEST", "通知配置不能为空"));
      }
      const current = accountStore.get(userId) ?? accountStore.get("101");
      if (!current) {
        return toResponse(404, failure("NOT_FOUND", "账户信息不存在"));
      }
      const next: AccountSettings = {
        ...current,
        notifications: body,
      };
      accountStore.set(userId, next);
      return toResponse(200, success(next));
    }
  }

  return toResponse(404, failure("NOT_FOUND", `未匹配接口: ${method} ${path}`));
}
