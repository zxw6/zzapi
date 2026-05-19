import React from "react";
import { authApi } from "../api/modules/auth";
import {
  clearSessionStorage,
  loadSessionFromStorage,
  saveSessionToStorage,
  setSessionToMemory,
} from "./session-storage";
import type { AuthContextValue, AuthState } from "./types";
import { configureHttpAuth } from "../../lib/http/client";

const AuthContext = React.createContext<AuthContextValue | null>(null);

const initialState: AuthState = {
  status: "initializing",
  session: null,
  user: null,
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = React.useState<AuthState>(initialState);
  const sessionRef = React.useRef(state.session);

  const applySession = React.useCallback((nextSession: AuthState["session"], status: AuthState["status"]) => {
    sessionRef.current = nextSession;
    setSessionToMemory(nextSession);

    if (nextSession) {
      saveSessionToStorage(nextSession);
      setState({
        status,
        session: nextSession,
        user: nextSession.user,
      });
      return;
    }

    clearSessionStorage();
    setState({
      status,
      session: null,
      user: null,
    });
  }, []);

  const refreshSession = React.useCallback(async (): Promise<boolean> => {
    applySession(null, "anonymous");
    return false;
  }, [applySession]);

  const logout = React.useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Logout should clear local session even if network fails.
    }

    applySession(null, "anonymous");
  }, [applySession]);

  React.useEffect(() => {
    configureHttpAuth({
      getAccessToken: () => sessionRef.current?.accessToken ?? null,
      refreshAuthToken: refreshSession,
      onUnauthorized: () => {
        applySession(null, "anonymous");
      },
    });

    const bootstrap = async () => {
      const stored = loadSessionFromStorage();
      if (!stored) {
        applySession(null, "anonymous");
        return;
      }

      // The backend contract does not provide a refresh-token flow.
      // On hard refresh we restore the persisted session directly instead of
      // forcing an immediate /admin/auth/me validation that can transiently
      // return 401 and incorrectly bounce the user back to the login page.
      applySession(stored, "authenticated");
    };

    void bootstrap();
  }, [applySession, refreshSession]);

  const login = React.useCallback(
    async (input: {
      username: string;
      password: string;
      captchaId: string;
      captchaCode: string;
    }) => {
      const session = await authApi.login(input);
      applySession(session, "authenticated");
      return session;
    },
    [applySession],
  );

  const register = React.useCallback(
    async (input: {
      username: string;
      email: string;
      password: string;
      verificationCode: string;
      nickname?: string;
      phone?: string;
    }) => {
      const session = await authApi.register(input);
      applySession(session, "authenticated");
      return session;
    },
    [applySession],
  );

  const value = React.useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      register,
      logout,
      refreshSession,
    }),
    [login, logout, refreshSession, register, state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = React.useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }

  return context;
}
