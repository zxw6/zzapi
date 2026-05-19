export type ApiErrorPayload = {
  code: string;
  message: string;
  details?: unknown;
};

export type ApiSuccess<T> = {
  success: true;
  data: T;
  message?: string;
  requestId: string;
  timestamp: string;
};

export type ApiFailure = {
  success: false;
  message?: string;
  data?: unknown;
  error: ApiErrorPayload;
  requestId: string;
  timestamp: string;
};

export type ApiResponse<T> = ApiSuccess<T> | ApiFailure;

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type RequestOptions = {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
  auth?: boolean;
  timeoutMs?: number;
  retry?: number;
  signal?: AbortSignal;
};
