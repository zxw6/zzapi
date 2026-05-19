import type { AuthSession } from "../api/types";

const STORAGE_KEY = "apihub.auth.session.v1";
let memorySession: AuthSession | null = null;

function canUseStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

export function getSessionFromMemory() {
  return memorySession;
}

export function setSessionToMemory(session: AuthSession | null) {
  memorySession = session;
}

export function loadSessionFromStorage(): AuthSession | null {
  if (!canUseStorage()) {
    return memorySession;
  }

  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const session = JSON.parse(raw) as AuthSession;
    memorySession = session;
    return session;
  } catch {
    window.localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function saveSessionToStorage(session: AuthSession) {
  memorySession = session;
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSessionStorage() {
  memorySession = null;
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.removeItem(STORAGE_KEY);
}
