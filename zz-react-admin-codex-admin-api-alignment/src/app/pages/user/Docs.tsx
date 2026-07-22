import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router";
import {
  ArrowRight,
  BookOpen,
  CheckCircle,
  Copy,
  ExternalLink,
  FolderCog,
  KeyRound,
  MonitorSmartphone,
  Rocket,
  Terminal,
  Wrench,
} from "lucide-react";
import { env } from "../../../lib/config/env";
import { useRouteTransition } from "../../components/layout/route-transition-context";

type Platform = "windows" | "mac";

const installCommands: Record<Platform, string> = {
  windows: `# 1. 安装 Node.js 20+
# https://nodejs.org/

# 2. 安装 Codex CLI
npm install -g @openai/codex

# 3. 验证版本
codex --version`,
  mac: `# 1. 安装 Node.js 20+
brew install node

# 2. 安装 Codex CLI
npm install -g @openai/codex

# 3. 验证版本
codex --version`,
};

function buildAuthJsonSample(apiKey: string) {
  return `{
  "OPENAI_API_KEY": "${apiKey}"
}`;
}

function buildConfigTomlSample(apiBaseUrl: string) {
  return `[default]
model = "gpt-5"
provider = "openai"

[providers.openai]
name = "openai"
base_url = "${apiBaseUrl}"
wire_api = "responses"`;
}

function buildRunExample(apiBaseUrl: string) {
  return `# 启动 Codex
codex

# 进入后可以直接提问
> 帮我分析当前项目结构
> 帮我写一个 React 表单组件

# 如果你要显式指定环境，也可以确认配置文件中的 base_url
# 当前接入地址
${apiBaseUrl}`;
}

const sections = [
  { id: "prepare", label: "准备环境" },
  { id: "install", label: "安装 Codex" },
  { id: "config", label: "配置认证" },
  { id: "run", label: "启动验证" },
  { id: "workflow", label: "推荐工作流" },
];

function findScrollContainer(element: HTMLElement | null) {
  if (!element) {
    return null;
  }

  let current = element.parentElement;
  while (current) {
    const style = window.getComputedStyle(current);
    const overflowY = style.overflowY;
    if (
      (overflowY === "auto" || overflowY === "scroll") &&
      current.scrollHeight > current.clientHeight
    ) {
      return current;
    }
    current = current.parentElement;
  }

  return document.scrollingElement instanceof HTMLElement ? document.scrollingElement : null;
}

export function DocsPage() {
  const navigate = useNavigate();
  const { beginTransition } = useRouteTransition();
  const [platform, setPlatform] = useState<Platform>("windows");
  const [copied, setCopied] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<string>(sections[0].id);
  const contentRef = useRef<HTMLDivElement | null>(null);
  const scrollContainerRef = useRef<HTMLElement | null>(null);

  const apiBaseUrl = useMemo(() => env.apiBaseUrl.replace(/\/$/, ""), []);
  const authJsonSample = useMemo(() => buildAuthJsonSample("sk-你的_API_Key"), []);
  const configTomlSample = useMemo(() => buildConfigTomlSample(apiBaseUrl), [apiBaseUrl]);
  const runExample = useMemo(() => buildRunExample(apiBaseUrl), [apiBaseUrl]);

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text).catch(() => {});
    setCopied(id);
    setTimeout(() => setCopied(null), 1500);
  };

  const navigateWithTransition = (path: string) => {
    beginTransition(path);
    navigate(path);
  };

  useEffect(() => {
    scrollContainerRef.current = findScrollContainer(contentRef.current);
    const container = scrollContainerRef.current;
    if (!container) {
      return;
    }

    const updateActiveSection = () => {
      const containerTop = container.getBoundingClientRect().top;
      let nextActive = sections[0].id;

      for (const section of sections) {
        const element = container.querySelector<HTMLElement>(`#${section.id}`);
        if (!element) {
          continue;
        }

        const offset = element.getBoundingClientRect().top - containerTop;
        if (offset <= 120) {
          nextActive = section.id;
        } else {
          break;
        }
      }

      setActiveSection((current) => (current === nextActive ? current : nextActive));
    };

    updateActiveSection();
    container.addEventListener("scroll", updateActiveSection, { passive: true });
    return () => container.removeEventListener("scroll", updateActiveSection);
  }, []);

  const scrollToSection = (sectionId: string) => {
    const container = scrollContainerRef.current ?? findScrollContainer(contentRef.current);
    const scope = contentRef.current;
    const element = scope?.querySelector<HTMLElement>(`#${sectionId}`);
    if (!container || !element) {
      return;
    }

    setActiveSection(sectionId);
    const containerTop = container.getBoundingClientRect().top;
    const elementTop = element.getBoundingClientRect().top;
    const targetTop = container.scrollTop + (elementTop - containerTop) - 24;
    container.scrollTo({ top: targetTop, behavior: "smooth" });
  };

  return (
    <div className="flex h-full text-slate-900 dark:text-slate-100">
      <aside className="hidden w-56 flex-shrink-0 border-r border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 lg:block">
        <div className="mb-3 px-2 text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-slate-500">
          Codex 接入
        </div>
        <nav className="sticky top-4 space-y-1">
          {sections.map((item) => (
            <button
              key={item.id}
              onClick={() => scrollToSection(item.id)}
              className={`block w-full rounded-xl px-3 py-2 text-left text-sm transition-colors ${
                activeSection === item.id
                  ? "bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-300"
                  : "text-slate-500 hover:bg-slate-50 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <div ref={contentRef} className="flex-1 overflow-auto bg-[#f6f7f9] dark:bg-slate-950">
        <div className="mx-auto max-w-5xl space-y-8 px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
          <section className="rounded-[28px] bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-950 p-6 text-white shadow-xl shadow-slate-950/20">
            <div className="flex flex-wrap items-center gap-2 text-xs text-blue-100/90">
              <span className="rounded-full bg-white/10 px-3 py-1">Codex CLI</span>
              <span className="rounded-full bg-white/10 px-3 py-1">GPT-5 工作流</span>
              <span className="rounded-full bg-white/10 px-3 py-1">适配当前平台地址</span>
            </div>
            <h1 className="mt-4 text-white">快速接入 Codex 工作流</h1>
            <p className="mt-3 max-w-3xl text-sm leading-6 text-blue-100/80">
              这里按你提供的参考文章结构，重做成适合当前站点的 Codex 接入页。核心流程是：
              安装 Node.js 与 Codex CLI，写入 `auth.json` 与 `config.toml`，把模型请求指向当前平台地址，然后直接在终端启动使用。
            </p>
            <div className="mt-5 flex flex-col items-stretch gap-3 sm:flex-row sm:items-center">
              <button
                onClick={() => navigateWithTransition("/console/keys")}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-medium text-slate-900 transition-colors hover:bg-blue-50"
              >
                获取 API Key
                <ArrowRight className="h-4 w-4" />
              </button>
              <a
                href="https://www.relaxycode.com/blog/codex-gpt"
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/10 px-4 py-2 text-sm text-white transition-colors hover:bg-white/15"
              >
                查看参考文章
                <ExternalLink className="h-4 w-4" />
              </a>
            </div>
          </section>

          <section
            id="prepare"
            className="rounded-2xl border border-slate-100 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-blue-50 dark:bg-blue-500/15">
                <MonitorSmartphone className="h-5 w-5 text-blue-600 dark:text-blue-300" />
              </div>
              <div>
                <h2 className="text-slate-900 dark:text-slate-100">准备环境</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  先准备好 Node.js、npm 和当前平台 API Key。
                </p>
              </div>
            </div>
            <div className="mt-5 grid gap-4 md:grid-cols-3">
              {[
                {
                  title: "Node.js 20+",
                  desc: "Codex CLI 依赖 Node.js 运行环境，建议使用最新 LTS 版本。",
                },
                {
                  title: "可用 API Key",
                  desc: "在「我的 API Keys」里创建 Key，后续写入 .codex/auth.json。",
                },
                {
                  title: "当前接入地址",
                  desc: apiBaseUrl,
                },
              ].map((item) => (
                <div
                  key={item.title}
                  className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-950"
                >
                  <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                    {item.title}
                  </div>
                  <div className="mt-2 break-all text-xs leading-5 text-slate-500 dark:text-slate-400">
                    {item.desc}
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section
            id="install"
            className="rounded-2xl border border-slate-100 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 dark:bg-indigo-500/15">
                  <Terminal className="h-5 w-5 text-indigo-600 dark:text-indigo-300" />
                </div>
                <div>
                  <h2 className="text-slate-900 dark:text-slate-100">安装 Codex</h2>
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                    按系统执行安装命令，安装后先用 `codex --version` 验证。
                  </p>
                </div>
              </div>
              <div className="flex gap-1 rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
                {([
                  ["windows", "Windows"],
                  ["mac", "macOS"],
                ] as const).map(([value, label]) => (
                  <button
                    key={value}
                    onClick={() => setPlatform(value)}
                    className={`rounded-lg px-3 py-1.5 text-xs transition-colors ${
                      platform === value
                        ? "bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-slate-100"
                        : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-100"
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="relative mt-5 overflow-hidden rounded-2xl bg-slate-950">
              <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
                <span className="text-xs text-slate-400">安装命令</span>
                <button
                  onClick={() => handleCopy(installCommands[platform], "install")}
                  className="inline-flex items-center gap-1.5 text-xs text-slate-400 transition-colors hover:text-white"
                >
                  {copied === "install" ? (
                    <CheckCircle className="h-3.5 w-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="h-3.5 w-3.5" />
                  )}
                  复制
                </button>
              </div>
              <pre className="overflow-x-auto p-4 text-sm leading-6 text-slate-200">
                <code>{installCommands[platform]}</code>
              </pre>
            </div>
          </section>

          <section
            id="config"
            className="rounded-2xl border border-slate-100 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-50 dark:bg-emerald-500/15">
                <FolderCog className="h-5 w-5 text-emerald-600 dark:text-emerald-300" />
              </div>
              <div>
                <h2 className="text-slate-900 dark:text-slate-100">配置认证</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  参考文章里的做法，配置 `.codex/auth.json` 和 `.codex/config.toml`。
                </p>
              </div>
            </div>

            <div className="mt-5 grid gap-4 xl:grid-cols-2">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-950">
                <div className="flex items-center justify-between">
                  <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                    `~/.codex/auth.json`
                  </div>
                  <button
                    onClick={() => handleCopy(authJsonSample, "auth-json")}
                    className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-100"
                  >
                    {copied === "auth-json" ? (
                      <CheckCircle className="h-4 w-4 text-emerald-500" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </button>
                </div>
                <pre className="mt-3 overflow-x-auto rounded-xl bg-slate-900 p-4 text-sm leading-6 text-slate-200">
                  <code>{authJsonSample}</code>
                </pre>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-950">
                <div className="flex items-center justify-between">
                  <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                    `~/.codex/config.toml`
                  </div>
                  <button
                    onClick={() => handleCopy(configTomlSample, "config-toml")}
                    className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-100"
                  >
                    {copied === "config-toml" ? (
                      <CheckCircle className="h-4 w-4 text-emerald-500" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </button>
                </div>
                <pre className="mt-3 overflow-x-auto rounded-xl bg-slate-900 p-4 text-sm leading-6 text-slate-200">
                  <code>{configTomlSample}</code>
                </pre>
              </div>
            </div>

            <div className="mt-4 rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm leading-6 text-blue-800 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-200">
              `base_url` 已按你的要求改成当前平台地址 `http://www.zlapi.online/api`。如果后续切换环境，只需要替换这里，不需要改 CLI 命令。
            </div>
          </section>

          <section
            id="run"
            className="rounded-2xl border border-slate-100 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-50 dark:bg-amber-500/15">
                <Rocket className="h-5 w-5 text-amber-600 dark:text-amber-300" />
              </div>
              <div>
                <h2 className="text-slate-900 dark:text-slate-100">启动验证</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  配置完成后直接运行 `codex`，确认能进入交互界面并正常响应。
                </p>
              </div>
            </div>
            <div className="relative mt-5 overflow-hidden rounded-2xl bg-slate-950">
              <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
                <span className="text-xs text-slate-400">启动示例</span>
                <button
                  onClick={() => handleCopy(runExample, "run-example")}
                  className="inline-flex items-center gap-1.5 text-xs text-slate-400 transition-colors hover:text-white"
                >
                  {copied === "run-example" ? (
                    <CheckCircle className="h-3.5 w-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="h-3.5 w-3.5" />
                  )}
                  复制
                </button>
              </div>
              <pre className="overflow-x-auto p-4 text-sm leading-6 text-slate-200">
                <code>{runExample}</code>
              </pre>
            </div>
          </section>

          <section
            id="workflow"
            className="rounded-2xl border border-slate-100 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-violet-50 dark:bg-violet-500/15">
                <Wrench className="h-5 w-5 text-violet-600 dark:text-violet-300" />
              </div>
              <div>
                <h2 className="text-slate-900 dark:text-slate-100">推荐工作流</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  这一部分按参考文章思路，整理成更适合当前项目的实际使用方式。
                </p>
              </div>
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {[
                {
                  icon: Terminal,
                  title: "终端直连",
                  desc: "适合直接分析仓库、改代码、跑命令，路径最短。",
                },
                {
                  icon: KeyRound,
                  title: "先配 Key 再配 Codex",
                  desc: "建议先在当前平台创建专用 Key，再写入 .codex 配置文件。",
                },
                {
                  icon: BookOpen,
                  title: "配合文档页使用",
                  desc: "把常用命令、配置模板、平台地址固定在这里，方便团队复用。",
                },
                {
                  icon: MonitorSmartphone,
                  title: "VS Code 联动",
                  desc: "如果你常在编辑器里工作，可以把同样的配置迁移到 VS Code 插件侧。",
                },
              ].map(({ icon: Icon, title, desc }) => (
                <div
                  key={title}
                  className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-950"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-white shadow-sm dark:bg-slate-900">
                    <Icon className="h-4 w-4 text-slate-700 dark:text-slate-200" />
                  </div>
                  <div className="mt-4 text-sm font-medium text-slate-900 dark:text-slate-100">
                    {title}
                  </div>
                  <div className="mt-2 text-xs leading-5 text-slate-500 dark:text-slate-400">
                    {desc}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-5 flex flex-col items-stretch gap-3 sm:flex-row">
              <button
                onClick={() => navigateWithTransition("/console/keys")}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
              >
                去创建 API Key
                <ArrowRight className="h-4 w-4" />
              </button>
              <a
                href="https://www.relaxycode.com/blog/codex-gpt"
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
              >
                查看原文参考
                <ExternalLink className="h-4 w-4" />
              </a>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
