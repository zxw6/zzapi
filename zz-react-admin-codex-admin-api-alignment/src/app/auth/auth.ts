import type { AuthSession } from "../api/types";
import {
  clearSessionStorage,
  getSessionFromMemory,
  loadSessionFromStorage,
  saveSessionToStorage,
} from "./session-storage";

export function getAuthToken(): string | null {
  const session = getSessionFromMemory() ?? loadSessionFromStorage();
  return session?.accessToken ?? null;
}

export function isAuthenticated(): boolean {
  return Boolean(getAuthToken());
}

export function setAuthSession(token: string): void {
  const existed = getSessionFromMemory() ?? loadSessionFromStorage();
  const next: AuthSession = {
    accessToken: token,
    user:
      existed?.user ??
      ({
        id: "u_legacy",
        username: "legacy",
        nickname: "Legacy User",
        email: "legacy@example.com",
        role: "user",
        roleCode: "USER",
      } as const),
  };

  saveSessionToStorage(next);
}

export function clearAuthSession(): void {
  clearSessionStorage();
}
