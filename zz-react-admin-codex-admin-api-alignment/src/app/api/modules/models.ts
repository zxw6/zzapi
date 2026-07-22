import { request } from "../../../lib/http/client";
import { MODELS_API_ENDPOINTS } from "../endpoints";
import type {
  ModelBatchImportRequest,
  ModelBatchImportResponse,
  ModelCreateRequest,
  ModelListItemResponse,
  ModelUpdateRequest,
  UpstreamModelOptionResponse,
} from "../types";

export const modelsApi = {
  list: () => request<ModelListItemResponse[]>(MODELS_API_ENDPOINTS.list),

  upstream: (providerId: string) =>
    request<UpstreamModelOptionResponse[]>(MODELS_API_ENDPOINTS.upstream(providerId)),

  create: (input: ModelCreateRequest) =>
    request<null>(MODELS_API_ENDPOINTS.create, {
      method: "POST",
      body: input,
    }),

  update: (id: string, input: ModelUpdateRequest) =>
    request<null>(MODELS_API_ENDPOINTS.update(id), {
      method: "PUT",
      body: input,
    }),

  importBatch: (input: ModelBatchImportRequest) =>
    request<ModelBatchImportResponse>(MODELS_API_ENDPOINTS.import, {
      method: "POST",
      body: input,
    }),
};
