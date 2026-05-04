# 余额套餐相关接口说明

本文档只说明本次新增或变更的套餐相关接口、请求参数和返回参数。

## 1. 通用返回结构

所有接口统一返回：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `message` | `string` | 提示信息 |
| `data` | `object / array / null` | 业务数据 |

## 2. 套餐类型说明

| 值 | 名称 | 说明 |
| --- | --- | --- |
| `QUOTA` | 额度套餐 | 调用时扣套餐额度，不扣钱包余额 |
| `BALANCE` | 余额套餐 | 购买价格可为 `0`，调用时直接扣钱包余额 |

余额套餐业务规则：

- 购买余额套餐前，钱包余额必须 `>= 1.00`
- 使用余额套餐创建的 API Key 调用模型时，钱包余额必须 `>= 0.50`
- 且钱包余额必须足以支付本次请求应扣金额
- 余额套餐和普通额度套餐之间不允许自动切换

## 3. 获取套餐总览

### 接口

- `GET /admin/model-access/summary`

### 说明

- 获取当前登录用户的套餐总览
- 返回当前激活套餐、套餐类型、额度使用情况、可购买分组列表

### 成功响应

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "packageRestrictionEnabled": true,
    "packageStatus": "ACTIVE",
    "packageStatusText": "使用中",
    "activeGroupId": 2,
    "activeGroupCode": "balance-basic",
    "activeGroupName": "余额套餐",
    "activePackageType": "BALANCE",
    "packagePrice": 0,
    "dailyQuota": 0,
    "weeklyQuota": 0,
    "monthlyQuota": 0,
    "dailyUsed": 0,
    "weeklyUsed": 0,
    "monthlyUsed": 0,
    "expiresAt": "2026-06-03T12:00:00",
    "remainingDays": 29,
    "groups": []
  }
}
```

### `data` 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `packageRestrictionEnabled` | `boolean` | 是否开启套餐限制 |
| `packageStatus` | `string` | 当前套餐状态：`ACTIVE` / `EXPIRED` / `NOT_PURCHASED` |
| `packageStatusText` | `string` | 套餐状态中文说明 |
| `activeGroupId` | `long` | 当前激活套餐分组 ID |
| `activeGroupCode` | `string` | 当前激活套餐分组编码 |
| `activeGroupName` | `string` | 当前激活套餐分组名称 |
| `activePackageType` | `string` | 当前激活套餐类型：`QUOTA` / `BALANCE` |
| `packagePrice` | `decimal` | 当前参考套餐价格 |
| `dailyQuota` | `decimal` | 日额度 |
| `weeklyQuota` | `decimal` | 周额度 |
| `monthlyQuota` | `decimal` | 月额度 |
| `dailyUsed` | `decimal` | 今日已用额度 |
| `weeklyUsed` | `decimal` | 本周已用额度 |
| `monthlyUsed` | `decimal` | 本月已用额度 |
| `expiresAt` | `datetime` | 当前激活套餐到期时间 |
| `remainingDays` | `long` | 当前激活套餐剩余天数 |
| `groups` | `ModelGroupOptionResponse[]` | 套餐分组列表 |

### `groups[]` 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 分组 ID |
| `groupCode` | `string` | 分组编码 |
| `groupName` | `string` | 分组名称 |
| `packageType` | `string` | 套餐类型：`QUOTA` / `BALANCE` |
| `salePrice` | `decimal` | 销售价 |
| `packageDays` | `int` | 有效天数 |
| `dailyQuota` | `decimal` | 日额度 |
| `weeklyQuota` | `decimal` | 周额度 |
| `monthlyQuota` | `decimal` | 月额度 |
| `modelCount` | `int` | 该套餐下模型数量 |
| `purchased` | `boolean` | 当前账号是否购买过 |
| `active` | `boolean` | 当前账号是否正在使用该套餐 |
| `expiresAt` | `datetime` | 该账号购买的最新套餐到期时间 |
| `remainingDays` | `long` | 剩余天数 |
| `dailyUsed` | `decimal` | 今日已用额度 |
| `weeklyUsed` | `decimal` | 本周已用额度 |
| `monthlyUsed` | `decimal` | 本月已用额度 |
| `packageStatus` | `string` | 套餐状态 |
| `packageStatusText` | `string` | 套餐状态说明 |
| `remark` | `string` | 备注 |
| `systemPreset` | `boolean` | 是否系统预置套餐 |

## 4. 创建套餐分组

### 接口

- `POST /admin/model-access/groups`

### 请求体

```json
{
  "groupCode": "balance-basic",
  "groupName": "余额基础套餐",
  "packageType": "BALANCE",
  "salePrice": 0,
  "packageDays": 30,
  "dailyQuota": 0,
  "weeklyQuota": 0,
  "monthlyQuota": 0,
  "remark": "购买后按余额实时扣费"
}
```

### 请求参数说明

| 字段 | 是否必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `groupCode` | 是 | `string` | 套餐编码，系统内唯一 |
| `groupName` | 是 | `string` | 套餐名称 |
| `packageType` | 是 | `string` | 套餐类型：`QUOTA` / `BALANCE` |
| `salePrice` | 是 | `decimal` | 套餐售价，余额套餐可传 `0` |
| `packageDays` | 是 | `int` | 套餐有效天数 |
| `dailyQuota` | 是 | `decimal` | 日额度；余额套餐建议传 `0` |
| `weeklyQuota` | 是 | `decimal` | 周额度；余额套餐建议传 `0` |
| `monthlyQuota` | 是 | `decimal` | 月额度；余额套餐建议传 `0` |
| `remark` | 否 | `string` | 备注 |

### 成功响应

- `data` 返回 `ModelAccessSummaryResponse`
- 字段结构同 `GET /admin/model-access/summary`

## 5. 修改套餐分组

### 接口

- `PUT /admin/model-access/groups/{groupId}`

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `groupId` | `long` | 套餐分组 ID |

### 请求体

```json
{
  "groupCode": "balance-basic",
  "groupName": "余额基础套餐",
  "packageType": "BALANCE",
  "salePrice": 0,
  "packageDays": 30,
  "dailyQuota": 0,
  "weeklyQuota": 0,
  "monthlyQuota": 0,
  "remark": "余额不足时禁止继续调用"
}
```

### 说明

- 现在支持显式修改 `packageType`
- 系统预置套餐不允许修改套餐类型
- `BALANCE` 套餐会按余额模式处理，额度字段会按 `0` 处理

### 成功响应

- `data` 返回 `ModelAccessSummaryResponse`
- 字段结构同 `GET /admin/model-access/summary`

## 6. 购买套餐

### 接口

- `POST /admin/model-access/purchase`

### 请求体

```json
{
  "groupId": 2
}
```

### 请求参数说明

| 字段 | 是否必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `groupId` | 是 | `long` | 要购买的套餐分组 ID |

### 购买规则

- 普通额度套餐：
  - 从钱包余额扣套餐售价
- 余额套餐：
  - 购买价格可为 `0`
  - 购买时不扣款
  - 但钱包余额必须 `>= 1.00`

### 失败提示

| 场景 | 提示 |
| --- | --- |
| 余额套餐购买时钱包余额低于 1 美元 | `当前余额不足 1 美元，无法购买该套餐，请先充值` |
| 普通套餐购买时余额不足 | `余额不足，请先充值` |

### 成功响应

- `data` 返回 `ModelAccessSummaryResponse`
- 字段结构同 `GET /admin/model-access/summary`

## 7. 获取套餐购买记录

### 接口

- `GET /admin/model-access/purchases`

### 说明

- 普通用户只看自己的购买记录
- 管理员查看全站购买记录

### 成功响应

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": 12,
      "userId": 1001,
      "username": "demo",
      "groupId": 2,
      "groupCode": "balance-basic",
      "groupName": "余额基础套餐",
      "packageType": "BALANCE",
      "modelCount": 8,
      "purchasePrice": 0,
      "startAt": "2026-05-04T10:00:00",
      "expiresAt": "2026-06-03T10:00:00",
      "status": "ACTIVE",
      "createdAt": "2026-05-04T10:00:00",
      "active": true,
      "dailyQuota": 0,
      "weeklyQuota": 0,
      "monthlyQuota": 0,
      "totalQuota": 0,
      "dailyUsed": 1.23,
      "weeklyUsed": 3.21,
      "monthlyUsed": 5.67,
      "totalUsed": 5.67,
      "remainingDays": 29
    }
  ]
}
```

### `data[]` 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `long` | 购买记录 ID |
| `userId` | `long` | 用户 ID |
| `username` | `string` | 用户名 |
| `groupId` | `long` | 套餐分组 ID |
| `groupCode` | `string` | 套餐分组编码 |
| `groupName` | `string` | 套餐分组名称 |
| `packageType` | `string` | 套餐类型：`QUOTA` / `BALANCE` |
| `modelCount` | `int` | 套餐下模型数量 |
| `purchasePrice` | `decimal` | 本次购买价格 |
| `startAt` | `datetime` | 生效时间 |
| `expiresAt` | `datetime` | 过期时间 |
| `status` | `string` | 购买状态 |
| `createdAt` | `datetime` | 创建时间 |
| `active` | `boolean` | 当前是否有效 |
| `dailyQuota` | `decimal` | 日额度 |
| `weeklyQuota` | `decimal` | 周额度 |
| `monthlyQuota` | `decimal` | 月额度 |
| `totalQuota` | `decimal` | 总额度 |
| `dailyUsed` | `decimal` | 今日已用额度 |
| `weeklyUsed` | `decimal` | 本周已用额度 |
| `monthlyUsed` | `decimal` | 本月已用额度 |
| `totalUsed` | `decimal` | 累计已用额度 |
| `remainingDays` | `long` | 剩余天数 |

## 8. 余额套餐调用规则说明

虽然网关调用接口本次没有新增独立管理接口，但返回和业务已经调整：

- 如果 API Key 绑定的是 `BALANCE` 套餐：
  - 只扣钱包余额
  - 不扣普通套餐额度
  - 不自动切到普通套餐
- 如果钱包余额 `< 0.50`：
  - 直接拒绝调用
  - 提示：`当前余额不足，请先充值`
- 如果本次请求应扣金额大于钱包当前余额：
  - 直接拒绝调用
  - 提示：`当前余额不足，请先充值`

