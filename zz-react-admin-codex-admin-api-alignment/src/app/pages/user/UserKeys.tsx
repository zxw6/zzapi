import { useMemo, useState } from "react";
import {
  Copy,
  CheckCircle,
  Eye,
  EyeOff,
  Key,
  Package,
  Plus,
  ShieldAlert,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";
import {
  useApiKeysQuery,
  useCreateApiKeyMutation,
  useDeleteApiKeyMutation,
  useModelAccessSummaryQuery,
  useUpdateApiKeyStatusMutation,
} from "../../api/queries";
import {
  formatApiKeyDate,
  formatQuota,
  getApiKeyStatusTone,
  getNextApiKeyStatus,
  normalizeApiKeyExpiresAt,
} from "../../api/api-key-utils";
import { useAuth } from "../../auth/auth-context";
import { isAppError } from "../../../lib/http/error";
import {
  ButtonLoadingContent,
  PageCardGridSkeleton,
  PageEmptyState,
  PageErrorState,
  PageHeaderSkeleton,
  PageListSkeleton,
} from "../../components/ui/feedback";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../../components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";

export function UserKeysPage() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch, error } = useApiKeysQuery();
  const {
    data: packageSummary,
    isLoading: packageLoading,
    isError: packageError,
  } = useModelAccessSummaryQuery(Boolean(user));
  const createMutation = useCreateApiKeyMutation();
  const updateStatusMutation = useUpdateApiKeyStatusMutation();
  const deleteMutation = useDeleteApiKeyMutation();

  const [copied, setCopied] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [showPackagePrompt, setShowPackagePrompt] = useState(false);
  const [createdValue, setCreatedValue] = useState<string | null>(null);
  const [revealCreatedValue, setRevealCreatedValue] = useState(true);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [form, setForm] = useState({
    name: "",
    modelGroupId: "",
    expiresAt: "",
    remark: "",
  });

  const keys = useMemo(
    () => (data ?? []).filter((item) => String(item.userId) === String(user?.id ?? "")),
    [data, user?.id],
  );
  const availableGroups = useMemo(
    () => (packageSummary?.groups ?? []).filter((item) => item.purchased),
    [packageSummary?.groups],
  );
  const selectedGroup = useMemo(
    () => availableGroups.find((item) => item.id === form.modelGroupId) ?? null,
    [availableGroups, form.modelGroupId],
  );
  const selectedGroupIsBalancePackage = selectedGroup?.packageType === "BALANCE";

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text).catch(() => {});
    setCopied(id);
    setTimeout(() => setCopied(null), 1500);
  };

  const openCreateDialog = () => {
    if (packageLoading) {
      toast.error("套餐信息加载中，请稍后重试");
      return;
    }

    if (packageError) {
      toast.error("套餐信息加载失败，请稍后重试");
      return;
    }

    if (availableGroups.length === 0) {
      setShowPackagePrompt(true);
      return;
    }

    setShowCreate(true);
  };

  const handleCreate = async () => {
    if (!user?.id || !form.name) {
      toast.error("请输入 Key 名称");
      return;
    }

    if (!form.modelGroupId) {
      toast.error("请选择一个已购套餐");
      return;
    }

    try {
      const created = await createMutation.mutateAsync({
        userId: user.id,
        name: form.name,
        modelGroupId: form.modelGroupId,
        expiresAt: normalizeApiKeyExpiresAt(form.expiresAt),
        remark: form.remark || undefined,
      });

      setCreatedValue(created.plainTextKey);
      setRevealCreatedValue(true);
      setForm({ name: "", modelGroupId: "", expiresAt: "", remark: "" });
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
      <div className="mx-auto max-w-5xl space-y-6 p-4 sm:p-6">
        <PageHeaderSkeleton />
        <PageCardGridSkeleton cards={3} />
        <PageListSkeleton items={4} />
      </div>
    );
  }

  if (isError) {
    const message = isAppError(error) ? error.message : "加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 text-slate-900 sm:p-6 dark:text-slate-100">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">我的 API Keys</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            仅展示当前登录用户的 Key 列表与状态，支持启用、禁用和删除
          </p>
        </div>
        <button
          onClick={openCreateDialog}
          className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-3 py-2 text-sm text-white hover:bg-blue-700 sm:w-auto"
        >
          <Plus className="h-4 w-4" />
          新建 Key
        </button>
      </div>

      {createdValue && (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-500/30 dark:bg-emerald-500/10">
          <div className="flex items-center gap-2 text-sm text-emerald-700">
            <CheckCircle className="h-4 w-4" />新 Key 已创建，完整密钥只会返回一次，请立即保存。
          </div>
          <div className="mt-3 flex flex-col items-stretch gap-2 rounded-xl bg-slate-900 px-4 py-3 sm:flex-row sm:items-center">
            <code className="flex-1 text-xs text-emerald-400 break-all">
              {revealCreatedValue ? createdValue : "••••••••••••••••••••"}
            </code>
            <button
              onClick={() => setRevealCreatedValue((value) => !value)}
              className="text-slate-400 hover:text-white"
            >
              {revealCreatedValue ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
            <button
              onClick={() => handleCopy(createdValue, "created-key")}
              className="text-slate-400 hover:text-white"
            >
              {copied === "created-key" ? (
                <CheckCircle className="h-4 w-4 text-emerald-400" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
            </button>
          </div>
        </div>
      )}

      <Dialog
        open={showCreate}
        onOpenChange={(open) => {
          if (!open && createMutation.isPending) {
            return;
          }
          setShowCreate(open);
          if (!open && !createMutation.isPending) {
            setForm({ name: "", modelGroupId: "", expiresAt: "", remark: "" });
          }
        }}
      >
        <DialogContent className="max-w-2xl border-slate-200 bg-white/95 p-0 backdrop-blur data-[state=open]:slide-in-from-bottom-4 dark:border-slate-800 dark:bg-slate-950/95">
          <DialogHeader className="border-b border-slate-100 px-6 py-5 dark:border-slate-800">
            <DialogTitle className="flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <div className="rounded-2xl bg-blue-50 p-2 dark:bg-blue-500/15">
                <Key className="h-5 w-5 text-blue-600 dark:text-blue-300" />
              </div>
              创建 API Key
            </DialogTitle>
            <DialogDescription className="text-slate-500 dark:text-slate-400">
              创建前必须绑定一个已购套餐。完整密钥仅在创建成功时返回一次。
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 px-6 py-5">
            <div className="grid gap-4 md:grid-cols-2">
              <label className="space-y-2">
                <div className="text-sm font-medium text-slate-700 dark:text-slate-200">Key 名称</div>
                <input
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                  placeholder="例如：生产环境 Key"
                  value={form.name}
                  onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                />
              </label>

              <label className="space-y-2">
                <div className="text-sm font-medium text-slate-700 dark:text-slate-200">绑定套餐</div>
                <Select
                  value={form.modelGroupId}
                  onValueChange={(value) => setForm((prev) => ({ ...prev, modelGroupId: value }))}
                >
                    <SelectTrigger className="h-11 rounded-xl border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
                      <SelectValue placeholder="请选择一个已购套餐" />
                    </SelectTrigger>
                  <SelectContent>
                    {availableGroups.map((group) => (
                      <SelectItem key={group.id} value={group.id}>
                        {group.groupName}
                        {group.packageType === "BALANCE" ? "（余额计费）" : ""}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </label>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <label className="space-y-2">
                <div className="text-sm font-medium text-slate-700 dark:text-slate-200">过期时间</div>
                <input
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                  type="datetime-local"
                  value={form.expiresAt}
                  onChange={(e) => setForm((prev) => ({ ...prev, expiresAt: e.target.value }))}
                />
              </label>

              <label className="space-y-2">
                <div className="text-sm font-medium text-slate-700 dark:text-slate-200">备注</div>
                <input
                  className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                  placeholder="备注（可选）"
                  value={form.remark}
                  onChange={(e) => setForm((prev) => ({ ...prev, remark: e.target.value }))}
                />
              </label>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-200">
                <Package className="h-4 w-4 text-blue-600 dark:text-blue-300" />
                套餐绑定说明
              </div>
              {selectedGroup ? (
                <div className="mt-3 grid gap-3 text-sm md:grid-cols-3">
                  <div className="rounded-xl bg-white px-3 py-3 dark:bg-slate-950">
                    <div className="text-xs text-slate-500 dark:text-slate-400">套餐名称</div>
                    <div className="mt-1 font-medium text-slate-900 dark:text-slate-100">
                      {selectedGroup.groupName}
                    </div>
                  </div>
                  <div className="rounded-xl bg-white px-3 py-3 dark:bg-slate-950">
                    <div className="text-xs text-slate-500 dark:text-slate-400">
                      {selectedGroupIsBalancePackage ? "计费方式" : "月额度"}
                    </div>
                    <div className="mt-1 font-medium text-slate-900 dark:text-slate-100">
                      {selectedGroupIsBalancePackage
                        ? "按钱包余额扣费"
                        : formatQuota(selectedGroup.monthlyQuota)}
                    </div>
                  </div>
                  <div className="rounded-xl bg-white px-3 py-3 dark:bg-slate-950">
                    <div className="text-xs text-slate-500 dark:text-slate-400">
                      {selectedGroupIsBalancePackage ? "最低门槛" : "剩余天数"}
                    </div>
                    <div className="mt-1 font-medium text-slate-900 dark:text-slate-100">
                      {selectedGroupIsBalancePackage
                        ? "购买 >= $1.00 / 调用 >= $0.50"
                        : selectedGroup.remainingDays != null
                          ? `${selectedGroup.remainingDays} 天`
                          : "—"}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="mt-3 rounded-xl border border-dashed border-slate-300 px-3 py-4 text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
                  创建前请先选择一个已购买的套餐分组，后端会将当前 Key 绑定到对应套餐。
                </div>
              )}
            </div>
          </div>

          <DialogFooter className="border-t border-slate-100 px-6 py-4 dark:border-slate-800">
            <button
              onClick={() => setShowCreate(false)}
              disabled={createMutation.isPending}
              className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-900"
            >
              取消
            </button>
            <button
              onClick={() => void handleCreate()}
              disabled={createMutation.isPending || !user?.id}
              className="rounded-xl bg-blue-600 px-4 py-2 text-sm text-white transition hover:bg-blue-700 disabled:opacity-60"
            >
              <ButtonLoadingContent loading={createMutation.isPending} loadingText="创建中...">
                确认创建
              </ButtonLoadingContent>
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={showPackagePrompt} onOpenChange={setShowPackagePrompt}>
        <AlertDialogContent className="border-amber-200 bg-white/95 backdrop-blur dark:border-amber-500/30 dark:bg-slate-950/95">
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <ShieldAlert className="h-5 w-5 text-amber-500" />
              当前账号尚未开通套餐
            </AlertDialogTitle>
            <AlertDialogDescription className="text-slate-500 dark:text-slate-400">
              根据当前后端接口规则，未开通套餐时不能创建 API Key。请先在“用量 & 账单”模块完成套餐购买后，再回来创建 Key。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>我知道了</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">当前用户 Key 数</div>
          <div className="mt-1 text-2xl font-semibold text-slate-900 dark:text-slate-100">{keys.length}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">正常使用</div>
          <div className="mt-1 text-2xl font-semibold text-emerald-600">
            {keys.filter((item) => getApiKeyStatusTone(item.status).label === "正常").length}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">当前用户</div>
          <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">{user?.username ?? "-"}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">可用套餐</div>
          <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
            {availableGroups.length > 0 ? `${availableGroups.length} 个已购套餐` : "未开通"}
          </div>
        </div>
      </div>

      {keys.length === 0 ? (
        <PageEmptyState
          title="暂无数据"
          description="当前账号还没有创建任何 API Key。"
        />
      ) : (
        <div className="space-y-3">
          {keys.map((key) => {
            const tone = getApiKeyStatusTone(key.status);
            return (
              <div key={key.id} className="rounded-2xl border border-slate-100 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
                <div className="flex items-start gap-3">
                  <div className="rounded-xl bg-blue-50 p-2 dark:bg-blue-500/15">
                    <Key className="h-4 w-4 text-blue-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <div className="truncate text-sm font-medium text-slate-800 dark:text-slate-100">{key.name}</div>
                      <span className={`rounded-full border px-2 py-0.5 text-xs ${tone.className}`}>
                        {tone.label}
                      </span>
                    </div>
                    <div className="mt-1 flex items-center gap-2">
                      <ShieldCheck className="h-4 w-4 text-slate-400 dark:text-slate-500" />
                      <code className="text-xs text-slate-600 dark:text-slate-300">{key.accessKey}</code>
                      <button
                        onClick={() => handleCopy(key.accessKey, key.id)}
                        className="text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-100"
                      >
                        {copied === key.id ? (
                          <CheckCircle className="h-4 w-4 text-emerald-500" />
                        ) : (
                          <Copy className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  </div>
                  <button
                    onClick={async () => {
                      try {
                        const nextStatus = getNextApiKeyStatus(key.status);
                        await updateStatusMutation.mutateAsync({
                          id: key.id,
                          input: { status: nextStatus },
                        });
                        toast.success(nextStatus === "ACTIVE" ? "API Key 已启用" : "API Key 已禁用");
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
                  <button
                    onClick={() => setDeleteTargetId(key.id)}
                    disabled={deleteMutation.isPending}
                    className="rounded-lg border border-rose-200 px-3 py-1.5 text-xs text-rose-600 hover:bg-rose-50 disabled:opacity-60 dark:border-rose-500/30 dark:text-rose-300 dark:hover:bg-rose-500/10"
                  >
                    <span className="inline-flex items-center gap-1">
                      <Trash2 className="h-3.5 w-3.5" />
                      删除
                    </span>
                  </button>
                </div>

                <div className="mt-4 grid grid-cols-4 gap-3 text-xs text-slate-500 dark:text-slate-400">
                  <div>
                    <div>所属模型组</div>
                    <div className="mt-1 text-sm text-slate-800 dark:text-slate-100">
                      {key.modelGroupName ?? "未分组"}
                    </div>
                  </div>
                  <div>
                    <div>已用额度</div>
                    <div className="mt-1 text-sm text-slate-800 dark:text-slate-100">{formatQuota(key.usedQuota)}</div>
                  </div>
                  <div>
                    <div>总额度</div>
                    <div className="mt-1 text-sm text-slate-800 dark:text-slate-100">{formatQuota(key.totalQuota)}</div>
                  </div>
                  <div>
                    <div>最后使用</div>
                    <div className="mt-1 text-sm text-slate-800 dark:text-slate-100">
                      {formatApiKeyDate(key.lastUsedAt)}
                    </div>
                  </div>
                  <div>
                    <div>创建时间</div>
                    <div className="mt-1 text-sm text-slate-800 dark:text-slate-100">
                      {formatApiKeyDate(key.createdAt)}
                    </div>
                  </div>
                  <div>
                    <div>过期时间</div>
                    <div className="mt-1 text-sm text-slate-800 dark:text-slate-100">
                      {key.expiresAt ? formatApiKeyDate(key.expiresAt) : "永不过期"}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <AlertDialog
        open={Boolean(deleteTargetId)}
        onOpenChange={(open) => {
          if (!open && !deleteMutation.isPending) {
            setDeleteTargetId(null);
          }
        }}
      >
        <AlertDialogContent className="border-rose-200 bg-white/95 backdrop-blur dark:border-rose-500/30 dark:bg-slate-950/95">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-slate-900 dark:text-slate-100">
              确认删除 API Key？
            </AlertDialogTitle>
            <AlertDialogDescription className="text-slate-500 dark:text-slate-400">
              删除后当前 Key 将立即失效，且无法恢复。请确认是否继续删除。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>取消</AlertDialogCancel>
            <AlertDialogAction
              disabled={deleteMutation.isPending || !deleteTargetId}
              onClick={async (event) => {
                event.preventDefault();
                if (!deleteTargetId) {
                  return;
                }

                try {
                  await deleteMutation.mutateAsync(deleteTargetId);
                  toast.success("API Key 已删除");
                  setDeleteTargetId(null);
                } catch (deleteError) {
                  if (isAppError(deleteError)) {
                    toast.error(deleteError.message);
                  } else {
                    toast.error("删除失败，请稍后重试");
                  }
                }
              }}
              className="bg-rose-600 text-white hover:bg-rose-700"
            >
              <ButtonLoadingContent loading={deleteMutation.isPending} loadingText="删除中...">
                确认删除
              </ButtonLoadingContent>
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
