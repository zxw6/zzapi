import { request } from "../../../lib/http/client";
import { API_KEYS_API_ENDPOINTS } from "../endpoints";
import type {
  ApiKeyCreateRequest,
  ApiKeyCreateResponse,
  ApiKeyListItemResponse,
  ApiKeyStatusUpdateRequest,
} from "../types";

export const apiKeysApi = {
  list: () => request<ApiKeyListItemResponse[]>(API_KEYS_API_ENDPOINTS.list),

  create: (input: ApiKeyCreateRequest) =>
    request<ApiKeyCreateResponse>(API_KEYS_API_ENDPOINTS.create, {
      method: "POST",
      body: input,
    }),

  updateStatus: (id: string, input: ApiKeyStatusUpdateRequest) =>
    request<null>(API_KEYS_API_ENDPOINTS.updateStatus(id), {
      method: "PUT",
      body: input,
    }),

  remove: (id: string) =>
    request<null>(API_KEYS_API_ENDPOINTS.remove(id), {
      method: "DELETE",
    }),
};
