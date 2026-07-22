import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router";
import { Sidebar } from "./Sidebar";
import { Bell, LogOut, Menu, MoonStar, Search, SunMedium, X } from "lucide-react";
import { motion } from "motion/react";
import { useAuth } from "../../auth/auth-context";
import { useThemeMode } from "../../providers/theme-context";
import { RouteTransitionOutlet, RouteTransitionProvider } from "./RouteTransition";
import { toast } from "sonner";
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

export function Layout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { isDark, themeMode, toggleThemeMode } = useThemeMode();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isDesktop, setIsDesktop] = useState(() =>
    typeof window === "undefined" ? true : window.innerWidth >= 1024,
  );
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);

  useEffect(() => {
    const syncViewport = () => {
      const desktop = window.innerWidth >= 1024;
      setIsDesktop(desktop);
      setSidebarOpen(desktop);
    };

    syncViewport();
    window.addEventListener("resize", syncViewport);
    return () => window.removeEventListener("resize", syncViewport);
  }, []);

  const handleLogout = async () => {
    try {
      await logout();
      toast.success("已退出登录");
      navigate("/admin/login", { replace: true });
    } catch {
      toast.error("退出登录失败，请稍后重试");
    } finally {
      setLogoutDialogOpen(false);
    }
  };

  return (
    <RouteTransitionProvider>
      <>
      <div className="flex h-screen overflow-hidden bg-slate-50 transition-colors duration-300 dark:bg-slate-950">
        {!isDesktop && sidebarOpen ? (
          <button
            aria-label="关闭导航"
            className="fixed inset-0 z-30 bg-slate-950/50 backdrop-blur-[1px] lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        ) : null}
        <Sidebar
          className={`fixed inset-y-0 left-0 z-40 w-72 max-w-[82vw] transform transition-transform duration-200 lg:static lg:z-auto lg:w-60 lg:max-w-none ${
            sidebarOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          }`}
          onNavigate={() => {
            if (!isDesktop) {
              setSidebarOpen(false);
            }
          }}
        />
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Top bar */}
          <header className="flex h-14 flex-shrink-0 items-center gap-3 border-b border-slate-200 bg-white px-4 transition-colors duration-300 dark:border-slate-800 dark:bg-slate-900 sm:px-6">
            <button
              onClick={() => setSidebarOpen((value) => !value)}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100 lg:hidden"
            >
              {sidebarOpen ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
            </button>
            <div className="flex max-w-md flex-1 items-center gap-3">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 dark:text-slate-500" />
                <input
                  type="text"
                  placeholder="搜索..."
                  className="hidden w-full rounded-lg border border-transparent bg-slate-100 py-1.5 pl-9 pr-4 text-sm text-slate-700 transition-all placeholder:text-slate-400 focus:border-blue-300 focus:bg-white focus:outline-none dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-sky-500 dark:focus:bg-slate-800 sm:block"
                />
              </div>
            </div>
            <div className="ml-auto flex items-center gap-2 sm:gap-3">
              <button className="relative flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100">
                <Bell className="w-4 h-4" />
                <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-red-500 rounded-full" />
              </button>
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
              <div className="hidden text-xs text-slate-500 dark:text-slate-400 sm:block">
                当前管理员：{user?.nickname ?? "Administrator"}
              </div>
              <button
                onClick={() => setLogoutDialogOpen(true)}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs text-slate-600 transition-colors hover:bg-red-50 hover:text-red-600 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-red-500/10 dark:hover:text-red-300"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">退出登录</span>
              </button>
            </div>
          </header>
          {/* Main content */}
          <main className="flex-1 overflow-auto">
            <RouteTransitionOutlet>
              <Outlet />
            </RouteTransitionOutlet>
          </main>
        </div>
      </div>
      <AlertDialog open={logoutDialogOpen} onOpenChange={setLogoutDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认退出登录？</AlertDialogTitle>
            <AlertDialogDescription>
              退出后将返回管理员登录页，如有未保存内容请先确认。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={() => void handleLogout()}>确认退出</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
      </>
    </RouteTransitionProvider>
  );
}
