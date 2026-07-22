# 统计图接口整理

## 统一返回格式

所有后台接口统一返回 `ApiResponse<T>`：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

## 一、核心统计图接口

| 接口 | 方法 | 参数 | 用途 | 返回 data |
| --- | --- | --- | --- | --- |
| `/admin/dashboard/trend` | `GET` | `days`，默认 `7`，范围 `1-30` | 首页“近 7 天请求趋势”折线图 | `List<DashboardTrendPointResponse>` |
| `/admin/dashboard/model-stats` | `GET` | 无 | “对外模型统计”，按模型统计本月调用情况 | `List<DashboardModelStatResponse>` |
| `/admin/dashboard/overview` | `GET` | 无 | 顶部统计卡片，不是图表但属于统计面板数据 | `DashboardOverviewResponse` |
| `/admin/request-logs` | `GET` | `limit`，默认 `20`，范围 `1-100` | 前端本地聚合“费用趋势”和“模型消费占比/用量” | `List<RequestLogItemResponse>` |

## 二、接口详情

### 1. 请求趋势统计

```http
GET /admin/dashboard/trend?days=7
```

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 限制 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `days` | `int` | 否 | `7` | `1-30` | 查询最近多少天的趋势数据 |

#### 返回字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `statDate` | `LocalDate` | 统计日期 |
| `requestCount` | `long` | 请求次数 |
| `successCount` | `long` | 成功次数 |
| `totalTokens` | `long` | 总 token 数 |
| `userAmount` | `BigDecimal` | 用户消费金额 |
| `costAmount` | `BigDecimal` | 成本金额 | `

#### 返回示例

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "statDate": "2026-04-21",
      "requestCount": 10,
      "successCount": 9,
      "totalTokens": 12345,
      "userAmount": 1.23,
      "costAmount": 0.56
    }
  ]
}
```

#### 页面用途

用于首页“近 7 天请求趋势”折线图。

前端使用字段：

| 字段 | 用途 |
| --- | --- |
| `statDate` | X 轴日期 |
| `requestCount` | Y 轴请求量 |
| `totalTokens` | 统计卡片或辅助统计 |

## 2. 模型统计

```http
GET /admin/dashboard/model-stats
```

#### 请求参数

无。

#### 返回字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `modelCode` | `String` | 对外模型编码，例如 `gpt-5.4` |
| `upstreamModels` | `String` | 上游模型名称，多个用逗号拼接 |
| `requestCount` | `long` | 本月请求次数 |
| `totalTokens` | `long` | 本月总 token 数 |
| `avgLatencyMs` | `double` | 平均耗时，单位毫秒 |
| `totalLatencyMs` | `long` | 总耗时，单位毫秒 |
| `successRate` | `double` | 成功率，百分比数值 |
| `userAmount` | `BigDecimal` | 用户消费金额 |

#### 返回示例

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "modelCode": "gpt-5.4",
      "upstreamModels": "gpt-5.4",
      "requestCount": 100,
      "totalTokens": 50000,
      "avgLatencyMs": 1200.5,
      "totalLatencyMs": 120050,
      "successRate": 98.5,
      "userAmount": 12.34
    }
  ]
}
```

#### 页面用途

用于“对外模型统计”表格。

前端使用字段：

| 字段 | 用途 |
| --- | --- |
| `modelCode` | 模型名称/模型编码 |
| `upstreamModels` | 上游模型展示 |
| `requestCount` | 调用次数 |
| `totalTokens` | token 总量 |
| `avgLatencyMs` | 平均耗时 |
| `totalLatencyMs` | 总耗时 |
| `successRate` | 成功率 |

## 3. 概览统计

```http
GET /admin/dashboard/overview
```

#### 请求参数

无。

#### 返回字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userCount` | `long` | 用户数量 |
| `apiKeyCount` | `long` | API Key 数量 |
| `providerCount` | `long` | 服务商数量 |
| `modelCount` | `long` | 模型数量 |
| `requestCountToday` | `long` | 今日请求数 |
| `totalTokensToday` | `long` | 今日 token 数 |
| `totalTokens7d` | `long` | 近 7 天 token 数 |
| `rechargeAmountToday` | `BigDecimal` | 今日充值金额 |
| `consumeAmountToday` | `BigDecimal` | 今日消费金额 |
| `walletBalanceTotal` | `BigDecimal` | 钱包总余额 |

#### 返回示例

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "userCount": 1,
    "apiKeyCount": 2,
    "providerCount": 1,
    "modelCount": 10,
    "requestCountToday": 5,
    "totalTokensToday": 3000,
    "totalTokens7d": 20000,
    "rechargeAmountToday": 100.00,
    "consumeAmountToday": 3.50,
    "walletBalanceTotal": 96.50
  }
}
```

#### 页面用途

用于仪表盘顶部统计卡片和账户概览，不直接绘制折线图，但属于统计面板数据源。

## 4. 请求日志列表

```http
GET /admin/request-logs?limit=50
```

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 限制 | 说明 |
| --- | --- | --- | --- | --- | --- |
| `limit` | `int` | 否 | `20` | `1-100` | 返回最近多少条请求日志 |

#### 返回字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `requestId` | `String` | 请求 ID |
| `username` | `String` | 用户名 |
| `modelCode` | `String` | 对外模型编码 |
| `upstreamModel` | `String` | 上游模型 |
| `statusCode` | `Integer` | HTTP 状态码 |
| `latencyMs` | `Integer` | 请求耗时，单位毫秒 |
| `promptTokens` | `Integer` | 输入 token 数 |
| `completionTokens` | `Integer` | 输出 token 数 |
| `totalTokens` | `Integer` | 总 token 数 |
| `userAmount` | `BigDecimal` | 用户消费金额 |
| `costAmount` | `BigDecimal` | 成本金额 |
| `success` | `Integer` | 是否成功，`1` 表示成功 |
| `createdAt` | `LocalDateTime` | 创建时间 |

#### 返回示例

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "requestId": "req_xxx",
      "username": "admin",
      "modelCode": "gpt-5.4",
      "upstreamModel": "gpt-5.4",
      "statusCode": 200,
      "latencyMs": 1200,
      "promptTokens": 100,
      "completionTokens": 200,
      "totalTokens": 300,
      "userAmount": 0.05,
      "costAmount": 0.02,
      "success": 1,
      "createdAt": "2026-04-21T10:00:00"
    }
  ]
}
```

#### 页面用途

该接口不是专门的统计接口，但当前前端会使用它做本地聚合。

| 图表/模块 | 聚合字段 |
| --- | --- |
| 费用趋势 | `createdAt`、`userAmount` |
| 模型消费占比/模型用量 | `modelCode`、`userAmount`、`totalTokens` |
| 请求日志表格 | 全量日志字段 |

## 三、权限和数据范围

| 用户角色 | 数据范围 |
| --- | --- |
| 管理员 | 查询全站统计数据 |
| 普通用户 | 只查询当前登录用户自己的统计数据 |

## 四、前端对应关系

| 页面模块 | 接口 | 前端处理 |
| --- | --- | --- |
| 首页请求趋势 | `/admin/dashboard/trend?days=7` | `renderOverviewTrendChart()` |
| 费用趋势 | `/admin/request-logs?limit=50` | `renderBillingTrendChart()` 本地聚合 |
| 模型消费/用量 | `/admin/request-logs?limit=50` | `renderBillingModelUsage()` 本地聚合 |
| 对外模型统计 | `/admin/dashboard/model-stats` | `renderBillingModelStats()` |
| 顶部概览卡片 | `/admin/dashboard/overview` | `renderOverviewCards()` |

## 五、代码位置

| 类型 | 文件 |
| --- | --- |
| 仪表盘接口 Controller | `Test/src/main/java/com/zxw/modules/dashboard/controller/AdminDashboardController.java` |
| 仪表盘统计 Service | `Test/src/main/java/com/zxw/modules/dashboard/service/AdminDashboardService.java` |
| 请求日志 Controller | `Test/src/main/java/com/zxw/modules/request/controller/AdminRequestLogController.java` |
| 请求日志 Service | `Test/src/main/java/com/zxw/modules/request/service/AdminRequestLogService.java` |
| 前端统计图渲染 | `Test/src/main/resources/static/console/app.js` |
