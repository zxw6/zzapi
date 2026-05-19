import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import { Eye, EyeOff, Zap, Shield, Globe, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../auth/auth-context";
import { authApi } from "../api/modules/auth";
import { getDefaultPathForRole } from "../auth/role-utils";
import { isAppError } from "../../lib/http/error";
import { useSiteBranding } from "../site-settings/site-branding";

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");
  const [captchaId, setCaptchaId] = useState("");
  const [captchaImage, setCaptchaImage] = useState("");
  const [loading, setLoading] = useState(false);
  const [captchaLoading, setCaptchaLoading] = useState(false);
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

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (!username || !password || !captchaCode) {
      const message = "请输入用户名、密码和验证码";
      setError(message);
      toast.error(message);
      return;
    }
    if (!captchaId) {
      const message = "图形验证码已失效，请刷新后重试";
      setError(message);
      toast.error(message);
      return;
    }
    setLoading(true);
    try {
      const session = await login({
        username,
        password,
        captchaId,
        captchaCode: captchaCode.trim(),
      });
      toast.success("登录成功，正在进入后台");
      navigate(redirectTo ?? getDefaultPathForRole(session.user.role), { replace: true });
    } catch (err) {
      setCaptchaCode("");
      void loadCaptcha(true);
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

  useEffect(() => {
    void loadCaptcha();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 flex">
      {/* Left panel */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 left-20 w-64 h-64 rounded-full bg-blue-400 blur-3xl" />
          <div className="absolute bottom-32 right-16 w-80 h-80 rounded-full bg-cyan-400 blur-3xl" />
        </div>
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 rounded-xl bg-blue-500 flex items-center justify-center">
              <Zap className="w-5 h-5 text-white" />
            </div>
            <span className="text-white text-xl font-semibold">{branding.siteName}</span>
          </div>
          <p className="text-blue-300 text-sm">{branding.siteDescription}</p>
        </div>
        <div className="relative z-10 space-y-8">
          <div>
            <h1 className="text-4xl text-white mb-4 leading-tight">
              统一管理您的<br />
              <span className="text-blue-400">AI 接口资源</span>
            </h1>
            <p className="text-slate-400 text-base leading-relaxed">
              智能路由、负载均衡、故障切换，为您的业务提供稳定可靠的 API 中转服务。
            </p>
          </div>
          <div className="grid grid-cols-3 gap-4">
            {[
              { icon: Zap, label: "智能路由", desc: "自动选择最优节点" },
              { icon: Shield, label: "安全防护", desc: "多重安全策略" },
              { icon: Globe, label: "全球覆盖", desc: "多区域就近接入" },
            ].map(({ icon: Icon, label, desc }) => (
              <div key={label} className="bg-white/5 rounded-xl p-4 border border-white/10">
                <Icon className="w-5 h-5 text-blue-400 mb-2" />
                <div className="text-white text-sm font-medium">{label}</div>
                <div className="text-slate-400 text-xs mt-0.5">{desc}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="relative z-10 flex gap-6 text-slate-500 text-sm">
          <span>© 2026 {branding.siteName}</span>
          <a href="#" className="hover:text-slate-300 transition-colors">隐私政策</a>
          <a href="#" className="hover:text-slate-300 transition-colors">服务条款</a>
        </div>
      </div>

      {/* Right panel - Login form */}
      <div className="flex-1 flex items-center justify-center p-6 lg:p-12">
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="flex items-center gap-3 mb-8 lg:hidden">
            <div className="w-9 h-9 rounded-xl bg-blue-500 flex items-center justify-center">
              <Zap className="w-4 h-4 text-white" />
            </div>
            <span className="text-white text-lg font-semibold">{branding.siteName}</span>
          </div>

          <div className="bg-white rounded-2xl shadow-2xl p-8">
            <div className="mb-7">
              <h2 className="text-slate-900 mb-1">欢迎回来</h2>
              <p className="text-slate-500 text-sm">请登录您的管理后台账户</p>
            </div>

            <form onSubmit={handleLogin} className="space-y-5">
              <div>
                <label className="block text-slate-700 text-sm mb-1.5">账号</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="请输入用户名"
                  className="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-slate-900 placeholder:text-slate-400 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all bg-white text-sm"
                />
              </div>
              <div>
                <label className="block text-slate-700 text-sm mb-1.5">密码</label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="请输入密码"
                    className="w-full px-4 py-2.5 pr-11 border border-slate-200 rounded-lg text-slate-900 placeholder:text-slate-400 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all bg-white text-sm"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
              <div>
                <label className="block text-slate-700 text-sm mb-1.5">图形验证码</label>
                <div className="flex items-stretch gap-2">
                  <input
                    type="text"
                    value={captchaCode}
                    onChange={(e) => setCaptchaCode(e.target.value.trim().toUpperCase().slice(0, 6))}
                    placeholder="请输入图形验证码"
                    className="flex-1 px-4 py-2.5 border border-slate-200 rounded-lg text-slate-900 placeholder:text-slate-400 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all bg-white text-sm"
                  />
                  <button
                    type="button"
                    onClick={() => void loadCaptcha()}
                    disabled={captchaLoading}
                    className="group relative flex h-[42px] w-32 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-70"
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
                <div className="mt-1 flex items-center justify-between text-xs text-slate-400">
                  <span>验证码必填，点击图片可刷新。</span>
                  <button
                    type="button"
                    onClick={() => void loadCaptcha()}
                    className="text-blue-600 transition-colors hover:text-blue-700"
                  >
                   刷新验证码
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-blue-600" />
                  <span className="text-slate-600 text-sm">记住我</span>
                </label>
                <a href="#" className="text-blue-600 text-sm hover:text-blue-700 transition-colors">忘记密码？</a>
              </div>

              {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-2.5 text-red-600 text-sm">
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white py-2.5 rounded-lg transition-colors flex items-center justify-center gap-2 text-sm"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    登录中...
                  </>
                ) : "登 录"}
              </button>
            </form>

            <div className="mt-5 pt-5 border-t border-slate-100 text-center">
              <p className="text-slate-500 text-sm">
                支持管理员与用户账号登录，系统会按角色自动跳转
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
