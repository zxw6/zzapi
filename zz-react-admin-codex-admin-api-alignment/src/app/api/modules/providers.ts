import { request } from "../../../lib/http/client";
import { PROVIDERS_API_ENDPOINTS } from "../endpoints";
import type {
  ProviderCreateRequest,
  ProviderListItemResponse,
  ProviderStatusUpdateRequest,
} from "../types";

export const providersApi = {
  list: () => request<ProviderListItemResponse[]>(PROVIDERS_API_ENDPOINTS.list),

  create: (input: ProviderCreateRequest) =>
    request<null>(PROVIDERS_API_ENDPOINTS.create, {
      method: "POST",
      body: input,
    }),

  updateStatus: (providerId: string, input: ProviderStatusUpdateRequest) =>
    request<null>(PROVIDERS_API_ENDPOINTS.updateStatus(providerId), {
      method: "PUT",
      body: input,
    }),
};
