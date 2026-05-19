import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { clearAuthSession, getAuthToken, isAuthenticated, setAuthSession } from "./auth";

describe("auth session", () => {
  beforeEach(() => {
    clearAuthSession();
  });

  afterEach(() => {
    clearAuthSession();
  });

  it("writes and reads auth token", () => {
    setAuthSession("token-123");
    expect(getAuthToken()).toBe("token-123");
    expect(isAuthenticated()).toBe(true);
  });

  it("clears auth token", () => {
    setAuthSession("token-123");
    clearAuthSession();
    expect(getAuthToken()).toBeNull();
    expect(isAuthenticated()).toBe(false);
  });
});
