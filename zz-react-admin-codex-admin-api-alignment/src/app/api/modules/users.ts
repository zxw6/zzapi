import { request } from "../../../lib/http/client";
import { USERS_API_ENDPOINTS } from "../endpoints";
import type {
  UserCreateRequest,
  UserListItemResponse,
  UserStatusUpdateRequest,
  UserUpdateRequest,
  WalletRechargeRequest,
} from "../types";

export const usersApi = {
  list: () => request<UserListItemResponse[]>(USERS_API_ENDPOINTS.list),

  detail: (userId: string) => request<UserListItemResponse>(USERS_API_ENDPOINTS.detail(userId)),

  create: (input: UserCreateRequest) =>
    request<null>(USERS_API_ENDPOINTS.create, {
      method: "POST",
      body: input,
    }),

  update: (userId: string, input: UserUpdateRequest) =>
    request<null>(USERS_API_ENDPOINTS.update(userId), {
      method: "PUT",
      body: input,
    }),

  updateStatus: (userId: string, input: UserStatusUpdateRequest) =>
    request<null>(USERS_API_ENDPOINTS.updateStatus(userId), {
      method: "PUT",
      body: input,
    }),

  remove: (userId: string) =>
    request<null>(USERS_API_ENDPOINTS.remove(userId), {
      method: "DELETE",
    }),

  recharge: (input: WalletRechargeRequest) =>
    request<null>(USERS_API_ENDPOINTS.recharge, {
      method: "POST",
      body: input,
    }),
};
