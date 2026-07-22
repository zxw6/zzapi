# API Key 套餐用量查询接口文档

## 接口说明

根据客户的 API Key 查询该 Key 当前绑定套餐的使用情况。

客户无需登录后台，只需要提供 API Key。

## 请求信息

| 项目 | 内容 |
| --- | --- |
| 请求地址 | `/api/key-usage/package` |
| 请求方式 | `GET` |
| 是否需要后台登录 | 否 |
| 鉴权方式 | `Authorization: Bearer <API Key>` 或 `X-API-Key: <API Key>` |

## 请求头

推荐使用 `Authorization`：

```http
Authorization: Bearer sk-live-xxxxxxxx
```

也支持使用 `X-API-Key`：

```http
X-API-Key: sk-live-xxxxxxxx
```

## 请求参数

无 URL 参数。

无 Body 参数。

## 请求示例

### Authorization 方式

```bash
curl -X GET "http://你的域名/api/key-usage/package" \
  -H "Authorization: Bearer sk-live-xxxxxxxx"
```

### X-API-Key 方式

```bash
curl -X GET "http://你的域名/api/key-usage/package" \
  -H "X-API-Key: sk-live-xxxxxxxx"
```

## 成功响应示例

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "apiKeyId": 12,
    "packageId": 53,
    "groupId": 8,
    "groupCode": "gpt",
    "groupName": "GPT 套餐",
    "packageType": "QUOTA",
    "status": "ACTIVE",
    "active": true,
    "statDate": "2026-05-21",
    "expiresAt": "2026-06-20T14:58:44",
    "remainingDays": 29,
    "packageDays": 30,
    "dailyQuota": 60.0000,
    "dailyUsed": 3.250000,
    "dailyRemaining": 56.750000,
    "weeklyQuota": 420.0000,
    "weeklyUsed": 18.600000,
    "weeklyRemaining": 401.400000,
    "monthlyQuota": 1800.0000,
    "monthlyUsed": 93.400000,
    "monthlyRemaining": 1706.600000,
    "totalQuota": 1800.0000,
    "totalUsed": 93.400000,
    "totalRemaining": 1706.600000
  }
}
```

## 返回参数说明

### 顶层字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | boolean | 请求是否成功 |
| `message` | string | 响应消息 |
| `data` | object/null | 成功时返回数据，失败时为 `null` |

### data 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `apiKeyId` | number | 当前 API Key 的 ID |
| `packageId` | number | API Key 绑定的套餐购买记录 ID |
| `groupId` | number | 套餐分组 ID |
| `groupCode` | string | 套餐分组编码 |
| `groupName` | string | 套餐名称 |
| `packageType` | string | 套餐类型，`QUOTA`=额度套餐，`BALANCE`=余额套餐 |
| `status` | string | 套餐状态，如 `ACTIVE`、`EXPIRED`、`DELETED` |
| `active` | boolean | 当前套餐是否可用 |
| `statDate` | string | 统计日期，格式 `yyyy-MM-dd` |
| `expiresAt` | string/null | 套餐过期时间 |
| `remainingDays` | number | 剩余天数 |
| `packageDays` | number | 套餐有效天数 |
| `dailyQuota` | number | 今日额度 |
| `dailyUsed` | number | 今日已使用额度 |
| `dailyRemaining` | number | 今日剩余额度 |
| `weeklyQuota` | number | 近 7 天额度 |
| `weeklyUsed` | number | 近 7 天已使用额度 |
| `weeklyRemaining` | number | 近 7 天剩余额度 |
| `monthlyQuota` | number | 本月额度 |
| `monthlyUsed` | number | 本月已使用额度 |
| `monthlyRemaining` | number | 本月剩余额度 |
| `totalQuota` | number | 总额度 |
| `totalUsed` | number | 累计已使用额度 |
| `totalRemaining` | number | 累计剩余额度 |

## 错误响应示例

### 未传 API Key

```json
{
  "success": false,
  "message": "Missing API key",
  "data": null
}
```

### API Key 无效

```json
{
  "success": false,
  "message": "Invalid API key",
  "data": null
}
```

### API Key 被禁用

```json
{
  "success": false,
  "message": "API key is disabled",
  "data": null
}
```

### API Key 已过期

```json
{
  "success": false,
  "message": "API key has expired",
  "data": null
}
```

### API Key 没有关联套餐

```json
{
  "success": false,
  "message": "API key has no available package binding",
  "data": null
}
```

## 前端接入建议

1. 输入框收集客户 API Key。
2. 点击查询时请求 `GET /api/key-usage/package`。
3. 推荐把 API Key 放在 `Authorization` 请求头里，不要放在 URL 参数里。
4. 页面重点展示 `groupName`、`status`、`active`、`expiresAt`、`dailyUsed`、`dailyRemaining`、`totalUsed`、`totalRemaining`。
5. 当 `active=false` 时，前端可提示套餐已过期或不可用。
