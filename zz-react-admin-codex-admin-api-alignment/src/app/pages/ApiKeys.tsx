import { useMemo, useState } from "react";
import { CheckCircle, Copy, Key, Plus, Users } from "lucide-react";
import { toast } from "sonner";
import {
  useApiKeysQuery,
  useCreateApiKeyMutation,
  useUpdateApiKeyStatusMutation,
} from "../api/queries";
import {
  formatApiKeyDate,
  formatQuota,
  getApiKeyStatusTone,
  getNextApiKeyStatus,
  normalizeApiKeyExpiresAt,
} from "../api/api-key-utils";
import { isAppError } from "../../lib/http/error";
import {
  ButtonLoadingContent,
  PageCardGridSkeleton,
  PageErrorState,
  PageHeaderSkeleton,
  PageTableSkeleton,
} from "../components/ui/feedback";

export function ApiKeysPage() {
  const { data, isLoading, isError, refetch, error } = useApiKeysQuery();
  const createMutation = useCreateApiKeyMutation();
  const updateStatusMutation = useUpdateApiKeyStatusMutation();

  const [copied, setCopied] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [createdValue, setCreatedValue] = useState<string | null>(null);
  const [form, setForm] = useState({
    userId: "",
    name: "",
    expiresAt: "",
    remark: "",
  });

  const keys = useMemo(() => data ?? [], [data]);

  const stats = useMemo(
    () => ({
      total: keys.length,
      active: keys.filter((item) => getApiKeyStatusTone(item.status).label === "正常").length,
      users: new Set(keys.map((item) => item.userId)).size,
    }),
    [keys],
  );

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text).catch(() => {});
    setCopied(id);
    setTimeout(() => setCopied(null), 1500);
  };

  const handleCreate = async () => {
    if (!form.userId || !form.name) {
      toast.error("请输入用户 ID 和 Key 名称");
      return;
    }

    try {
      const created = await createMutation.mutateAsync({
        userId: form.userId,
        name: form.name,
        expiresAt: normalizeApiKeyExpiresAt(form.expiresAt),
        remark: form.remark || undefined,
      });

      setCreatedValue(created.plainTextKey);
      setForm({
        userId: "",
        name: "",
        expiresAt: "",
        remark: "",
      });
      setShowCreate(false);
      toast.success("API Key 创建成功");
    } catch (err) {
      if (isAppError(err)) {
        toast.error(err.message);
      } else {
        toast.error("创建失败，请稍后重试");
      }
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6 p-6">
        <PageHeaderSkeleton />
        <PageCardGridSkeleton cards={3} />
        <PageTableSkeleton columns={7} rows={6} />
      </div>
    );
  }

  if (isError) {
    const message = isAppError(error) ? error.message : "加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
      <div className="space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">API Keys 管理</h1>
          <p className="text-slate-500 text-sm mt-0.5 dark:text-slate-400">
            管理员视角展示全部用户的 API Key，并支持创建和状态管理，删除接口当前未开放
          </p>
        </div>
        <button
          onClick={() => setShowCreate((value) => !value)}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          创建 Key
        </button>
      </div>

      {createdValue && (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-500/20 dark:bg-emerald-500/10">
          <div className="flex items-center gap-2 text-sm text-emerald-700 dark:text-emerald-300">
            <CheckCircle className="h-4 w-4" />新 Key 已创建，完整密钥只返回一次。
          </div>
          <div className="mt-3 flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-3">
            <code className="flex-1 break-all text-xs text-emerald-400">{createdValue}</code>
            <button
              onClick={() => handleCopy(createdValue, "admin-created")}
              className="text-slate-400 hover:text-white"
            >
              {copied === "admin-created" ? (
                <CheckCircle className="h-4 w-4 text-emerald-400" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
            </button>
          </div>
        </div>
      )}

      {showCreate && (
        <div className="grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900 md:grid-cols-2 xl:grid-cols-4">
          <input
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
            placeholder="用户 ID"
            value={form.userId}
            onChange={(e) => setForm((prev) => ({ ...prev, userId: e.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
            placeholder="Key 名称"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
            type="datetime-local"
            value={form.expiresAt}
            onChange={(e) => setForm((prev) => ({ ...prev, expiresAt: e.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
            placeholder="备注（可选）"
            value={form.remark}
            onChange={(e) => setForm((prev) => ({ ...prev, remark: e.target.value }))}
          />
          <div className="col-span-4 flex justify-end gap-2">
            <button
              onClick={() => setShowCreate(false)}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={() => void handleCreate()}
              disabled={createMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent loading={createMutation.isPending} loadingText="创建中...">
                创建
              </ButtonLoadingContent>
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">全部 Key</div>
          <div className="mt-1 text-2xl font-semibold text-slate-900 dark:text-slate-100">{stats.total}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">正常使用</div>
          <div className="mt-1 text-2xl font-semibold text-emerald-600">{stats.active}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">涉及用户</div>
          <div className="mt-1 flex items-center gap-2 text-2xl font-semibold text-blue-600">
            <Users className="h-5 w-5" />
            {stats.users}
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
        <table className="min-w-[920px] w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-950">
            <tr>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-500">用户</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">Key 信息</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">模型组</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">额度</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">时间</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">状态</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {keys.map((key) => {
              const tone = getApiKeyStatusTone(key.status);
              return (
                <tr key={key.id}>
                  <td className="px-5 py-4">
                    <div className="text-sm font-medium text-slate-800 dark:text-slate-100">{key.username}</div>
                    <div className="text-xs text-slate-400 dark:text-slate-500">userId: {key.userId}</div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-2">
                      <Key className="h-4 w-4 text-slate-400 dark:text-slate-500" />
                      <div className="text-sm font-medium text-slate-800 dark:text-slate-100">{key.name}</div>
                    </div>
                    <div className="mt-1 flex items-center gap-2">
                      <code className="text-xs text-slate-500 dark:text-slate-400">{key.accessKey}</code>
                      <button
                        onClick={() => handleCopy(key.accessKey, key.id)}
                        className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-200"
                      >
                        {copied === key.id ? (
                          <CheckCircle className="h-4 w-4 text-emerald-500" />
                        ) : (
                          <Copy className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  </td>
                  <td className="px-4 py-4 text-sm text-slate-700 dark:text-slate-300">
                    {key.modelGroupName ?? "未分组"}
                  </td>
                  <td className="px-4 py-4 text-xs text-slate-500 dark:text-slate-400">
                    <div>已用：{formatQuota(key.usedQuota)}</div>
                    <div className="mt-1">总额：{formatQuota(key.totalQuota)}</div>
                  </td>
                  <td className="px-4 py-4 text-xs text-slate-500">
                    <div>创建：{formatApiKeyDate(key.createdAt)}</div>
                    <div className="mt-1">最后使用：{formatApiKeyDate(key.lastUsedAt)}</div>
                    <div className="mt-1">
                      过期：{key.expiresAt ? formatApiKeyDate(key.expiresAt) : "永不过期"}
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <span className={`rounded-full border px-2 py-1 text-xs ${tone.className}`}>
                      {tone.label}
                    </span>
                  </td>
                  <td className="px-4 py-4">
                    <button
                      onClick={async () => {
                        try {
                          const nextStatus = getNextApiKeyStatus(key.status);
                          await updateStatusMutation.mutateAsync({
                            id: key.id,
                            input: { status: nextStatus },
                          });
                          toast.success(
                            nextStatus === "ACTIVE" ? "API Key 已启用" : "API Key 已禁用",
                          );
                        } catch (err) {
                          if (isAppError(err)) {
                            toast.error(err.message);
                          } else {
                            toast.error("状态更新失败，请稍后重试");
                          }
                        }
                      }}
                      disabled={updateStatusMutation.isPending}
                      className="rounded-lg border px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                    >
                      {getNextApiKeyStatus(key.status) === "ACTIVE" ? "启用" : "禁用"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        </div>
      </div>
    </div>
  );
}
