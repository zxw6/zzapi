# 注册邮箱验证码接口文档

本文档只说明当前后端“邮箱验证码注册”相关接口：

- 发送注册验证码
- 用户注册

当前实现说明：

- 只允许 `QQ 邮箱` 注册
- 验证码会真实发送到用户填写的邮箱
- 后端不会再把验证码明文返回给前端
- 验证码默认 `5 分钟` 有效

## 1. 通用返回结构

所有接口统一返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `message` | `string` | 提示信息 |
| `data` | `object / null` | 业务数据 |

## 2. 发送注册验证码

### 接口地址

`POST /admin/auth/register/code`

### 请求头

```http
Content-Type: application/json
```

### 请求参数

```json
{
  "email": "123456@qq.com"
}
```

### 请求参数说明

| 字段 | 是否必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `email` | 是 | `string` | 注册邮箱，当前只允许 `@qq.com` |

### 请求校验规则

| 字段 | 规则 |
| --- | --- |
| `email` | 不能为空 |
| `email` | 必须是合法邮箱格式 |
| `email` | 必须匹配 `@qq.com` |

### 成功返回示例

```json
{
  "success": true,
  "message": "验证码发送成功",
  "data": {
    "email": "123456@qq.com",
    "expireSeconds": 300
  }
}
```

### 返回参数说明

#### 外层 `ApiResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `message` | `string` | 一般成功时为 `验证码发送成功` |
| `data` | `VerificationCodeSendResponse` | 发送结果 |

#### `data` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `email` | `string` | 实际发送到的邮箱 |
| `expireSeconds` | `long` | 验证码有效期，单位秒 |

### 失败场景示例

#### 邮箱为空

```json
{
  "success": false,
  "message": "QQ 邮箱不能为空",
  "data": null
}
```

#### 不是 QQ 邮箱

```json
{
  "success": false,
  "message": "请使用 QQ 邮箱",
  "data": null
}
```

#### 发送过于频繁

```json
{
  "success": false,
  "message": "验证码发送过于频繁，请稍后再试",
  "data": null
}
```

#### 邮件发送失败

```json
{
  "success": false,
  "message": "验证码邮件发送失败，请稍后重试",
  "data": null
}
```

## 3. 用户注册

### 接口地址

`POST /admin/auth/register`

### 请求头

```http
Content-Type: application/json
```

### 请求参数

```json
{
  "username": "test001",
  "password": "123456",
  "nickname": "测试用户",
  "email": "123456@qq.com",
  "phone": "13800000000",
  "verificationCode": "123456"
}
```

### 请求参数说明

| 字段 | 是否必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `username` | 是 | `string` | 用户名 |
| `password` | 是 | `string` | 登录密码 |
| `nickname` | 否 | `string` | 昵称 |
| `email` | 是 | `string` | QQ 邮箱 |
| `phone` | 否 | `string` | 手机号 |
| `verificationCode` | 是 | `string` | 邮箱收到的 6 位验证码 |

### 请求校验规则

| 字段 | 规则 |
| --- | --- |
| `username` | 不能为空 |
| `password` | 不能为空 |
| `email` | 不能为空 |
| `email` | 必须是合法邮箱 |
| `email` | 必须是 `@qq.com` |
| `verificationCode` | 不能为空 |
| `verificationCode` | 必须是 6 位数字 |

### 成功返回示例

```json
{
  "success": true,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx",
    "userId": 1001,
    "username": "test001",
    "nickname": "测试用户",
    "roleCode": "USER",
    "balance": 0
  }
}
```

### 返回参数说明

#### 外层 `ApiResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `message` | `string` | 一般成功时为 `注册成功` |
| `data` | `AdminLoginResponse` | 注册成功后的登录信息 |

#### `data` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `token` | `string` | 注册成功后自动登录返回的 JWT |
| `userId` | `long` | 用户 ID |
| `username` | `string` | 用户名 |
| `nickname` | `string` | 昵称 |
| `roleCode` | `string` | 角色，当前普通注册用户固定为 `USER` |
| `balance` | `decimal` | 钱包余额，默认一般为 `0` |

### 失败场景示例

#### 验证码为空

```json
{
  "success": false,
  "message": "验证码不能为空",
  "data": null
}
```

#### 验证码错误

```json
{
  "success": false,
  "message": "验证码不正确",
  "data": null
}
```

#### 验证码过期

```json
{
  "success": false,
  "message": "验证码已过期",
  "data": null
}
```

#### 用户名已存在

```json
{
  "success": false,
  "message": "用户名已存在",
  "data": null
}
```

#### QQ 邮箱已存在

```json
{
  "success": false,
  "message": "QQ 邮箱已存在",
  "data": null
}
```

## 4. 推荐联调顺序

### 第一步：发送验证码

```http
POST /admin/auth/register/code
```

确认返回成功，并且目标邮箱确实收到验证码。

### 第二步：提交注册

```http
POST /admin/auth/register
```

把邮箱里收到的 6 位验证码填入 `verificationCode`，注册成功后会直接返回 `token`。

## 5. 当前后端限制

- 当前只支持 `QQ 邮箱`
- 验证码默认有效期为 `300 秒`
- 同一个邮箱发送验证码有冷却时间
- 注册成功后自动创建普通用户和默认钱包

