import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { Layers3, PencilLine, Plus, Power, ShieldCheck, Sparkles, X } from "lucide-react";
import { toast } from "sonner";
import {
  useCreateModelAccessGroupMutation,
  useModelAccessSummaryQuery,
  useRemoveModelAccessGroupMutation,
} from "../api/queries";
import type { ModelGroupCreateRequest, ModelGroupOptionResponse } from "../api/types";
import { isAppError } from "../../lib/http/error";
import {
  ButtonLoadingContent,
  PageEmptyState,
  PageErrorState,
  PageTableSkeleton,
} from "../components/ui/feedback";

type GroupFormState = {
  groupCode: string;
  groupName: string;
  packageType: "QUOTA" | "BALANCE";
  salePrice: string;
  packageDays: string;
  dailyQuota: string;
  weeklyQuota: string;
  monthlyQuota: string;
  remark: string;
};

const defaultFormState: GroupFormState = {
  groupCode: "",
  groupName: "",
  packageType: "QUOTA",
  salePrice: "",
  packageDays: "",
  dailyQuota: "",
  weeklyQuota: "",
  monthlyQuota: "",
  remark: "",
};

type ModalProps = {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
};

function Modal({ title, onClose, children }: ModalProps) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  if (typeof document === "undefined") {
    return null;
  }

  return createPortal(
    <div className="fixed inset-0 z-[60] bg-slate-950/45 p-4 backdrop-blur-[1px]">
      <div className="flex h-full w-full items-start justify-center overflow-y-auto sm:items-center">
        <div className="my-auto flex max-h-[calc(100vh-2rem)] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl dark:bg-slate-900 dark:shadow-slate-950/60">
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
    </div>,
    document.body,
  );
}

function formatCurrency(value: number | null | undefined) {
  return `¥ ${Number(value ?? 0).toFixed(2)}`;
}

function formatQuota(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString();
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("zh-CN", { hour12: false });
}

function getGroupStatusTone(group: ModelGroupOptionResponse) {
  if (group.active) {
    return "border-emerald-200 bg-emerald-50 text-emerald-600 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-300";
  }

  if (group.purchased) {
    return "border-blue-200 bg-blue-50 text-blue-600 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300";
  }

  return "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300";
}

function buildCreatePayload(form: GroupFormState): ModelGroupCreateRequest {
  const isBalancePackage = form.packageType === "BALANCE";
  return {
    groupCode: form.groupCode.trim(),
    groupName: form.groupName.trim(),
    packageType: form.packageType,
    salePrice: isBalancePackage ? 0 : Number(form.salePrice || 0),
    packageDays: isBalancePackage ? 0 : Number(form.packageDays || 0),
    dailyQuota: isBalancePackage ? 0 : Number(form.dailyQuota || 0),
    weeklyQuota: isBalancePackage ? 0 : Number(form.weeklyQuota || 0),
    monthlyQuota: isBalancePackage ? 0 : Number(form.monthlyQuota || 0),
    remark: form.remark.trim() || undefined,
  };
}

function createEditDraft(group: ModelGroupOptionResponse): GroupFormState {
  return {
    groupCode: group.groupCode,
    groupName: group.groupName,
    packageType: group.packageType === "BALANCE" ? "BALANCE" : "QUOTA",
    salePrice: String(group.salePrice ?? 0),
    packageDays: String(group.packageDays ?? 0),
    dailyQuota: String(group.dailyQuota ?? 0),
    weeklyQuota: String(group.weeklyQuota ?? 0),
    monthlyQuota: String(group.monthlyQuota ?? 0),
    remark: group.remark ?? "",
  };
}

function getPackageTypeLabel(group: Pick<ModelGroupOptionResponse, "packageType" | "packageTypeText">) {
  if (group.packageType === "BALANCE") {
    return group.packageTypeText || "余额套餐";
  }

  return group.packageTypeText || "普通套餐";
}

const groupFormFields: Array<{ key: keyof GroupFormState; label: string; placeholder?: string }> = [
  { key: "groupCode", label: "套餐编码", placeholder: "starter-pro" },
  { key: "groupName", label: "套餐名称", placeholder: "旗舰套餐" },
  { key: "salePrice", label: "套餐价格", placeholder: "可选，留空即可" },
  { key: "packageDays", label: "有效天数", placeholder: "可选，留空即可" },
  { key: "dailyQuota", label: "日额度", placeholder: "可选，留空即可" },
  { key: "weeklyQuota", label: "周额度", placeholder: "可选，留空即可" },
  { key: "monthlyQuota", label: "月额度", placeholder: "可选，留空即可" },
];

export function PackageCenterPage() {
  const [keyword, setKeyword] = useState("");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState<GroupFormState>(defaultFormState);
  const [editingGroup, setEditingGroup] = useState<ModelGroupOptionResponse | null>(null);
  const { data, isLoading, isError, error, refetch } = useModelAccessSummaryQuery(true);
  const createMutation = useCreateModelAccessGroupMutation();
  const removeMutation = useRemoveModelAccessGroupMutation();

  const groups = useMemo(() => data?.groups ?? [], [data?.groups]);

  const filteredGroups = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return groups.filter((group) => {
      if (!normalizedKeyword) {
        return true;
      }

      return (
        group.groupName.toLowerCase().includes(normalizedKeyword) ||
        group.groupCode.toLowerCase().includes(normalizedKeyword) ||
        (group.remark ?? "").toLowerCase().includes(normalizedKeyword)
      );
    });
  }, [groups, keyword]);

  const summary = useMemo(() => {
    return {
      total: groups.length,
      preset: groups.filter((group) => group.systemPreset).length,
      active: groups.filter((group) => group.active).length,
      modelCount: groups.reduce((sum, group) => sum + Number(group.modelCount ?? 0), 0),
    };
  }, [groups]);

  if (isLoading) {
    return (
      <div className="space-y-6 p-4 sm:p-6">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div
              key={index}
              className="h-28 rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900"
            />
          ))}
        </div>
        <PageTableSkeleton columns={8} rows={6} />
      </div>
    );
  }

  if (isError) {
    const message = isAppError(error) ? error.message : "套餐中心加载失败";
    return <PageErrorState message={message} onRetry={() => void refetch()} />;
  }

  return (
    <div className="space-y-6 p-4 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-slate-900 dark:text-slate-100">套餐中心</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            管理普通套餐与余额套餐的分组、额度配置和访问策略。
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          新增套餐
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">套餐总数</div>
          <div className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">{summary.total}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">系统预置</div>
          <div className="mt-2 text-2xl font-semibold text-violet-600">{summary.preset}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">当前生效</div>
          <div className="mt-2 text-2xl font-semibold text-emerald-600">{summary.active}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">关联模型数</div>
          <div className="mt-2 text-2xl font-semibold text-blue-600">{summary.modelCount}</div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-xs text-blue-700 dark:border-blue-500/20 dark:bg-blue-500/10 dark:text-blue-300">
            <Sparkles className="h-3.5 w-3.5" />
            套餐与模型访问模块
          </div>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索套餐名称、编码或备注"
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:bg-white dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 lg:max-w-xs"
          />
        </div>
      </div>

      {filteredGroups.length === 0 ? (
        <PageEmptyState
          title="暂无套餐分组"
          description="当前没有可展示的套餐分组，创建后会显示在这里。"
        />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
          <div className="overflow-x-auto">
            <table className="min-w-[1160px] w-full text-sm">
              <thead className="border-b border-slate-100 bg-slate-50 dark:border-slate-800 dark:bg-slate-950">
                <tr className="text-left text-xs text-slate-500 dark:text-slate-400">
                  <th className="px-5 py-3 font-medium">套餐</th>
                  <th className="px-4 py-3 font-medium">类型</th>
                  <th className="px-4 py-3 font-medium">价格 / 周期</th>
                  <th className="px-4 py-3 font-medium">额度配置</th>
                  <th className="px-4 py-3 font-medium">模型 / 状态</th>
                  <th className="px-4 py-3 font-medium">到期 / 备注</th>
                  <th className="px-4 py-3 font-medium">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredGroups.map((group) => (
                  <tr key={group.id}>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-300">
                          <Layers3 className="h-4 w-4" />
                        </div>
                        <div>
                          <div className="font-medium text-slate-900 dark:text-slate-100">
                            {group.groupName}
                          </div>
                          <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                            {group.groupCode}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                      <span
                        className={`inline-flex rounded-full border px-2 py-1 text-[11px] ${
                          group.packageType === "BALANCE"
                            ? "border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300"
                            : "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                        }`}
                      >
                        {getPackageTypeLabel(group)}
                      </span>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                      <div>{formatCurrency(group.salePrice)}</div>
                      <div className="mt-1">
                        {group.packageType === "BALANCE" ? "按余额实时扣费" : `${group.packageDays} 天`}
                      </div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                      {group.packageType === "BALANCE" ? (
                        <>
                          <div>购买门槛：余额 &gt;= $1.00</div>
                          <div className="mt-1">调用门槛：余额 &gt;= $0.50</div>
                          <div className="mt-1">不消耗普通套餐额度</div>
                        </>
                      ) : (
                        <>
                          <div>日额度：{formatQuota(group.dailyQuota)}</div>
                          <div className="mt-1">周额度：{formatQuota(group.weeklyQuota)}</div>
                          <div className="mt-1">月额度：{formatQuota(group.monthlyQuota)}</div>
                        </>
                      )}
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                      <div className="flex items-center gap-2">
                        <span>{group.modelCount} 个模型</span>
                        {group.systemPreset ? (
                          <span className="rounded-full border border-violet-200 bg-violet-50 px-2 py-0.5 text-[11px] text-violet-600 dark:border-violet-500/20 dark:bg-violet-500/10 dark:text-violet-300">
                            系统预置
                          </span>
                        ) : null}
                      </div>
                      <div className="mt-2">
                        <span className={`inline-flex rounded-full border px-2 py-1 text-[11px] ${getGroupStatusTone(group)}`}>
                          {group.packageStatusText}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                      <div>{formatDateTime(group.expiresAt)}</div>
                      <div className="mt-1 text-slate-400 dark:text-slate-500">
                        {group.remark || "—"}
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex flex-wrap gap-2">
                        <button
                          onClick={() => setEditingGroup(group)}
                          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          <PencilLine className="h-3.5 w-3.5" />
                          修改
                        </button>
                        <button
                          onClick={async () => {
                            if (!window.confirm(`确认停用套餐「${group.groupName}」？`)) {
                              return;
                            }

                            try {
                              await removeMutation.mutateAsync(group.id);
                              toast.success("套餐已停用");
                            } catch (removeError) {
                              if (isAppError(removeError)) {
                                toast.error(removeError.message);
                              } else {
                                toast.error("套餐停用失败，请稍后重试");
                              }
                            }
                          }}
                          disabled={removeMutation.isPending}
                          className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-3 py-1.5 text-xs text-rose-600 transition-colors hover:bg-rose-50 disabled:opacity-60 dark:border-rose-500/20 dark:text-rose-300 dark:hover:bg-rose-500/10"
                        >
                          <Power className="h-3.5 w-3.5" />
                          停用
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showCreateModal ? (
        <Modal
          title="新增套餐"
          onClose={() => {
            setShowCreateModal(false);
            setCreateForm(defaultFormState);
          }}
        >
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                套餐类型
              </label>
              <select
                value={createForm.packageType}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    packageType: event.target.value as GroupFormState["packageType"],
                    salePrice: event.target.value === "BALANCE" ? "" : prev.salePrice,
                    packageDays: event.target.value === "BALANCE" ? "" : prev.packageDays,
                    dailyQuota: event.target.value === "BALANCE" ? "" : prev.dailyQuota,
                    weeklyQuota: event.target.value === "BALANCE" ? "" : prev.weeklyQuota,
                    monthlyQuota: event.target.value === "BALANCE" ? "" : prev.monthlyQuota,
                  }))
                }
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              >
                <option value="QUOTA">普通套餐</option>
                <option value="BALANCE">余额套餐</option>
              </select>
            </div>
            {groupFormFields.map((field) => (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                  {field.label}
                </label>
                <input
                  value={createForm[field.key as keyof GroupFormState]}
                  disabled={
                    createForm.packageType === "BALANCE" &&
                    ["salePrice", "packageDays", "dailyQuota", "weeklyQuota", "monthlyQuota"].includes(field.key)
                  }
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
              placeholder="可选，填写套餐说明"
            />
            {createForm.packageType === "BALANCE" ? (
              <div className="mt-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300">
                余额套餐不需要填写价格和额度。用户余额需大于等于 $1.00 才能开通，调用时直接按钱包余额实时扣费。
              </div>
            ) : null}
          </div>

          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setShowCreateModal(false);
                setCreateForm(defaultFormState);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                if (!createForm.groupCode.trim() || !createForm.groupName.trim()) {
                  toast.error("请先填写套餐编码和套餐名称");
                  return;
                }

                try {
                  await createMutation.mutateAsync(buildCreatePayload(createForm));
                  toast.success("套餐创建成功");
                  setShowCreateModal(false);
                  setCreateForm(defaultFormState);
                } catch (createError) {
                  if (isAppError(createError)) {
                    toast.error(createError.message);
                  } else {
                    toast.error("套餐创建失败，请稍后重试");
                  }
                }
              }}
              disabled={createMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent loading={createMutation.isPending} loadingText="保存中...">
                保存套餐
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      ) : null}

      {editingGroup ? (
        <Modal title={`修改套餐 · ${editingGroup.groupName}`} onClose={() => setEditingGroup(null)}>
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300">
            当前仅提供新增和停用接口，套餐修改暂时只做只读展示，等后端补充更新接口后再接入提交能力。
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                套餐类型
              </label>
              <input
                value={getPackageTypeLabel(editingGroup)}
                readOnly
                className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              />
            </div>
            {groupFormFields.map((field) => (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">
                  {field.label}
                </label>
                <input
                  value={createEditDraft(editingGroup)[field.key]}
                  readOnly
                  className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
                />
              </div>
            ))}
          </div>

          <div className="mt-3">
            <label className="mb-1.5 block text-sm text-slate-700 dark:text-slate-300">备注</label>
            <textarea
              rows={3}
              readOnly
              value={editingGroup.remark ?? ""}
              className="w-full resize-none rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
            />
          </div>

          <div className="mt-5 flex justify-end">
            <button
              onClick={() => {
                toast.error("当前文档未提供套餐更新接口，暂时无法提交修改");
              }}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-500 dark:border-slate-700 dark:text-slate-300"
            >
              <ShieldCheck className="h-4 w-4" />
              等待更新接口
            </button>
          </div>
        </Modal>
      ) : null}
    </div>
  );
}

