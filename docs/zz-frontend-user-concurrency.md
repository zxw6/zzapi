# ZZ 前端用户并发配置接入说明

## 背景

后端已经支持给单个用户单独设置 AI 并发额度。

本次不要改旧前端：

- `src/main/resources/static/console/index.html`
- `src/main/resources/static/console/app.js`

当前真实前端是：

- `zz-react-admin-codex-admin-api-alignment`

所以前端只需要在 `zz-react-admin-codex-admin-api-alignment` 里接入。

## 后端字段

用户接口现在会多返回两个字段：

```ts
maxConcurrentRequests: number | null;
maxConcurrentStreams: number | null;
```

含义：

- `maxConcurrentRequests`：该用户最大同时请求数。
- `maxConcurrentStreams`：该用户最大同时流式请求数。
- `0`、`null` 或不传：使用后端全局默认。

创建用户和更新用户时，也可以传这两个字段。

## 需要改的前端文件

### 1. 修改接口类型

文件：

```text
zz-react-admin-codex-admin-api-alignment/src/app/api/types.ts
```

需要修改：

```ts
export type UserListItemResponse = {
  id: string;
  username: string;
  nickname: string;
  roleCode: string;
  status: string;
  email: string | null;
  phone: string | null;
  balance: number | null;
  maxConcurrentRequests: number | null;
  maxConcurrentStreams: number | null;
  lastLoginAt: string | null;
  createdAt: string;
};
```

创建用户请求增加：

```ts
export type UserCreateRequest = {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  phone?: string;
  roleCode?: string;
  initialBalance?: number;
  maxConcurrentRequests?: number;
  maxConcurrentStreams?: number;
};
```

更新用户请求增加：

```ts
export type UserUpdateRequest = {
  nickname?: string;
  email?: string;
  phone?: string;
  roleCode?: string;
  status?: string;
  password?: string;
  maxConcurrentRequests?: number;
  maxConcurrentStreams?: number;
};
```

### 2. 修改用户管理页状态

文件：

```text
zz-react-admin-codex-admin-api-alignment/src/app/pages/Users.tsx
```

`CreateFormState` 增加：

```ts
type CreateFormState = {
  username: string;
  password: string;
  nickname: string;
  email: string;
  phone: string;
  roleCode: string;
  initialBalance: string;
  maxConcurrentRequests: string;
  maxConcurrentStreams: string;
};
```

`EditFormState` 增加：

```ts
type EditFormState = {
  nickname: string;
  email: string;
  phone: string;
  roleCode: string;
  status: string;
  password: string;
  maxConcurrentRequests: string;
  maxConcurrentStreams: string;
};
```

默认值增加：

```ts
const defaultCreateForm: CreateFormState = {
  username: "",
  password: "",
  nickname: "",
  email: "",
  phone: "",
  roleCode: "USER",
  initialBalance: "",
  maxConcurrentRequests: "0",
  maxConcurrentStreams: "0",
};

const defaultEditForm: EditFormState = {
  nickname: "",
  email: "",
  phone: "",
  roleCode: "USER",
  status: "ACTIVE",
  password: "",
  maxConcurrentRequests: "0",
  maxConcurrentStreams: "0",
};
```

### 3. 修改请求体归一化

文件：

```text
zz-react-admin-codex-admin-api-alignment/src/app/pages/Users.tsx
```

建议加一个工具函数：

```ts
function numberOrUndefined(value: string) {
  return value === "" ? undefined : Number(value);
}
```

修改创建请求：

```ts
function normalizeCreatePayload(form: CreateFormState) {
  return {
    username: form.username.trim(),
    password: form.password,
    nickname: form.nickname.trim() || undefined,
    email: form.email.trim() || undefined,
    phone: form.phone.trim() || undefined,
    roleCode: form.roleCode || undefined,
    initialBalance: numberOrUndefined(form.initialBalance),
    maxConcurrentRequests: numberOrUndefined(form.maxConcurrentRequests),
    maxConcurrentStreams: numberOrUndefined(form.maxConcurrentStreams),
  };
}
```

修改更新请求：

```ts
function normalizeUpdatePayload(form: EditFormState) {
  return {
    nickname: form.nickname.trim() || undefined,
    email: form.email.trim() || undefined,
    phone: form.phone.trim() || undefined,
    roleCode: form.roleCode || undefined,
    status: form.status || undefined,
    password: form.password || undefined,
    maxConcurrentRequests: numberOrUndefined(form.maxConcurrentRequests),
    maxConcurrentStreams: numberOrUndefined(form.maxConcurrentStreams),
  };
}
```

修改编辑表单回填：

```ts
function pickEditableForm(user: UserListItemResponse): EditFormState {
  return {
    nickname: user.nickname || "",
    email: user.email || "",
    phone: user.phone || "",
    roleCode: user.roleCode || "USER",
    status: user.status || "ACTIVE",
    password: "",
    maxConcurrentRequests: String(user.maxConcurrentRequests ?? 0),
    maxConcurrentStreams: String(user.maxConcurrentStreams ?? 0),
  };
}
```

### 4. 修改用户列表展示

文件：

```text
zz-react-admin-codex-admin-api-alignment/src/app/pages/Users.tsx
```

建议增加格式化函数：

```ts
function formatConcurrencyLimit(value: number | null | undefined) {
  return value && value > 0 ? String(value) : "默认";
}
```

在用户表格中增加一列：

```tsx
<th className="px-4 py-3 text-left text-xs font-medium text-slate-500">并发</th>
```

每行增加：

```tsx
<td className="px-4 py-4 text-sm text-slate-700 dark:text-slate-200">
  <div>请求：{formatConcurrencyLimit(user.maxConcurrentRequests)}</div>
  <div className="text-xs text-slate-500">
    流式：{formatConcurrencyLimit(user.maxConcurrentStreams)}
  </div>
</td>
```

推荐放在“余额”和“时间”之间，方便管理员看用户权限。

### 5. 修改创建用户弹窗

文件：

```text
zz-react-admin-codex-admin-api-alignment/src/app/pages/Users.tsx
```

在创建用户表单里增加两个输入框：

```tsx
<input
  className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
  type="number"
  min="0"
  step="1"
  placeholder="请求并发，0 使用全局默认"
  value={createForm.maxConcurrentRequests}
  onChange={(event) =>
    setCreateForm((prev) => ({ ...prev, maxConcurrentRequests: event.target.value }))
  }
/>

<input
  className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
  type="number"
  min="0"
  step="1"
  placeholder="流式并发，0 使用全局默认"
  value={createForm.maxConcurrentStreams}
  onChange={(event) =>
    setCreateForm((prev) => ({ ...prev, maxConcurrentStreams: event.target.value }))
  }
/>
```

### 6. 修改编辑用户弹窗

文件：

```text
zz-react-admin-codex-admin-api-alignment/src/app/pages/Users.tsx
```

在编辑用户表单里增加两个输入框：

```tsx
<input
  className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
  type="number"
  min="0"
  step="1"
  placeholder="请求并发，0 使用全局默认"
  value={editForm.maxConcurrentRequests}
  onChange={(event) =>
    setEditForm((prev) => ({ ...prev, maxConcurrentRequests: event.target.value }))
  }
/>

<input
  className="rounded-lg border border-slate-200 px-3 py-2 text-sm dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
  type="number"
  min="0"
  step="1"
  placeholder="流式并发，0 使用全局默认"
  value={editForm.maxConcurrentStreams}
  onChange={(event) =>
    setEditForm((prev) => ({ ...prev, maxConcurrentStreams: event.target.value }))
  }
/>
```

### 7. Mock 数据可选修改

如果本地前端使用 mock server，需要同步改：

```text
zz-react-admin-codex-admin-api-alignment/src/mocks/server.ts
```

在用户 mock 数据里增加：

```ts
maxConcurrentRequests: 0,
maxConcurrentStreams: 0,
```

创建和更新用户 mock 逻辑也要保留这两个字段。

如果前端直接连真实后端，可以先不改 mock。

## 接口请求示例

创建用户：

```json
{
  "username": "vip001",
  "password": "123456",
  "nickname": "VIP 用户",
  "email": "vip001@qq.com",
  "phone": "13800000000",
  "roleCode": "USER",
  "initialBalance": 100,
  "maxConcurrentRequests": 50,
  "maxConcurrentStreams": 20
}
```

更新用户：

```json
{
  "nickname": "VIP 用户",
  "email": "vip001@qq.com",
  "phone": "13800000000",
  "roleCode": "USER",
  "status": "ACTIVE",
  "maxConcurrentRequests": 50,
  "maxConcurrentStreams": 20
}
```

恢复全局默认：

```json
{
  "maxConcurrentRequests": 0,
  "maxConcurrentStreams": 0
}
```

## 前端校验建议

输入限制：

- 类型：整数。
- 最小值：`0`。
- `0` 表示使用全局默认。
- 不建议前端强行限制最大值，因为后端还有全局总阀门。

展示文案建议：

- 请求并发：用户同时可发起的 AI 请求数。
- 流式并发：用户同时保持的流式 AI 对话数。
- 默认：使用系统全局配置。

## 验证步骤

1. 管理员打开用户管理页。
2. 创建用户，填写请求并发 `50`、流式并发 `20`。
3. 创建成功后刷新用户列表，能看到并发列为 `请求：50 / 流式：20`。
4. 点击编辑用户，把两个值改为 `0`。
5. 保存后刷新，列表显示 `默认`。
6. 用该用户的 API Key 发起请求，后端会按该用户配置限流。

## 后端已完成

后端已经完成以下内容，前端只需要传字段：

- `users.max_concurrent_requests`
- `users.max_concurrent_streams`
- `GET /admin/users`
- `GET /admin/users/{userId}`
- `POST /admin/users`
- `PUT /admin/users/{userId}`
- 网关限流实际生效

