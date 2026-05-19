import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../auth/auth-context";
import { getDefaultPathForRole, hasRole } from "../auth/role-utils";

export function RequireAuth() {
  const location = useLocation();
  const { status } = useAuth();

  if (status === "initializing") {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">会话校验中...</div>;
  }

  if (status !== "authenticated") {
    return <Navigate to="/" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}

export function RedirectIfAuthenticated() {
  const { status, user } = useAuth();

  if (status === "initializing") {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">加载中...</div>;
  }

  if (status === "authenticated") {
    return <Navigate to={getDefaultPathForRole(user?.role ?? "user")} replace />;
  }

  return <Outlet />;
}

export function RequireUserRole() {
  const location = useLocation();
  const { status, user } = useAuth();

  if (status === "initializing") {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">权限校验中...</div>;
  }

  if (status !== "authenticated") {
    return <Navigate to="/" replace state={{ from: location.pathname }} />;
  }

  if (!hasRole(user, "user")) {
    return <Navigate to={getDefaultPathForRole(user?.role ?? "user")} replace />;
  }

  return <Outlet />;
}

export function RequireAdminRole() {
  const location = useLocation();
  const { status, user } = useAuth();

  if (status === "initializing") {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">权限校验中...</div>;
  }

  if (status !== "authenticated") {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />;
  }

  if (!hasRole(user, "admin")) {
    return <Navigate to={getDefaultPathForRole(user?.role ?? "user")} replace />;
  }

  return <Outlet />;
}
