# 接口统计与入参出参说明

## 1. 基本信息

- 项目路径: `e:\project-ai\Test`
- 服务默认端口: `9988`
- 配置文件: `src/main/resources/application.yml`
- 逻辑接口数: `36`
- 实际可访问 URL 数: `41`

## 2. 统一规则

### 2.1 后台接口统一返回

大部分后台接口返回统一结构:

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

字段说明:

- `success`: 是否成功
- `message`: 提示信息
- `data`: 业务数据，可能是对象、数组或 `null`

### 2.2 鉴权规则

后台接口默认需要:

```http
Authorization: Bearer <JWT>
```

以下后台接口免 JWT:

- `POST /admin/auth/login`
- `POST /admin/auth/register`
- `GET /admin/system/health`
- `GET /admin/system/sse-protocol`

网关接口默认使用:

```http
Authorization: Bearer <API_KEY>
```

Anthropic Messages 兼容接口还支持:

```http
x-api-key: <API_KEY>
```

## 3. 后台管理接口

### 3.1 认证模块

#### 1. `POST /admin/auth/login`

- 作用: 后台登录
- 鉴权: 无
- 请求头:
  - `Content-Type: application/json`
- 路径参数: 无
- 查询参数: 无
- 请求体:
  - `username`: `string`，必填
  - `password`: `string`，必填
- 响应:
  - `success`: `boolean`
  - `message`: `string`
  - `data.token`: `string`
  - `data.userId`: `long`
  - `data.username`: `string`
  - `data.nickname`: `string`
  - `data.roleCode`: `string`
  - `data.balance`: `decimal`

#### 2. `POST /admin/auth/register`

- 作用: 普通用户注册并自动登录
- 鉴权: 无
- 请求头:
  - `Content-Type: application/json`
- 路径参数: 无
- 查询参数: 无
- 请求体:
  - `username`: `string`，必填
  - `password`: `string`，必填
  - `nickname`: `string`，选填
  - `email`: `string`，必填，当前限制为 QQ 邮箱
  - `phone`: `string`，选填
- 响应:
  - 与登录接口相同

#### 3. `GET /admin/auth/me`

- 作用: 获取当前登录用户信息
- 鉴权: `Authorization: Bearer <JWT>`
- 路径参数: 无
- 查询参数: 无
- 请求体: 无
- 响应:
  - 与登录接口相同

### 3.2 用户模块

#### 4. `GET /admin/users`

- 作用: 获取用户列表
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data[]` 为 `UserListItemResponse`
  - `data[].id`: `long`
  - `data[].username`: `string`
  - `data[].nickname`: `string`
  - `data[].roleCode`: `string`
  - `data[].status`: `string`
  - `data[].email`: `string`
  - `data[].phone`: `string`
  - `data[].balance`: `decimal`
  - `data[].lastLoginAt`: `datetime`
  - `data[].createdAt`: `datetime`

#### 5. `GET /admin/users/{userId}`

- 作用: 获取单个用户详情
- 鉴权: `Authorization: Bearer <JWT>`
- 路径参数:
  - `userId`: `long`
- 响应:
  - `data` 字段结构与 `UserListItemResponse` 相同

#### 6. `POST /admin/users`

- 作用: 创建用户
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `username`: `string`，必填
  - `password`: `string`，必填
  - `nickname`: `string`，选填
  - `email`: `string`，选填
  - `phone`: `string`，选填
  - `roleCode`: `string`，选填
  - `initialBalance`: `decimal`，选填，最小 `0`
- 响应:
  - `success`: `boolean`
  - `message`: `string`
  - `data`: `null`

#### 7. `PUT /admin/users/{userId}`

- 作用: 更新用户资料
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 路径参数:
  - `userId`: `long`
- 请求体:
  - `nickname`: `string`，选填
  - `email`: `string`，选填
  - `phone`: `string`，选填
  - `roleCode`: `string`，选填
  - `status`: `string`，选填
  - `password`: `string`，选填
- 响应:
  - `data`: `null`

#### 8. `PUT /admin/users/{userId}/status`

- 作用: 修改用户状态
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 路径参数:
  - `userId`: `long`
- 请求体:
  - `status`: `string`，必填
- 响应:
  - `data`: `null`

#### 9. `DELETE /admin/users/{userId}`

- 作用: 删除用户
- 鉴权: `Authorization: Bearer <JWT>`
- 路径参数:
  - `userId`: `long`
- 响应:
  - `data`: `null`

#### 10. `POST /admin/users/recharge`

- 作用: 给用户钱包充值
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `userId`: `long`，必填
  - `amount`: `decimal`，必填，最小 `0.01`
  - `remark`: `string`，选填
- 响应:
  - `data`: `null`

### 3.3 API Key 模块

#### 11. `GET /admin/api-keys`

- 作用: 获取 API Key 列表
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data[]` 为 `ApiKeyListItemResponse`
  - `data[].id`: `long`
  - `data[].userId`: `long`
  - `data[].username`: `string`
  - `data[].name`: `string`
  - `data[].accessKey`: `string`
  - `data[].status`: `string`
  - `data[].modelGroupId`: `long`
  - `data[].modelGroupName`: `string`
  - `data[].totalQuota`: `decimal`
  - `data[].usedQuota`: `decimal`
  - `data[].expiresAt`: `datetime`
  - `data[].lastUsedAt`: `datetime`
  - `data[].createdAt`: `datetime`

#### 12. `POST /admin/api-keys`

- 作用: 创建 API Key
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `userId`: `long`，必填
  - `name`: `string`，必填
  - `modelGroupId`: `long`，选填
  - `expiresAt`: `string`，选填
  - `remark`: `string`，选填
- 响应:
  - `data.id`: `long`
  - `data.plainTextKey`: `string`
- 说明:
  - `plainTextKey` 只会在创建时返回一次

#### 13. `PUT /admin/api-keys/{id}/status`

- 作用: 修改 API Key 状态
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 路径参数:
  - `id`: `long`
- 请求体:
  - `status`: `string`，必填
- 响应:
  - `data`: `null`

### 3.4 渠道模块

#### 14. `GET /admin/providers`

- 作用: 获取渠道列表
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data[]` 为 `ProviderListItemResponse`
  - `data[].id`: `long`
  - `data[].providerCode`: `string`
  - `data[].providerName`: `string`
  - `data[].baseUrl`: `string`
  - `data[].providerType`: `string`
  - `data[].status`: `string`
  - `data[].priorityNo`: `int`
  - `data[].timeoutMs`: `int`
  - `data[].tokenCount`: `int`
  - `data[].createdAt`: `datetime`

#### 15. `POST /admin/providers`

- 作用: 创建渠道与默认 token
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `providerCode`: `string`，必填
  - `providerName`: `string`，必填
  - `baseUrl`: `string`，必填
  - `providerType`: `string`，选填
  - `priorityNo`: `int`，选填
  - `timeoutMs`: `int`，选填
  - `remark`: `string`，选填
  - `tokenName`: `string`，选填
  - `tokenValue`: `string`，选填
  - `weightNo`: `int`，选填
  - `rpmLimit`: `int`，选填
  - `tpmLimit`: `int`，选填
- 响应:
  - `data`: `null`

#### 16. `PUT /admin/providers/{id}/status`

- 作用: 修改渠道状态
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 路径参数:
  - `id`: `long`
- 请求体:
  - `status`: `string`，必填
- 响应:
  - `data`: `null`

### 3.5 模型模块

#### 17. `GET /admin/models`

- 作用: 获取模型列表
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data[]` 为 `ModelListItemResponse`
  - `data[].id`: `long`
  - `data[].modelCode`: `string`
  - `data[].modelName`: `string`
  - `data[].modelType`: `string`
  - `data[].billingType`: `string`
  - `data[].promptPrice`: `decimal`
  - `data[].completionPrice`: `decimal`
  - `data[].requestPrice`: `decimal`
  - `data[].multiplier`: `decimal`
  - `data[].isPublic`: `int`
  - `data[].status`: `string`
  - `data[].providerId`: `long`
  - `data[].providerName`: `string`
  - `data[].providerType`: `string`
  - `data[].upstreamModel`: `string`
  - `data[].createdAt`: `datetime`

#### 18. `GET /admin/models/upstream`

- 作用: 拉取指定渠道的上游模型
- 鉴权: `Authorization: Bearer <JWT>`
- 查询参数:
  - `providerId`: `long`，必填
- 响应:
  - `data[]` 为 `UpstreamModelOptionResponse`
  - `data[].id`: `string`
  - `data[].displayName`: `string`
  - `data[].ownedBy`: `string`
  - `data[].providerType`: `string`

#### 19. `POST /admin/models`

- 作用: 创建模型
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `modelCode`: `string`，必填
  - `modelName`: `string`，必填
  - `modelType`: `string`，选填
  - `billingType`: `string`，选填
  - `promptPrice`: `decimal`，选填
  - `completionPrice`: `decimal`，选填
  - `requestPrice`: `decimal`，选填
  - `imagePrice`: `decimal`，选填
  - `multiplier`: `decimal`，选填
  - `isPublic`: `boolean`，选填
  - `providerId`: `long`，必填
  - `upstreamModel`: `string`，必填
- 响应:
  - `data`: `null`

#### 20. `PUT /admin/models/{id}`

- 作用: 更新模型
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 路径参数:
  - `id`: `long`
- 请求体:
  - `modelName`: `string`，必填
  - `modelType`: `string`，选填
  - `billingType`: `string`，选填
  - `promptPrice`: `decimal`，选填
  - `completionPrice`: `decimal`，选填
  - `requestPrice`: `decimal`，选填
  - `multiplier`: `decimal`，选填
  - `isPublic`: `boolean`，选填
  - `providerId`: `long`，必填
  - `upstreamModel`: `string`，必填
- 响应:
  - `data`: `null`

#### 21. `DELETE /admin/models/{id}`

- 作用: 删除模型
- 鉴权: `Authorization: Bearer <JWT>`
- 路径参数:
  - `id`: `long`
- 响应:
  - `data`: `null`

#### 22. `POST /admin/models/import`

- 作用: 批量导入上游模型
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `providerId`: `long`，必填
  - `upstreamModels`: `string[]`，必填
  - `promptPrice`: `decimal`，选填
  - `completionPrice`: `decimal`，选填
  - `multiplier`: `decimal`，选填
  - `isPublic`: `boolean`，选填
- 响应:
  - `data.importedCount`: `int`
  - `data.skippedCount`: `int`
  - `data.importedModels`: `string[]`
  - `data.skippedModels`: `string[]`

#### 23. `PUT /admin/models/{id}/status`

- 作用: 修改模型状态
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 路径参数:
  - `id`: `long`
- 请求体:
  - `status`: `string`，必填
- 响应:
  - `data`: `null`

### 3.6 请求日志模块

#### 24. `GET /admin/request-logs`

- 作用: 获取最近请求日志
- 鉴权: `Authorization: Bearer <JWT>`
- 查询参数:
  - `limit`: `int`，选填，默认 `20`，范围 `1~100`
- 响应:
  - `data[]` 为 `RequestLogItemResponse`
  - `data[].requestId`: `string`
  - `data[].username`: `string`
  - `data[].modelCode`: `string`
  - `data[].upstreamModel`: `string`
  - `data[].statusCode`: `int`
  - `data[].latencyMs`: `int`
  - `data[].promptTokens`: `int`
  - `data[].completionTokens`: `int`
  - `data[].totalTokens`: `int`
  - `data[].userAmount`: `decimal`
  - `data[].costAmount`: `decimal`
  - `data[].success`: `int`
  - `data[].createdAt`: `datetime`

### 3.7 仪表盘模块

#### 25. `GET /admin/dashboard/overview`

- 作用: 获取概览统计
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data.userCount`: `long`
  - `data.apiKeyCount`: `long`
  - `data.providerCount`: `long`
  - `data.modelCount`: `long`
  - `data.requestCountToday`: `long`
  - `data.totalTokensToday`: `long`
  - `data.totalTokens7d`: `long`
  - `data.rechargeAmountToday`: `decimal`
  - `data.consumeAmountToday`: `decimal`
  - `data.walletBalanceTotal`: `decimal`

#### 26. `GET /admin/dashboard/trend`

- 作用: 获取趋势数据
- 鉴权: `Authorization: Bearer <JWT>`
- 查询参数:
  - `days`: `int`，选填，默认 `7`，范围 `1~30`
- 响应:
  - `data[]` 为 `DashboardTrendPointResponse`
  - `data[].statDate`: `date`
  - `data[].requestCount`: `long`
  - `data[].successCount`: `long`
  - `data[].totalTokens`: `long`
  - `data[].userAmount`: `decimal`
  - `data[].costAmount`: `decimal`

#### 27. `GET /admin/dashboard/model-stats`

- 作用: 获取模型统计
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data[]` 为 `DashboardModelStatResponse`
  - `data[].modelCode`: `string`
  - `data[].upstreamModels`: `string`
  - `data[].requestCount`: `long`
  - `data[].totalTokens`: `long`
  - `data[].avgLatencyMs`: `double`
  - `data[].totalLatencyMs`: `long`
  - `data[].successRate`: `double`
  - `data[].userAmount`: `decimal`

### 3.8 系统模块

#### 28. `GET /admin/system/health`

- 作用: 健康检查
- 鉴权: 无
- 响应:
  - `data.status`: `string`
  - `data.service`: `string`
  - `data.timestamp`: `datetime`

#### 29. `GET /admin/system/sse-protocol`

- 作用: 返回 SSE 协议说明
- 鉴权: 无
- 响应:
  - `data.version`: `string`
  - `data.transport`: `string`
  - `data.format`: `string`
  - `data.events`: `object[]`
  - `data.example_flow`: `string[]`

### 3.9 套餐/模型访问模块

#### 30. `GET /admin/model-access/summary`

- 作用: 获取当前用户套餐摘要、分组和额度信息
- 鉴权: `Authorization: Bearer <JWT>`
- 响应:
  - `data.packageRestrictionEnabled`: `boolean`
  - `data.packageStatus`: `string`
  - `data.packageStatusText`: `string`
  - `data.activeGroupId`: `long`
  - `data.activeGroupCode`: `string`
  - `data.activeGroupName`: `string`
  - `data.packagePrice`: `decimal`
  - `data.dailyQuota`: `decimal`
  - `data.weeklyQuota`: `decimal`
  - `data.monthlyQuota`: `decimal`
  - `data.dailyUsed`: `decimal`
  - `data.weeklyUsed`: `decimal`
  - `data.monthlyUsed`: `decimal`
  - `data.expiresAt`: `datetime`
  - `data.remainingDays`: `long`
  - `data.groups[]`: `ModelGroupOptionResponse`

`data.groups[]` 字段:

- `id`: `long`
- `groupCode`: `string`
- `groupName`: `string`
- `salePrice`: `decimal`
- `packageDays`: `int`
- `dailyQuota`: `decimal`
- `weeklyQuota`: `decimal`
- `monthlyQuota`: `decimal`
- `modelCount`: `int`
- `purchased`: `boolean`
- `active`: `boolean`
- `expiresAt`: `datetime`
- `remainingDays`: `long`
- `dailyUsed`: `decimal`
- `weeklyUsed`: `decimal`
- `monthlyUsed`: `decimal`
- `packageStatus`: `string`
- `packageStatusText`: `string`
- `remark`: `string`

#### 31. `POST /admin/model-access/purchase`

- 作用: 购买模型套餐
- 鉴权: `Authorization: Bearer <JWT>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `groupId`: `long`，必填
- 响应:
  - 返回结构与 `GET /admin/model-access/summary` 相同

#### 32. `DELETE /admin/model-access/{groupId}`

- 作用: 删除或停用当前用户已购套餐分组
- 鉴权: `Authorization: Bearer <JWT>`
- 路径参数:
  - `groupId`: `long`
- 响应:
  - `data`: `null`

## 4. 网关对外接口

### 4.1 模型列表

#### 33. `GET /v1/models`

- 作用: 获取可用模型列表
- 鉴权: `Authorization: Bearer <API_KEY>`
- 响应:
  - `object`: `string`
  - `data[]`: `object`
  - `data[].id`: `string`
  - `data[].object`: `string`
  - `data[].created`: `long`
  - `data[].owned_by`: `string`

#### 34. `GET /models`

- 作用: `/v1/models` 的兼容别名
- 鉴权: `Authorization: Bearer <API_KEY>`
- 响应:
  - 与 `GET /v1/models` 相同

### 4.2 OpenAI Chat Completions

#### 35. `POST /v1/chat/completions`

- 作用: OpenAI Chat Completions 兼容接口
- 鉴权: `Authorization: Bearer <API_KEY>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `model`: `string`，必填
  - `messages`: `array`，必填
  - `stream`: `boolean`，选填
  - `temperature`: `number`，选填
  - `max_tokens`: `int`，选填
- 响应:
  - 非流式: OpenAI Chat Completions JSON
  - 流式: `text/event-stream`
  - 常见字段:
    - `id`
    - `object`
    - `created`
    - `model`
    - `choices`
    - `usage`

#### 36. `POST /chat/completions`

- 作用: `/v1/chat/completions` 的兼容别名
- 鉴权: `Authorization: Bearer <API_KEY>`
- 请求体与响应:
  - 与 `POST /v1/chat/completions` 相同

### 4.3 OpenAI Responses

#### 37. `POST /v1/responses`

- 作用: OpenAI Responses 接口
- 鉴权: `Authorization: Bearer <API_KEY>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `model`: `string`，必填
  - `input`: `string | array`，必填
  - `stream`: `boolean`，选填
  - `instructions`: `string`，选填
  - `tools`: `array`，选填
- 响应:
  - 非流式: Responses JSON
  - 流式: `text/event-stream`
  - 常见字段:
    - `id`
    - `object`
    - `status`
    - `model`
    - `output`
    - `usage`

#### 38. `POST /responses`

- 作用: `/v1/responses` 的兼容别名
- 鉴权: `Authorization: Bearer <API_KEY>`
- 请求体与响应:
  - 与 `POST /v1/responses` 相同

### 4.4 Anthropic Messages

#### 39. `POST /v1/messages`

- 作用: Anthropic Messages 接口
- 鉴权:
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-api-key: <API_KEY>`
- 请求头:
  - `Content-Type: application/json`
- 请求体:
  - `model`: `string`，必填
  - `messages`: `array`，必填
  - `max_tokens`: `int`，常见必填
  - `stream`: `boolean`，选填
  - `system`: `string | array`，选填
- 响应:
  - 非流式: Anthropic Messages JSON
  - 流式: `text/event-stream`
  - 常见字段:
    - `id`
    - `type`
    - `role`
    - `model`
    - `content`
    - `usage`

#### 40. `POST /messages`

- 作用: `/v1/messages` 的兼容别名
- 鉴权:
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-api-key: <API_KEY>`
- 请求体与响应:
  - 与 `POST /v1/messages` 相同

#### 41. `POST /v1/v1/messages`

- 作用: 重复前缀场景兼容别名
- 鉴权:
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-api-key: <API_KEY>`
- 请求体与响应:
  - 与 `POST /v1/messages` 相同

## 5. DTO 对照

### 5.1 认证相关

- `AdminLoginRequest`: `username`, `password`
- `AdminLoginResponse`: `token`, `userId`, `username`, `nickname`, `roleCode`, `balance`
- `UserRegisterRequest`: `username`, `password`, `nickname`, `email`, `phone`

### 5.2 用户相关

- `UserCreateRequest`: `username`, `password`, `nickname`, `email`, `phone`, `roleCode`, `initialBalance`
- `UserUpdateRequest`: `nickname`, `email`, `phone`, `roleCode`, `status`, `password`
- `UserStatusUpdateRequest`: `status`
- `UserListItemResponse`: `id`, `username`, `nickname`, `roleCode`, `status`, `email`, `phone`, `balance`, `lastLoginAt`, `createdAt`
- `WalletRechargeRequest`: `userId`, `amount`, `remark`

### 5.3 API Key 相关

- `ApiKeyCreateRequest`: `userId`, `name`, `modelGroupId`, `expiresAt`, `remark`
- `ApiKeyCreateResponse`: `id`, `plainTextKey`
- `ApiKeyStatusUpdateRequest`: `status`
- `ApiKeyListItemResponse`: `id`, `userId`, `username`, `name`, `accessKey`, `status`, `modelGroupId`, `modelGroupName`, `totalQuota`, `usedQuota`, `expiresAt`, `lastUsedAt`, `createdAt`

### 5.4 渠道相关

- `ProviderCreateRequest`: `providerCode`, `providerName`, `baseUrl`, `providerType`, `priorityNo`, `timeoutMs`, `remark`, `tokenName`, `tokenValue`, `weightNo`, `rpmLimit`, `tpmLimit`
- `ProviderStatusUpdateRequest`: `status`
- `ProviderListItemResponse`: `id`, `providerCode`, `providerName`, `baseUrl`, `providerType`, `status`, `priorityNo`, `timeoutMs`, `tokenCount`, `createdAt`

### 5.5 模型相关

- `ModelCreateRequest`: `modelCode`, `modelName`, `modelType`, `billingType`, `promptPrice`, `completionPrice`, `requestPrice`, `imagePrice`, `multiplier`, `isPublic`, `providerId`, `upstreamModel`
- `ModelUpdateRequest`: `modelName`, `modelType`, `billingType`, `promptPrice`, `completionPrice`, `requestPrice`, `multiplier`, `isPublic`, `providerId`, `upstreamModel`
- `ModelStatusUpdateRequest`: `status`
- `ModelBatchImportRequest`: `providerId`, `upstreamModels`, `promptPrice`, `completionPrice`, `multiplier`, `isPublic`
- `ModelBatchImportResponse`: `importedCount`, `skippedCount`, `importedModels`, `skippedModels`
- `UpstreamModelOptionResponse`: `id`, `displayName`, `ownedBy`, `providerType`
- `ModelListItemResponse`: `id`, `modelCode`, `modelName`, `modelType`, `billingType`, `promptPrice`, `completionPrice`, `requestPrice`, `multiplier`, `isPublic`, `status`, `providerId`, `providerName`, `providerType`, `upstreamModel`, `createdAt`

### 5.6 日志与统计相关

- `RequestLogItemResponse`: `requestId`, `username`, `modelCode`, `upstreamModel`, `statusCode`, `latencyMs`, `promptTokens`, `completionTokens`, `totalTokens`, `userAmount`, `costAmount`, `success`, `createdAt`
- `DashboardOverviewResponse`: `userCount`, `apiKeyCount`, `providerCount`, `modelCount`, `requestCountToday`, `totalTokensToday`, `totalTokens7d`, `rechargeAmountToday`, `consumeAmountToday`, `walletBalanceTotal`
- `DashboardTrendPointResponse`: `statDate`, `requestCount`, `successCount`, `totalTokens`, `userAmount`, `costAmount`
- `DashboardModelStatResponse`: `modelCode`, `upstreamModels`, `requestCount`, `totalTokens`, `avgLatencyMs`, `totalLatencyMs`, `successRate`, `userAmount`

### 5.7 套餐相关

- `PurchaseModelPackageRequest`: `groupId`
- `ModelAccessSummaryResponse`: `packageRestrictionEnabled`, `packageStatus`, `packageStatusText`, `activeGroupId`, `activeGroupCode`, `activeGroupName`, `packagePrice`, `dailyQuota`, `weeklyQuota`, `monthlyQuota`, `dailyUsed`, `weeklyUsed`, `monthlyUsed`, `expiresAt`, `remainingDays`, `groups`
- `ModelGroupOptionResponse`: `id`, `groupCode`, `groupName`, `salePrice`, `packageDays`, `dailyQuota`, `weeklyQuota`, `monthlyQuota`, `modelCount`, `purchased`, `active`, `expiresAt`, `remainingDays`, `dailyUsed`, `weeklyUsed`, `monthlyUsed`, `packageStatus`, `packageStatusText`, `remark`

## 6. 统计修正记录

- 旧版 `txt` 统计漏掉了 `DELETE /admin/model-access/{groupId}`
- 已修正后:
  - 逻辑接口数: `36`
  - 实际可访问 URL 数: `41`

## 7. 来源文件

- `src/main/java/com/zxw/modules/auth/controller/AdminAuthController.java`
- `src/main/java/com/zxw/modules/user/controller/AdminUserController.java`
- `src/main/java/com/zxw/modules/apikey/controller/AdminApiKeyController.java`
- `src/main/java/com/zxw/modules/provider/controller/AdminProviderController.java`
- `src/main/java/com/zxw/modules/model/controller/AdminModelController.java`
- `src/main/java/com/zxw/modules/request/controller/AdminRequestLogController.java`
- `src/main/java/com/zxw/modules/dashboard/controller/AdminDashboardController.java`
- `src/main/java/com/zxw/modules/system/controller/AdminSystemController.java`
- `src/main/java/com/zxw/modules/access/controller/AdminModelAccessController.java`
- `src/main/java/com/zxw/modules/gateway/controller/GatewayChatController.java`
