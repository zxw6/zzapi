import type { AuthSession, UserProfile } from "../api/types";

export type AuthState = {
  status: "initializing" | "anonymous" | "authenticated";
  session: AuthSession | null;
  user: UserProfile | null;
};

export type AuthContextValue = AuthState & {
  login: (input: {
    username: string;
    password: string;
    captchaId: string;
    captchaCode: string;
  }) => Promise<AuthSession>;
  register: (input: {
    username: string;
    email: string;
    password: string;
    verificationCode: string;
    nickname?: string;
    phone?: string;
  }) => Promise<AuthSession>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
};
