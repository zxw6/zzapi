import { env, isMockApi } from "../config/env";
import { AppError } from "./error";
import type { ApiResponse, RequestOptions } from "./types";
import { mockFetch } from "../../mocks/server";

type AuthHooks = {
  getAccessToken: () => string | null;
  refreshAuthToken: () => Promise<boolean>;
  onUnauthorized: () => void;
};

let authHooks: AuthHooks = {
  getAccessToken: () => null,
  refreshAuthToken: async () => false,
  onUnauthorized: () => undefined,
};

export function configureHttpAuth(nextHooks: Partial<AuthHooks>) {
  authHooks = {
    ...authHooks,
    ...nextHooks,
  };
}

function makeUrl(path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  return `${env.apiBaseUrl}${path.startsWith("/") ? path : `/${path}`}`;
}

function buildHeaders(options: RequestOptions): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: "application/json",
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...options.headers,
  };

  if (options.auth !== false) {
    const token = authHooks.getAccessToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  return headers;
}

function shouldRetry(error: unknown): boolean {
  if (!(error instanceof AppError)) {
    return true;
  }

  if (error.code === "TIMEOUT") {
    return true;
  }

  return error.status >= 500;
}

async function parseResponse<T>(response: Response): Promise<T> {
  const payload = (await response.json()) as ApiResponse<T>;

  if (!payload.success) {
    const errorPayload = "error" in payload ? payload.error : undefined;
    throw new AppError({
      code: errorPayload?.code ?? `HTTP_${response.status}`,
      message: errorPayload?.message ?? payload.message ?? response.statusText ?? "请求失败",
      status: response.status,
      requestId: "requestId" in payload ? payload.requestId : undefined,
      details: errorPayload?.details ?? ("data" in payload ? payload.data : undefined),
    });
  }

  return payload.data;
}

async function runFetch(url: string, init: RequestInit): Promise<Response> {
  if (isMockApi) {
    return mockFetch(url, init);
  }

  return fetch(url, init);
}

async function requestInternal<T>(
  path: string,
  options: RequestOptions,
  allowRefresh = true,
): Promise<T> {
  const timeoutMs = options.timeoutMs ?? env.requestTimeoutMs;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await runFetch(makeUrl(path), {
      method: options.method ?? "GET",
      headers: buildHeaders(options),
      body: options.body ? JSON.stringify(options.body) : undefined,
      signal: options.signal ?? controller.signal,
    });

    if (response.status === 401 && options.auth !== false && allowRefresh) {
      const refreshed = await authHooks.refreshAuthToken();
      if (refreshed) {
        return requestInternal<T>(path, options, false);
      }

      authHooks.onUnauthorized();
    }

    return parseResponse<T>(response);
  } catch (error) {
    if ((error as DOMException)?.name === "AbortError") {
      throw new AppError({
        code: "TIMEOUT",
        message: `请求超时（${timeoutMs}ms）`,
        status: 408,
      });
    }

    if (error instanceof AppError) {
      throw error;
    }

    throw new AppError({
      code: "NETWORK_ERROR",
      message: "网络异常，请稍后重试",
      status: 503,
      details: error,
    });
  } finally {
    clearTimeout(timeout);
  }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const retry = options.retry ?? env.requestRetry;

  let latestError: unknown;
  for (let attempt = 0; attempt <= retry; attempt += 1) {
    try {
      return await requestInternal<T>(path, options);
    } catch (error) {
      latestError = error;
      if (attempt >= retry || !shouldRetry(error)) {
        throw error;
      }
    }
  }

  throw latestError;
}
