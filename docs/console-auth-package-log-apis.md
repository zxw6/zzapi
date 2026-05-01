# 控制台认证、套餐与请求日志接口文档

## 1. 发送验证码接口

### 接口信息

- 接口名称：发送注册验证码
- 请求方式：`POST`
- 请求地址：`/admin/auth/register/code`
- 接口说明：向指定 QQ 邮箱发送 6 位注册验证码

### 请求参数

```json
{
  "email": "123456@qq.com"
}
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| email | String | 是 | QQ 邮箱地址 |

### 成功响应

```json
{
  "success": true,
  "message": "验证码发送成功",
  "data": {
    "email": "123456@qq.com",
    "expireSeconds": 300,
    "code": "123456"
  }
}
```

### data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| email | String | 验证码发送目标邮箱 |
| expireSeconds | long | 验证码有效期，单位秒 |
| code | String | 当前验证码。仅在系统允许回传验证码时返回，否则为 `null` |

### 常见失败响应

```json
{
  "success": false,
  "message": "验证码发送过于频繁，请稍后再试",
  "data": null
}
```

```json
{
  "success": false,
  "message": "请使用 QQ 邮箱",
  "data": null
}
```

## 2. 登录接口

### 接口信息

- 接口名称：管理员登录
- 请求方式：`POST`
- 请求地址：`/admin/auth/login`
- 接口说明：使用用户名和密码登录控制台

### 请求参数

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

### 成功响应

```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx",
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "roleCode": "ADMIN",
    "balance": 1000.00
  }
}
```

### data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| token | String | 登录令牌 |
| userId | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| roleCode | String | 角色编码，常见值：`ADMIN`、`USER` |
| balance | BigDecimal | 账户余额 |

### 登录失败中文响应示例

```json
{
  "success": false,
  "message": "用户名或密码错误",
  "data": null
}
```

```json
{
  "success": false,
  "message": "账号已被禁用",
  "data": null
}
```

```json
{
  "success": false,
  "message": "用户名不能为空",
  "data": null
}
```

## 3. 修改套餐接口

### 接口信息

- 接口名称：修改套餐分组
- 请求方式：`PUT`
- 请求地址：`/admin/model-access/groups/{groupId}`
- 接口说明：管理员修改指定套餐分组的编码、名称、价格、额度、有效期和备注信息

### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| groupId | Long | 是 | 套餐分组 ID |

### 请求体

```json
{
  "groupCode": "gpt-5-5",
  "groupName": "GPT 5.5 套餐",
  "salePrice": 15.00,
  "packageDays": 30,
  "dailyQuota": 60.00,
  "weeklyQuota": 420.00,
  "monthlyQuota": 1800.00,
  "remark": "管理员修改后的套餐说明"
}
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| groupCode | String | 是 | 套餐分组编码，不能为空，不能重复 |
| groupName | String | 是 | 套餐分组名称 |
| salePrice | BigDecimal | 是 | 套餐售价，不能小于 0 |
| packageDays | Integer | 是 | 套餐有效天数 |
| dailyQuota | BigDecimal | 是 | 日额度，不能小于 0 |
| weeklyQuota | BigDecimal | 是 | 周额度，不能小于 0 |
| monthlyQuota | BigDecimal | 是 | 月额度，不能小于 0 |
| remark | String | 否 | 备注说明 |

### 成功响应

```json
{
  "success": true,
  "message": "套餐修改成功",
  "data": {
    "packageRestrictionEnabled": true,
    "packageStatus": "ACTIVE",
    "packageStatusText": "使用中",
    "activeGroupId": 1,
    "activeGroupCode": "gpt-5-5",
    "activeGroupName": "GPT 5.5 套餐",
    "packagePrice": 15.00,
    "dailyQuota": 60.00,
    "weeklyQuota": 420.00,
    "monthlyQuota": 1800.00,
    "dailyUsed": 0.00,
    "weeklyUsed": 0.00,
    "monthlyUsed": 0.00,
    "expiresAt": "2026-05-29T10:00:00",
    "remainingDays": 30,
    "groups": [
      {
        "id": 1,
        "groupCode": "gpt-5-5",
        "groupName": "GPT 5.5 套餐",
        "salePrice": 15.00,
        "packageDays": 30,
        "dailyQuota": 60.00,
        "weeklyQuota": 420.00,
        "monthlyQuota": 1800.00,
        "modelCount": 12,
        "purchased": true,
        "active": true,
        "expiresAt": "2026-05-29T10:00:00",
        "remainingDays": 30,
        "dailyUsed": 0.00,
        "weeklyUsed": 0.00,
        "monthlyUsed": 0.00,
        "packageStatus": "ACTIVE",
        "packageStatusText": "使用中",
        "remark": "管理员修改后的套餐说明",
        "systemPreset": false
      }
    ]
  }
}
```

### 最外层返回参数

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| success | boolean | 是否成功 |
| message | String | 返回消息 |
| data | ModelAccessSummaryResponse | 最新套餐概览数据 |

### data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| packageRestrictionEnabled | boolean | 是否开启套餐限制 |
| packageStatus | String | 当前套餐状态 |
| packageStatusText | String | 当前套餐状态说明 |
| activeGroupId | Long | 当前激活套餐分组 ID |
| activeGroupCode | String | 当前激活套餐分组编码 |
| activeGroupName | String | 当前激活套餐分组名称 |
| packagePrice | BigDecimal | 当前套餐价格 |
| dailyQuota | BigDecimal | 日额度 |
| weeklyQuota | BigDecimal | 周额度 |
| monthlyQuota | BigDecimal | 月额度 |
| dailyUsed | BigDecimal | 今日已用额度 |
| weeklyUsed | BigDecimal | 本周已用额度 |
| monthlyUsed | BigDecimal | 本月已用额度 |
| expiresAt | LocalDateTime | 到期时间 |
| remainingDays | Long | 剩余天数 |
| groups | List<ModelGroupOptionResponse> | 当前账号可见的套餐分组列表 |

### groups 数组元素说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 套餐分组 ID |
| groupCode | String | 套餐分组编码 |
| groupName | String | 套餐分组名称 |
| salePrice | BigDecimal | 售价 |
| packageDays | Integer | 有效天数 |
| dailyQuota | BigDecimal | 日额度 |
| weeklyQuota | BigDecimal | 周额度 |
| monthlyQuota | BigDecimal | 月额度 |
| modelCount | Integer | 套餐下模型数量 |
| purchased | boolean | 当前账号是否已购买 |
| active | boolean | 当前账号是否正在使用中 |
| expiresAt | LocalDateTime | 到期时间 |
| remainingDays | Long | 剩余天数 |
| dailyUsed | BigDecimal | 今日已用额度 |
| weeklyUsed | BigDecimal | 本周已用额度 |
| monthlyUsed | BigDecimal | 本月已用额度 |
| packageStatus | String | 套餐状态 |
| packageStatusText | String | 套餐状态说明 |
| remark | String | 备注 |
| systemPreset | boolean | 是否系统预置套餐 |

## 4. 请求日志分页接口

### 接口信息

- 接口名称：分页查询请求日志
- 请求方式：`GET`
- 请求地址：`/admin/request-logs?page=1&pageSize=20`
- 接口说明：分页查询请求日志，替代原来的 `limit` 查询方式

### 查询参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| page | Integer | 否 | 1 | 当前页码，从 1 开始 |
| pageSize | Integer | 否 | 20 | 每页条数，最大 100 |

### 成功响应

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 156,
    "totalPages": 8,
    "hasPrevious": false,
    "hasNext": true,
    "records": [
      {
        "requestId": "req_xxx",
        "username": "admin",
        "modelCode": "gpt-5.5",
        "upstreamModel": "gpt-5.5",
        "packageName": "GPT 5.5 套餐",
        "multiplier": 1.0,
        "statusCode": 200,
        "latencyMs": 920,
        "promptTokens": 120,
        "completionTokens": 300,
        "totalTokens": 420,
        "cachedPromptTokens": 0,
        "userAmount": 0.12,
        "costAmount": 0.09,
        "success": 1,
        "createdAt": "2026-04-29T10:20:30"
      }
    ]
  }
}
```

### data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| page | int | 当前页码 |
| pageSize | int | 每页条数 |
| total | long | 总记录数 |
| totalPages | long | 总页数 |
| hasPrevious | boolean | 是否有上一页 |
| hasNext | boolean | 是否有下一页 |
| records | List<RequestLogItemResponse> | 当前页日志数据 |

### records 数组元素说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| requestId | String | 请求 ID |
| username | String | 用户名 |
| modelCode | String | 模型编码 |
| upstreamModel | String | 上游模型名 |
| packageName | String | 套餐名称 |
| multiplier | BigDecimal | 倍率 |
| statusCode | Integer | HTTP 状态码 |
| latencyMs | Integer | 耗时，毫秒 |
| promptTokens | Integer | 输入 token |
| completionTokens | Integer | 输出 token |
| totalTokens | Integer | 总 token |
| cachedPromptTokens | Integer | 缓存读取 token |
| userAmount | BigDecimal | 用户侧金额 |
| costAmount | BigDecimal | 成本金额 |
| success | Integer | 是否成功，1 成功，0 失败 |
| createdAt | LocalDateTime | 创建时间 |

## 备注

- 登录失败提示已统一改为中文
- 请求日志接口已改成分页，不再要求前端传 `limit`
- 前端已对“登录提交”和“发送验证码”加了防抖，避免连续点击重复请求
