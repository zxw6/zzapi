import { request } from "../../../lib/http/client";
import { REQUEST_LOGS_API_ENDPOINTS } from "../endpoints";
import type { RequestLogItemResponse } from "../types";

type RequestLogListEnvelope =
  | RequestLogItemResponse[]
  | {
      records?: RequestLogItemResponse[] | null;
      list?: RequestLogItemResponse[] | null;
      items?: RequestLogItemResponse[] | null;
      data?: RequestLogItemResponse[] | null;
    };

function normalizeRequestLogsResponse(payload: RequestLogListEnvelope | null | undefined) {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (!payload || typeof payload !== "object") {
    return [];
  }

  const candidates = [payload.records, payload.list, payload.items, payload.data];
  return candidates.find(Array.isArray) ?? [];
}

export const requestLogsApi = {
  list: async (limit = 20) => {
    const response = await request<RequestLogListEnvelope>(REQUEST_LOGS_API_ENDPOINTS.list(limit));
    return normalizeRequestLogsResponse(response);
  },
};
