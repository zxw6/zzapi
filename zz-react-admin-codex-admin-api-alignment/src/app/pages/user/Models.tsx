import { useEffect, useMemo, useState } from "react";
import {
  ArrowUpRight,
  ChevronRight,
  Code2,
  Layers3,
  Search,
  Sparkles,
  Star,
} from "lucide-react";
import { useModelsQuery } from "../../api/queries";
import { isUserVisibleModel, mapModelListItemToCatalog } from "../../api/model-utils";
import { isAppError } from "../../../lib/http/error";
import { PageErrorState, PagePanelSkeleton } from "../../components/ui/feedback";
import { Skeleton } from "../../components/ui/skeleton";

type Category = "全部" | "对话" | "推理" | "代码" | "多模态" | "嵌入";
const EMPTY_MODELS: never[] = [];
const categories: Category[] = ["全部", "对话", "推理", "代码", "多模态", "嵌入"];

const providerTones: Record<string, string> = {
  OpenAI: "border-emerald-200 bg-emerald-50 text-emerald-700",
  Anthropic: "border-orange-200 bg-orange-50 text-orange-700",
  Google: "border-sky-200 bg-sky-50 text-sky-700",
  DeepSeek: "border-violet-200 bg-violet-50 text-violet-700",
  阿里云: "border-rose-200 bg-rose-50 text-rose-700",
};

function getProviderTone(provider: string) {
  return (
    providerTones[provider] ??
    "border-slate-200 bg-slate-50 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
  );
}

function formatPrice(value: number) {
  if (!value) {
    return "免费";
  }

  return `$${value}/M`;
}

export function ModelsPage() {
  const { data, isLoading, isError, refetch, error } = useModelsQuery();
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<Category>("全部");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const models = useMemo(
    () => (data ?? EMPTY_MODELS).filter(isUserVisibleModel).map(mapModelListItemToCatalog),
    [data],
  );

  const filtered = useMemo(
    () =>
      models.filter((model) => {
        const keyword = search.trim().toLowerCase();
        const matchSearch =
          !keyword ||
          model.name.toLowerCase().includes(keyword) ||
          model.provider.toLowerCase().includes(keyword) ||
          model.desc.toLowerCase().includes(keyword);
        const matchCategory = category === "全部" || model.category.includes(category);

        return matchSearch && matchCategory;
      }),
    [category, models, search],
  );

  useEffect(() => {
    if (filtered.length === 0) {
      setSelectedId(null);
      return;
    }

    if (!selectedId || !filtered.some((item) => item.id === selectedId)) {
      setSelectedId(filtered[0]?.id ?? null);
    }
  }, [filtered, selectedId]);

  const selected = filtered.find((item) => item.id === selectedId) ?? null;
  const providerCount = new Set(filtered.map((item) => item.provider)).size;

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6 p-4 sm:p-6">
        <div className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-6">
          <div className="space-y-3">
            <Skeleton className="h-8 w-32 rounded-xl" />
            <Skeleton className="h-4 w-72 rounded-lg" />
          </div>
          <div className="mt-6 grid gap-3 lg:grid-cols-[minmax(0,20rem)_1fr]">
            <Skeleton className="h-12 rounded-2xl" />
            <Skeleton className="h-12 rounded-2xl" />
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <div className="grid gap-4 lg:grid-cols-2">
            {Array.from({ length: 6 }).map((_, index) => (
              <PagePanelSkeleton key={index} lines={5} />
            ))}
          </div>
          <PagePanelSkeleton className="h-[32rem]" lines={8} />
        </div>
      </div>
    );
  }

  if (isError) {
    const message = isAppError(error) ? error.message : "加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-4 text-slate-900 dark:text-slate-100 sm:p-6">
      <section className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="relative overflow-hidden px-5 py-6 sm:px-6">
          <div className="absolute inset-y-0 right-0 hidden w-1/2 bg-[radial-gradient(circle_at_top_right,_rgba(59,130,246,0.12),_transparent_60%)] xl:block" />
          <div className="relative space-y-5">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <div className="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-xs text-blue-700 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300">
                  <Sparkles className="h-3.5 w-3.5" />
                  精选公开模型
                </div>
                <h1 className="mt-3 text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
                  模型广场
                </h1>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  浏览当前可调用模型、厂商分布和计费方式，右侧详情会固定展示，不会随着页面下滑消失。
                </p>
              </div>

              <div className="grid grid-cols-3 gap-3 lg:w-[22rem]">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/40">
                  <div className="text-xs text-slate-500 dark:text-slate-400">公开模型</div>
                  <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {filtered.length}
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/40">
                  <div className="text-xs text-slate-500 dark:text-slate-400">服务商</div>
                  <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {providerCount}
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/40">
                  <div className="text-xs text-slate-500 dark:text-slate-400">当前筛选</div>
                  <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                    {category}
                  </div>
                </div>
              </div>
            </div>

            <div className="grid gap-3 lg:grid-cols-[minmax(0,20rem)_1fr]">
              <div className="relative">
                <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="搜索模型、厂商或上游模型..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:focus:border-blue-400"
                />
              </div>
              <div className="flex flex-wrap gap-2">
                {categories.map((item) => (
                  <button
                    key={item}
                    onClick={() => setCategory(item)}
                    className={`rounded-2xl px-4 py-2 text-sm transition-all ${
                      category === item
                        ? "bg-slate-900 text-white shadow-sm dark:bg-white dark:text-slate-900"
                        : "border border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:text-slate-700 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-400 dark:hover:border-slate-600 dark:hover:text-slate-100"
                    }`}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {filtered.length === 0 ? (
        <div className="rounded-[28px] border border-slate-200 bg-white px-6 py-12 text-center shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 dark:bg-slate-800">
            <Search className="h-5 w-5 text-slate-400 dark:text-slate-500" />
          </div>
          <h3 className="mt-4 text-lg font-medium text-slate-900 dark:text-slate-100">暂无可用模型</h3>
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
            当前分类或搜索条件下没有匹配结果，可以切换类型或清空关键词后重试。
          </p>
          <button
            onClick={() => {
              setSearch("");
              setCategory("全部");
            }}
            className="mt-5 rounded-2xl border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
          >
            清空筛选
          </button>
        </div>
      ) : (
        <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <section className="grid content-start gap-4 lg:grid-cols-2">
            {filtered.map((model) => {
              const selectedCard = selectedId === model.id;

              return (
                <button
                  key={model.id}
                  onClick={() => setSelectedId(model.id)}
                  className={`group self-start overflow-hidden rounded-[26px] border p-5 text-left transition-all ${
                    selectedCard
                      ? "border-blue-300 bg-white shadow-[0_24px_60px_-28px_rgba(59,130,246,0.45)] dark:border-blue-500/50 dark:bg-slate-900"
                      : "border-slate-200 bg-white hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-[0_20px_50px_-32px_rgba(15,23,42,0.25)] dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700"
                  }`}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="space-y-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className={`rounded-full border px-2.5 py-1 text-xs ${getProviderTone(model.provider)}`}>
                          {model.provider}
                        </span>
                        {model.hot ? (
                          <span className="inline-flex items-center gap-1 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs text-amber-700">
                            <Star className="h-3.5 w-3.5" />
                            推荐
                          </span>
                        ) : null}
                        {model.tags.slice(0, 2).map((tag) => (
                          <span
                            key={tag}
                            className="rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>

                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-lg font-semibold tracking-tight text-slate-900 dark:text-slate-100">
                            {model.name}
                          </h3>
                          <ArrowUpRight
                            className={`h-4 w-4 transition-transform ${
                              selectedCard
                                ? "translate-x-0 text-blue-500"
                                : "text-slate-300 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-slate-500"
                            }`}
                          />
                        </div>
                        <p className="mt-1 line-clamp-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
                          {model.desc}
                        </p>
                      </div>
                    </div>

                    <div
                      className={`rounded-2xl border px-3 py-2 text-xs ${
                        selectedCard
                          ? "border-blue-100 bg-blue-50 text-blue-700 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300"
                          : "border-slate-200 bg-slate-50 text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                      }`}
                    >
                      {selectedCard ? "查看中" : "查看详情"}
                    </div>
                  </div>

                  <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <div className="rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-950/60">
                      <div className="text-xs text-slate-400 dark:text-slate-500">输入价格</div>
                      <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
                        {formatPrice(model.inputPrice)}
                      </div>
                    </div>
                    <div className="rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-950/60">
                      <div className="text-xs text-slate-400 dark:text-slate-500">缓存读取</div>
                      <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
                        {formatPrice(model.cachedInputPrice)}
                      </div>
                    </div>
                    <div className="rounded-2xl bg-slate-50 px-4 py-3 dark:bg-slate-950/60">
                      <div className="text-xs text-slate-400 dark:text-slate-500">输出价格</div>
                      <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
                        {formatPrice(model.outputPrice)}
                      </div>
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2">
                    {model.category.map((item) => (
                      <span
                        key={item}
                        className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300"
                      >
                        {item}
                      </span>
                    ))}
                  </div>
                </button>
              );
            })}
          </section>

          <aside className="xl:block">
            <div className="xl:sticky xl:top-6">
              <div className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
                {selected ? (
                  <div className="max-h-[calc(100vh-8rem)] overflow-auto">
                    <div className="border-b border-slate-100 bg-[linear-gradient(135deg,rgba(59,130,246,0.12),transparent_70%)] px-5 py-5 dark:border-slate-800">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className={`inline-flex rounded-full border px-2.5 py-1 text-xs ${getProviderTone(selected.provider)}`}>
                            {selected.provider}
                          </div>
                          <h3 className="mt-3 text-xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
                            {selected.name}
                          </h3>
                          <p className="mt-2 text-sm leading-6 text-slate-500 dark:text-slate-400">
                            {selected.desc}
                          </p>
                        </div>
                        <ChevronRight className="mt-1 h-4 w-4 text-slate-300 dark:text-slate-600" />
                      </div>
                    </div>

                    <div className="space-y-5 px-5 py-5">
                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                        <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/50">
                          <div className="text-xs text-slate-400 dark:text-slate-500">输入单价</div>
                          <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
                            {formatPrice(selected.inputPrice)}
                          </div>
                        </div>
                        <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/50">
                          <div className="text-xs text-slate-400 dark:text-slate-500">缓存读取</div>
                          <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
                            {formatPrice(selected.cachedInputPrice)}
                          </div>
                        </div>
                        <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-800 dark:bg-slate-950/50">
                          <div className="text-xs text-slate-400 dark:text-slate-500">输出单价</div>
                          <div className="mt-1 text-sm font-medium text-slate-900 dark:text-slate-100">
                            {formatPrice(selected.outputPrice)}
                          </div>
                        </div>
                      </div>

                      <div className="space-y-3">
                        <div className="flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                          <Layers3 className="h-4 w-4 text-blue-500" />
                          模型能力
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {selected.category.map((item) => (
                            <span
                              key={item}
                              className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            >
                              {item}
                            </span>
                          ))}
                        </div>
                      </div>

                      <div className="space-y-3">
                        <div className="flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                          <Code2 className="h-4 w-4 text-blue-500" />
                          接入建议
                        </div>
                        <div className="rounded-2xl border border-dashed border-slate-300 px-4 py-3 text-sm leading-6 text-slate-500 dark:border-slate-700 dark:text-slate-400">
                          适合用于 {selected.category.join(" / ")} 场景。优先根据输入规模选择成本档位，再结合套餐额度规划生产调用。
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="flex min-h-[24rem] items-center justify-center px-6 text-center text-sm text-slate-500 dark:text-slate-400">
                    暂无可查看详情的模型
                  </div>
                )}
              </div>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
