import { useState } from "react";
import {
  BarChart3,
  ScrollText,
  Settings,
  Network,
  Key,
  Users,
  Zap,
  LogOut,
  ChevronRight,
  User,
  ReceiptText,
  Wallet,
  Layers3,
} from "lucide-react";
import { useAuth } from "../../auth/auth-context";
import { useSiteBranding } from "../../site-settings/site-branding";
import { TransitionNavLink } from "./RouteTransition";
import { cn } from "../ui/utils";

const navItems = [
  {
    path: "/admin/stats",
    icon: BarChart3,
    label: "用量统计",
    desc: "请求分析与费用",
  },
  {
    path: "/admin/logs",
    icon: ScrollText,
    label: "请求日志",
    desc: "请求记录与追踪",
  },
  {
    path: "/admin/channels",
    icon: Network,
    label: "渠道 / 模型商店",
    desc: "上游接口与路由",
  },
  {
    path: "/admin/package-center",
    icon: Layers3,
    label: "套餐中心",
    desc: "套餐额度与价格配置",
  },
  {
    path: "/admin/package-purchases",
    icon: ReceiptText,
    label: "套餐购买记录",
    desc: "套餐订单与到期情况",
  },
  {
    path: "/admin/amount-flows",
    icon: Wallet,
    label: "金额流水",
    desc: "入账、扣费与退款",
  },
  {
    path: "/admin/users",
    icon: Users,
    label: "用户管理",
    desc: "用户资料与钱包",
  },
  {
    path: "/admin/keys",
    icon: Key,
    label: "API Keys 管理",
    desc: "密钥创建与管理",
  },
  {
    path: "/admin/settings",
    icon: Settings,
    label: "系统设置",
    desc: "站点与安全配置",
  },
];

type SidebarProps = {
  className?: string;
  onNavigate?: () => void;
};

export function Sidebar({ className, onNavigate }: SidebarProps) {
  const { user } = useAuth();
  const { branding } = useSiteBranding(true);

  return (
    <>
      <aside
        className={cn(
          "flex min-h-screen w-60 flex-shrink-0 flex-col border-r border-slate-800 bg-slate-900",
          className,
        )}
      >
        {/* Logo */}
        <div className="px-5 py-5 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-blue-500 flex items-center justify-center flex-shrink-0">
              <Zap className="w-4 h-4 text-white" />
            </div>
            <div>
              <div className="text-white text-sm font-semibold truncate">{branding.siteName}</div>
              <div className="text-slate-500 text-xs truncate">{branding.siteDescription}</div>
            </div>
          </div>
        </div>

        {/* Status indicator */}
        <div className="px-4 py-3 mx-3 my-3 bg-emerald-500/10 border border-emerald-500/20 rounded-lg">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse flex-shrink-0" />
            <span className="text-emerald-400 text-xs">服务运行正常</span>
          </div>
          <div className="text-slate-400 text-xs mt-0.5">可用率 99.98%</div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-3 space-y-0.5 pb-4">
          <div className="text-slate-600 text-xs px-3 py-2 uppercase tracking-wider">导航菜单</div>
          {navItems.map(({ path, icon: Icon, label, desc }) => (
            <TransitionNavLink
              key={path}
              to={path}
              onClick={() => onNavigate?.()}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all group ${
                  isActive
                    ? "bg-blue-600/20 text-blue-400 border border-blue-500/30"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    className={`w-4 h-4 flex-shrink-0 ${isActive ? "text-blue-400" : "text-slate-500 group-hover:text-slate-300"}`}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="text-sm truncate">{label}</div>
                    <div className="text-xs text-slate-600 truncate">{desc}</div>
                  </div>
                  {isActive && <ChevronRight className="w-3 h-3 text-blue-400 flex-shrink-0" />}
                </>
              )}
            </TransitionNavLink>
          ))}
        </nav>

        {/* User */}
        <div className="px-3 pb-4 border-t border-slate-800 pt-4">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-slate-800 transition-colors cursor-pointer">
            <div className="w-7 h-7 rounded-full bg-blue-600 flex items-center justify-center flex-shrink-0">
              <User className="w-3.5 h-3.5 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-slate-300 text-sm truncate">
                {user?.nickname ?? "Administrator"}
              </div>
              <div className="text-slate-600 text-xs truncate">
                {user?.email ?? "admin@apihub.io"}
              </div>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}
