# 项目接口与返回参数统计

生成时间：2026-04-29

## 1. 概览

- 项目路径：`e:\project-ai\Test`
- 服务端口：`9988`
- 配置文件：`src/main/resources/application.yml`
- 后台管理接口默认前缀：`/admin`
- 网关对外接口前缀：`/v1`、`/v1beta`、兼容别名路径
- 逻辑接口数：`46`
- 实际可访问 URL 数：`53`

### 1.1 后台接口统一返回结构

除网关兼容接口外，后台接口统一返回：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

字段说明：

- `success`：是否成功
- `message`：提示信息
- `data`：业务数据，可能是对象、数组或 `null`

### 1.2 后台接口统一错误结构

```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

### 1.3 鉴权规则

后台接口默认要求：

```http
Authorization: Bearer <JWT>
```

免 JWT 的后台接口：

- `POST /admin/auth/login`
- `POST /admin/auth/register/code`
- `POST /admin/auth/register`
- `GET /admin/system/health`
- `GET /admin/system/sse-protocol`

网关接口默认要求：

```http
Authorization: Bearer <API_KEY>
```

补充说明：

- `POST /v1/messages`、`POST /messages`、`POST /v1/v1/messages` 还支持 `x-api-key: <API_KEY>`
- Gemini 兼容接口还支持 `x-goog-api-key: <API_KEY>` 或查询参数 `?key=<API_KEY>`

### 1.4 时间与金额字段说明

- `datetime`：一般为 `LocalDateTime`
- `date`：一般为 `LocalDate`
- `decimal`：一般为 `BigDecimal`

## 2. 后台管理接口

### 2.1 认证模块

#### 1. `POST /admin/auth/login`

- 说明：后台登录
- 鉴权：否
- 请求体：`AdminLoginRequest`
  - `username`: `string`，必填
  - `password`: `string`，必填
- 成功响应：`ApiResponse<AdminLoginResponse>`
  - `data.token`: `string`
  - `data.userId`: `long`
  - `data.username`: `string`
  - `data.nickname`: `string`
  - `data.roleCode`: `string`
  - `data.balance`: `decimal`

#### 2. `POST /admin/auth/register/code`

- 说明：发送注册验证码
- 鉴权：否
- 请求体：`VerificationCodeSendRequest`
  - `email`: `string`，必填，仅允许 `@qq.com`
- 成功响应：`ApiResponse<VerificationCodeSendResponse>`
  - `data.email`: `string`
  - `data.expireSeconds`: `long`
  - `data.code`: `string`
- 备注：
  - `code` 是否回传受配置 `app.auth.verification-code.return-code-in-response` 控制
  - 当前 `application.yml` 默认值为 `true`

#### 3. `POST /admin/auth/register`

- 说明：用户注册并自动登录
- 鉴权：否
- 请求体：`UserRegisterRequest`
  - `username`: `string`，必填
  - `password`: `string`，必填
  - `nickname`: `string`
  - `email`: `string`，必填，仅允许 `@qq.com`
  - `phone`: `string`
  - `verificationCode`: `string`，必填，6 位数字
- 成功响应：`ApiResponse<AdminLoginResponse>`
  - `data.token`: `string`
  - `data.userId`: `long`
  - `data.username`: `string`
  - `data.nickname`: `string`
  - `data.roleCode`: `string`
  - `data.balance`: `decimal`

#### 4. `GET /admin/auth/me`

- 说明：获取当前登录用户信息
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<AdminLoginResponse>`
  - `data.token`: `string`
  - `data.userId`: `long`
  - `data.username`: `string`
  - `data.nickname`: `string`
  - `data.roleCode`: `string`
  - `data.balance`: `decimal`

### 2.2 用户模块

#### 5. `GET /admin/users`

- 说明：查询用户列表
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<UserListItemResponse>>`
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

#### 6. `GET /admin/users/{userId}`

- 说明：查询用户详情
- 鉴权：`Bearer JWT`
- 路径参数：
  - `userId`: `long`
- 成功响应：`ApiResponse<UserListItemResponse>`
  - `data.id`: `long`
  - `data.username`: `string`
  - `data.nickname`: `string`
  - `data.roleCode`: `string`
  - `data.status`: `string`
  - `data.email`: `string`
  - `data.phone`: `string`
  - `data.balance`: `decimal`
  - `data.lastLoginAt`: `datetime`
  - `data.createdAt`: `datetime`

#### 7. `POST /admin/users`

- 说明：创建用户
- 鉴权：`Bearer JWT`
- 请求体：`UserCreateRequest`
  - `username`: `string`，必填
  - `password`: `string`，必填
  - `nickname`: `string`
  - `email`: `string`
  - `phone`: `string`
  - `roleCode`: `string`
  - `initialBalance`: `decimal`，最小 `0.00`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 8. `PUT /admin/users/{userId}`

- 说明：更新用户信息
- 鉴权：`Bearer JWT`
- 路径参数：
  - `userId`: `long`
- 请求体：`UserUpdateRequest`
  - `nickname`: `string`
  - `email`: `string`
  - `phone`: `string`
  - `roleCode`: `string`
  - `status`: `string`
  - `password`: `string`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 9. `PUT /admin/users/{userId}/status`

- 说明：更新用户状态
- 鉴权：`Bearer JWT`
- 路径参数：
  - `userId`: `long`
- 请求体：`UserStatusUpdateRequest`
  - `status`: `string`，必填
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 10. `DELETE /admin/users/{userId}`

- 说明：删除用户
- 鉴权：`Bearer JWT`
- 路径参数：
  - `userId`: `long`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 11. `POST /admin/users/recharge`

- 说明：用户充值
- 鉴权：`Bearer JWT`
- 请求体：`WalletRechargeRequest`
  - `userId`: `long`，必填
  - `amount`: `decimal`，必填，最小 `0.01`
  - `remark`: `string`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

### 2.3 API Key 模块

#### 12. `GET /admin/api-keys`

- 说明：查询 API Key 列表
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<ApiKeyListItemResponse>>`
  - `data[].id`: `long`
  - `data[].userId`: `long`
  - `data[].username`: `string`
  - `data[].name`: `string`
  - `data[].accessKey`: `string`
  - `data[].status`: `string`
  - `data[].modelPackageId`: `long`
  - `data[].modelPackageName`: `string`
  - `data[].modelGroupId`: `long`
  - `data[].modelGroupName`: `string`
  - `data[].totalQuota`: `decimal`
  - `data[].usedQuota`: `decimal`
  - `data[].expiresAt`: `datetime`
  - `data[].lastUsedAt`: `datetime`
  - `data[].createdAt`: `datetime`

#### 13. `POST /admin/api-keys`

- 说明：创建 API Key
- 鉴权：`Bearer JWT`
- 请求体：`ApiKeyCreateRequest`
  - `userId`: `long`，必填
  - `name`: `string`，必填
  - `modelPackageId`: `long`
  - `modelGroupId`: `long`
  - `expiresAt`: `string`
  - `remark`: `string`
- 成功响应：`ApiResponse<ApiKeyCreateResponse>`
  - `data.id`: `long`
  - `data.plainTextKey`: `string`
- 备注：
  - `plainTextKey` 通常只会在创建时返回一次

#### 14. `PUT /admin/api-keys/{id}/status`

- 说明：修改 API Key 状态
- 鉴权：`Bearer JWT`
- 路径参数：
  - `id`: `long`
- 请求体：`ApiKeyStatusUpdateRequest`
  - `status`: `string`，必填
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 15. `DELETE /admin/api-keys/{id}`

- 说明：删除 API Key
- 鉴权：`Bearer JWT`
- 路径参数：
  - `id`: `long`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

### 2.4 渠道模块

#### 16. `GET /admin/providers`

- 说明：查询渠道列表
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<ProviderListItemResponse>>`
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

#### 17. `POST /admin/providers`

- 说明：创建渠道
- 鉴权：`Bearer JWT`
- 请求体：`ProviderCreateRequest`
  - `providerCode`: `string`，必填
  - `providerName`: `string`，必填
  - `baseUrl`: `string`，必填
  - `providerType`: `string`
  - `priorityNo`: `int`
  - `timeoutMs`: `int`
  - `remark`: `string`
  - `tokenName`: `string`
  - `tokenValue`: `string`
  - `weightNo`: `int`
  - `rpmLimit`: `int`
  - `tpmLimit`: `int`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 18. `PUT /admin/providers/{id}/status`

- 说明：修改渠道状态
- 鉴权：`Bearer JWT`
- 路径参数：
  - `id`: `long`
- 请求体：`ProviderStatusUpdateRequest`
  - `status`: `string`，必填
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

### 2.5 模型模块

#### 19. `GET /admin/models`

- 说明：查询模型列表
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<ModelListItemResponse>>`
  - `data[].id`: `long`
  - `data[].bindingId`: `long`
  - `data[].modelCode`: `string`
  - `data[].modelName`: `string`
  - `data[].modelType`: `string`
  - `data[].billingType`: `string`
  - `data[].promptPrice`: `decimal`
  - `data[].cachedPromptPrice`: `decimal`
  - `data[].completionPrice`: `decimal`
  - `data[].requestPrice`: `decimal`
  - `data[].multiplier`: `decimal`
  - `data[].isPublic`: `int`
  - `data[].status`: `string`
  - `data[].groupId`: `long`
  - `data[].groupCode`: `string`
  - `data[].groupName`: `string`
  - `data[].providerId`: `long`
  - `data[].providerName`: `string`
  - `data[].providerType`: `string`
  - `data[].upstreamModel`: `string`
  - `data[].createdAt`: `datetime`

#### 20. `GET /admin/models/upstream`

- 说明：拉取指定渠道的上游模型列表
- 鉴权：`Bearer JWT`
- 查询参数：
  - `providerId`: `long`，必填
- 成功响应：`ApiResponse<List<UpstreamModelOptionResponse>>`
  - `data[].id`: `string`
  - `data[].displayName`: `string`
  - `data[].ownedBy`: `string`
  - `data[].providerType`: `string`

#### 21. `POST /admin/models`

- 说明：创建模型
- 鉴权：`Bearer JWT`
- 请求体：`ModelCreateRequest`
  - `modelCode`: `string`，必填
  - `modelName`: `string`，必填
  - `modelType`: `string`
  - `billingType`: `string`
  - `promptPrice`: `decimal`
  - `cachedPromptPrice`: `decimal`
  - `completionPrice`: `decimal`
  - `requestPrice`: `decimal`
  - `imagePrice`: `decimal`
  - `multiplier`: `decimal`
  - `isPublic`: `boolean`
  - `groupId`: `long`，必填
  - `providerId`: `long`，必填
  - `upstreamModel`: `string`，必填
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 22. `PUT /admin/models/{id}`

- 说明：更新模型
- 鉴权：`Bearer JWT`
- 路径参数：
  - `id`: `long`
- 请求体：`ModelUpdateRequest`
  - `bindingId`: `long`
  - `modelName`: `string`，必填
  - `modelType`: `string`
  - `billingType`: `string`
  - `promptPrice`: `decimal`
  - `cachedPromptPrice`: `decimal`
  - `completionPrice`: `decimal`
  - `requestPrice`: `decimal`
  - `multiplier`: `decimal`
  - `isPublic`: `boolean`
  - `groupId`: `long`，必填
  - `providerId`: `long`，必填
  - `upstreamModel`: `string`，必填
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 23. `DELETE /admin/models/{id}`

- 说明：删除模型
- 鉴权：`Bearer JWT`
- 路径参数：
  - `id`: `long`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 24. `POST /admin/models/import`

- 说明：批量导入上游模型
- 鉴权：`Bearer JWT`
- 请求体：`ModelBatchImportRequest`
  - `groupId`: `long`，必填
  - `providerId`: `long`，必填
  - `upstreamModels`: `string[]`，必填
  - `promptPrice`: `decimal`
  - `cachedPromptPrice`: `decimal`
  - `completionPrice`: `decimal`
  - `multiplier`: `decimal`
  - `isPublic`: `boolean`
- 成功响应：`ApiResponse<ModelBatchImportResponse>`
  - `data.importedCount`: `int`
  - `data.skippedCount`: `int`
  - `data.importedModels`: `string[]`
  - `data.skippedModels`: `string[]`

#### 25. `PUT /admin/models/{id}/status`

- 说明：修改模型状态
- 鉴权：`Bearer JWT`
- 路径参数：
  - `id`: `long`
- 请求体：`ModelStatusUpdateRequest`
  - `status`: `string`，必填
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

### 2.6 请求日志模块

#### 26. `GET /admin/request-logs`

- 说明：查询最近请求日志
- 鉴权：`Bearer JWT`
- 查询参数：
  - `limit`: `int`，默认 `20`，范围 `1~100`
- 成功响应：`ApiResponse<List<RequestLogItemResponse>>`
  - `data[].requestId`: `string`
  - `data[].username`: `string`
  - `data[].modelCode`: `string`
  - `data[].upstreamModel`: `string`
  - `data[].packageName`: `string`
  - `data[].multiplier`: `decimal`
  - `data[].statusCode`: `int`
  - `data[].latencyMs`: `int`
  - `data[].promptTokens`: `int`
  - `data[].completionTokens`: `int`
  - `data[].totalTokens`: `int`
  - `data[].cachedPromptTokens`: `int`
  - `data[].userAmount`: `decimal`
  - `data[].costAmount`: `decimal`
  - `data[].success`: `int`
  - `data[].createdAt`: `datetime`

### 2.7 仪表盘模块

#### 27. `GET /admin/dashboard/overview`

- 说明：仪表盘总览
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<DashboardOverviewResponse>`
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

#### 28. `GET /admin/dashboard/trend`

- 说明：趋势统计
- 鉴权：`Bearer JWT`
- 查询参数：
  - `days`: `int`，默认 `7`，范围 `1~30`
- 成功响应：`ApiResponse<List<DashboardTrendPointResponse>>`
  - `data[].statDate`: `date`
  - `data[].requestCount`: `long`
  - `data[].successCount`: `long`
  - `data[].totalTokens`: `long`
  - `data[].userAmount`: `decimal`
  - `data[].costAmount`: `decimal`

#### 29. `GET /admin/dashboard/model-stats`

- 说明：模型统计
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<DashboardModelStatResponse>>`
  - `data[].modelCode`: `string`
  - `data[].upstreamModels`: `string`
  - `data[].requestCount`: `long`
  - `data[].totalTokens`: `long`
  - `data[].avgLatencyMs`: `double`
  - `data[].totalLatencyMs`: `long`
  - `data[].successRate`: `double`
  - `data[].userAmount`: `decimal`

### 2.8 系统模块

#### 30. `GET /admin/system/health`

- 说明：健康检查
- 鉴权：否
- 成功响应：`ApiResponse<Map<String,Object>>`
  - `data.status`: `string`
  - `data.service`: `string`
  - `data.timestamp`: `datetime`

#### 31. `GET /admin/system/sse-protocol`

- 说明：返回系统 SSE 协议说明
- 鉴权：否
- 成功响应：`ApiResponse<Map<String,Object>>`
  - `data.version`: `string`
  - `data.transport`: `string`
  - `data.format`: `string`
  - `data.events`: `object[]`
  - `data.example_flow`: `string[]`
- `data.events[]` 常见字段：
  - `type`: `string`
  - `desc`: `string`
  - `fields`: `string[]`
  - `kind_values`: `string[]`
  - `item_types`: `string[]`

#### 32. `GET /admin/system/site-settings`

- 说明：获取站点设置
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<SiteSettingsResponse>`
  - `data.id`: `long`
  - `data.siteName`: `string`
  - `data.adminEmail`: `string`
  - `data.siteDescription`: `string`
  - `data.baseUrl`: `string`
  - `data.footerText`: `string`
  - `data.themeMode`: `string`
  - `data.updatedAt`: `datetime`

#### 33. `PUT /admin/system/site-settings`

- 说明：更新站点设置
- 鉴权：`Bearer JWT`
- 请求体：`SiteSettingsUpdateRequest`
  - `siteName`: `string`，必填，最大 `128`
  - `adminEmail`: `string`，必填，邮箱格式，最大 `128`
  - `siteDescription`: `string`，最大 `255`
  - `baseUrl`: `string`，必填，必须以 `http://` 或 `https://` 开头，最大 `255`
  - `footerText`: `string`，最大 `255`
  - `themeMode`: `string`，必填，仅支持 `LIGHT` 或 `DARK`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

### 2.9 套餐与模型访问模块

#### 34. `GET /admin/model-access/summary`

- 说明：获取当前用户套餐摘要
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<ModelAccessSummaryResponse>`
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
  - `data.groups`: `ModelGroupOptionResponse[]`

`data.groups[]` 字段：

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
- `systemPreset`: `boolean`

#### 35. `POST /admin/model-access/groups`

- 说明：创建套餐分组
- 鉴权：`Bearer JWT`
- 请求体：`ModelGroupCreateRequest`
  - `groupCode`: `string`，必填
  - `groupName`: `string`，必填
  - `salePrice`: `decimal`，必填，最小 `0.0000`
  - `packageDays`: `int`，必填
  - `dailyQuota`: `decimal`，必填
  - `weeklyQuota`: `decimal`，必填
  - `monthlyQuota`: `decimal`，必填
  - `remark`: `string`
- 成功响应：`ApiResponse<ModelAccessSummaryResponse>`
  - 返回字段与 `GET /admin/model-access/summary` 相同

#### 36. `POST /admin/model-access/purchase`

- 说明：购买套餐
- 鉴权：`Bearer JWT`
- 请求体：`PurchaseModelPackageRequest`
  - `groupId`: `long`，必填
- 成功响应：`ApiResponse<ModelAccessSummaryResponse>`
  - 返回字段与 `GET /admin/model-access/summary` 相同

#### 37. `GET /admin/model-access/purchases`

- 说明：查询套餐购买记录
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<ModelPackagePurchaseRecordResponse>>`
  - `data[].id`: `long`
  - `data[].userId`: `long`
  - `data[].username`: `string`
  - `data[].groupId`: `long`
  - `data[].groupCode`: `string`
  - `data[].groupName`: `string`
  - `data[].modelCount`: `int`
  - `data[].purchasePrice`: `decimal`
  - `data[].startAt`: `datetime`
  - `data[].expiresAt`: `datetime`
  - `data[].status`: `string`
  - `data[].createdAt`: `datetime`
  - `data[].active`: `boolean`
  - `data[].dailyQuota`: `decimal`
  - `data[].weeklyQuota`: `decimal`
  - `data[].monthlyQuota`: `decimal`
  - `data[].totalQuota`: `decimal`
  - `data[].dailyUsed`: `decimal`
  - `data[].weeklyUsed`: `decimal`
  - `data[].monthlyUsed`: `decimal`
  - `data[].totalUsed`: `decimal`
  - `data[].remainingDays`: `long`

#### 38. `GET /admin/model-access/wallet-transactions`

- 说明：查询钱包流水
- 鉴权：`Bearer JWT`
- 成功响应：`ApiResponse<List<WalletTransactionItemResponse>>`
  - `data[].id`: `long`
  - `data[].userId`: `long`
  - `data[].username`: `string`
  - `data[].walletId`: `long`
  - `data[].orderNo`: `string`
  - `data[].transactionType`: `string`
  - `data[].direction`: `string`
  - `data[].amount`: `decimal`
  - `data[].balanceBefore`: `decimal`
  - `data[].balanceAfter`: `decimal`
  - `data[].status`: `string`
  - `data[].descriptionText`: `string`
  - `data[].transactionDate`: `date`
  - `data[].createdAt`: `datetime`

#### 39. `DELETE /admin/model-access/{groupId}`

- 说明：停用套餐分组
- 鉴权：`Bearer JWT`
- 路径参数：
  - `groupId`: `long`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

#### 40. `DELETE /admin/model-access/purchases/{packageId}`

- 说明：删除已购买套餐
- 鉴权：`Bearer JWT`
- 路径参数：
  - `packageId`: `long`
- 成功响应：`ApiResponse<Void>`
  - `data`: `null`

## 3. 网关对外接口

说明：

- 以下接口不使用 `ApiResponse` 包装
- 返回结构以 OpenAI / Anthropic / Gemini 兼容协议为主
- 当 `stream=true` 时，多数接口返回 `text/event-stream`

### 3.1 模型列表

#### 41. `GET /v1/models`

- 说明：获取模型列表
- 鉴权：`Bearer API_KEY`
- 成功响应：
  - `object`: 固定为 `list`
  - `data[]`: 模型列表
  - `data[].id`: `string`
  - `data[].object`: 固定为 `model`
  - `data[].owned_by`: `string`
  - `data[].type`: `string`

#### 42. `GET /models`

- 说明：`/v1/models` 的兼容别名
- 鉴权：`Bearer API_KEY`
- 成功响应：
  - 与 `GET /v1/models` 相同

### 3.2 OpenAI Chat Completions 兼容接口

#### 43. `POST /v1/chat/completions`

- 说明：OpenAI Chat Completions 兼容接口
- 鉴权：`Bearer API_KEY`
- 请求体常见字段：
  - `model`: `string`，必填
  - `messages`: `array`，必填
  - `stream`: `boolean`
  - `temperature`: `number`
  - `max_tokens`: `int`
  - `top_p`: `number`
  - `tools`: `array`
- 成功响应：
  - 非流式：OpenAI Chat Completions 兼容 JSON
  - 流式：`text/event-stream`
  - 常见顶层字段：
    - `id`
    - `object`
    - `created`
    - `model`
    - `choices`
    - `usage`

#### 44. `POST /chat/completions`

- 说明：`/v1/chat/completions` 的兼容别名
- 鉴权：`Bearer API_KEY`
- 请求体与响应：
  - 与 `POST /v1/chat/completions` 相同

### 3.3 OpenAI Responses 兼容接口

#### 45. `POST /v1/responses`

- 说明：OpenAI Responses 兼容接口
- 鉴权：`Bearer API_KEY`
- 请求体常见字段：
  - `model`: `string`，必填
  - `input`: `string | array`，必填
  - `stream`: `boolean`
  - `instructions`: `string`
  - `tools`: `array`
- 成功响应：
  - 非流式：OpenAI Responses 兼容 JSON
  - 流式：`text/event-stream`
  - 常见顶层字段：
    - `id`
    - `object`
    - `status`
    - `model`
    - `output`
    - `usage`
- 备注：
  - 本项目在部分 Agent 场景下会输出扩展 SSE 事件
  - 相关事件说明可参考 `GET /admin/system/sse-protocol`

#### 46. `POST /responses`

- 说明：`/v1/responses` 的兼容别名
- 鉴权：`Bearer API_KEY`
- 请求体与响应：
  - 与 `POST /v1/responses` 相同

### 3.4 Anthropic Messages 兼容接口

#### 47. `POST /v1/messages`

- 说明：Anthropic Messages 兼容接口
- 鉴权：
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-api-key: <API_KEY>`
- 请求体常见字段：
  - `model`: `string`，必填
  - `messages`: `array`，必填
  - `max_tokens`: `int`
  - `stream`: `boolean`
  - `system`: `string | array`
- 成功响应：
  - 非流式：Anthropic Messages 兼容 JSON
  - 流式：`text/event-stream`
  - 常见顶层字段：
    - `id`
    - `type`
    - `role`
    - `model`
    - `content`
    - `usage`
    - `stop_reason`
    - `stop_sequence`

#### 48. `POST /messages`

- 说明：`/v1/messages` 的兼容别名
- 鉴权：
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-api-key: <API_KEY>`
- 请求体与响应：
  - 与 `POST /v1/messages` 相同

#### 49. `POST /v1/v1/messages`

- 说明：重复前缀场景兼容别名
- 鉴权：
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-api-key: <API_KEY>`
- 请求体与响应：
  - 与 `POST /v1/messages` 相同

### 3.5 Gemini 兼容接口

#### 50. `POST /v1beta/models/{model}:generateContent`

- 说明：Gemini `generateContent` 兼容接口
- 鉴权：
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-goog-api-key: <API_KEY>`
  - 或查询参数 `key`
- 路径参数：
  - `model`: `string`
- 请求体常见字段：
  - `contents[]`
  - `systemInstruction` / `system_instruction`
  - `generationConfig.temperature`
  - `generationConfig.topP`
  - `generationConfig.maxOutputTokens`
- 成功响应：
  - `candidates[]`
  - `candidates[].index`
  - `candidates[].finishReason`
  - `candidates[].content.role`
  - `candidates[].content.parts[]`
  - `candidates[].content.parts[].text`
  - `usageMetadata.promptTokenCount`
  - `usageMetadata.candidatesTokenCount`
  - `usageMetadata.totalTokenCount`
  - `modelVersion`

#### 51. `POST /v1/models/{model}:generateContent`

- 说明：同上，`v1` 版本路径
- 鉴权与返回：
  - 与 `POST /v1beta/models/{model}:generateContent` 相同

#### 52. `POST /v1beta/models/{model}:streamGenerateContent`

- 说明：Gemini 流式生成接口
- 鉴权：
  - `Authorization: Bearer <API_KEY>`
  - 或 `x-goog-api-key: <API_KEY>`
  - 或查询参数 `key`
- 路径参数：
  - `model`: `string`
- 查询参数：
  - `alt`: `string`
- 请求体常见字段：
  - 与 `generateContent` 相同
- 成功响应：
  - `text/event-stream`
  - 每个 `data:` 事件体常见字段：
    - `candidates[]`
    - `candidates[].index`
    - `candidates[].finishReason`
    - `candidates[].content.role`
    - `candidates[].content.parts[].text`
    - `modelVersion`
  - 结束事件示例：
    - `{"done":true,"modelVersion":"<model>"}`

#### 53. `POST /v1/models/{model}:streamGenerateContent`

- 说明：同上，`v1` 版本路径
- 鉴权与返回：
  - 与 `POST /v1beta/models/{model}:streamGenerateContent` 相同

## 4. 统计结论

- 后台逻辑接口：`40`
- 网关逻辑接口：`6`
- 逻辑接口总数：`46`
- 实际 URL 总数：`53`

## 5. 本次统计依据文件

- `src/main/java/com/zxw/common/api/ApiResponse.java`
- `src/main/java/com/zxw/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/zxw/config/WebMvcConfig.java`
- `src/main/java/com/zxw/common/security/AdminAuthInterceptor.java`
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
- `src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java`
- `src/main/java/com/zxw/modules/**/dto/*.java`
