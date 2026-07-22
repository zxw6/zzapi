const isTestRuntime = import.meta.env.MODE === "test" || Boolean(import.meta.env.VITEST);
const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL?.trim() || "http://www.zlapi.online/api";
const apiMode = import.meta.env.VITE_API_MODE?.trim() || (isTestRuntime ? "mock" : "real");
const appName = import.meta.env.VITE_APP_NAME?.trim() || "API Hub";

export const env = {
  apiBaseUrl,
  apiMode,
  appName,
  requestTimeoutMs: 10_000,
  requestRetry: 1,
} as const;

export const isMockApi = env.apiMode === "mock";
