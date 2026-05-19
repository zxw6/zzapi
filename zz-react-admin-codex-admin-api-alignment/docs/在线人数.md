# 仪表盘概览接口

## 接口信息

- 接口名称：查询仪表盘概览
- 请求方式：`GET`
- 请求地址：`/admin/dashboard/overview`
- 鉴权方式：需要登录
- 请求头：`Authorization: Bearer {token}`

---

## 接口说明

这个接口返回控制台首页概览数据。

这次你关心的两个总统计字段是：

- `onlineUserCount`：实时活跃人数总统计
- `todayActiveUserCount`：今天使用过的人数总统计

统计口径如下：

- `onlineUserCount`
  最近 `5` 分钟内访问过后台受保护接口的登录用户总数
- `todayActiveUserCount`
  今天在系统里产生过使用记录的去重用户总数

---

## 请求示例

```http
GET /admin/dashboard/overview HTTP/1.1
Host: localhost:9988
Authorization: Bearer your_token_here
```

---

## 成功响应示例

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "userCount": 12,
    "apiKeyCount": 35,
    "providerCount": 4,
    "modelCount": 28,
    "requestCountToday": 156,
    "totalTokensToday": 892341,
    "totalTokens7d": 5234481,
    "onlineUserCount": 3,
    "todayActiveUserCount": 9,
    "rechargeAmountToday": 200.0000,
    "consumeAmountToday": 86.3200,
    "walletBalanceTotal": 1543.8800
  }
}
```

---

## 返回参数说明

### 外层响应

| 参数名 | 类型 | 说明 |
|---|---|---|
| success | Boolean | 是否成功 |
| message | String | 响应消息 |
| data | Object | 概览数据 |

### data 字段

| 参数名 | 类型 | 说明 |
|---|---|---|
| userCount | Long | 用户总数 |
| apiKeyCount | Long | API Key 总数 |
| providerCount | Long | 供应商总数 |
| modelCount | Long | 模型总数 |
| requestCountToday | Long | 今日请求次数 |
| totalTokensToday | Long | 今日总 Token 数 |
| totalTokens7d | Long | 最近 7 天总 Token 数 |
| onlineUserCount | Long | 实时活跃人数总统计，最近 5 分钟内活跃的登录用户数 |
| todayActiveUserCount | Long | 今天使用过的人数总统计，今天产生过使用记录的去重用户数 |
| rechargeAmountToday | BigDecimal | 今日充值金额 |
| consumeAmountToday | BigDecimal | 今日消费金额 |
| walletBalanceTotal | BigDecimal | 钱包总余额 |

---

## 失败响应示例

```json
{
  "success": false,
  "message": "请先登录",
  "data": null
}
```

---

## 统计 SQL

### 1. 实时活跃人数总统计

```sql
SELECT COUNT(*) AS online_user_count
FROM users
WHERE deleted = 0
  AND last_active_at IS NOT NULL
  AND last_active_at >= NOW() - INTERVAL 5 MINUTE;
```

### 2. 今天使用过的人数总统计

```sql
SELECT COUNT(DISTINCT user_id) AS today_active_user_count
FROM request_logs
WHERE request_date = CURDATE()
  AND user_id IS NOT NULL;
```

---

## 数据库变更

如果是已有数据库，需要执行：

```sql
ALTER TABLE users
ADD COLUMN last_active_at DATETIME NULL AFTER last_login_at;
```
