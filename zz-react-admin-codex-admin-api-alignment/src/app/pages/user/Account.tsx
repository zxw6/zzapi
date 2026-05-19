import { useState } from "react";
import {
  BadgeCheck,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Phone,
  Save,
  Shield,
  User,
  Wallet,
} from "lucide-react";
import { toast } from "sonner";
import { useUpdateUserMutation, useUserDetailQuery } from "../../api/queries";
import { useAuth } from "../../auth/auth-context";
import { isAppError } from "../../../lib/http/error";
import {
  ButtonLoadingContent,
  PageErrorState,
  PageHeaderSkeleton,
  PagePanelSkeleton,
} from "../../components/ui/feedback";

type Tab = "profile" | "security";

type AccountFormProps = {
  initial: {
    username: string;
    nickname: string;
    email: string;
    phone: string;
    balance: number;
    roleCode: string;
  };
  onSaveProfile: (profile: { nickname: string; email: string; phone: string }) => Promise<void>;
  onSavePassword: (password: string) => Promise<void>;
  isSaving: boolean;
};

function AccountForm({ initial, onSaveProfile, onSavePassword, isSaving }: AccountFormProps) {
  const [tab, setTab] = useState<Tab>("profile");
  const [saved, setSaved] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [profile, setProfile] = useState({
    nickname: initial.nickname,
    email: initial.email,
    phone: initial.phone,
  });
  const [passwordForm, setPasswordForm] = useState({
    password: "",
    confirmPassword: "",
  });

  const handleSave = async () => {
    if (tab === "profile") {
      await onSaveProfile(profile);
    }

    if (tab === "security") {
      if (!passwordForm.password) {
        toast.error("请输入新密码");
        return;
      }

      if (passwordForm.password !== passwordForm.confirmPassword) {
        toast.error("两次输入的新密码不一致");
        return;
      }

      await onSavePassword(passwordForm.password);
      setPasswordForm({
        password: "",
        confirmPassword: "",
      });
    }

    setSaved(true);
    window.setTimeout(() => setSaved(false), 2000);
  };

  const tabs = [
    { id: "profile" as const, label: "个人资料", icon: User, desc: "维护昵称、邮箱和手机号" },
    { id: "security" as const, label: "账号安全", icon: Lock, desc: "更新登录密码" },
  ];

  return (
    <div className="grid gap-6 xl:grid-cols-[18rem_minmax(0,1fr)]">
      <aside className="space-y-4">
        <div className="overflow-hidden rounded-[26px] border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="border-b border-slate-100 bg-[linear-gradient(135deg,rgba(59,130,246,0.14),transparent_72%)] px-5 py-5 dark:border-slate-800">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-900 text-white dark:bg-white dark:text-slate-900">
                <User className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <div className="truncate text-base font-semibold text-slate-900 dark:text-slate-100">
                  {initial.nickname || initial.username}
                </div>
                <div className="truncate text-xs text-slate-500 dark:text-slate-400">
                  @{initial.username}
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-3 px-5 py-5 text-sm">
            <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-950/50">
              <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                <BadgeCheck className="h-4 w-4 text-blue-500" />
                账户角色
              </div>
              <span className="font-medium text-slate-900 dark:text-slate-100">{initial.roleCode}</span>
            </div>
            <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-950/50">
              <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                <Wallet className="h-4 w-4 text-emerald-500" />
                当前余额
              </div>
              <span className="font-medium text-slate-900 dark:text-slate-100">¥{initial.balance.toFixed(2)}</span>
            </div>
            <div className="flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-950/50">
              <div className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
                <Mail className="h-4 w-4 text-amber-500" />
                邮箱
              </div>
              <span className="max-w-[8rem] truncate font-medium text-slate-900 dark:text-slate-100">
                {initial.email || "未设置"}
              </span>
            </div>
          </div>
        </div>

        <nav className="rounded-[26px] border border-slate-200 bg-white p-3 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          {tabs.map(({ id, label, icon: Icon, desc }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex w-full items-start gap-3 rounded-2xl px-4 py-3 text-left transition-all ${
                tab === id
                  ? "bg-slate-900 text-white shadow-sm dark:bg-white dark:text-slate-900"
                  : "text-slate-500 hover:bg-slate-50 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
              }`}
            >
              <Icon className="mt-0.5 h-4 w-4 flex-shrink-0" />
              <div>
                <div className="text-sm font-medium">{label}</div>
                <div className={`mt-0.5 text-xs ${tab === id ? "text-white/70 dark:text-slate-500" : "text-slate-400 dark:text-slate-500"}`}>
                  {desc}
                </div>
              </div>
            </button>
          ))}
        </nav>
      </aside>

      <section className="overflow-hidden rounded-[26px] border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="border-b border-slate-100 px-5 py-5 dark:border-slate-800 sm:px-6">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                {tab === "profile" ? "个人资料" : "账号安全"}
              </h2>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                {tab === "profile" ? "让账户信息保持最新，方便系统识别和联系。" : "修改登录密码，保护你的控制台账号安全。"}
              </p>
            </div>
            {saved ? (
              <div className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-3 py-1 text-xs text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
                <Shield className="h-3.5 w-3.5" />
                已保存
              </div>
            ) : null}
          </div>
        </div>

        <div className="space-y-6 px-5 py-5 sm:px-6 sm:py-6">
          {tab === "profile" ? (
            <>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/50">
                  <div className="text-xs text-slate-500 dark:text-slate-400">用户名</div>
                  <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">{initial.username}</div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/50">
                  <div className="text-xs text-slate-500 dark:text-slate-400">手机号</div>
                  <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">{initial.phone || "未设置"}</div>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <label className="space-y-2">
                  <div className="text-sm font-medium text-slate-700 dark:text-slate-200">昵称</div>
                  <div className="relative">
                    <User className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      value={profile.nickname}
                      onChange={(event) =>
                        setProfile((prev) => ({
                          ...prev,
                          nickname: event.target.value,
                        }))
                      }
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    />
                  </div>
                </label>

                <label className="space-y-2">
                  <div className="text-sm font-medium text-slate-700 dark:text-slate-200">邮箱</div>
                  <div className="relative">
                    <Mail className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      value={profile.email}
                      onChange={(event) =>
                        setProfile((prev) => ({
                          ...prev,
                          email: event.target.value,
                        }))
                      }
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    />
                  </div>
                </label>
              </div>

              <label className="space-y-2">
                <div className="text-sm font-medium text-slate-700 dark:text-slate-200">手机号</div>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    value={profile.phone}
                    onChange={(event) =>
                      setProfile((prev) => ({
                        ...prev,
                        phone: event.target.value,
                      }))
                    }
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                  />
                </div>
              </label>
            </>
          ) : (
            <>
              <div className="rounded-2xl border border-blue-100 bg-blue-50/80 px-4 py-3 text-sm text-blue-700 dark:border-blue-500/30 dark:bg-blue-500/10 dark:text-blue-300">
                当前后端仅提供“设置新密码”能力，保存后会直接更新账户密码。
              </div>

              <div className="grid gap-4">
                <label className="space-y-2">
                  <div className="text-sm font-medium text-slate-700 dark:text-slate-200">新密码</div>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      value={passwordForm.password}
                      onChange={(event) =>
                        setPasswordForm((prev) => ({
                          ...prev,
                          password: event.target.value,
                        }))
                      }
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 pr-11 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                      placeholder="请输入新密码"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((value) => !value)}
                      className="absolute right-3 top-3 text-slate-400 dark:text-slate-500"
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </label>

                <label className="space-y-2">
                  <div className="text-sm font-medium text-slate-700 dark:text-slate-200">确认新密码</div>
                  <div className="relative">
                    <input
                      type={showConfirmPassword ? "text" : "password"}
                      value={passwordForm.confirmPassword}
                      onChange={(event) =>
                        setPasswordForm((prev) => ({
                          ...prev,
                          confirmPassword: event.target.value,
                        }))
                      }
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 pr-11 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                      placeholder="请再次输入新密码"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((value) => !value)}
                      className="absolute right-3 top-3 text-slate-400 dark:text-slate-500"
                    >
                      {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </label>
              </div>
            </>
          )}

          <div className="flex items-center gap-3 pt-2">
            <button
              onClick={() => void handleSave()}
              disabled={isSaving}
              className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-4 py-2.5 text-sm text-white transition hover:bg-slate-800 disabled:opacity-60 dark:bg-white dark:text-slate-900 dark:hover:bg-slate-200"
            >
              {isSaving ? null : <Save className="h-4 w-4" />}
              <ButtonLoadingContent
                loading={isSaving}
                loadingText="保存中..."
                spinnerClassName="h-4 w-4"
              >
                保存修改
              </ButtonLoadingContent>
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

export function AccountPage() {
  const { user } = useAuth();
  const updateUserMutation = useUpdateUserMutation();
  const { data, isLoading, isError, refetch, error } = useUserDetailQuery(
    user?.id ?? "",
    Boolean(user?.id),
  );

  if (isLoading || !user?.id) {
    return (
      <div className="mx-auto max-w-6xl space-y-6 p-4 sm:p-6">
        <PageHeaderSkeleton showAction={false} />
        <PagePanelSkeleton lines={10} />
      </div>
    );
  }

  if (isError || !data) {
    const message = isAppError(error) ? error.message : "加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6 p-4 text-slate-900 dark:text-slate-100 sm:p-6">
      <div>
        <h1 className="text-slate-900 dark:text-slate-100">账户设置</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          统一维护个人资料、联系方式与账号安全配置。
        </p>
      </div>

      <AccountForm
        key={`${data.nickname}:${data.email ?? ""}:${data.phone ?? ""}:${data.balance ?? 0}`}
        initial={{
          username: data.username,
          nickname: data.nickname ?? "",
          email: data.email ?? "",
          phone: data.phone ?? "",
          balance: Number(data.balance ?? 0),
          roleCode: data.roleCode ?? "USER",
        }}
        onSaveProfile={async (profile) => {
          await updateUserMutation.mutateAsync({
            userId: data.id,
            input: {
              nickname: profile.nickname.trim() || undefined,
              email: profile.email.trim() || undefined,
              phone: profile.phone.trim() || undefined,
            },
          });
          toast.success("账户资料已更新");
        }}
        onSavePassword={async (password) => {
          await updateUserMutation.mutateAsync({
            userId: data.id,
            input: {
              password,
            },
          });
          toast.success("密码已更新");
        }}
        isSaving={updateUserMutation.isPending}
      />
    </div>
  );
}
