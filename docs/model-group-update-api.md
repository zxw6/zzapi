# 套餐修改接口文档

本文档说明后台修改套餐分组接口。

## 1. 修改套餐

### 接口信息

| 项目 | 内容 |
| --- | --- |
| 请求方式 | `PUT` |
| 请求路径 | `/admin/model-access/groups/{groupId}` |
| 是否需要登录 | 是 |
| 权限要求 | 管理员 |
| Content-Type | `application/json` |
| 说明 | 根据套餐分组 ID 修改套餐编码、名称、类型、价格、有效天数、额度和备注 |

### 路径参数

| 参数 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `groupId` | `long` | 是 | 套餐分组 ID |

### 请求参数

```json
{
  "groupCode": "codex",
  "groupName": "Codex 套餐",
  "packageType": "QUOTA",
  "salePrice": 0,
  "packageDays": 30,
  "dailyQuota": 60,
  "weeklyQuota": 420,
  "monthlyQuota": 1800,
  "remark": "Codex 模型套餐"
}
```

| 参数 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `groupCode` | `string` | 是 | 套餐编码，不能重复 |
| `groupName` | `string` | 是 | 套餐名称 |
| `packageType` | `string` | 是 | 套餐类型，只支持 `QUOTA` 或 `BALANCE` |
| `salePrice` | `decimal` | 是 | 套餐价格，必须大于或等于 `0` |
| `packageDays` | `integer` | 是 | 套餐有效天数；后端在值为空或小于等于 `0` 时会使用默认有效天数 |
| `dailyQuota` | `decimal` | 是 | 日额度，必须大于或等于 `0` |
| `weeklyQuota` | `decimal` | 是 | 周额度，必须大于或等于 `0` |
| `monthlyQuota` | `decimal` | 是 | 月额度，必须大于或等于 `0` |
| `remark` | `string` | 否 | 备注，最长按后端限制截断到 `255` 字符 |

### packageType 说明

| 值 | 含义 | 说明 |
| --- | --- | --- |
| `QUOTA` | 额度套餐 | 调用模型时扣套餐额度，不直接扣钱包余额 |
| `BALANCE` | 余额套餐 | 调用模型时按实际消耗扣钱包余额 |

注意：前端可以显示“普通套餐”“额度套餐”等中文文案，但提交给后端的 `packageType` 必须是 `QUOTA` 或 `BALANCE`，否则会返回参数校验错误。

### 成功返回示例

```json
{
  "success": true,
  "message": "套餐修改成功",
  "data": {
    "packageRestrictionEnabled": true,
    "packageStatus": "ACTIVE",
    "packageStatusText": "使用中",
    "activeGroupId": 1,
    "activeGroupCode": "codex",
    "activeGroupName": "Codex 套餐",
    "activePackageType": "QUOTA",
    "packagePrice": 0,
    "dailyQuota": 60,
    "weeklyQuota": 420,
    "monthlyQuota": 1800,
    "dailyUsed": 0,
    "weeklyUsed": 0,
    "monthlyUsed": 0,
    "expiresAt": "2026-06-06T10:00:00",
    "remainingDays": 30,
    "groups": [
      {
        "id": 1,
        "groupCode": "codex",
        "groupName": "Codex 套餐",
        "packageType": "QUOTA",
        "salePrice": 0,
        "packageDays": 30,
        "dailyQuota": 60,
        "weeklyQuota": 420,
        "monthlyQuota": 1800,
        "modelCount": 3,
        "purchased": true,
        "active": true,
        "expiresAt": "2026-06-06T10:00:00",
        "remainingDays": 30,
        "dailyUsed": 0,
        "weeklyUsed": 0,
        "monthlyUsed": 0,
        "packageStatus": "ACTIVE",
        "packageStatusText": "使用中",
        "remark": "Codex 模型套餐",
        "systemPreset": false
      }
    ]
  }
}
```

## 2. 返回参数

### 外层返回结构

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `message` | `string` | 返回提示，成功时为“套餐修改成功” |
| `data` | `ModelAccessSummaryResponse` | 修改后的套餐访问汇总数据 |

### data 字段

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `packageRestrictionEnabled` | `boolean` | 当前用户是否启用套餐限制 |
| `packageStatus` | `string` | 当前套餐状态，如 `ACTIVE`、`EXPIRED`、`NONE` |
| `packageStatusText` | `string` | 当前套餐状态文案 |
| `activeGroupId` | `long / null` | 当前激活套餐分组 ID |
| `activeGroupCode` | `string / null` | 当前激活套餐编码 |
| `activeGroupName` | `string / null` | 当前激活套餐名称 |
| `activePackageType` | `string / null` | 当前激活套餐类型，`QUOTA` 或 `BALANCE` |
| `packagePrice` | `decimal` | 当前套餐价格 |
| `dailyQuota` | `decimal` | 当前套餐日额度 |
| `weeklyQuota` | `decimal` | 当前套餐周额度 |
| `monthlyQuota` | `decimal` | 当前套餐月额度 |
| `dailyUsed` | `decimal` | 当前套餐日已用额度 |
| `weeklyUsed` | `decimal` | 当前套餐周已用额度 |
| `monthlyUsed` | `decimal` | 当前套餐月已用额度 |
| `expiresAt` | `datetime / null` | 当前套餐过期时间 |
| `remainingDays` | `long / null` | 当前套餐剩余天数 |
| `groups` | `array` | 当前可见套餐分组列表 |

### data.groups[] 字段

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 套餐分组 ID |
| `groupCode` | `string` | 套餐编码 |
| `groupName` | `string` | 套餐名称 |
| `packageType` | `string` | 套餐类型，`QUOTA` 或 `BALANCE` |
| `salePrice` | `decimal` | 套餐价格 |
| `packageDays` | `integer` | 套餐有效天数 |
| `dailyQuota` | `decimal` | 日额度 |
| `weeklyQuota` | `decimal` | 周额度 |
| `monthlyQuota` | `decimal` | 月额度 |
| `modelCount` | `integer` | 套餐下绑定的模型数量 |
| `purchased` | `boolean` | 当前用户是否已购买该套餐 |
| `active` | `boolean` | 当前用户是否正在使用该套餐 |
| `expiresAt` | `datetime / null` | 该套餐购买记录过期时间 |
| `remainingDays` | `long / null` | 该套餐剩余天数 |
| `dailyUsed` | `decimal` | 日已用额度 |
| `weeklyUsed` | `decimal` | 周已用额度 |
| `monthlyUsed` | `decimal` | 月已用额度 |
| `packageStatus` | `string` | 套餐状态 |
| `packageStatusText` | `string` | 套餐状态文案 |
| `remark` | `string / null` | 备注 |
| `systemPreset` | `boolean` | 是否系统预置套餐 |

## 3. 常见失败返回

### 未登录

```json
{
  "success": false,
  "message": "请先登录",
  "data": null
}
```

### 无管理员权限

```json
{
  "success": false,
  "message": "需要管理员权限",
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

### 套餐类型不合法

```json
{
  "success": false,
  "message": "packageType: 套餐类型仅支持 QUOTA 或 BALANCE",
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

### 系统预置套餐不允许修改编码

```json
{
  "success": false,
  "message": "系统预置套餐编码不允许修改",
  "data": null
}
```

### 系统预置套餐不允许修改套餐类型

```json
{
  "success": false,
  "message": "系统预置套餐不允许修改套餐类型",
  "data": null
}
```

## 4. 前端接入示例

```js
await fetchJson(`/admin/model-access/groups/${groupId}`, {
  method: "PUT",
  body: {
    groupCode: form.groupCode,
    groupName: form.groupName,
    packageType: form.packageType,
    salePrice: Number(form.salePrice),
    packageDays: Number(form.packageDays),
    dailyQuota: Number(form.dailyQuota),
    weeklyQuota: Number(form.weeklyQuota),
    monthlyQuota: Number(form.monthlyQuota),
    remark: form.remark
  }
});
```

## 5. 注意事项

- 当前接口是全量更新，请提交完整字段，不要只提交改动字段。
- `packageType` 是后端枚举值，必须提交 `QUOTA` 或 `BALANCE`。
- `BALANCE` 类型套餐的额度字段后端会按余额套餐逻辑处理，通常会归零或不作为扣额度依据。
- 系统预置套餐可修改部分展示信息和额度，但不允许修改套餐编码，也不允许修改套餐类型。
