# 仪表盘 / 套餐 / 模型相关接口统计

本文按你提到的 4 块内容整理：

1. 仪表盘接口和返回参数
2. 套餐购买记录、余额流水接口和返回参数
3. 图一“从上游批量导入模型”相关接口和参数
4. 图二“分组模型预览”相关接口和参数

## 统一返回结构

项目里的后台接口统一返回：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

通用字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `message` | `string` | 提示信息 |
| `data` | `object/array/null` | 业务数据 |

---

## 1. 仪表盘接口

前端仪表盘加载时，核心会调用这几个接口：

| 接口 | 方法 | 请求参数 | 用途 |
| --- | --- | --- | --- |
| `/admin/dashboard/overview` | `GET` | 无 | 顶部概览卡片 |
| `/admin/dashboard/trend?days=7` | `GET` | `days`，默认 `7`，范围 `1-30` | 请求趋势图 |
| `/admin/dashboard/model-stats` | `GET` | 无 | 模型调用统计表 |
| `/admin/model-access/summary` | `GET` | 无 | 仪表盘里的套餐/额度摘要卡片 |
| `/admin/model-access/purchases` | `GET` | 无 | 仪表盘套餐购买记录区块 |

### 1.1 `GET /admin/dashboard/overview`

用途：仪表盘顶部统计卡片。

请求参数：无

返回：`ApiResponse<DashboardOverviewResponse>`

`data` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userCount` | `long` | 用户数。普通用户视角固定为 `1`，管理员为全平台用户数 |
| `apiKeyCount` | `long` | API Key 数量 |
| `providerCount` | `long` | 渠道数。普通用户视角固定为 `0` |
| `modelCount` | `long` | 模型数。普通用户看到的是公开模型数，管理员看到的是全部模型数 |
| `requestCountToday` | `long` | 今日请求次数 |
| `totalTokensToday` | `long` | 今日总 Tokens |
| `totalTokens7d` | `long` | 近 7 天总 Tokens |
| `rechargeAmountToday` | `BigDecimal` | 今日充值金额 |
| `consumeAmountToday` | `BigDecimal` | 今日消费金额 |
| `walletBalanceTotal` | `BigDecimal` | 钱包余额。普通用户为自己的余额，管理员为全平台钱包余额总和 |

### 1.2 `GET /admin/dashboard/trend`

用途：仪表盘“近 N 天请求趋势图”。

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `days` | `int` | 否 | 查询天数，默认 `7`，最小 `1`，最大 `30` |

返回：`ApiResponse<List<DashboardTrendPointResponse>>`

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `statDate` | `LocalDate` | 统计日期 |
| `requestCount` | `long` | 当天请求次数 |
| `successCount` | `long` | 当天成功次数 |
| `totalTokens` | `long` | 当天总 Tokens |
| `userAmount` | `BigDecimal` | 当天用户侧金额 |
| `costAmount` | `BigDecimal` | 当天成本金额 |

### 1.3 `GET /admin/dashboard/model-stats`

用途：仪表盘模型调用统计表。

请求参数：无

返回：`ApiResponse<List<DashboardModelStatResponse>>`

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `modelCode` | `String` | 模型编码 |
| `upstreamModels` | `String` | 上游模型名，多个时逗号拼接 |
| `requestCount` | `long` | 调用次数 |
| `totalTokens` | `long` | 总 Tokens |
| `avgLatencyMs` | `double` | 平均耗时，单位毫秒 |
| `totalLatencyMs` | `long` | 总耗时，单位毫秒 |
| `successRate` | `double` | 成功率，百分比值，如 `98.5` |
| `userAmount` | `BigDecimal` | 用户侧累计金额 |

### 1.4 `GET /admin/model-access/summary`

用途：仪表盘套餐额度卡片、套餐中心、图二区块共用。

请求参数：无

返回：`ApiResponse<ModelAccessSummaryResponse>`

`data` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `packageRestrictionEnabled` | `boolean` | 是否启用套餐限制 |
| `packageStatus` | `String` | 当前套餐状态：如 `ACTIVE` / `EXPIRED` / `NOT_PURCHASED` |
| `packageStatusText` | `String` | 当前套餐状态中文文案 |
| `activeGroupId` | `Long` | 当前激活分组 ID |
| `activeGroupCode` | `String` | 当前激活分组编码 |
| `activeGroupName` | `String` | 当前激活分组名称 |
| `packagePrice` | `BigDecimal` | 参考套餐价格 |
| `dailyQuota` | `BigDecimal` | 日额度 |
| `weeklyQuota` | `BigDecimal` | 周额度 |
| `monthlyQuota` | `BigDecimal` | 月额度 |
| `dailyUsed` | `BigDecimal` | 当日已用额度 |
| `weeklyUsed` | `BigDecimal` | 当周已用额度 |
| `monthlyUsed` | `BigDecimal` | 当月已用额度 |
| `expiresAt` | `LocalDateTime` | 到期时间 |
| `remainingDays` | `Long` | 剩余天数 |
| `groups` | `List<ModelGroupOptionResponse>` | 套餐分组列表 |

`data.groups[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 分组 ID |
| `groupCode` | `String` | 分组编码 |
| `groupName` | `String` | 分组名称 |
| `salePrice` | `BigDecimal` | 售价 |
| `packageDays` | `Integer` | 套餐有效天数 |
| `dailyQuota` | `BigDecimal` | 日额度 |
| `weeklyQuota` | `BigDecimal` | 周额度 |
| `monthlyQuota` | `BigDecimal` | 月额度 |
| `modelCount` | `Integer` | 当前分组下的启用模型数 |
| `purchased` | `boolean` | 是否已购买 |
| `active` | `boolean` | 是否当前可用 |
| `expiresAt` | `LocalDateTime` | 到期时间 |
| `remainingDays` | `Long` | 剩余天数 |
| `dailyUsed` | `BigDecimal` | 当日已用 |
| `weeklyUsed` | `BigDecimal` | 当周已用 |
| `monthlyUsed` | `BigDecimal` | 当月已用 |
| `packageStatus` | `String` | 分组套餐状态 |
| `packageStatusText` | `String` | 分组套餐状态文案 |
| `remark` | `String` | 备注 |
| `systemPreset` | `boolean` | 是否系统预置套餐 |

---

## 2. 套餐购买记录和余额流水接口

### 2.1 `GET /admin/model-access/purchases`

用途：套餐购买记录表格。

请求参数：无

返回：`ApiResponse<List<ModelPackagePurchaseRecordResponse>>`

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 购买记录 ID |
| `userId` | `Long` | 用户 ID |
| `username` | `String` | 用户名 |
| `groupId` | `Long` | 套餐分组 ID |
| `groupCode` | `String` | 套餐分组编码 |
| `groupName` | `String` | 套餐分组名称 |
| `modelCount` | `Integer` | 该分组下启用模型数量 |
| `purchasePrice` | `BigDecimal` | 购买价格 |
| `startAt` | `LocalDateTime` | 生效时间 |
| `expiresAt` | `LocalDateTime` | 到期时间 |
| `status` | `String` | 状态，如 `ACTIVE` / `EXPIRED` / `DELETED` |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `active` | `boolean` | 当前是否可用 |
| `dailyQuota` | `BigDecimal` | 日额度 |
| `weeklyQuota` | `BigDecimal` | 周额度 |
| `monthlyQuota` | `BigDecimal` | 月额度 |
| `totalQuota` | `BigDecimal` | 总额度 |
| `dailyUsed` | `BigDecimal` | 当日已用 |
| `weeklyUsed` | `BigDecimal` | 当周已用 |
| `monthlyUsed` | `BigDecimal` | 当月已用 |
| `totalUsed` | `BigDecimal` | 总已用 |
| `remainingDays` | `Long` | 剩余天数 |

### 2.2 `GET /admin/model-access/wallet-transactions`

用途：余额流水表格，显示充值、套餐购买、模型扣费等钱包变动记录。

请求参数：无

返回：`ApiResponse<List<WalletTransactionItemResponse>>`

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 流水 ID |
| `userId` | `Long` | 用户 ID |
| `username` | `String` | 用户名 |
| `walletId` | `Long` | 钱包 ID |
| `orderNo` | `String` | 订单号 |
| `transactionType` | `String` | 流水类型，如 `RECHARGE` / `PACKAGE_BUY` / `CONSUME` |
| `direction` | `String` | 方向，如 `IN` / `OUT` |
| `amount` | `BigDecimal` | 变动金额 |
| `balanceBefore` | `BigDecimal` | 变动前余额 |
| `balanceAfter` | `BigDecimal` | 变动后余额 |
| `status` | `String` | 状态 |
| `descriptionText` | `String` | 描述，例如“购买 xxx 套餐”“模型调用扣费: gpt-5.5” |
| `transactionDate` | `LocalDate` | 交易日期 |
| `createdAt` | `LocalDateTime` | 创建时间 |

### 2.3 `POST /admin/model-access/purchase`

用途：执行套餐购买动作。虽然你这次主要问的是“购买记录”，但这个是购买记录的来源接口，建议一起留档。

请求体：`PurchaseModelPackageRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `groupId` | `Long` | 是 | 要购买的套餐分组 ID |

返回：`ApiResponse<ModelAccessSummaryResponse>`

返回的 `data` 结构与上面的 `/admin/model-access/summary` 一致，购买成功后会直接返回最新套餐摘要。

---

## 3. 图一：从上游批量导入模型

图一对应页面区块标题：`从上游批量导入模型`

这块前端实际依赖 4 个接口：

| 接口 | 方法 | 用途 |
| --- | --- | --- |
| `/admin/model-access/summary` | `GET` | 获取套餐分组下拉数据 |
| `/admin/providers` | `GET` | 获取渠道下拉数据 |
| `/admin/models/upstream?providerId=xxx` | `GET` | 读取上游模型列表 |
| `/admin/models/import` | `POST` | 批量导入选中的上游模型 |

### 3.1 `GET /admin/providers`

用途：图一“渠道”下拉框。

请求参数：无

返回：`ApiResponse<List<ProviderListItemResponse>>`

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 渠道 ID |
| `providerCode` | `String` | 渠道编码 |
| `providerName` | `String` | 渠道名称 |
| `baseUrl` | `String` | 基础地址 |
| `providerType` | `String` | 渠道类型 |
| `status` | `String` | 状态 |
| `priorityNo` | `Integer` | 优先级 |
| `timeoutMs` | `Integer` | 超时时间 |
| `tokenCount` | `Integer` | Token 配置数量 |
| `createdAt` | `LocalDateTime` | 创建时间 |

前端实际下拉展示主要用到：

| 字段 | 用途 |
| --- | --- |
| `id` | 作为 `providerId` |
| `providerName` | 下拉文本 |
| `providerType` | 下拉文本补充说明 |
| `status` | 前端会过滤出 `ACTIVE` 的渠道 |

### 3.2 `GET /admin/models/upstream`

用途：点击“读取上游模型”后，拉取指定渠道可用的上游模型。

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `providerId` | `Long` | 是 | 渠道 ID |

返回：`ApiResponse<List<UpstreamModelOptionResponse>>`

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `String` | 上游模型标识 |
| `displayName` | `String` | 展示名称 |
| `ownedBy` | `String` | 所属方 |
| `providerType` | `String` | 渠道类型 |

前端实际展示主要用到：

| 字段 | 用途 |
| --- | --- |
| `id` | 复选框值、模型元信息 |
| `displayName` | 卡片标题 |
| `ownedBy` | 元信息展示 |
| `providerType` | `ownedBy` 为空时的兜底展示 |

### 3.3 `POST /admin/models/import`

用途：批量导入已勾选的上游模型到指定套餐分组。

请求体：`ModelBatchImportRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `groupId` | `Long` | 是 | 导入到哪个套餐分组 |
| `providerId` | `Long` | 是 | 来自哪个渠道 |
| `upstreamModels` | `List<String>` | 是 | 勾选的上游模型 ID 列表 |
| `promptPrice` | `BigDecimal` | 否 | 输入价格 |
| `cachedPromptPrice` | `BigDecimal` | 否 | 缓存读取价格 |
| `completionPrice` | `BigDecimal` | 否 | 输出价格 |
| `multiplier` | `BigDecimal` | 否 | 倍率 |
| `isPublic` | `Boolean` | 否 | 是否公开 |

返回：`ApiResponse<ModelBatchImportResponse>`

`data` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `importedCount` | `int` | 成功导入数量 |
| `skippedCount` | `int` | 跳过数量 |
| `importedModels` | `List<String>` | 成功导入的模型列表 |
| `skippedModels` | `List<String>` | 跳过的模型列表 |

### 3.4 图一里的套餐下拉来源

图一“套餐分组”下拉数据来自 `GET /admin/model-access/summary` 的 `data.groups[]`。

前端主要使用字段：

| 字段 | 用途 |
| --- | --- |
| `id` | 作为 `groupId` |
| `groupName` | 下拉文本 |
| `groupCode` | 下拉文本补充 |

---

## 4. 图二：分组模型预览

图二对应页面区块标题：`分组模型预览`

这块前端主要依赖 2 个核心接口：

| 接口 | 方法 | 用途 |
| --- | --- | --- |
| `/admin/model-access/summary` | `GET` | 渲染“全部 / 按套餐分组”的筛选和分组信息 |
| `/admin/models` | `GET` | 渲染模型卡片与右侧详情 |

### 4.1 `GET /admin/models`

用途：图二模型卡片列表、模型详情面板。

请求参数：无

返回：`ApiResponse<List<ModelListItemResponse>>`

返回范围说明：

- 管理员：返回全部未删除模型
- 普通用户：只返回 `status = ACTIVE` 且 `isPublic = 1` 的模型

`data[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 模型 ID |
| `bindingId` | `Long` | 模型与套餐分组绑定关系 ID |
| `modelCode` | `String` | 模型编码 |
| `modelName` | `String` | 模型名称 |
| `modelType` | `String` | 模型类型，如 `CHAT` / `IMAGE` |
| `billingType` | `String` | 计费方式 |
| `promptPrice` | `BigDecimal` | 输入价格 |
| `cachedPromptPrice` | `BigDecimal` | 缓存读取价格 |
| `completionPrice` | `BigDecimal` | 输出价格 |
| `requestPrice` | `BigDecimal` | 单次最低扣费 |
| `multiplier` | `BigDecimal` | 倍率 |
| `isPublic` | `Integer` | 是否公开，通常 `1` 公开、`0` 不公开 |
| `status` | `String` | 状态，如 `ACTIVE` / `DISABLED` |
| `groupId` | `Long` | 所属套餐分组 ID |
| `groupCode` | `String` | 所属套餐分组编码 |
| `groupName` | `String` | 所属套餐分组名称 |
| `providerId` | `Long` | 渠道 ID |
| `providerName` | `String` | 渠道名称 |
| `providerType` | `String` | 渠道类型 |
| `upstreamModel` | `String` | 对应上游模型名 |
| `createdAt` | `LocalDateTime` | 创建时间 |

图二左侧卡片实际重点展示字段：

| 字段 | 用途 |
| --- | --- |
| `providerName` / `providerType` | 卡片左上角来源标识 |
| `modelName` / `modelCode` | 模型标题 |
| `promptPrice` | “输入”价格 |
| `cachedPromptPrice` | “缓存读取”价格 |
| `completionPrice` | “输出”价格 |
| `requestPrice` | “最低扣费” |
| `multiplier` | 倍率 |
| `groupName` | 套餐分组名 |
| `status` | 启用/禁用状态 |

图二右侧详情面板实际重点展示字段：

| 字段 | 用途 |
| --- | --- |
| `modelCode` | 模型标识符 |
| `modelName` | 标题 |
| `groupName` | 所属套餐 |
| `modelType` | 模型类型 |
| `upstreamModel` | 上游模型 |
| `status` | 当前状态 |
| `promptPrice` | 输入价格 |
| `cachedPromptPrice` | 缓存读取价格 |
| `completionPrice` | 输出价格 |
| `requestPrice` | 最低扣费 |
| `multiplier` | 倍率 |

### 4.2 `GET /admin/model-access/summary`

用途：图二里的分组筛选、套餐状态、分组模型数量等信息。

请求参数：无

返回：`ApiResponse<ModelAccessSummaryResponse>`

图二区域最常用的是 `data.groups[]` 里的这些字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 分组 ID，用于筛选当前分组模型 |
| `groupCode` | `String` | 分组编码 |
| `groupName` | `String` | 分组名称 |
| `modelCount` | `Integer` | 该分组下模型数 |
| `purchased` | `boolean` | 是否购买过 |
| `active` | `boolean` | 当前是否可用 |
| `salePrice` | `BigDecimal` | 售价 |
| `packageStatus` | `String` | 套餐状态 |
| `packageStatusText` | `String` | 套餐状态文案 |
| `remark` | `String` | 套餐备注 |
| `systemPreset` | `boolean` | 是否系统预置套餐 |

---

## 5. 页面和接口对应关系速查

| 页面区块 | 对应接口 |
| --- | --- |
| 仪表盘顶部卡片 | `/admin/dashboard/overview` |
| 仪表盘请求趋势图 | `/admin/dashboard/trend` |
| 仪表盘模型统计 | `/admin/dashboard/model-stats` |
| 仪表盘/套餐中心额度摘要 | `/admin/model-access/summary` |
| 套餐购买记录 | `/admin/model-access/purchases` |
| 余额流水 | `/admin/model-access/wallet-transactions` |
| 图一套餐下拉 | `/admin/model-access/summary` |
| 图一渠道下拉 | `/admin/providers` |
| 图一读取上游模型 | `/admin/models/upstream` |
| 图一批量导入 | `/admin/models/import` |
| 图二分组筛选 | `/admin/model-access/summary` |
| 图二模型预览卡片 | `/admin/models` |
