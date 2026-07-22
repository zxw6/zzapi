import type { UserProfile } from "../api/types";

export function getDefaultPathForRole(role: UserProfile["role"]): string {
  return role === "admin" ? "/admin/stats" : "/console/overview";
}

export function hasRole(
  user: UserProfile | null | undefined,
  role: UserProfile["role"],
): boolean {
  return user?.role === role;
}
