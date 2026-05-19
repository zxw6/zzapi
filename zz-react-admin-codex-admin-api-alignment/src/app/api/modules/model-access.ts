import { request } from "../../../lib/http/client";
import { MODEL_ACCESS_API_ENDPOINTS } from "../endpoints";
import type {
  ModelAccessSummaryResponse,
  ModelGroupCreateRequest,
  ModelPackagePurchaseRecordResponse,
  PurchaseModelPackageRequest,
  WalletTransactionItemResponse,
} from "../types";

export const modelAccessApi = {
  summary: () => request<ModelAccessSummaryResponse>(MODEL_ACCESS_API_ENDPOINTS.summary),

  createGroup: (input: ModelGroupCreateRequest) =>
    request<ModelAccessSummaryResponse>(MODEL_ACCESS_API_ENDPOINTS.groups, {
      method: "POST",
      body: input,
    }),

  purchases: () =>
    request<ModelPackagePurchaseRecordResponse[]>(MODEL_ACCESS_API_ENDPOINTS.purchases),

  walletTransactions: () =>
    request<WalletTransactionItemResponse[]>(MODEL_ACCESS_API_ENDPOINTS.walletTransactions),

  purchase: (input: PurchaseModelPackageRequest) =>
    request<ModelAccessSummaryResponse>(MODEL_ACCESS_API_ENDPOINTS.purchase, {
      method: "POST",
      body: input,
    }),

  remove: (groupId: string) =>
    request<null>(MODEL_ACCESS_API_ENDPOINTS.remove(groupId), {
      method: "DELETE",
    }),
};
