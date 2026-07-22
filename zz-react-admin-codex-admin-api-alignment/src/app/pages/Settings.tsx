import { useEffect, useMemo, useState } from "react";
import {
  Globe,
  Shield,
  Webhook,
  Save,
  Plus,
  Trash2,
  CheckCircle,
  RefreshCw,
  Eye,
  EyeOff,
} from "lucide-react";
import { toast } from "sonner";
import { useSiteSettingsQuery, useUpdateSiteSettingsMutation } from "../api/queries";
import { isAppError } from "../../lib/http/error";
import { mapSiteSettingsToBranding, saveSiteBranding } from "../site-settings/site-branding";
import { ButtonLoadingContent, PagePanelSkeleton } from "../components/ui/feedback";

type Tab = "site" | "security" | "webhooks";

const defaultSiteSettings = {
  siteName: "",
  adminEmail: "",
  siteDescription: "",
  baseUrl: "https://api.yourdomain.com",
  footerText: "Powered by API Hub",
  themeMode: "LIGHT" as "LIGHT" | "DARK",
};

type SiteSettingsFormState = typeof defaultSiteSettings;

const mockWebhooks = [
  {
    id: "w1",
    name: "请求成功通知",
    url: "https://hooks.example.com/api/success",
    events: ["request.success", "request.completed"],
    secret: "whsec_abc123xyz",
    enabled: true,
    lastTriggered: "2分钟前",
    successRate: 99.8,
  },
  {
    id: "w2",
    name: "故障告警",
    url: "https://discord.com/api/webhooks/xxx/yyy",
    events: ["channel.error", "channel.failover"],
    secret: "whsec_def456uvw",
    enabled: true,
    lastTriggered: "3小时前",
    successRate: 100,
  },
  {
    id: "w3",
    name: "费用超限告警",
    url: "https://hooks.slack.com/services/T00/B00/xxx",
    events: ["budget.exceeded", "budget.warning"],
    secret: "whsec_ghi789rst",
    enabled: false,
    lastTriggered: "从未触发",
    successRate: 0,
  },
];

const allEvents = [
  "request.success",
  "request.failed",
  "request.completed",
  "channel.error",
  "channel.failover",
  "budget.warning",
  "budget.exceeded",
  "key.disabled",
];

export function SettingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>("site");
  const [webhooks, setWebhooks] = useState(mockWebhooks);
  const [showAddWebhook, setShowAddWebhook] = useState(false);
  const [showSecret, setShowSecret] = useState<Record<string, boolean>>({});
  const [saved, setSaved] = useState(false);
  const {
    data: siteSettingsData,
    isLoading: isSiteSettingsLoading,
    isError: isSiteSettingsError,
    error: siteSettingsError,
    refetch: refetchSiteSettings,
  } = useSiteSettingsQuery();
  const updateSiteSettingsMutation = useUpdateSiteSettingsMutation();

  const [siteSettingsDraft, setSiteSettingsDraft] = useState<SiteSettingsFormState | null>(null);

  const [securitySettings, setSecuritySettings] = useState({
    ipWhitelist: "192.168.0.0/16\n10.0.0.0/8",
    enableIPWhitelist: false,
    enableRateLimit: true,
    globalRateLimit: "10000",
    logRetentionDays: "30",
    enableAuditLog: true,
    requireHttps: true,
    corsOrigins: "https://yourdomain.com\nhttps://app.yourdomain.com",
  });

  useEffect(() => {
    if (!siteSettingsData) {
      return;
    }

    saveSiteBranding(mapSiteSettingsToBranding(siteSettingsData));
  }, [siteSettingsData]);

  const siteSettings = useMemo<SiteSettingsFormState>(() => {
    if (siteSettingsDraft) {
      return siteSettingsDraft;
    }

    if (!siteSettingsData) {
      return defaultSiteSettings;
    }

    return {
      siteName: siteSettingsData.siteName,
      adminEmail: siteSettingsData.adminEmail,
      siteDescription: siteSettingsData.siteDescription ?? "",
      baseUrl: siteSettingsData.baseUrl,
      footerText: siteSettingsData.footerText ?? "",
      themeMode: siteSettingsData.themeMode,
    };
  }, [siteSettingsData, siteSettingsDraft]);

  const updateSiteSetting = <Key extends keyof SiteSettingsFormState>(
    key: Key,
    value: SiteSettingsFormState[Key],
  ) => {
    setSiteSettingsDraft((prev) => ({
      ...(prev ?? siteSettings),
      [key]: value,
    }));
  };

  const handleSiteSettingsSave = async () => {
    try {
      await updateSiteSettingsMutation.mutateAsync({
        siteName: siteSettings.siteName,
        adminEmail: siteSettings.adminEmail,
        siteDescription: siteSettings.siteDescription,
        baseUrl: siteSettings.baseUrl,
        footerText: siteSettings.footerText,
        themeMode: siteSettings.themeMode,
      });
      saveSiteBranding({
        siteName: siteSettings.siteName,
        siteDescription: siteSettings.siteDescription,
        footerText: siteSettings.footerText,
        themeMode: siteSettings.themeMode,
      });
      setSaved(true);
      toast.success("站点信息保存成功");
      setTimeout(() => setSaved(false), 2500);
    } catch (error) {
      const message = isAppError(error) ? error.message : "站点信息保存失败";
      toast.error(message);
    }
  };

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const toggleWebhook = (id: string) => {
    setWebhooks((prev) => prev.map((w) => (w.id === id ? { ...w, enabled: !w.enabled } : w)));
  };

  const tabs: { id: Tab; label: string; icon: typeof Globe }[] = [
    { id: "site", label: "站点信息", icon: Globe },
    { id: "security", label: "安全管理", icon: Shield },
    { id: "webhooks", label: "Webhooks / 回调", icon: Webhook },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-slate-900 dark:text-slate-100">系统设置</h1>
        <p className="text-slate-500 text-sm mt-0.5 dark:text-slate-400">配置站点信息、安全策略与回调通知</p>
      </div>

      <div className="flex gap-6">
        {/* Sidebar tabs */}
        <div className="w-48 flex-shrink-0">
          <nav className="space-y-0.5">
            {tabs.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id)}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all text-left ${
                  activeTab === id
                    ? "border border-blue-200 bg-blue-50 text-blue-600 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-800 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
                }`}
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                {label}
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          {activeTab === "site" && (
            <div className="space-y-5 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
              <h2 className="mb-5 text-slate-900 dark:text-slate-100">站点基本信息</h2>
              {isSiteSettingsLoading && !siteSettingsData ? <PagePanelSkeleton lines={6} /> : null}
              {isSiteSettingsError && !siteSettingsData ? (
                <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-600">
                  <div>
                    {isAppError(siteSettingsError) ? siteSettingsError.message : "站点信息加载失败"}
                  </div>
                  <button
                    onClick={() => void refetchSiteSettings()}
                    className="mt-3 inline-flex items-center gap-2 rounded-lg bg-slate-900 px-3 py-2 text-xs text-white"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    重试
                  </button>
                </div>
              ) : null}
              {!isSiteSettingsLoading || siteSettingsData ? (
                <>
                  {isSiteSettingsError ? (
                    <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-600 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
                      <div>
                        {isAppError(siteSettingsError)
                          ? siteSettingsError.message
                          : "站点信息加载失败"}
                      </div>
                      <button
                        onClick={() => void refetchSiteSettings()}
                        className="mt-3 inline-flex items-center gap-2 rounded-lg bg-slate-900 px-3 py-2 text-xs text-white dark:bg-slate-700"
                      >
                        <RefreshCw className="w-3.5 h-3.5" />
                        重试
                      </button>
                    </div>
                  ) : null}
                  <div className="grid grid-cols-2 gap-5">
                    {[
                      { key: "siteName", label: "站点名称", placeholder: "API Hub 中转站" },
                      { key: "adminEmail", label: "管理员邮箱", placeholder: "admin@example.com" },
                    ].map(({ key, label, placeholder }) => (
                      <div key={key}>
                        <label className="block text-sm text-slate-700 mb-1.5 dark:text-slate-300">{label}</label>
                        <input
                          type="text"
                          value={siteSettings[key as keyof typeof siteSettings]}
                          onChange={(e) =>
                            updateSiteSetting(key as keyof SiteSettingsFormState, e.target.value)
                          }
                          placeholder={placeholder}
                          className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                        />
                      </div>
                    ))}
                  </div>
                  <div>
                    <label className="block text-sm text-slate-700 mb-1.5 dark:text-slate-300">站点描述</label>
                    <input
                      type="text"
                      value={siteSettings.siteDescription}
                      onChange={(e) => updateSiteSetting("siteDescription", e.target.value)}
                      className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-700 mb-1.5 dark:text-slate-300">
                      接口基础地址 (Base URL)
                    </label>
                    <div className="flex gap-2">
                      <input
                        type="url"
                        value={siteSettings.baseUrl}
                        onChange={(e) => updateSiteSetting("baseUrl", e.target.value)}
                        className="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm font-mono focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                      />
                      <button
                        onClick={() => {
                          navigator.clipboard.writeText(siteSettings.baseUrl).catch(() => {});
                          toast.success("Base URL 已复制");
                        }}
                        className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        复制
                      </button>
                    </div>
                    <p className="text-xs text-slate-400 mt-1 dark:text-slate-500">客户端将使用此地址作为 API 端点</p>
                  </div>
                  <div>
                    <label className="block text-sm text-slate-700 mb-1.5 dark:text-slate-300">页脚文字</label>
                    <input
                      type="text"
                      value={siteSettings.footerText}
                      onChange={(e) => updateSiteSetting("footerText", e.target.value)}
                      className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-slate-700 mb-2 dark:text-slate-300">界面主题</label>
                    <div className="flex gap-3">
                      {[
                        { value: "LIGHT" as const, label: "浅色模式" },
                        { value: "DARK" as const, label: "深色模式" },
                      ].map((themeItem) => (
                        <label
                          key={themeItem.value}
                          className={`flex items-center gap-2 px-4 py-2 border rounded-lg cursor-pointer transition-all ${siteSettings.themeMode === themeItem.value ? "border-blue-500 bg-blue-50 dark:border-blue-500/30 dark:bg-blue-500/10" : "border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-600"}`}
                        >
                          <input
                            type="radio"
                            name="theme"
                            value={themeItem.value}
                            checked={siteSettings.themeMode === themeItem.value}
                            onChange={() => updateSiteSetting("themeMode", themeItem.value)}
                            className="text-blue-600"
                          />
                          <span className="text-sm text-slate-700 dark:text-slate-200">{themeItem.label}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                  <div className="flex justify-end pt-2">
                    <button
                      onClick={() => void handleSiteSettingsSave()}
                      disabled={isSiteSettingsLoading || updateSiteSettingsMutation.isPending}
                      className={`flex items-center gap-2 px-5 py-2 rounded-lg text-sm transition-all disabled:cursor-not-allowed disabled:opacity-60 ${saved ? "bg-emerald-500 text-white" : "bg-blue-600 hover:bg-blue-700 text-white"}`}
                    >
                      {saved ? <CheckCircle className="w-4 h-4" /> : <Save className="w-4 h-4" />}
                      {saved ? (
                        "已保存"
                      ) : (
                        <ButtonLoadingContent
                          loading={updateSiteSettingsMutation.isPending}
                          loadingText="保存中..."
                        >
                          保存设置
                        </ButtonLoadingContent>
                      )}
                    </button>
                  </div>
                </>
              ) : null}
            </div>
          )}

          {activeTab === "security" && (
            <div className="space-y-4">
              {/* IP Whitelist */}
              <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-slate-900 dark:text-slate-100">IP 白名单</h3>
                    <p className="text-slate-500 text-xs mt-0.5 dark:text-slate-400">仅允许白名单 IP 访问 API 接口</p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={securitySettings.enableIPWhitelist}
                      onChange={(e) =>
                        setSecuritySettings((prev) => ({
                          ...prev,
                          enableIPWhitelist: e.target.checked,
                        }))
                      }
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 rounded-full bg-slate-200 transition-colors after:absolute after:left-0.5 after:top-0.5 after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-transform after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-5 dark:bg-slate-700" />
                  </label>
                </div>
                <textarea
                  rows={4}
                  value={securitySettings.ipWhitelist}
                  onChange={(e) =>
                    setSecuritySettings((prev) => ({ ...prev, ipWhitelist: e.target.value }))
                  }
                  disabled={!securitySettings.enableIPWhitelist}
                  placeholder="每行一个 IP 或 CIDR，如：&#10;192.168.0.0/16&#10;10.0.0.0/8"
                  className="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm font-mono focus:border-blue-400 focus:outline-none disabled:bg-slate-50 disabled:text-slate-400 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:disabled:bg-slate-900 dark:disabled:text-slate-500"
                />
              </div>

              {/* Rate limit */}
              <div className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-slate-900 dark:text-slate-100">全局频率限制</h3>
                    <p className="text-slate-500 text-xs mt-0.5 dark:text-slate-400">对所有请求应用全局速率限制</p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={securitySettings.enableRateLimit}
                      onChange={(e) =>
                        setSecuritySettings((prev) => ({
                          ...prev,
                          enableRateLimit: e.target.checked,
                        }))
                      }
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 rounded-full bg-slate-200 transition-colors after:absolute after:left-0.5 after:top-0.5 after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-transform after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-5 dark:bg-slate-700" />
                  </label>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs text-slate-500 mb-1.5 dark:text-slate-400">每分钟最大请求数</label>
                    <input
                      type="number"
                      value={securitySettings.globalRateLimit}
                      onChange={(e) =>
                        setSecuritySettings((prev) => ({
                          ...prev,
                          globalRateLimit: e.target.value,
                        }))
                      }
                      disabled={!securitySettings.enableRateLimit}
                      className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none disabled:bg-slate-50 disabled:text-slate-400 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:disabled:bg-slate-900 dark:disabled:text-slate-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500 mb-1.5 dark:text-slate-400">日志保留天数</label>
                    <input
                      type="number"
                      value={securitySettings.logRetentionDays}
                      onChange={(e) =>
                        setSecuritySettings((prev) => ({
                          ...prev,
                          logRetentionDays: e.target.value,
                        }))
                      }
                      className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                    />
                  </div>
                </div>
              </div>

              {/* Other settings */}
              <div className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
                <h3 className="text-slate-900 dark:text-slate-100">其他安全选项</h3>
                {[
                  { key: "requireHttps", label: "强制 HTTPS", desc: "拒绝所有 HTTP 请求" },
                  { key: "enableAuditLog", label: "启用审计日志", desc: "记录所有管理操作" },
                ].map(({ key, label, desc }) => (
                  <div key={key} className="flex items-center justify-between py-2">
                    <div>
                      <div className="text-sm text-slate-700 dark:text-slate-200">{label}</div>
                      <div className="text-xs text-slate-400 dark:text-slate-500">{desc}</div>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={securitySettings[key as keyof typeof securitySettings] as boolean}
                        onChange={(e) =>
                          setSecuritySettings((prev) => ({ ...prev, [key]: e.target.checked }))
                        }
                        className="sr-only peer"
                      />
                      <div className="w-9 h-5 rounded-full bg-slate-200 transition-colors after:absolute after:left-0.5 after:top-0.5 after:h-4 after:w-4 after:rounded-full after:bg-white after:transition-transform after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-4 dark:bg-slate-700" />
                    </label>
                  </div>
                ))}
                <div>
                  <label className="block text-sm text-slate-700 mb-1.5 dark:text-slate-300">
                    允许的 CORS 源（每行一个）
                  </label>
                  <textarea
                    rows={3}
                    value={securitySettings.corsOrigins}
                    onChange={(e) =>
                      setSecuritySettings((prev) => ({ ...prev, corsOrigins: e.target.value }))
                    }
                    className="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm font-mono focus:border-blue-400 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                  />
                </div>
              </div>

              <div className="flex justify-end">
                <button
                  onClick={handleSave}
                  className={`flex items-center gap-2 px-5 py-2 rounded-lg text-sm transition-all ${saved ? "bg-emerald-500 text-white" : "bg-blue-600 hover:bg-blue-700 text-white"}`}
                >
                  {saved ? <CheckCircle className="w-4 h-4" /> : <Save className="w-4 h-4" />}
                  {saved ? "已保存" : "保存设置"}
                </button>
              </div>
            </div>
          )}

          {activeTab === "webhooks" && (
            <div className="space-y-4">
              <div className="flex justify-end">
                <button
                  onClick={() => setShowAddWebhook(true)}
                  className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
                >
                  <Plus className="w-4 h-4" />
                  添加 Webhook
                </button>
              </div>

              {webhooks.map((wh) => (
                <div key={wh.id} className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-start gap-3">
                      <label className="relative inline-flex items-center cursor-pointer mt-0.5">
                        <input
                          type="checkbox"
                          checked={wh.enabled}
                          onChange={() => toggleWebhook(wh.id)}
                          className="sr-only peer"
                        />
                        <div className="w-9 h-5 bg-slate-200 peer-checked:bg-blue-600 rounded-full transition-colors after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:w-4 after:h-4 after:transition-transform peer-checked:after:translate-x-4" />
                      </label>
                      <div>
                        <div className="flex items-center gap-2">
                            <span className="text-slate-900 font-medium text-sm dark:text-slate-100">{wh.name}</span>
                          {wh.enabled ? (
                            <span className="text-xs bg-emerald-50 text-emerald-600 border border-emerald-200 px-1.5 py-0.5 rounded">
                              已启用
                            </span>
                          ) : (
                            <span className="text-xs bg-slate-100 text-slate-500 border border-slate-200 px-1.5 py-0.5 rounded">
                              已停用
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-slate-400 mt-0.5 font-mono dark:text-slate-500">{wh.url}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <button className="p-1.5 rounded text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200">
                        <RefreshCw className="w-3.5 h-3.5" />
                      </button>
                      <button className="p-1.5 rounded hover:bg-red-50 text-slate-400 hover:text-red-500 transition-colors">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 mt-3 border-t border-slate-100 pt-3 dark:border-slate-800">
                    <div>
                      <div className="text-xs text-slate-500 mb-1.5 dark:text-slate-400">订阅事件</div>
                      <div className="flex flex-wrap gap-1">
                        {wh.events.map((ev) => (
                          <span
                            key={ev}
                            className="rounded border border-blue-100 bg-blue-50 px-1.5 py-0.5 text-xs font-mono text-blue-600 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300"
                          >
                            {ev}
                          </span>
                        ))}
                      </div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500 mb-1.5 dark:text-slate-400">签名密钥</div>
                      <div className="flex items-center gap-2">
                        <code className="text-xs text-slate-700 font-mono dark:text-slate-200">
                          {showSecret[wh.id] ? wh.secret : "whsec_••••••••••••"}
                        </code>
                        <button
                          onClick={() =>
                            setShowSecret((prev) => ({ ...prev, [wh.id]: !prev[wh.id] }))
                          }
                          className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                        >
                          {showSecret[wh.id] ? (
                            <EyeOff className="w-3 h-3" />
                          ) : (
                            <Eye className="w-3 h-3" />
                          )}
                        </button>
                      </div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">最后触发</div>
                      <div className="text-xs text-slate-700 mt-0.5 dark:text-slate-200">{wh.lastTriggered}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">成功率</div>
                      <div
                        className={`text-xs mt-0.5 ${wh.successRate >= 99 ? "text-emerald-600" : wh.successRate === 0 ? "text-slate-400" : "text-orange-500"}`}
                      >
                        {wh.successRate === 0 ? "—" : `${wh.successRate}%`}
                      </div>
                    </div>
                  </div>
                </div>
              ))}

              {showAddWebhook && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
                  <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl dark:bg-slate-900 dark:shadow-slate-950/60">
                    <div className="flex items-center justify-between border-b border-slate-200 px-6 py-5 dark:border-slate-800">
                      <h2 className="text-slate-900 dark:text-slate-100">添加 Webhook</h2>
                      <button
                        onClick={() => setShowAddWebhook(false)}
                        className="text-slate-400 hover:text-slate-600 text-lg"
                      >
                        ×
                      </button>
                    </div>
                    <div className="p-6 space-y-4">
                      <div>
                        <label className="block text-sm text-slate-700 mb-1.5">名称</label>
                        <input
                          type="text"
                          placeholder="如：请求告警通知"
                          className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-400 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                        />
                      </div>
                      <div>
                        <label className="block text-sm text-slate-700 mb-1.5">回调 URL</label>
                        <input
                          type="url"
                          placeholder="https://your-server.com/webhook"
                          className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm font-mono focus:border-blue-400 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                        />
                      </div>
                      <div>
                        <label className="block text-sm text-slate-700 mb-2">订阅事件</label>
                        <div className="grid grid-cols-2 gap-2">
                          {allEvents.map((ev) => (
                            <label key={ev} className="flex items-center gap-2 cursor-pointer">
                              <input
                                type="checkbox"
                                className="w-3.5 h-3.5 rounded text-blue-600"
                              />
                              <span className="text-xs text-slate-600 font-mono dark:text-slate-300">{ev}</span>
                            </label>
                          ))}
                        </div>
                      </div>
                      <div className="flex gap-3 pt-2">
                        <button
                          onClick={() => setShowAddWebhook(false)}
                          className="flex-1 rounded-lg border border-slate-200 py-2 text-sm text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          取消
                        </button>
                        <button
                          onClick={() => setShowAddWebhook(false)}
                          className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm transition-colors"
                        >
                          创建 Webhook
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
