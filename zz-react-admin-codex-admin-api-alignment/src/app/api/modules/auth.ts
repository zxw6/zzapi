import { request } from "../../../lib/http/client";
import type {
  AuthSession,
  LoginCaptchaResponse,
  LoginInput,
  PasswordResetCodeRequest,
  PasswordResetRequest,
  RegisterInput,
  SendRegisterCodeRequest,
  UserProfile,
  VerificationCodeSendResponse,
} from "../types";
import { AUTH_API_ENDPOINTS } from "../endpoints";

export type AdminAuthResponse = {
  token: string;
  userId: number;
  username: string;
  nickname: string;
  roleCode: "ADMIN" | "USER";
  balance?: number;
};

function mapAuthSession(data: AdminAuthResponse): AuthSession {
  return {
    accessToken: data.token,
    user: {
      id: String(data.userId),
      username: data.username,
      nickname: data.nickname,
      email: data.username,
      role: data.roleCode === "ADMIN" ? "admin" : "user",
      roleCode: data.roleCode,
      balance: data.balance,
    },
  };
}

export const authApi = {
  getLoginCaptcha: () =>
    request<LoginCaptchaResponse>(AUTH_API_ENDPOINTS.loginCaptcha, {
      method: "GET",
      auth: false,
    }),

  login: (input: LoginInput) =>
    request<AdminAuthResponse>(AUTH_API_ENDPOINTS.login, {
      method: "POST",
      auth: false,
      body: input,
    }).then(mapAuthSession),

  register: (input: RegisterInput) =>
    request<AdminAuthResponse>(AUTH_API_ENDPOINTS.register, {
      method: "POST",
      auth: false,
      body: input,
    }).then(mapAuthSession),

  sendRegisterCode: (input: SendRegisterCodeRequest) =>
    request<VerificationCodeSendResponse>(AUTH_API_ENDPOINTS.registerCode, {
      method: "POST",
      auth: false,
      body: input,
    }),

  sendPasswordResetCode: (input: PasswordResetCodeRequest) =>
    request<VerificationCodeSendResponse>(AUTH_API_ENDPOINTS.passwordResetCode, {
      method: "POST",
      auth: false,
      body: input,
    }),

  resetPassword: (input: PasswordResetRequest) =>
    request<null>(AUTH_API_ENDPOINTS.passwordReset, {
      method: "POST",
      auth: false,
      body: input,
    }),

  me: () =>
    request<AdminAuthResponse>(AUTH_API_ENDPOINTS.me).then((data) => mapAuthSession(data).user),

  logout: async () => ({ ok: true }),
};

export function mapRoleCodeToUserProfile(roleCode: AdminAuthResponse["roleCode"]): UserProfile["role"] {
  return roleCode === "ADMIN" ? "admin" : "user";
}
