import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router";
import {
  LayoutDashboard,
  Key,
  Cpu,
  CreditCard,
  BookOpen,
  Settings,
  Zap,
  LogOut,
  ChevronRight,
  User,
  Menu,
  X,
  MoonStar,
  SunMedium,
} from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";
import { useModelAccessSummaryQuery, useUserDetailQuery } from "../../api/queries";
import { useAuth } from "../../auth/auth-context";
import { useThemeMode } from "../../providers/theme-context";
import { useSiteBranding } from "../../site-settings/site-branding";
import {
  RouteTransitionOutlet,
  RouteTransitionProvider,
  TransitionNavLink,
} from "../layout/RouteTransition";
import { useRouteTransition } from "../layout/route-transition-context";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../ui/alert-dialog";

const navItems = [
  { path: "/console/overview", icon: LayoutDashboard, label: "控制台概览" },
  { path: "/console/keys", icon: Key, label: "我的 API Keys" },
  { path: "/console/models", icon: Cpu, label: "模型广场" },
  { path: "/console/billing", icon: CreditCard, label: "用量 & 账单" },
  { path: "/console/docs", icon: BookOpen, label: "快速接入" },
  { path: "/console/account", icon: Settings, label: "账户设置" },
];

export function UserLayout() {
  return (
    <RouteTransitionProvider>
      <UserLayoutContent />
    </RouteTransitionProvider>
  );
}

function UserLayoutContent() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { branding } = useSiteBranding(true);
  const { beginTransition } = useRouteTransition();
  const { isDark, themeMode, toggleThemeMode } = useThemeMode();
  const { data: currentUser } = useUserDetailQuery(user?.id ?? "", Boolean(user?.id));
  const { data: packageSummary } = useModelAccessSummaryQuery(Boolean(user));
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const [isDesktop, setIsDesktop] = useState(() =>
    typeof window === "undefined" ? true : window.innerWidth >= 1024,
  );
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);

  useEffect(() => {
    const syncViewport = () => {
      setIsDesktop(window.innerWidth >= 1024);
      if (window.innerWidth >= 1024) {
        setMobileSidebarOpen(false);
      }
    };

    syncViewport();
    window.addEventListener("resize", syncViewport);
    return () => window.removeEventListener("resize", syncViewport);
  }, []);
  const handleLogout = async () => {
    try {
      await logout();
      toast.success("已退出登录");
      navigate("/", { replace: true });
    } catch {
      toast.error("退出登录失败，请稍后重试");
    } finally {
      setLogoutDialogOpen(false);
    }
  };
  const navigateWithTransition = (path: string) => {
    beginTransition(path);
    navigate(path);
  };
  const packageName =
    packageSummary?.activeGroupName || packageSummary?.packageStatusText || "未开通套餐";

  return (
    <div className="flex h-screen overflow-hidden bg-[#f6f7f9] transition-colors duration-300 dark:bg-slate-950">
      {!isDesktop && mobileSidebarOpen ? (
        <button
          aria-label="关闭导航"
          className="fixed inset-0 z-30 bg-slate-950/50 backdrop-blur-[1px] lg:hidden"
          onClick={() => setMobileSidebarOpen(false)}
        />
      ) : null}
      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-72 max-w-[82vw] flex-shrink-0 flex-col border-r border-slate-100 bg-white shadow-sm transition-all duration-200 dark:border-slate-800 dark:bg-slate-900 dark:shadow-slate-950/40 lg:static lg:max-w-none ${
          isDesktop
            ? sidebarCollapsed
              ? "lg:w-16"
              : "lg:w-56"
            : mobileSidebarOpen
              ? "translate-x-0"
              : "-translate-x-full"
        }`}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 border-b border-slate-100 px-4 py-4 dark:border-slate-800">
          <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center flex-shrink-0">
            <Zap className="w-4 h-4 text-white" />
          </div>
          {(isDesktop ? !sidebarCollapsed : true) && (
            <div className="min-w-0">
              <div className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100">
                {branding.siteName}
              </div>
              <div className="truncate text-xs text-slate-400 dark:text-slate-500">
                {branding.siteDescription}
              </div>
            </div>
          )}
        </div>

        {/* Balance card */}
        {(isDesktop ? !sidebarCollapsed : true) && (
          <div className="mx-3 my-3 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-600 p-3 shadow-lg shadow-blue-950/10">
            <div className="text-blue-100 text-xs mb-0.5">账户余额</div>
            <div className="text-white text-lg font-semibold">
              ¥ {Number(currentUser?.balance ?? user?.balance ?? 0).toFixed(2)}
            </div>
            <div className="mt-1 text-[11px] text-blue-100">当前套餐：{packageName}</div>
            <button
              onClick={() => {
                navigateWithTransition("/console/billing?tab=package");
                if (!isDesktop) {
                  setMobileSidebarOpen(false);
                }
              }}
              className="mt-2 w-full text-xs bg-white/20 hover:bg-white/30 text-white py-1 rounded-lg transition-colors"
            >
              购买套餐
            </button>
          </div>
        )}

        {/* Nav */}
        <nav className="flex-1 px-2 py-2 space-y-0.5 overflow-y-auto">
          {navItems.map(({ path, icon: Icon, label }) => (
            <TransitionNavLink
              key={path}
              to={path}
              onClick={() => {
                if (!isDesktop) {
                  setMobileSidebarOpen(false);
                }
              }}
              title={isDesktop && sidebarCollapsed ? label : undefined}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all group ${
                  isActive
                    ? "bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-300"
                    : "text-slate-500 hover:bg-slate-50 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    className={`w-4 h-4 flex-shrink-0 ${isActive ? "text-blue-600 dark:text-blue-300" : "text-slate-400 group-hover:text-slate-600 dark:text-slate-500 dark:group-hover:text-slate-200"}`}
                  />
                  {(isDesktop ? !sidebarCollapsed : true) && (
                    <span className="truncate text-sm">{label}</span>
                  )}
                  {(isDesktop ? !sidebarCollapsed : true) && isActive && (
                    <ChevronRight className="ml-auto h-3 w-3 text-blue-400 dark:text-blue-300" />
                  )}
                </>
              )}
            </TransitionNavLink>
          ))}
        </nav>

        {/* User */}
        <div className="space-y-1 border-t border-slate-100 p-3 dark:border-slate-800">
          {isDesktop ? !sidebarCollapsed : true ? (
            <div className="flex items-center gap-2.5 px-2 py-2">
              <div className="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center flex-shrink-0">
                <User className="w-3.5 h-3.5 text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="truncate text-sm font-medium text-slate-700 dark:text-slate-100">
                  {currentUser?.nickname ?? user?.nickname ?? "未登录用户"}
                </div>
                <div className="truncate text-xs text-slate-400 dark:text-slate-500">
                  {currentUser?.email ?? user?.email ?? user?.username ?? "unknown"}
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Topbar */}
        <header className="flex h-14 flex-shrink-0 items-center gap-3 border-b border-slate-100 bg-white px-4 transition-colors duration-300 dark:border-slate-800 dark:bg-slate-900 sm:px-5">
          <button
            onClick={() => {
              if (isDesktop) {
                setSidebarCollapsed((value) => !value);
              } else {
                setMobileSidebarOpen((value) => !value);
              }
            }}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
          >
            {isDesktop ? (
              sidebarCollapsed ? (
                <Menu className="w-4 h-4" />
              ) : (
                <X className="w-4 h-4" />
              )
            ) : mobileSidebarOpen ? (
              <X className="w-4 h-4" />
            ) : (
              <Menu className="w-4 h-4" />
            )}
          </button>
          <div className="flex-1" />
          <div className="flex items-center gap-2">
            <motion.button
              aria-label={isDark ? "切换为浅色模式" : "切换为深色模式"}
              onClick={toggleThemeMode}
              whileTap={{ scale: 0.94 }}
              className="relative flex h-8 w-8 items-center justify-center overflow-hidden rounded-xl border border-slate-200 bg-white text-slate-600 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-amber-200 dark:hover:bg-slate-700"
              title={themeMode === "DARK" ? "浅色模式" : "深色模式"}
            >
              <motion.span
                animate={{
                  opacity: isDark ? 1 : 0,
                  scale: isDark ? 1 : 0.5,
                  rotate: isDark ? 0 : -50,
                }}
                className="absolute"
                transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
              >
                <SunMedium className="h-4 w-4" />
              </motion.span>
              <motion.span
                animate={{
                  opacity: isDark ? 0 : 1,
                  scale: isDark ? 0.5 : 1,
                  rotate: isDark ? 50 : 0,
                }}
                className="absolute"
                transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
              >
                <MoonStar className="h-4 w-4" />
              </motion.span>
            </motion.button>

            <div className="hidden h-5 w-px bg-slate-200 dark:bg-slate-700 sm:block" />

            <button
              onClick={() => setLogoutDialogOpen(true)}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 transition-colors hover:bg-red-50 hover:text-red-600 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-red-500/10 dark:hover:text-red-300"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">退出登录</span>
            </button>

            {/* Avatar */}
            <button
              onClick={() => navigateWithTransition("/console/account")}
              className="flex items-center gap-2 rounded-xl px-2 py-1.5 transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              <div className="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center">
                <User className="w-3.5 h-3.5 text-white" />
              </div>
              <span className="hidden text-sm text-slate-700 dark:text-slate-100 sm:block">
                {currentUser?.nickname ?? user?.nickname ?? "未登录用户"}
              </span>
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-auto">
          <RouteTransitionOutlet>
            <Outlet />
          </RouteTransitionOutlet>
        </main>
      </div>

      <AlertDialog open={logoutDialogOpen} onOpenChange={setLogoutDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认退出登录？</AlertDialogTitle>
            <AlertDialogDescription>
              退出后将返回登录页，如有未保存内容请先确认。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={() => void handleLogout()}>确认退出</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
