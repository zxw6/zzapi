import { useMemo, useState } from "react";
import {
  CircleDollarSign,
  PencilLine,
  Plus,
  Trash2,
  UserRound,
  UserRoundCheck,
  UserRoundX,
  X,
} from "lucide-react";
import { toast } from "sonner";
import {
  useCreateUserMutation,
  useDeleteUserMutation,
  useRechargeUserMutation,
  useUpdateUserMutation,
  useUpdateUserStatusMutation,
  useUserDetailQuery,
  useUsersQuery,
} from "../api/queries";
import type { UserListItemResponse } from "../api/types";
import { isAppError } from "../../lib/http/error";
import {
  ButtonLoadingContent,
  PageCardGridSkeleton,
  PageErrorState,
  PageHeaderSkeleton,
  PageTableSkeleton,
} from "../components/ui/feedback";

type UserStatusTone = {
  label: string;
  className: string;
};

type CreateFormState = {
  username: string;
  password: string;
  nickname: string;
  email: string;
  phone: string;
  roleCode: string;
  initialBalance: string;
};

type EditFormState = {
  nickname: string;
  email: string;
  phone: string;
  roleCode: string;
  status: string;
  password: string;
};

type RechargeFormState = {
  amount: string;
  remark: string;
};

const defaultCreateForm: CreateFormState = {
  username: "",
  password: "",
  nickname: "",
  email: "",
  phone: "",
  roleCode: "USER",
  initialBalance: "",
};

const defaultEditForm: EditFormState = {
  nickname: "",
  email: "",
  phone: "",
  roleCode: "USER",
  status: "ACTIVE",
  password: "",
};

const defaultRechargeForm: RechargeFormState = {
  amount: "",
  remark: "",
};

function getUserStatusTone(status: string): UserStatusTone {
  const normalized = status.toUpperCase();

  if (normalized === "ACTIVE" || normalized === "ENABLED" || normalized === "NORMAL") {
    return {
      label: "正常",
      className: "border-emerald-200 bg-emerald-50 text-emerald-600",
    };
  }

  if (
    normalized === "DISABLED" ||
    normalized === "INACTIVE" ||
    normalized === "LOCKED" ||
    normalized === "BANNED"
  ) {
    return {
      label: "停用",
      className: "border-slate-200 bg-slate-100 text-slate-600",
    };
  }

  return {
    label: status || "未知",
    className: "border-amber-200 bg-amber-50 text-amber-700",
  };
}

function getNextUserStatus(status: string): string {
  const normalized = status.toUpperCase();
  if (normalized === "ACTIVE" || normalized === "ENABLED" || normalized === "NORMAL") {
    return "DISABLED";
  }

  return "ACTIVE";
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("zh-CN", { hour12: false });
}

function formatMoney(value: number | null): string {
  return `¥${Number(value ?? 0).toFixed(2)}`;
}

function normalizeCreatePayload(form: CreateFormState) {
  return {
    username: form.username.trim(),
    password: form.password,
    nickname: form.nickname.trim() || undefined,
    email: form.email.trim() || undefined,
    phone: form.phone.trim() || undefined,
    roleCode: form.roleCode || undefined,
    initialBalance: form.initialBalance === "" ? undefined : Number(form.initialBalance),
  };
}

function normalizeUpdatePayload(form: EditFormState) {
  return {
    nickname: form.nickname.trim() || undefined,
    email: form.email.trim() || undefined,
    phone: form.phone.trim() || undefined,
    roleCode: form.roleCode || undefined,
    status: form.status || undefined,
    password: form.password || undefined,
  };
}

function pickEditableForm(user: UserListItemResponse): EditFormState {
  return {
    nickname: user.nickname || "",
    email: user.email || "",
    phone: user.phone || "",
    roleCode: user.roleCode || "USER",
    status: user.status || "ACTIVE",
    password: "",
  };
}

type ModalProps = {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
};

function Modal({ title, onClose, children }: ModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-3xl rounded-2xl bg-white shadow-2xl dark:bg-slate-900 dark:shadow-slate-950/60">
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <h2 className="text-slate-900 dark:text-slate-100">{title}</h2>
          <button
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}

export function UsersPage() {
  const { data, isLoading, isError, refetch, error } = useUsersQuery();

  const createMutation = useCreateUserMutation();
  const updateMutation = useUpdateUserMutation();
  const updateStatusMutation = useUpdateUserStatusMutation();
  const deleteMutation = useDeleteUserMutation();
  const rechargeMutation = useRechargeUserMutation();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [rechargeUserId, setRechargeUserId] = useState<string | null>(null);
  const [deletingUserId, setDeletingUserId] = useState<string | null>(null);

  const [createForm, setCreateForm] = useState<CreateFormState>(defaultCreateForm);
  const [editForm, setEditForm] = useState<EditFormState>(defaultEditForm);
  const [rechargeForm, setRechargeForm] = useState<RechargeFormState>(defaultRechargeForm);

  const users = useMemo(() => data ?? [], [data]);

  const activeCount = useMemo(
    () =>
      users.filter((item) => {
        const normalized = item.status.toUpperCase();
        return normalized === "ACTIVE" || normalized === "ENABLED" || normalized === "NORMAL";
      }).length,
    [users],
  );

  const disabledCount = useMemo(() => users.length - activeCount, [users, activeCount]);

  const totalBalance = useMemo(
    () => users.reduce((sum, user) => sum + Number(user.balance ?? 0), 0),
    [users],
  );

  const editingUser = useMemo(
    () => users.find((item) => item.id === editingUserId) ?? null,
    [users, editingUserId],
  );

  const rechargeUser = useMemo(
    () => users.find((item) => item.id === rechargeUserId) ?? null,
    [users, rechargeUserId],
  );

  const deletingUser = useMemo(
    () => users.find((item) => item.id === deletingUserId) ?? null,
    [users, deletingUserId],
  );

  const { data: userDetail } = useUserDetailQuery(editingUserId ?? "", Boolean(editingUserId));

  if (isLoading) {
    return (
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeaderSkeleton />
        <PageCardGridSkeleton />
        <PageTableSkeleton columns={6} rows={6} />
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
          <h1 className="text-slate-900 dark:text-slate-100">用户管理</h1>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">管理平台用户资料、状态、角色与钱包余额</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white transition-colors hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          新增用户
        </button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">用户总数</div>
          <div className="mt-1 flex items-center gap-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">
            <UserRound className="h-5 w-5 text-slate-500" />
            {users.length}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">正常用户</div>
          <div className="mt-1 flex items-center gap-2 text-2xl font-semibold text-emerald-600">
            <UserRoundCheck className="h-5 w-5" />
            {activeCount}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">停用用户</div>
          <div className="mt-1 flex items-center gap-2 text-2xl font-semibold text-slate-700 dark:text-slate-200">
            <UserRoundX className="h-5 w-5" />
            {disabledCount}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900">
          <div className="text-xs text-slate-500 dark:text-slate-400">用户总余额</div>
          <div className="mt-1 flex items-center gap-2 text-2xl font-semibold text-blue-600">
            <CircleDollarSign className="h-5 w-5" />
            {formatMoney(totalBalance)}
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
        <table className="min-w-[880px] w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-950">
            <tr>
              <th className="px-5 py-3 text-left text-xs font-medium text-slate-500">用户</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">联系方式</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">
                角色 / 状态
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">余额</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">时间</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-500">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {users.map((user) => {
              const tone = getUserStatusTone(user.status);

              return (
                <tr key={user.id}>
                  <td className="px-5 py-4">
                    <div className="text-sm font-medium text-slate-800 dark:text-slate-100">{user.username}</div>
                    <div className="text-xs text-slate-400 dark:text-slate-500">ID: {user.id}</div>
                    <div className="mt-1 text-xs text-slate-500 dark:text-slate-400">{user.nickname || "-"}</div>
                  </td>
                  <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">
                    <div>{user.email || "-"}</div>
                    <div className="mt-1">{user.phone || "-"}</div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="text-xs text-slate-700 dark:text-slate-200">{user.roleCode || "-"}</div>
                    <span
                      className={`mt-2 inline-block rounded-full border px-2 py-1 text-xs ${tone.className}`}
                    >
                      {tone.label}
                    </span>
                  </td>
                  <td className="px-4 py-4 text-sm font-medium text-slate-700 dark:text-slate-200">
                    {formatMoney(user.balance)}
                  </td>
                  <td className="px-4 py-4 text-xs text-slate-500 dark:text-slate-400">
                    <div>注册：{formatDateTime(user.createdAt)}</div>
                    <div className="mt-1">登录：{formatDateTime(user.lastLoginAt)}</div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => {
                          setEditingUserId(user.id);
                          setEditForm(pickEditableForm(user));
                        }}
                        className="rounded-lg border px-2.5 py-1.5 text-xs text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        <span className="flex items-center gap-1">
                          <PencilLine className="h-3.5 w-3.5" />
                          编辑
                        </span>
                      </button>
                      <button
                        onClick={() => {
                          setRechargeUserId(user.id);
                          setRechargeForm(defaultRechargeForm);
                        }}
                        className="rounded-lg border px-2.5 py-1.5 text-xs text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        充值
                      </button>
                      <button
                        onClick={async () => {
                          try {
                            const nextStatus = getNextUserStatus(user.status);
                            await updateStatusMutation.mutateAsync({
                              userId: user.id,
                              input: { status: nextStatus },
                            });
                            toast.success(nextStatus === "ACTIVE" ? "用户已启用" : "用户已停用");
                          } catch (err) {
                            if (isAppError(err)) {
                              toast.error(err.message);
                            } else {
                              toast.error("状态更新失败，请稍后重试");
                            }
                          }
                        }}
                        disabled={updateStatusMutation.isPending}
                        className="rounded-lg border px-2.5 py-1.5 text-xs text-slate-600 hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                      >
                        {getNextUserStatus(user.status) === "ACTIVE" ? "启用" : "停用"}
                      </button>
                      <button
                        onClick={() => setDeletingUserId(user.id)}
                        className="rounded-lg border border-red-200 px-2.5 py-1.5 text-xs text-red-500 hover:bg-red-50"
                      >
                        <span className="flex items-center gap-1">
                          <Trash2 className="h-3.5 w-3.5" />
                          删除
                        </span>
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        </div>
      </div>

      {showCreateModal && (
        <Modal
          title="新增用户"
          onClose={() => {
            setShowCreateModal(false);
            setCreateForm(defaultCreateForm);
          }}
        >
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="用户名（必填）"
              value={createForm.username}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, username: event.target.value }))
              }
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="密码（必填）"
              type="password"
              value={createForm.password}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, password: event.target.value }))
              }
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="昵称"
              value={createForm.nickname}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, nickname: event.target.value }))
              }
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="邮箱"
              value={createForm.email}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, email: event.target.value }))
              }
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="手机号"
              value={createForm.phone}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, phone: event.target.value }))
              }
            />
            <select
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
              value={createForm.roleCode}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, roleCode: event.target.value }))
              }
            >
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <input
              className="col-span-2 rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              type="number"
              min="0"
              step="0.01"
              placeholder="初始余额"
              value={createForm.initialBalance}
              onChange={(event) =>
                setCreateForm((prev) => ({ ...prev, initialBalance: event.target.value }))
              }
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
                if (!createForm.username.trim() || !createForm.password) {
                  toast.error("请填写用户名和密码");
                  return;
                }

                try {
                  await createMutation.mutateAsync(normalizeCreatePayload(createForm));
                  toast.success("用户创建成功");
                  setShowCreateModal(false);
                  setCreateForm(defaultCreateForm);
                } catch (err) {
                  if (isAppError(err)) {
                    toast.error(err.message);
                  } else {
                    toast.error("创建失败，请稍后重试");
                  }
                }
              }}
              disabled={createMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent loading={createMutation.isPending} loadingText="创建中...">
                创建
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      )}

      {editingUser && (
        <Modal
          title="编辑用户"
          onClose={() => {
            setEditingUserId(null);
            setEditForm(defaultEditForm);
          }}
        >
          <div className="mb-3 text-xs text-slate-500 dark:text-slate-400">
            编辑用户：{userDetail?.username ?? editingUser.username}（ID: {editingUser.id}）
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="昵称"
              value={editForm.nickname}
              onChange={(event) =>
                setEditForm((prev) => ({ ...prev, nickname: event.target.value }))
              }
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="邮箱"
              value={editForm.email}
              onChange={(event) => setEditForm((prev) => ({ ...prev, email: event.target.value }))}
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="手机号"
              value={editForm.phone}
              onChange={(event) => setEditForm((prev) => ({ ...prev, phone: event.target.value }))}
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="重置密码（可选）"
              type="password"
              value={editForm.password}
              onChange={(event) =>
                setEditForm((prev) => ({ ...prev, password: event.target.value }))
              }
            />
            <select
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              value={editForm.roleCode}
              onChange={(event) =>
                setEditForm((prev) => ({ ...prev, roleCode: event.target.value }))
              }
            >
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <select
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              value={editForm.status}
              onChange={(event) => setEditForm((prev) => ({ ...prev, status: event.target.value }))}
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setEditingUserId(null);
                setEditForm(defaultEditForm);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                try {
                  await updateMutation.mutateAsync({
                    userId: editingUser.id,
                    input: normalizeUpdatePayload(editForm),
                  });
                  toast.success("用户信息已更新");
                  setEditingUserId(null);
                  setEditForm(defaultEditForm);
                } catch (err) {
                  if (isAppError(err)) {
                    toast.error(err.message);
                  } else {
                    toast.error("更新失败，请稍后重试");
                  }
                }
              }}
              disabled={updateMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent loading={updateMutation.isPending} loadingText="保存中...">
                保存修改
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      )}

      {rechargeUser && (
        <Modal
          title="用户充值"
          onClose={() => {
            setRechargeUserId(null);
            setRechargeForm(defaultRechargeForm);
          }}
        >
          <div className="mb-3 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-700 dark:bg-slate-800 dark:text-slate-200">
            充值目标：{rechargeUser.username}（ID: {rechargeUser.id}）
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="充值金额（最小 0.01）"
              value={rechargeForm.amount}
              onChange={(event) =>
                setRechargeForm((prev) => ({ ...prev, amount: event.target.value }))
              }
            />
            <input
              className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
              placeholder="备注（可选）"
              value={rechargeForm.remark}
              onChange={(event) =>
                setRechargeForm((prev) => ({ ...prev, remark: event.target.value }))
              }
            />
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => {
                setRechargeUserId(null);
                setRechargeForm(defaultRechargeForm);
              }}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                const amount = Number(rechargeForm.amount);
                if (!Number.isFinite(amount) || amount < 0.01) {
                  toast.error("充值金额需大于等于 0.01");
                  return;
                }

                try {
                  await rechargeMutation.mutateAsync({
                    userId: rechargeUser.id,
                    amount,
                    remark: rechargeForm.remark.trim() || undefined,
                  });
                  toast.success("充值成功");
                  setRechargeUserId(null);
                  setRechargeForm(defaultRechargeForm);
                } catch (err) {
                  if (isAppError(err)) {
                    toast.error(err.message);
                  } else {
                    toast.error("充值失败，请稍后重试");
                  }
                }
              }}
              disabled={rechargeMutation.isPending}
              className="rounded-lg bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent loading={rechargeMutation.isPending} loadingText="充值中...">
                确认充值
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      )}

      {deletingUser && (
        <Modal title="删除用户" onClose={() => setDeletingUserId(null)}>
          <div className="text-sm text-slate-700 dark:text-slate-300">
            确认删除用户 <span className="font-medium text-slate-900 dark:text-slate-100">{deletingUser.username}</span>
            （ID: {deletingUser.id}）吗？
          </div>
          <div className="mt-2 text-xs text-slate-500 dark:text-slate-400">删除后不可恢复，请谨慎操作。</div>
          <div className="mt-5 flex justify-end gap-2">
            <button
              onClick={() => setDeletingUserId(null)}
              className="rounded-lg border px-3 py-2 text-sm dark:border-slate-700 dark:text-slate-300"
            >
              取消
            </button>
            <button
              onClick={async () => {
                try {
                  await deleteMutation.mutateAsync(deletingUser.id);
                  toast.success("用户已删除");
                  setDeletingUserId(null);
                } catch (err) {
                  if (isAppError(err)) {
                    toast.error(err.message);
                  } else {
                    toast.error("删除失败，请稍后重试");
                  }
                }
              }}
              disabled={deleteMutation.isPending}
              className="rounded-lg bg-red-600 px-3 py-2 text-sm text-white disabled:opacity-60"
            >
              <ButtonLoadingContent loading={deleteMutation.isPending} loadingText="删除中...">
                确认删除
              </ButtonLoadingContent>
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
