import { useMemo, useState } from "react";
import {
  ArrowUpDown,
  CheckCircle,
  DownloadCloud,
  Network,
  Package,
  PencilLine,
  Plus,
  Power,
  X,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCreateModelMutation,
  useCreateProviderMutation,
  useFetchUpstreamModelsMutation,
  useImportModelsMutation,
  useModelAccessSummaryQuery,
  useModelsQuery,
  useProvidersQuery,
  useUpdateModelMutation,
  useUpdateProviderStatusMutation,
} from "../api/queries";
import type {
  ModelBatchImportRequest,
  ModelCreateRequest,
  ModelGroupOptionResponse,
  ModelListItemResponse,
  ModelUpdateRequest,
  ProviderCreateRequest,
  UpstreamModelOptionResponse,
} from "../api/types";
import { isAppError } from "../../lib/http/error";
import {
  ButtonLoadingContent,
  PageCardGridSkeleton,
  PageErrorState,
  PageHeaderSkeleton,
  PageTableSkeleton,
} from "../components/ui/feedback";

type ActiveTab = "channels" | "catalog";

type CreateFormState = {
  providerCode: string;
  providerName: string;
  baseUrl: string;
  providerType: string;
  priorityNo: string;
  timeoutMs: string;
  tokenName: string;
  tokenValue: string;
  remark: string;
};

type CreateModelFormState = {
  modelCode: string;
  modelName: string;
  modelType: string;
  billingType: string;
  promptPrice: string;
  cachedPromptPrice: string;
  completionPrice: string;
  requestPrice: string;
  imagePrice: string;
  multiplier: string;
  providerId: string;
  groupId: string;
  upstreamModel: string;
  isPublic: boolean;
};

type ImportModelFormState = {
  providerId: string;
  groupId: string;
  isPublic: boolean;
  selectedUpstreamModels: string[];
};

const defaultCreateForm: CreateFormState = {
  providerCode: "",
  providerName: "",
  baseUrl: "",
  providerType: "",
  priorityNo: "1",
  timeoutMs: "30000",
  tokenName: "",
  tokenValue: "",
  remark: "",
};

const defaultCreateModelForm: CreateModelFormState = {
  modelCode: "",
  modelName: "",
  modelType: "chat",
  billingType: "TOKEN",
  promptPrice: "",
  cachedPromptPrice: "",
  completionPrice: "",
  requestPrice: "",
  imagePrice: "",
  multiplier: "1",
  providerId: "",
  groupId: "",
  upstreamModel: "",
  isPublic: true,
};

const defaultImportModelForm: ImportModelFormState = {
  providerId: "",
  groupId: "",
  isPublic: true,
  selectedUpstreamModels: [],
};

function createModelEditForm(model: ModelListItemResponse): CreateModelFormState {
  return {
    modelCode: model.modelCode,
    modelName: model.modelName,
    modelType: model.modelType ?? "chat",
    billingType: model.billingType ?? "TOKEN",
    promptPrice: model.promptPrice != null ? String(model.promptPrice) : "",
    cachedPromptPrice: model.cachedPromptPrice != null ? String(model.cachedPromptPrice) : "",
    completionPrice: model.completionPrice != null ? String(model.completionPrice) : "",
    requestPrice: model.requestPrice != null ? String(model.requestPrice) : "",
    imagePrice: "",
    multiplier: model.multiplier != null ? String(model.multiplier) : "1",
    providerId: model.providerId ?? "",
    groupId: model.groupId ?? "",
    upstreamModel: model.upstreamModel ?? "",
    isPublic: model.isPublic === true || model.isPublic === 1,
  };
}

function normalizeProviderStatus(status: string) {
  const normalized = status.trim().toUpperCase();

  if (normalized === "ACTIVE" || normalized === "ENABLED" || normalized === "NORMAL") {
    return {
      label: "运行中",
      className: "border-emerald-200 bg-emerald-50 text-emerald-600",
      nextStatus: "DISABLED",
      nextLabel: "停用",
      icon: CheckCircle,
    };
  }

  return {
    label: "已停用",
    className: "border-slate-200 bg-slate-100 text-slate-600",
    nextStatus: "ACTIVE",
    nextLabel: "启用",
    icon: XCircle,
  };
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("zh-CN", { hour12: false });
}

function buildCreatePayload(form: CreateFormState): ProviderCreateRequest {
  return {
    providerCode: form.providerCode.trim(),
    providerName: form.providerName.trim(),
    baseUrl: form.baseUrl.trim(),
    providerType: form.providerType.trim() || undefined,
    priorityNo: form.priorityNo ? Number(form.priorityNo) : undefined,
    timeoutMs: form.timeoutMs ? Number(form.timeoutMs) : undefined,
    tokenName: form.tokenName.trim() || undefined,
    tokenValue: form.tokenValue.trim() || undefined,
    remark: form.remark.trim() || undefined,
  };
}

function toNumberOrUndefined(value: string) {
  const normalized = value.trim();
  if (!normalized) {
    return undefined;
  }

  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function buildCreateModelPayload(form: CreateModelFormState): ModelCreateRequest {
  return {
    modelCode: form.modelCode.trim(),
    modelName: form.modelName.trim(),
    modelType: form.modelType.trim() || undefined,
    billingType: form.billingType.trim() || undefined,
    promptPrice: toNumberOrUndefined(form.promptPrice),
    cachedPromptPrice: toNumberOrUndefined(form.cachedPromptPrice),
    completionPrice: toNumberOrUndefined(form.completionPrice),
    requestPrice: toNumberOrUndefined(form.requestPrice),
    imagePrice: toNumberOrUndefined(form.imagePrice),
    multiplier: toNumberOrUndefined(form.multiplier),
    isPublic: form.isPublic,
    providerId: form.providerId,
    groupId: form.groupId,
    upstreamModel: form.upstreamModel.trim(),
  };
}

function buildUpdateModelPayload(
  model: ModelListItemResponse,
  form: CreateModelFormState,
): ModelUpdateRequest {
  return {
    bindingId: model.bindingId ?? undefined,
    modelName: form.modelName.trim(),
    modelType: form.modelType.trim() || undefined,
    billingType: form.billingType.trim() || undefined,
    promptPrice: toNumberOrUndefined(form.promptPrice),
    cachedPromptPrice: toNumberOrUndefined(form.cachedPromptPrice),
    completionPrice: toNumberOrUndefined(form.completionPrice),
    requestPrice: toNumberOrUndefined(form.requestPrice),
    multiplier: toNumberOrUndefined(form.multiplier),
    isPublic: form.isPublic,
    providerId: form.providerId,
    groupId: form.groupId,
    upstreamModel: form.upstreamModel.trim(),
  };
}

function buildImportPayload(form: ImportModelFormState): ModelBatchImportRequest {
  return {
    providerId: form.providerId,
    groupId: form.groupId,
    upstreamModels: form.selectedUpstreamModels,
    isPublic: form.isPublic,
  };
}

function getProviderModelCount(providerId: string, models: ModelListItemResponse[]) {
  return models.filter((item) => item.providerId === providerId).length;
}

function getModelGroups(model: ModelListItemResponse) {
  const groups = (model.groups ?? [])
    .map((group) => ({
      groupName: group.groupName?.trim() || "—",
      groupCode: group.groupCode?.trim() || "未分组",
    }))
    .filter(
      (group, index, array) =>
        array.findIndex(
          (candidate) =>
            candidate.groupName === group.groupName && candidate.groupCode === group.groupCode,
        ) === index,
    );

  if (groups.length > 0) {
    return groups;
  }

  return [
    {
      groupName: model.groupName || "—",
      groupCode: model.groupCode || "未分组",
    },
  ];
}

type ModalProps = {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
};

function Modal({ title, onClose, children }: ModalProps) {
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/40 p-4">
      <div className="flex min-h-full items-start justify-center py-4 sm:items-center">
        <div className="flex max-h-[calc(100vh-2rem)] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl dark:bg-slate-900 dark:shadow-slate-950/60">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <h2 className="text-slate-900 dark:text-slate-100">{title}</h2>
          <button
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
          <div className="overflow-y-auto p-6">{children}</div>
        </div>
      </div>
    </div>
  );
}

export function ChannelsPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("channels");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showCreateModelModal, setShowCreateModelModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelListItemResponse | null>(null);
  const [createForm, setCreateForm] = useState<CreateFormState>(defaultCreateForm);
  const [createModelForm, setCreateModelForm] = useState<CreateModelFormState>(
    defaultCreateModelForm,
  );
  const [importModelForm, setImportModelForm] = useState<ImportModelFormState>(
    defaultImportModelForm,
  );
  const [upstreamOptions, setUpstreamOptions] = useState<UpstreamModelOptionResponse[]>([]);

  const {
    data: providers,
    isLoading: providersLoading,
    isError: providersError,
    refetch: refetchProviders,
    error: providersQueryError,
  } = useProvidersQuery();
  const {
    data: models,
    isLoading: modelsLoading,
    isError: modelsError,
    refetch: refetchModels,
    error: modelsQueryError,
  } = useModelsQuery();
  const { data: packageSummary } = useModelAccessSummaryQuery(true);
  const createProviderMutation = useCreateProviderMutation();
  const updateProviderStatusMutation = useUpdateProviderStatusMutation();
  const createModelMutation = useCreateModelMutation();
  const updateModelMutation = useUpdateModelMutation();
  const fetchUpstreamMutation = useFetchUpstreamModelsMutation();
  const importModelsMutation = useImportModelsMutation();

  const providerList = useMemo(() => providers ?? [], [providers]);
  const modelList = useMemo(
    () =>
      (models ?? []).map((model) => {
        const groups = getModelGroups(model);
        return {
          ...model,
          groupName: groups.map((group) => group.groupName).join(" / "),
          groupCode: groups.map((group) => group.groupCode).join(" / "),
        };
      }),
    [models],
  );
  const groupList = useMemo(() => packageSummary?.groups ?? [], [packageSummary?.groups]);

  const activeProviders = useMemo(
    () =>
      providerList.filter((item) => {
        const status = item.status.trim().toUpperCase();
        return status === "ACTIVE" || status === "ENABLED" || status === "NORMAL";
      }).length,
    [providerList],
  );

  const providerModelSummary = useMemo(() => {
    const groups = new Map<
      string,
      {
        providerId: string;
        providerName: string;
        providerType: string;
        total: number;
        publicCount: number;
        activeCount: number;
      }
    >();

    for (const item of modelList) {
      const key = item.providerId ?? item.providerName ?? "unknown";
      const current = groups.get(key) ?? {
        providerId: item.providerId ?? key,
        providerName: item.providerName ?? item.providerType ?? "未归类渠道",
        providerType: item.providerType ?? "未知类型",
        total: 0,
        publicCount: 0,
        activeCount: 0,
      };

      current.total += 1;
      if (item.isPublic === true || item.isPublic === 1) {
        current.publicCount += 1;
      }
      if (["ACTIVE", "ENABLED", "NORMAL"].includes(item.status.trim().toUpperCase())) {
        current.activeCount += 1;
      }

      groups.set(key, current);
    }

    return [...groups.values()].sort((left, right) => right.total - left.total);
  }, [modelList]);

  const selectedCreateProvider = useMemo(
    () => providerList.find((item) => item.id === createModelForm.providerId) ?? null,
    [createModelForm.providerId, providerList],
  );

  const selectedImportProvider = useMemo(
    () => providerList.find((item) => item.id === importModelForm.providerId) ?? null,
    [importModelForm.providerId, providerList],
  );

  const selectedEditProvider = useMemo(
    () => providerList.find((item) => item.id === createModelForm.providerId) ?? null,
    [createModelForm.providerId, providerList],
  );

  if (providersLoading || modelsLoading) {
    return (
      <div className="space-y-6 p-6">
        <PageHeaderSkeleton />
        <PageCardGridSkeleton />
        <PageTableSkeleton columns={6} rows={6} />
      </div>
    );
  }

  if (providersError || modelsError) {
    const message = isAppError(providersQueryError)
      ? providersQueryError.message
      : isAppError(modelsQueryError)
        ? modelsQueryError.message
        : "加载失败";

    return (
      <PageErrorState
        message={message}
        onRetry={() => {
          void refetchProviders();
          void refetchModels();
        }}
      />
    );
  }

  return (
      <div className="space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">渠道 / 模型商店</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            使用真实渠道接口管理上游提供商，并支持模型新增与上游模型拉取导入
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          {activeTab === "catalog" ? (
            <>
              <button
                onClick={() => setShowImportModal(true)}
                className="flex items-center justify-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-4 py-2 text-sm text-blue-700 transition-colors hover:bg-blue-100 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300 dark:hover:bg-blue-500/20"
              >
                <DownloadCloud className="h-4 w-4" />
                拉取上游模型
              </button>
              <button
                onClick={() => setShowCreateModelModal(true)}
                className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
              >
                <Plus className="h-4 w-4" />
                添加模型
              </button>
            </>
          ) : (
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              添加渠道
            </button>
          )}
        </div>
      </div>

      <div className="flex w-fit gap-1 rounded-lg bg-slate-100 p-0.5 dark:bg-slate-800">
        {[
          { id: "channels" as const, label: "渠道列表" },
          { id: "catalog" as const, label: "模型商店" },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`rounded-md px-4 py-1.5 text-sm transition-all ${
              activeTab === tab.id
                ? "bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-slate-100"
                : "text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-100"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">渠道总数</div>
          <div className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">{providerList.length}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">运行中</div>
          <div className="mt-2 text-2xl font-semibold text-emerald-600">{activeProviders}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">模型总数</div>
          <div className="mt-2 text-2xl font-semibold text-blue-600">{modelList.length}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">公开模型</div>
          <div className="mt-2 text-2xl font-semibold text-violet-600">
            {modelList.filter((item) => item.isPublic === true || item.isPublic === 1).length}
          </div>
        </div>
      </div>

      {activeTab === "channels" ? (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center gap-2 border-b border-slate-100 bg-slate-50 px-5 py-3 dark:border-slate-800 dark:bg-slate-950">
            <ArrowUpDown className="h-4 w-4 text-slate-400 dark:text-slate-500" />
            <span className="text-xs text-slate-500 dark:text-slate-400">按优先级与状态查看渠道配置</span>
          </div>

          <div className="overflow-x-auto">
          <table className="min-w-[820px] w-full text-sm">
            <thead className="border-b border-slate-100 bg-white dark:border-slate-800 dark:bg-slate-900">
              <tr className="text-left text-xs text-slate-500 dark:text-slate-400">
                <th className="px-5 py-3 font-medium">渠道</th>
                <th className="px-4 py-3 font-medium">接口地址</th>
                <th className="px-4 py-3 font-medium">优先级 / 超时</th>
                <th className="px-4 py-3 font-medium">Token / 模型</th>
                <th className="px-4 py-3 font-medium">状态</th>
                <th className="px-4 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {providerList.map((provider) => {
                const tone = normalizeProviderStatus(provider.status);
                const StatusIcon = tone.icon;

                return (
                  <tr key={provider.id}>
                    <td className="px-5 py-4">
                      <div className="text-sm font-medium text-slate-800 dark:text-slate-100">
                        {provider.providerName}
                      </div>
                      <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                        {provider.providerCode} · {provider.providerType || "未分类"}
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <div className="max-w-xs truncate font-mono text-xs text-slate-600 dark:text-slate-300">
                        {provider.baseUrl}
                      </div>
                      <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                        创建于 {formatDateTime(provider.createdAt)}
                      </div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600">
                      <div>优先级：{provider.priorityNo ?? "—"}</div>
                      <div className="mt-1">超时：{provider.timeoutMs ?? "—"} ms</div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600">
                      <div>Token 数：{provider.tokenCount ?? 0}</div>
                      <div className="mt-1">
                        模型数：{getProviderModelCount(provider.id, modelList)}
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs ${tone.className}`}
                      >
                        <StatusIcon className="h-3 w-3" />
                        {tone.label}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <button
                        onClick={async () => {
                          try {
                            await updateProviderStatusMutation.mutateAsync({
                              providerId: provider.id,
                              input: { status: tone.nextStatus },
                            });
                            toast.success(`渠道已${tone.nextLabel === "停用" ? "停用" : "启用"}`);
                          } catch (statusError) {
                            if (isAppError(statusError)) {
                              toast.error(statusError.message);
                            } else {
                              toast.error("状态更新失败，请稍后重试");
                            }
                          }
                        }}
                        disabled={updateProviderStatusMutation.isPending}
                      className="inline-flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        <Power className="h-3.5 w-3.5" />
                        {tone.nextLabel}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          </div>
        </div>
      ) : null}

      {activeTab === "catalog" ? (
        <div className="space-y-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  模型商店操作台
                </div>
                <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  支持单个新增模型，或按渠道先拉取上游模型列表，再批量导入到指定套餐分组。
                </div>
              </div>
              <div className="flex flex-wrap gap-2 text-xs text-slate-500 dark:text-slate-400">
                <span className="rounded-full border border-slate-200 px-2.5 py-1 dark:border-slate-700">
                  可用渠道 {providerList.length}
                </span>
                <span className="rounded-full border border-slate-200 px-2.5 py-1 dark:border-slate-700">
                  可选套餐 {groupList.length}
                </span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
            {providerModelSummary.map((item) => (
              <div
                key={item.providerId}
                className="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900"
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-300">
                    <Network className="h-4 w-4" />
                  </div>
                  <div>
                    <div className="text-sm font-medium text-slate-900 dark:text-slate-100">{item.providerName}</div>
                    <div className="text-xs text-slate-400 dark:text-slate-500">{item.providerType}</div>
                  </div>
                </div>
                <div className="mt-4 grid grid-cols-3 gap-3 text-sm">
                  <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                    <div className="text-xs text-slate-500 dark:text-slate-400">全部</div>
                    <div className="mt-1 font-medium text-slate-800 dark:text-slate-100">{item.total}</div>
                  </div>
                  <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                    <div className="text-xs text-slate-500 dark:text-slate-400">公开</div>
                    <div className="mt-1 font-medium text-slate-800 dark:text-slate-100">{item.publicCount}</div>
                  </div>
                  <div className="rounded-xl bg-slate-50 px-3 py-2 dark:bg-slate-800">
                    <div className="text-xs text-slate-500 dark:text-slate-400">启用</div>
                    <div className="mt-1 font-medium text-slate-800 dark:text-slate-100">{item.activeCount}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
            <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full text-sm">
              <thead className="border-b border-slate-100 bg-slate-50 dark:border-slate-800 dark:bg-slate-950">
                <tr className="text-left text-xs text-slate-500 dark:text-slate-400">
                  <th className="px-5 py-3 font-medium">模型</th>
                  <th className="px-4 py-3 font-medium">渠道</th>
                  <th className="px-4 py-3 font-medium">套餐组</th>
                  <th className="px-4 py-3 font-medium">上游模型</th>
                  <th className="px-4 py-3 font-medium">计费</th>
                  <th className="px-4 py-3 font-medium">状态</th>
                  <th className="px-4 py-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {modelList.map((model) => (
                  <tr key={model.id}>
                    <td className="px-5 py-4">
                      <div className="text-sm font-medium text-slate-800 dark:text-slate-100">
                        {model.modelName || model.modelCode}
                      </div>
                      <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{model.modelCode}</div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600">
                      <div>{model.providerName || "—"}</div>
                      <div className="mt-1">{model.providerType || "—"}</div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                      <div>{model.groupName || "—"}</div>
                      <div className="mt-1 text-slate-400 dark:text-slate-500">
                        {model.groupCode || "未分组"}
                      </div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600">
                      {model.upstreamModel || "—"}
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600">
                      <div>输入：{model.promptPrice ?? model.requestPrice ?? 0}</div>
                      <div className="mt-1">缓存：{model.cachedPromptPrice ?? 0}</div>
                      <div className="mt-1">
                        输出：{model.completionPrice ?? model.requestPrice ?? 0}
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2 py-1 text-xs text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                        <Package className="h-3 w-3" />
                        {model.status}
                        {model.isPublic === true || model.isPublic === 1 ? " · 公开" : " · 私有"}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <button
                        onClick={() => {
                          setEditingModel(model);
                          setCreateModelForm(createModelEditForm(model));
                        }}
                        className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        <PencilLine className="h-3.5 w-3.5" />
                        修改
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          </div>
        </div>
      ) : null}

      {showCreateModal ? (
        <Modal
          title="添加渠道"
          onClose={() => {
            setShowCreateModal(false);
            setCreateForm(defaultCreateForm);
          }}
        >
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {false && [
              { key: "providerCode", label: "渠道编码", placeholder: "openai-main" },
              { key: "providerName", label: "渠道名称", placeholder: "OpenAI 主线" },
              { key: "baseUrl", label: "接口地址", placeholder: "https://api.openai.com/v1" },
              { key: "providerType", label: "渠道类型", placeholder: "OpenAI / Anthropic" },
              { key: "priorityNo", label: "优先级", placeholder: "1" },
              { key: "timeoutMs", label: "超时(ms)", placeholder: "30000" },
              { key: "tokenName", label: "默认 Token 名称", placeholder: "主线路由 Token" },
              { key: "tokenValue", label: "默认 Token 值", placeholder: "sk-..." },
            ].map((field) => (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">{field.label}</label>
                <input
                  value={createForm[field.key as keyof CreateFormState]}
                  onChange={(event) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      [field.key]: event.target.value,
                    }))
                  }
                  placeholder={field.placeholder}
                  className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                />
              </div>
            ))}
          </div>

          <div className="mt-3">
            <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">备注</label>
            <textarea
              rows={3}
              value={createForm.remark}
              onChange={(event) =>
                setCreateForm((prev) => ({
                  ...prev,
                  remark: event.target.value,
                }))
              }
              className="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="可选，记录该渠道的用途或说明"
            />
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setShowCreateModal(false);
                setCreateForm(defaultCreateForm);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
               取消
            </button>
            <button
              onClick={async () => {
                if (
                  !createForm.providerCode.trim() ||
                  !createForm.providerName.trim() ||
                  !createForm.baseUrl.trim()
                ) {
                  toast.error("请先填写渠道编码、渠道名称和接口地址");
                  return;
                }

                try {
                  await createProviderMutation.mutateAsync(buildCreatePayload(createForm));
                  toast.success("渠道创建成功");
                  setShowCreateModal(false);
                  setCreateForm(defaultCreateForm);
                } catch (createError) {
                  if (isAppError(createError)) {
                    toast.error(createError.message);
                  } else {
                    toast.error("渠道创建失败，请稍后重试");
                  }
                }
              }}
              disabled={createProviderMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent
                loading={createProviderMutation.isPending}
                loadingText="保存中..."
              >
                保存渠道
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      ) : null}

      {showCreateModelModal ? (
        <Modal
          title="添加模型"
          onClose={() => {
            setShowCreateModelModal(false);
            setCreateModelForm(defaultCreateModelForm);
          }}
        >
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                所属渠道
              </label>
              <select
                value={createModelForm.providerId}
                onChange={(event) =>
                  setCreateModelForm((prev) => ({ ...prev, providerId: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">请选择渠道</option>
                {providerList.map((provider) => (
                  <option key={provider.id} value={provider.id}>
                    {provider.providerName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                所属套餐组
              </label>
              <select
                value={createModelForm.groupId}
                onChange={(event) =>
                  setCreateModelForm((prev) => ({ ...prev, groupId: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">请选择套餐组</option>
                {groupList.map((group) => (
                  <option key={group.id} value={group.id}>
                    {group.groupName}
                  </option>
                ))}
              </select>
            </div>
            {false && [
              { key: "modelCode", label: "模型编码", placeholder: "gpt-4.1-mini" },
              { key: "modelName", label: "模型名称", placeholder: "GPT-4.1 mini" },
              { key: "upstreamModel", label: "上游模型", placeholder: "gpt-4.1-mini" },
              { key: "modelType", label: "模型类型", placeholder: "chat / image / audio" },
              { key: "billingType", label: "计费类型", placeholder: "TOKEN" },
              { key: "promptPrice", label: "输入价格", placeholder: "0.8" },
              { key: "cachedPromptPrice", label: "缓存价格", placeholder: "0.2" },
              { key: "completionPrice", label: "输出价格", placeholder: "3.2" },
              { key: "requestPrice", label: "按次价格", placeholder: "0" },
              { key: "imagePrice", label: "图片价格", placeholder: "0" },
              { key: "multiplier", label: "倍率", placeholder: "1" },
            ].map((field) => (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                  {field.label}
                </label>
                <input
                  value={createModelForm[field.key as keyof CreateModelFormState] as string}
                  onChange={(event) =>
                    setCreateModelForm((prev) => ({
                      ...prev,
                      [field.key]: event.target.value,
                    }))
                  }
                  placeholder={field.placeholder}
                  className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                />
              </div>
            ))}
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950">
            <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
              <input
                type="checkbox"
                checked={createModelForm.isPublic}
                onChange={(event) =>
                  setCreateModelForm((prev) => ({ ...prev, isPublic: event.target.checked }))
                }
                className="h-4 w-4 rounded border-slate-300 text-blue-600"
              />
              公开模型
            </label>
            {selectedCreateProvider ? (
              <div className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                当前渠道：{selectedCreateProvider.providerName} · {selectedCreateProvider.providerType || "未分类"}
              </div>
            ) : null}
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setShowCreateModelModal(false);
                setCreateModelForm(defaultCreateModelForm);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                if (
                  !createModelForm.providerId ||
                  !createModelForm.groupId ||
                  !createModelForm.modelCode.trim() ||
                  !createModelForm.modelName.trim() ||
                  !createModelForm.upstreamModel.trim()
                ) {
                  toast.error("请先填写渠道、套餐组、模型编码、模型名称和上游模型");
                  return;
                }

                try {
                  await createModelMutation.mutateAsync(buildCreateModelPayload(createModelForm));
                  toast.success("模型创建成功");
                  setShowCreateModelModal(false);
                  setCreateModelForm(defaultCreateModelForm);
                } catch (createError) {
                  if (isAppError(createError)) {
                    toast.error(createError.message);
                  } else {
                    toast.error("模型创建失败，请稍后重试");
                  }
                }
              }}
              disabled={createModelMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent
                loading={createModelMutation.isPending}
                loadingText="保存中..."
              >
                保存模型
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      ) : null}

      {editingModel ? (
        <Modal
          title={`修改模型 · ${editingModel.modelCode}`}
          onClose={() => {
            setEditingModel(null);
            setCreateModelForm(defaultCreateModelForm);
          }}
        >
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                模型编码
              </label>
              <input
                value={editingModel.modelCode}
                readOnly
                className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                缁戝畾 ID
              </label>
              <input
                value={editingModel.bindingId ?? "—"}
                readOnly
                className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                所属渠道
              </label>
              <select
                value={createModelForm.providerId}
                onChange={(event) =>
                  setCreateModelForm((prev) => ({ ...prev, providerId: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">请选择渠道</option>
                {providerList.map((provider) => (
                  <option key={provider.id} value={provider.id}>
                    {provider.providerName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                所属套餐组
              </label>
              <select
                value={createModelForm.groupId}
                onChange={(event) =>
                  setCreateModelForm((prev) => ({ ...prev, groupId: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">请选择套餐组</option>
                {groupList.map((group) => (
                  <option key={group.id} value={group.id}>
                    {group.groupName}
                  </option>
                ))}
              </select>
            </div>
            {[
              { key: "modelName", label: "模型名称", placeholder: "GPT-4.1 mini" },
              { key: "upstreamModel", label: "上游模型", placeholder: "gpt-4.1-mini" },
              { key: "modelType", label: "模型类型", placeholder: "chat / image / audio" },
              { key: "billingType", label: "计费类型", placeholder: "TOKEN" },
              { key: "promptPrice", label: "输入价格", placeholder: "0.8" },
              { key: "cachedPromptPrice", label: "缓存价格", placeholder: "0.2" },
              { key: "completionPrice", label: "输出价格", placeholder: "3.2" },
              { key: "requestPrice", label: "按次价格", placeholder: "0" },
              { key: "multiplier", label: "倍率", placeholder: "1" },
            ].map((field) => (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                  {field.label}
                </label>
                <input
                  value={createModelForm[field.key as keyof CreateModelFormState] as string}
                  onChange={(event) =>
                    setCreateModelForm((prev) => ({
                      ...prev,
                      [field.key]: event.target.value,
                    }))
                  }
                  placeholder={field.placeholder}
                  className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                />
              </div>
            ))}
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950">
            <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
              <input
                type="checkbox"
                checked={createModelForm.isPublic}
                onChange={(event) =>
                  setCreateModelForm((prev) => ({ ...prev, isPublic: event.target.checked }))
                }
                className="h-4 w-4 rounded border-slate-300 text-blue-600"
              />
              公开模型
            </label>
            {selectedEditProvider ? (
              <div className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                当前渠道：{selectedEditProvider.providerName} ·{" "}
                {selectedEditProvider.providerType || "未分类"}
              </div>
            ) : null}
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setEditingModel(null);
                setCreateModelForm(defaultCreateModelForm);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                if (
                  !createModelForm.providerId ||
                  !createModelForm.groupId ||
                  !createModelForm.modelName.trim() ||
                  !createModelForm.upstreamModel.trim()
                ) {
                  toast.error("请先填写渠道、套餐组、模型名称和上游模型");
                  return;
                }

                try {
                  await updateModelMutation.mutateAsync({
                    id: editingModel.id,
                    input: buildUpdateModelPayload(editingModel, createModelForm),
                  });
                  toast.success("模型更新成功");
                  setEditingModel(null);
                  setCreateModelForm(defaultCreateModelForm);
                } catch (updateError) {
                  if (isAppError(updateError)) {
                    toast.error(updateError.message);
                  } else {
                    toast.error("模型更新失败，请稍后重试");
                  }
                }
              }}
              disabled={updateModelMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent
                loading={updateModelMutation.isPending}
                loadingText="保存中..."
              >
                保存修改
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      ) : null}

      {showImportModal ? (
        <Modal
          title="拉取并导入上游模型"
          onClose={() => {
            setShowImportModal(false);
            setImportModelForm(defaultImportModelForm);
            setUpstreamOptions([]);
          }}
        >
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                渠道
              </label>
              <select
                value={importModelForm.providerId}
                onChange={(event) => {
                  setUpstreamOptions([]);
                  setImportModelForm((prev) => ({
                    ...prev,
                    providerId: event.target.value,
                    selectedUpstreamModels: [],
                  }));
                }}
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">请选择渠道</option>
                {providerList.map((provider) => (
                  <option key={provider.id} value={provider.id}>
                    {provider.providerName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                导入到套餐组
              </label>
              <select
                value={importModelForm.groupId}
                onChange={(event) =>
                  setImportModelForm((prev) => ({ ...prev, groupId: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="">请选择套餐组</option>
                {groupList.map((group) => (
                  <option key={group.id} value={group.id}>
                    {group.groupName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                <input
                  type="checkbox"
                  checked={importModelForm.isPublic}
                  onChange={(event) =>
                    setImportModelForm((prev) => ({ ...prev, isPublic: event.target.checked }))
                  }
                  className="h-4 w-4 rounded border-slate-300 text-blue-600"
                />
                导入后设为公开模型
              </label>
              <button
                onClick={async () => {
                  if (!importModelForm.providerId) {
                    toast.error("请先选择一个渠道");
                    return;
                  }

                  try {
                    const models = await fetchUpstreamMutation.mutateAsync(importModelForm.providerId);
                    setUpstreamOptions(models);
                    setImportModelForm((prev) => ({ ...prev, selectedUpstreamModels: [] }));
                    toast.success(`已拉取 ${models.length} 个上游模型`);
                  } catch (fetchError) {
                    if (isAppError(fetchError)) {
                      toast.error(fetchError.message);
                    } else {
                      toast.error("上游模型拉取失败，请稍后重试");
                    }
                  }
                }}
                disabled={fetchUpstreamMutation.isPending}
                className="inline-flex items-center justify-center gap-2 rounded-lg border border-blue-200 px-3 py-2 text-sm text-blue-700 hover:bg-blue-50 disabled:opacity-60 dark:border-blue-500/20 dark:text-blue-300 dark:hover:bg-blue-500/10"
              >
                <ButtonLoadingContent
                  loading={fetchUpstreamMutation.isPending}
                  loadingText="拉取中..."
                >
                  <>
                    <DownloadCloud className="h-4 w-4" />
                    拉取上游模型
                  </>
                </ButtonLoadingContent>
              </button>
            </div>

            <div className="mt-4">
              {upstreamOptions.length === 0 ? (
                <div className="rounded-xl border border-dashed border-slate-300 px-3 py-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
                  先选择渠道并拉取上游模型，拉取后可批量勾选导入。
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <span>
                      已拉取 {upstreamOptions.length} 个上游模型
                      {selectedImportProvider ? ` · ${selectedImportProvider.providerName}` : ""}
                    </span>
                    <button
                      onClick={() =>
                        setImportModelForm((prev) => ({
                          ...prev,
                          selectedUpstreamModels:
                            prev.selectedUpstreamModels.length === upstreamOptions.length
                              ? []
                              : upstreamOptions.map((item) => item.id),
                        }))
                      }
                      className="text-blue-600 hover:text-blue-700 dark:text-blue-300"
                    >
                      {importModelForm.selectedUpstreamModels.length === upstreamOptions.length
                        ? "取消全选"
                        : "全选"}
                    </button>
                  </div>
                  <div className="grid max-h-72 grid-cols-1 gap-2 overflow-y-auto md:grid-cols-2">
                    {upstreamOptions.map((option) => {
                      const checked = importModelForm.selectedUpstreamModels.includes(option.id);
                      return (
                        <label
                          key={option.id}
                          className={`flex cursor-pointer items-start gap-3 rounded-xl border px-3 py-3 transition-colors ${
                            checked
                              ? "border-blue-300 bg-blue-50 dark:border-blue-500/30 dark:bg-blue-500/10"
                              : "border-slate-200 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:hover:bg-slate-800"
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={(event) =>
                              setImportModelForm((prev) => ({
                                ...prev,
                                selectedUpstreamModels: event.target.checked
                                  ? [...prev.selectedUpstreamModels, option.id]
                                  : prev.selectedUpstreamModels.filter((item) => item !== option.id),
                              }))
                            }
                            className="mt-1 h-4 w-4 rounded border-slate-300 text-blue-600"
                          />
                          <div className="min-w-0">
                            <div className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">
                              {option.displayName || option.id}
                            </div>
                            <div className="mt-1 break-all text-xs text-slate-500 dark:text-slate-400">
                              {option.id}
                            </div>
                            <div className="mt-1 text-[11px] text-slate-400 dark:text-slate-500">
                              {option.ownedBy || "未知来源"} · {option.providerType || "未知类型"}
                            </div>
                          </div>
                        </label>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setShowImportModal(false);
                setImportModelForm(defaultImportModelForm);
                setUpstreamOptions([]);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                if (!importModelForm.providerId || !importModelForm.groupId) {
                  toast.error("请先选择渠道和套餐组");
                  return;
                }
                if (importModelForm.selectedUpstreamModels.length === 0) {
                  toast.error("请至少勾选一个上游模型");
                  return;
                }

                try {
                  const result = await importModelsMutation.mutateAsync(
                    buildImportPayload(importModelForm),
                  );
                  toast.success(
                    `导入完成：成功 ${result.importedCount} 个，跳过 ${result.skippedCount} 个`,
                  );
                  setShowImportModal(false);
                  setImportModelForm(defaultImportModelForm);
                  setUpstreamOptions([]);
                } catch (importError) {
                  if (isAppError(importError)) {
                    toast.error(importError.message);
                  } else {
                    toast.error("模型导入失败，请稍后重试");
                  }
                }
              }}
              disabled={importModelsMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent
                loading={importModelsMutation.isPending}
                loadingText="导入中..."
              >
                确认导入
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}
