import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import { Eye, EyeOff, Zap, Github, Mail, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../../auth/auth-context";
import { authApi } from "../../api/modules/auth";
import { getDefaultPathForRole } from "../../auth/role-utils";
import { isAppError } from "../../../lib/http/error";
import { useSiteBranding } from "../../site-settings/site-branding";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";

type AuthMode = "login" | "register";
const QQ_EMAIL_PATTERN = /^[1-9]\d{4,12}@qq\.com$/i;

export function UserLoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register } = useAuth();
  const [mode, setMode] = useState<AuthMode>("login");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [codeCountdown, setCodeCountdown] = useState(0);
  const [resetDialogOpen, setResetDialogOpen] = useState(false);
  const [sendingResetCode, setSendingResetCode] = useState(false);
  const [resetCodeCountdown, setResetCodeCountdown] = useState(0);
  const [captchaLoading, setCaptchaLoading] = useState(false);
  const [captchaId, setCaptchaId] = useState("");
  const [captchaImage, setCaptchaImage] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");
  const [registerCodeComposing, setRegisterCodeComposing] = useState(false);
  const [captchaCodeComposing, setCaptchaCodeComposing] = useState(false);
  const [resetCodeComposing, setResetCodeComposing] = useState(false);
  const [form, setForm] = useState({
    username: "",
    email: "",
    password: "",
    code: "",
    nickname: "",
    confirmPwd: "",
  });
  const [resetForm, setResetForm] = useState({
    email: "",
    code: "",
    newPassword: "",
    confirmPwd: "",
  });
  const [error, setError] = useState("");
  const redirectTo = (location.state as { from?: string } | null)?.from;
  const { branding } = useSiteBranding();

  const loadCaptcha = async (silent = false) => {
    if (!silent) {
      setCaptchaLoading(true);
    }
    try {
      const data = await authApi.getLoginCaptcha();
      setCaptchaId(data.captchaId);
      setCaptchaImage(data.imageBase64);
      setCaptchaCode("");
    } catch (err) {
      if (!silent) {
        if (isAppError(err)) {
          setError(err.message);
          toast.error(err.message);
        } else {
          const message = "图形验证码加载失败，请稍后重试";
          setError(message);
          toast.error(message);
        }
      }
    } finally {
      if (!silent) {
        setCaptchaLoading(false);
      }
    }
  };

  const handleSendCode = async () => {
    const email = form.email.trim();
    if (!email) {
      const message = "请先输入 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }
    if (!QQ_EMAIL_PATTERN.test(email)) {
      const message = "请输入有效的 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }

    setError("");
    setSendingCode(true);
    try {
      const result = await authApi.sendRegisterCode({ email });
      toast.success(`验证码已发送至 ${result.email}，5 分钟内有效`);
      setCodeCountdown(60);
    } catch (err) {
      if (isAppError(err)) {
        setError(err.message);
        toast.error(err.message);
      } else {
        const message = "验证码发送失败，请稍后重试";
        setError(message);
        toast.error(message);
      }
    } finally {
      setSendingCode(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (!form.username || !form.password) {
      const message = "请填写必要信息";
      setError(message);
      toast.error(message);
      return;
    }
    if (mode === "register" && form.password !== form.confirmPwd) {
      const message = "两次密码不一致";
      setError(message);
      toast.error(message);
      return;
    }
    if (mode === "register" && !form.email) {
      const message = "注册时请填写 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }
    if (mode === "register" && !QQ_EMAIL_PATTERN.test(form.email.trim())) {
      const message = "请输入有效的 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }
    if (mode === "register" && !form.code) {
      const message = "请输入邮箱验证码";
      setError(message);
      toast.error(message);
      return;
    }
    if (mode === "login" && !captchaCode) {
      const message = "请输入图形验证码";
      setError(message);
      toast.error(message);
      return;
    }
    if (mode === "login" && !captchaId) {
      const message = "图形验证码已失效，请刷新后重试";
      setError(message);
      toast.error(message);
      return;
    }
    setLoading(true);
    try {
      let session;

      if (mode === "login") {
        session = await login({
          username: form.username,
          password: form.password,
          captchaId,
          captchaCode: captchaCode.trim(),
        });
      } else {
        session = await register({
          username: form.username,
          email: form.email.trim(),
          password: form.password,
          verificationCode: form.code.trim(),
          nickname: form.nickname,
        });
      }

      toast.success(mode === "login" ? "登录成功，正在进入控制台" : "注册成功");
      navigate(redirectTo ?? getDefaultPathForRole(session.user.role), { replace: true });
    } catch (err) {
      if (mode === "login") {
        setCaptchaCode("");
        void loadCaptcha(true);
      }
      if (isAppError(err)) {
        setError(err.message);
        toast.error(err.message);
      } else {
        const message = "请求失败，请稍后重试";
        setError(message);
        toast.error(message);
      }
    } finally {
      setLoading(false);
    }
  };

  const canSendCode = !sendingCode && codeCountdown === 0;

  const handleSendResetCode = async () => {
    const email = resetForm.email.trim();
    if (!email) {
      const message = "请先输入 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }
    if (!QQ_EMAIL_PATTERN.test(email)) {
      const message = "请输入有效的 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }

    setError("");
    setSendingResetCode(true);
    try {
      const result = await authApi.sendPasswordResetCode({ email });
      toast.success(`重置验证码已发送至 ${result.email}，5 分钟内有效`);
      setResetCodeCountdown(60);
    } catch (err) {
      if (isAppError(err)) {
        setError(err.message);
        toast.error(err.message);
      } else {
        const message = "重置验证码发送失败，请稍后重试";
        setError(message);
        toast.error(message);
      }
    } finally {
      setSendingResetCode(false);
    }
  };

  const canSendResetCode = !sendingResetCode && resetCodeCountdown === 0;

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!QQ_EMAIL_PATTERN.test(resetForm.email.trim())) {
      const message = "请输入有效的 QQ 邮箱";
      setError(message);
      toast.error(message);
      return;
    }
    if (!resetForm.code.trim()) {
      const message = "请输入邮箱验证码";
      setError(message);
      toast.error(message);
      return;
    }
    if (!resetForm.newPassword) {
      const message = "请输入新密码";
      setError(message);
      toast.error(message);
      return;
    }
    if (resetForm.newPassword.length < 6 || resetForm.newPassword.length > 72) {
      const message = "新密码长度必须为 6-72 位";
      setError(message);
      toast.error(message);
      return;
    }
    if (resetForm.newPassword !== resetForm.confirmPwd) {
      const message = "两次密码不一致";
      setError(message);
      toast.error(message);
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({
        email: resetForm.email.trim(),
        verificationCode: resetForm.code.trim(),
        newPassword: resetForm.newPassword,
      });
      toast.success("密码重置成功，请使用新密码登录");
      setForm((prev) => ({ ...prev, username: resetForm.email.trim() }));
      setResetDialogOpen(false);
      setResetForm({
        email: "",
        code: "",
        newPassword: "",
        confirmPwd: "",
      });
    } catch (err) {
      if (isAppError(err)) {
        setError(err.message);
        toast.error(err.message);
      } else {
        const message = "密码重置失败，请稍后重试";
        setError(message);
        toast.error(message);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadCaptcha();
  }, []);

  useEffect(() => {
    if (codeCountdown <= 0) {
      return;
    }

    const timer = window.setTimeout(() => {
      setCodeCountdown((value) => Math.max(value - 1, 0));
    }, 1000);

    return () => window.clearTimeout(timer);
  }, [codeCountdown]);

  useEffect(() => {
    if (resetCodeCountdown <= 0) {
      return;
    }

    const timer = window.setTimeout(() => {
      setResetCodeCountdown((value) => Math.max(value - 1, 0));
    }, 1000);

    return () => window.clearTimeout(timer);
  }, [resetCodeCountdown]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 flex items-center justify-center p-4">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-blue-200/30 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-indigo-200/30 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-[420px]">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 shadow-lg shadow-blue-500/30 mb-4">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-slate-900 text-2xl">{branding.siteName}</h1>
          <p className="text-slate-500 text-sm mt-1">{branding.siteDescription}</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/60 border border-slate-100 overflow-hidden">
          {/* Tabs */}
          <div className="flex border-b border-slate-100">
            {(["login", "register"] as const).map((m) => (
              <button
                key={m}
                onClick={() => { setMode(m); setError(""); }}
                className={`flex-1 py-4 text-sm transition-all relative ${
                  mode === m ? "text-blue-600" : "text-slate-500 hover:text-slate-700"
                }`}
              >
                {m === "login" ? "登录" : "注册"}
                {mode === m && (
                  <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-blue-600 rounded-full" />
                )}
              </button>
            ))}
          </div>

          <div className="p-6">
            <form onSubmit={handleSubmit} className="space-y-4">
              {mode === "register" && (
                <div>
                  <label className="block text-sm text-slate-600 mb-1.5">昵称</label>
                  <input
                    type="text"
                    value={form.nickname}
                    onChange={(e) => setForm((p) => ({ ...p, nickname: e.target.value }))}
                    placeholder="您的昵称"
                    className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                  />
                </div>
              )}
              <div>
                <label className="block text-sm text-slate-600 mb-1.5">账号</label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={form.username}
                    onChange={(e) => setForm((p) => ({ ...p, username: e.target.value }))}
                    placeholder="请输入用户名"
                    className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                  />
                </div>
              </div>
              {mode !== "login" ? (
                <>
                  <div>
                    <label className="block text-sm text-slate-600 mb-1.5">QQ 邮箱</label>
                    <div className="relative">
                      <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="email"
                        value={form.email}
                        onChange={(e) => setForm((p) => ({ ...p, email: e.target.value.trim() }))}
                        placeholder="123456@qq.com"
                        className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm text-slate-600 mb-1.5">验证码</label>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={form.code}
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        onCompositionStart={() => setRegisterCodeComposing(true)}
                        onCompositionEnd={(e) => {
                          setRegisterCodeComposing(false);
                          setForm((p) => ({
                            ...p,
                            code: e.currentTarget.value.replace(/\D/g, "").slice(0, 6),
                          }));
                        }}
                        onChange={(e) =>
                          setForm((p) => ({
                            ...p,
                            code: registerCodeComposing
                              ? e.target.value
                              : e.target.value.replace(/\D/g, "").slice(0, 6),
                          }))
                        }
                        placeholder="请输入 6 位验证码"
                        className="flex-1 px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                      />
                      <button
                        type="button"
                        disabled={!canSendCode}
                        onClick={handleSendCode}
                        className="shrink-0 min-w-28 px-3 py-2.5 rounded-xl border border-blue-200 bg-blue-50 text-blue-600 text-sm transition-colors hover:bg-blue-100 disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                      >
                        {sendingCode
                          ? "发送中..."
                          : codeCountdown > 0
                            ? `${codeCountdown}s 后重发`
                            : "发送验证码"}
                      </button>
                    </div>
                    <p className="mt-1 text-xs text-slate-400">验证码会发送到 QQ 邮箱，有效期 5 分钟。</p>
                  </div>
                </>
              ) : null}
              <div>
                <div className="flex justify-between mb-1.5">
                  <label className="text-sm text-slate-600">密码</label>
                  {mode === "login" && (
                    <button
                      type="button"
                      onClick={() => {
                        setError("");
                        setResetDialogOpen(true);
                        setResetForm((prev) => ({
                          ...prev,
                          email: form.username.includes("@") ? form.username.trim() : prev.email,
                        }));
                      }}
                      className="text-xs text-blue-600 transition-colors hover:text-blue-700"
                    >
                      忘记密码？
                    </button>
                  )}
                </div>
                <div className="relative">
                  <input
                    type={showPwd ? "text" : "password"}
                    value={form.password}
                    onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
                    placeholder="••••••••"
                    className="w-full px-3.5 py-2.5 pr-11 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                  />
                  <button type="button" onClick={() => setShowPwd(!showPwd)} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                    {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              {mode === "login" ? (
                <div>
                  <label className="block text-sm text-slate-600 mb-1.5">图形验证码</label>
                  <div className="flex items-stretch gap-2">
                    <input
                      type="text"
                      value={captchaCode}
                      inputMode="text"
                      autoCapitalize="characters"
                      autoComplete="one-time-code"
                      onCompositionStart={() => setCaptchaCodeComposing(true)}
                      onCompositionEnd={(e) => {
                        setCaptchaCodeComposing(false);
                        setCaptchaCode(
                          e.currentTarget.value.replace(/\s+/g, "").toUpperCase().slice(0, 6),
                        );
                      }}
                      onChange={(e) =>
                        setCaptchaCode(
                          captchaCodeComposing
                            ? e.target.value
                            : e.target.value.replace(/\s+/g, "").toUpperCase().slice(0, 6),
                        )
                      }
                      placeholder="请输入图形验证码"
                      className="flex-1 px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => void loadCaptcha()}
                      disabled={captchaLoading}
                      className="group relative flex h-[42px] w-32 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-slate-200 bg-slate-50 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-70"
                      title="点击刷新验证码"
                    >
                      {captchaImage ? (
                        <img src={captchaImage} alt="图形验证码" className="h-full w-full object-cover" />
                      ) : (
                        <span className="text-xs text-slate-400">
                          {captchaLoading ? "加载中..." : "加载失败"}
                        </span>
                      )}
                      <span className="absolute inset-0 hidden items-center justify-center bg-slate-900/8 text-slate-600 group-hover:flex">
                        <RefreshCw className={`h-4 w-4 ${captchaLoading ? "animate-spin" : ""}`} />
                      </span>
                    </button>
                  </div>
                  <div className="mt-1 text-xs text-slate-400">登录时验证码必填，点击图片可刷新。</div>
                </div>
              ) : null}
              {mode === "register" && (
                <div>
                  <label className="block text-sm text-slate-600 mb-1.5">确认密码</label>
                  <input
                    type="password"
                    value={form.confirmPwd}
                    onChange={(e) => setForm((p) => ({ ...p, confirmPwd: e.target.value }))}
                    placeholder="••••••••"
                    className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                  />
                </div>
              )}
              {error && (
                <div className="text-red-500 text-xs bg-red-50 border border-red-100 px-3 py-2 rounded-lg">{error}</div>
              )}

              {mode === "login" && (
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="remember" className="w-3.5 h-3.5 rounded text-blue-600" />
                  <label htmlFor="remember" className="text-xs text-slate-500 cursor-pointer">保持登录状态</label>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 disabled:opacity-60 text-white py-2.5 rounded-xl text-sm font-medium transition-all shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 mt-1"
              >
                {loading && <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />}
                {loading ? "处理中..." : mode === "login" ? "登 录" : "创建账户"}
              </button>
            </form>

            {/* Divider */}
            <div className="relative my-5">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-100" />
              </div>
              <div className="relative text-center">
                <span className="bg-white px-3 text-xs text-slate-400">或通过第三方登录</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => toast.info("此功能暂未开放")}
              className="w-full flex items-center justify-center gap-2.5 py-2.5 border border-slate-200 rounded-xl text-slate-600 hover:bg-slate-50 text-sm transition-colors"
            >
              <Github className="w-4 h-4" />
              使用 GitHub 登录
            </button>
          </div>
        </div>

        <p className="text-center text-xs text-slate-400 mt-5">
          登录即表示您同意我们的
          <a href="#" className="text-blue-500 hover:underline mx-1">服务条款</a>和
          <a href="#" className="text-blue-500 hover:underline mx-1">隐私政策</a>
        </p>
      </div>

      <Dialog
        open={resetDialogOpen}
        onOpenChange={(open) => {
          setResetDialogOpen(open);
          if (!open) {
            setError("");
          }
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>忘记密码</DialogTitle>
            <DialogDescription>通过 QQ 邮箱验证码重置登录密码，重置成功后请使用新密码登录。</DialogDescription>
          </DialogHeader>

          <form onSubmit={handleResetPassword} className="space-y-4">
            <div>
              <label className="block text-sm text-slate-600 mb-1.5">QQ 邮箱</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  value={resetForm.email}
                  onChange={(e) => setResetForm((prev) => ({ ...prev, email: e.target.value.trim() }))}
                  placeholder="123456@qq.com"
                  className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm text-slate-600 mb-1.5">验证码</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={resetForm.code}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  onCompositionStart={() => setResetCodeComposing(true)}
                  onCompositionEnd={(e) => {
                    setResetCodeComposing(false);
                    setResetForm((prev) => ({
                      ...prev,
                      code: e.currentTarget.value.replace(/\D/g, "").slice(0, 6),
                    }));
                  }}
                  onChange={(e) =>
                    setResetForm((prev) => ({
                      ...prev,
                      code: resetCodeComposing
                        ? e.target.value
                        : e.target.value.replace(/\D/g, "").slice(0, 6),
                    }))
                  }
                  placeholder="请输入 6 位验证码"
                  className="flex-1 px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
                />
                <button
                  type="button"
                  disabled={!canSendResetCode}
                  onClick={handleSendResetCode}
                  className="shrink-0 min-w-28 px-3 py-2.5 rounded-xl border border-blue-200 bg-blue-50 text-blue-600 text-sm transition-colors hover:bg-blue-100 disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                >
                  {sendingResetCode
                    ? "发送中..."
                    : resetCodeCountdown > 0
                      ? `${resetCodeCountdown}s 后重发`
                      : "发送验证码"}
                </button>
              </div>
              <p className="mt-1 text-xs text-slate-400">重置验证码会发送到 QQ 邮箱，有效期 5 分钟。</p>
            </div>
            <div>
              <label className="block text-sm text-slate-600 mb-1.5">新密码</label>
              <input
                type={showPwd ? "text" : "password"}
                value={resetForm.newPassword}
                onChange={(e) => setResetForm((prev) => ({ ...prev, newPassword: e.target.value }))}
                placeholder="请输入新密码"
                className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
              />
            </div>
            <div>
              <label className="block text-sm text-slate-600 mb-1.5">确认密码</label>
              <input
                type={showPwd ? "text" : "password"}
                value={resetForm.confirmPwd}
                onChange={(e) => setResetForm((prev) => ({ ...prev, confirmPwd: e.target.value }))}
                placeholder="请再次输入新密码"
                className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm placeholder:text-slate-400 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-500/15 bg-slate-50/50 transition-all"
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={() => setResetDialogOpen(false)}
                className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-50"
              >
                取消
              </button>
              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700 disabled:opacity-60"
              >
                {loading ? <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" /> : null}
                {loading ? "提交中..." : "确认重置"}
              </button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
