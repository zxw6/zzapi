# QQ 邮箱重置密码接口文档

本文档说明“根据 QQ 邮箱重新修改密码”的后端接口。

接口统一返回结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 请求是否成功 |
| `message` | `string` | 返回提示信息 |
| `data` | `object / null` | 业务数据；无业务数据时为 `null` |

## 1. 发送重置密码验证码

### 接口信息

| 项目 | 内容 |
| --- | --- |
| 请求方式 | `POST` |
| 请求路径 | `/admin/auth/password/reset/code` |
| 是否需要登录 | 否 |
| Content-Type | `application/json` |
| 说明 | 给已注册且状态正常的 QQ 邮箱发送重置密码验证码 |

### 请求参数

```json
{
  "email": "123456@qq.com"
}
```

| 参数 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `email` | `string` | 是 | QQ 邮箱，必须符合 `xxx@qq.com` 格式 |

### 成功返回示例

```json
{
  "success": true,
  "message": "重置密码验证码发送成功",
  "data": {
    "email": "123456@qq.com",
    "expireSeconds": 300
  }
}
```

### 返回参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 固定为 `true` 表示发送成功 |
| `message` | `string` | 成功提示，当前为 `重置密码验证码发送成功` |
| `data.email` | `string` | 接收验证码的 QQ 邮箱 |
| `data.expireSeconds` | `long` | 验证码有效期，单位秒，默认 `300` |

### 常见失败返回

#### QQ 邮箱为空

```json
{
  "success": false,
  "message": "QQ邮箱不能为空",
  "data": null
}
```

#### 不是 QQ 邮箱

```json
{
  "success": false,
  "message": "请使用QQ邮箱",
  "data": null
}
```

#### 邮箱未注册

```json
{
  "success": false,
  "message": "QQ邮箱不存在",
  "data": null
}
```

#### 用户账号被禁用

```json
{
  "success": false,
  "message": "账号已被禁用",
  "data": null
}
```

#### 验证码发送太频繁

```json
{
  "success": false,
  "message": "验证码发送过于频繁，请稍后再试",
  "data": null
}
```

## 2. 根据 QQ 邮箱重置密码

### 接口信息

| 项目 | 内容 |
| --- | --- |
| 请求方式 | `POST` |
| 请求路径 | `/admin/auth/password/reset` |
| 是否需要登录 | 否 |
| Content-Type | `application/json` |
| 说明 | 校验 QQ 邮箱验证码，校验通过后更新该邮箱对应用户的新密码 |

### 请求参数

```json
{
  "email": "123456@qq.com",
  "verificationCode": "123456",
  "newPassword": "newPassword123"
}
```

| 参数 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `email` | `string` | 是 | QQ 邮箱，必须符合 `xxx@qq.com` 格式 |
| `verificationCode` | `string` | 是 | 6 位数字验证码 |
| `newPassword` | `string` | 是 | 新密码，长度 `6-72` 个字符 |

### 成功返回示例

```json
{
  "success": true,
  "message": "密码重置成功",
  "data": null
}
```

### 返回参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 固定为 `true` 表示密码重置成功 |
| `message` | `string` | 成功提示，当前为 `密码重置成功` |
| `data` | `null` | 该接口成功时不返回业务数据 |

### 常见失败返回

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

#### 新密码为空

```json
{
  "success": false,
  "message": "新密码不能为空",
  "data": null
}
```

#### 新密码长度不合法

```json
{
  "success": false,
  "message": "新密码长度必须为6-72位",
  "data": null
}
```

#### 邮箱未注册

```json
{
  "success": false,
  "message": "QQ邮箱不存在",
  "data": null
}
```

## 3. 前端调用流程

1. 用户输入 QQ 邮箱。
2. 调用 `POST /admin/auth/password/reset/code` 发送验证码。
3. 用户输入邮箱验证码和新密码。
4. 调用 `POST /admin/auth/password/reset` 完成密码重置。
5. 重置成功后，引导用户回到登录页使用新密码登录。

## 4. 后端实现说明

| 项目 | 说明 |
| --- | --- |
| 验证码 Redis Key | `auth:verify:password-reset:{email}` |
| 验证码冷却 Key | `auth:verify:password-reset:cooldown:{email}` |
| 验证码有效期 | 默认 `300` 秒，由 `app.auth.verification-code.expire-seconds` 控制 |
| 重发冷却时间 | 默认 `60` 秒，由 `app.auth.verification-code.resend-interval-seconds` 控制 |
| 邮件标题配置 | `app.mail.password-reset-code-subject`，默认 `AI LayCode 重置密码验证码` |
| 密码保存方式 | 使用项目现有 `PasswordService.encode(...)` 加密后写入 `users.password_hash` |
