export const AUTH_API_ENDPOINTS = {
  login: "/admin/auth/login",
  loginCaptcha: "/admin/auth/login/captcha",
  register: "/admin/auth/register",
  registerCode: "/admin/auth/register/code",
  passwordResetCode: "/admin/auth/password/reset/code",
  passwordReset: "/admin/auth/password/reset",
  me: "/admin/auth/me",
  logout: "/admin/auth/logout",
} as const;

export const API_KEYS_API_ENDPOINTS = {
  list: "/admin/api-keys",
  create: "/admin/api-keys",
  updateStatus: (id: string) => `/admin/api-keys/${id}/status`,
  remove: (id: string) => `/admin/api-keys/${id}`,
} as const;

export const USERS_API_ENDPOINTS = {
  list: "/admin/users",
  detail: (userId: string) => `/admin/users/${userId}`,
  create: "/admin/users",
  update: (userId: string) => `/admin/users/${userId}`,
  updateStatus: (userId: string) => `/admin/users/${userId}/status`,
  remove: (userId: string) => `/admin/users/${userId}`,
  recharge: "/admin/users/recharge",
} as const;

export const PROVIDERS_API_ENDPOINTS = {
  list: "/admin/providers",
  create: "/admin/providers",
  updateStatus: (id: string) => `/admin/providers/${id}/status`,
} as const;

export const MODELS_API_ENDPOINTS = {
  list: "/admin/models",
  upstream: (providerId: string) => `/admin/models/upstream?providerId=${providerId}`,
  create: "/admin/models",
  update: (id: string) => `/admin/models/${id}`,
  import: "/admin/models/import",
} as const;

export const DASHBOARD_API_ENDPOINTS = {
  overview: "/admin/dashboard/overview",
  trend: (days = 7) => `/admin/dashboard/trend?days=${days}`,
  modelStats: "/admin/dashboard/model-stats",
} as const;

export const REQUEST_LOGS_API_ENDPOINTS = {
  list: (limit = 20) => `/admin/request-logs?limit=${limit}`,
} as const;

export const SYSTEM_API_ENDPOINTS = {
  siteSettings: "/admin/system/site-settings",
} as const;

export const CONSOLE_API_ENDPOINTS = {
  overview: "/console/overview",
  keys: "/console/keys",
  keyDetail: (keyId: string) => `/console/keys/${keyId}`,
  billing: "/console/billing",
  account: "/console/account",
  accountProfile: "/console/account/profile",
  accountNotifications: "/console/account/notifications",
} as const;

export const MODEL_ACCESS_API_ENDPOINTS = {
  summary: "/admin/model-access/summary",
  groups: "/admin/model-access/groups",
  purchases: "/admin/model-access/purchases",
  walletTransactions: "/admin/model-access/wallet-transactions",
  purchase: "/admin/model-access/purchase",
  remove: (groupId: string) => `/admin/model-access/${groupId}`,
} as const;
