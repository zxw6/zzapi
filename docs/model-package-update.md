# 套餐修改接口文档

## 接口说明

用于修改套餐分组配置，例如套餐名称、售价、有效天数、日额度、周额度、月额度等。

注意：这个接口修改的是“套餐分组配置”，不是某个用户已经购买的套餐记录。

## 是否已有接口

已有。

| 功能 | 接口 |
| --- | --- |
| 修改套餐分组 | `PUT /admin/model-access/groups/{groupId}` |
| 查询套餐列表和当前套餐状态 | `GET /admin/model-access/summary` |
| 删除/禁用套餐分组 | `DELETE /admin/model-access/{groupId}` |
| 删除/禁用用户已购买套餐记录 | `DELETE /admin/model-access/purchases/{packageId}` |

## 1. 修改套餐分组

### 请求地址

```http
PUT /admin/model-access/groups/{groupId}
```

### 请求方式

`PUT`

### 是否需要登录

需要后台登录。

### 权限要求

管理员。

### Header

```http
Authorization: Bearer <后台登录 token>
Content-Type: application/json
```

### Path 参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `groupId` | number | 是 | 套餐分组 ID |

### Body 参数

```json
{
  "groupCode": "gpt",
  "groupName": "GPT 套餐",
  "packageType": "QUOTA",
  "salePrice": 53.0000,
  "packageDays": 30,
  "dailyQuota": 60.0000,
  "weeklyQuota": 420.0000,
  "monthlyQuota": 1800.0000,
  "remark": "适合 GPT 系列模型"
}
```

### Body 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `groupCode` | string | 是 | 套餐编码，只能包含字母、数字、点、下划线、短横线；后端会转小写并清洗非法字符 |
| `groupName` | string | 是 | 套餐名称 |
| `packageType` | string | 是 | 套餐类型：`QUOTA`=额度套餐，`BALANCE`=余额套餐 |
| `salePrice` | number | 是 | 套餐售价，不能小于 0 |
| `packageDays` | number | 是 | 套餐有效天数；小于等于 0 时后端按默认 30 天处理 |
| `dailyQuota` | number | 是 | 每日额度，不能小于 0；余额套餐会被后端处理为 0 |
| `weeklyQuota` | number | 是 | 7 天额度，不能小于 0；余额套餐会被后端处理为 0 |
| `monthlyQuota` | number | 是 | 月额度，不能小于 0；余额套餐会被后端处理为 0 |
| `remark` | string | 否 | 备注，最长保留 255 个字符 |

### 请求示例

```bash
curl -X PUT "http://你的域名/admin/model-access/groups/8" \
  -H "Authorization: Bearer 后台登录token" \
  -H "Content-Type: application/json" \
  -d '{
    "groupCode": "gpt",
    "groupName": "GPT 套餐",
    "packageType": "QUOTA",
    "salePrice": 53.0000,
    "packageDays": 30,
    "dailyQuota": 60.0000,
    "weeklyQuota": 420.0000,
    "monthlyQuota": 1800.0000,
    "remark": "适合 GPT 系列模型"
  }'
```

## 成功响应示例

```json
{
  "success": true,
  "message": "套餐修改成功",
  "data": {
    "packageRestrictionEnabled": true,
    "packageStatus": "ACTIVE",
    "packageStatusText": "生效中",
    "activeGroupId": 8,
    "activeGroupCode": "gpt",
    "activeGroupName": "GPT 套餐",
    "activePackageType": "QUOTA",
    "packagePrice": 53.0000,
    "dailyQuota": 60.0000,
    "weeklyQuota": 420.0000,
    "monthlyQuota": 1800.0000,
    "dailyUsed": 3.250000,
    "weeklyUsed": 18.600000,
    "monthlyUsed": 93.400000,
    "expiresAt": "2026-06-20T14:58:44",
    "remainingDays": 29,
    "groups": [
      {
        "id": 8,
        "groupCode": "gpt",
        "groupName": "GPT 套餐",
        "packageType": "QUOTA",
        "salePrice": 53.0000,
        "packageDays": 30,
        "dailyQuota": 60.0000,
        "weeklyQuota": 420.0000,
        "monthlyQuota": 1800.0000,
        "modelCount": 12,
        "purchased": true,
        "active": true,
        "expiresAt": "2026-06-20T14:58:44",
        "remainingDays": 29,
        "dailyUsed": 3.250000,
        "weeklyUsed": 18.600000,
        "monthlyUsed": 93.400000,
        "packageStatus": "ACTIVE",
        "packageStatusText": "生效中",
        "remark": "适合 GPT 系列模型",
        "systemPreset": true
      }
    ]
  }
}
```

## 返回参数说明

### 顶层字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | boolean | 请求是否成功 |
| `message` | string | 响应消息 |
| `data` | object/null | 修改成功后返回套餐汇总数据 |

### data 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `packageRestrictionEnabled` | boolean | 是否启用套餐限制 |
| `packageStatus` | string | 当前用户套餐状态：`ACTIVE`、`EXPIRED`、`NOT_PURCHASED` |
| `packageStatusText` | string | 套餐状态文案 |
| `activeGroupId` | number/null | 当前生效套餐分组 ID |
| `activeGroupCode` | string/null | 当前生效套餐编码 |
| `activeGroupName` | string/null | 当前生效套餐名称 |
| `activePackageType` | string/null | 当前生效套餐类型：`QUOTA` 或 `BALANCE` |
| `packagePrice` | number | 当前展示套餐价格 |
| `dailyQuota` | number | 每日额度 |
| `weeklyQuota` | number | 7 天额度 |
| `monthlyQuota` | number | 月额度 |
| `dailyUsed` | number | 今日已使用额度 |
| `weeklyUsed` | number | 近 7 天已使用额度 |
| `monthlyUsed` | number | 本月已使用额度 |
| `expiresAt` | string/null | 当前生效套餐过期时间 |
| `remainingDays` | number/null | 当前生效套餐剩余天数 |
| `groups` | array | 套餐分组列表 |

### groups 数组字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 套餐分组 ID |
| `groupCode` | string | 套餐编码 |
| `groupName` | string | 套餐名称 |
| `packageType` | string | 套餐类型：`QUOTA`=额度套餐，`BALANCE`=余额套餐 |
| `salePrice` | number | 套餐售价 |
| `packageDays` | number | 套餐有效天数 |
| `dailyQuota` | number | 每日额度 |
| `weeklyQuota` | number | 7 天额度 |
| `monthlyQuota` | number | 月额度 |
| `modelCount` | number | 当前套餐分组可用模型数量 |
| `purchased` | boolean | 当前用户是否购买过该套餐 |
| `active` | boolean | 当前用户该套餐是否正在生效 |
| `expiresAt` | string/null | 当前用户该套餐过期时间 |
| `remainingDays` | number/null | 当前用户该套餐剩余天数 |
| `dailyUsed` | number | 今日已使用额度 |
| `weeklyUsed` | number | 近 7 天已使用额度 |
| `monthlyUsed` | number | 本月已使用额度 |
| `packageStatus` | string | 当前用户该套餐状态：`ACTIVE`、`EXPIRED`、`NOT_PURCHASED` |
| `packageStatusText` | string | 套餐状态文案 |
| `remark` | string/null | 备注 |
| `systemPreset` | boolean | 是否系统预设套餐 |

## 错误响应示例

### 未登录或 Token 无效

```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null
}
```

### 非管理员

```json
{
  "success": false,
  "message": "Forbidden",
  "data": null
}
```

### 套餐不存在

```json
{
  "success": false,
  "message": "套餐分组不存在",
  "data": null
}
```

### 套餐编码重复

```json
{
  "success": false,
  "message": "套餐编码已存在",
  "data": null
}
```

## 重要规则

1. 系统预设套餐的 `groupCode` 不允许修改。
2. 系统预设套餐的 `packageType` 不允许修改。
3. 修改的是套餐分组配置，不会直接改写用户历史购买记录里的购买价格。
4. 用户新购买套餐时，会按修改后的套餐配置生成新的购买记录。
5. 已经创建并绑定某个套餐购买记录的 API Key，仍然绑定原购买记录。
