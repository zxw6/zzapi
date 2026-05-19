# 登录图形验证码与登录接口文档

## 1. 获取登录图形验证码

### 接口信息

- 接口名称：获取登录图形验证码
- 请求方式：`GET`
- 请求地址：`/admin/auth/login/captcha`
- 接口说明：登录前先获取一张图形验证码图片，前端展示给用户输入

### 请求参数

无

### 成功响应

```json
{
  "success": true,
  "message": "获取图形验证码成功",
  "data": {
    "captchaId": "9f7dc5db6e0e4bc88d4a36f7f7d15b0d",
    "imageBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIQAAAAsCAIAAAD0...",
    "expireSeconds": 120
  }
}
```

### 最外层返回参数

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| success | boolean | 是否成功 |
| message | String | 返回消息 |
| data | LoginCaptchaResponse | 图形验证码数据 |

### data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| captchaId | String | 本次图形验证码唯一标识，登录时必须回传 |
| imageBase64 | String | 图形验证码图片的 Base64 完整数据，前端可直接赋值给 `img.src` |
| expireSeconds | long | 图形验证码有效期，单位秒 |

### 前端接入说明

- 页面打开时先调用一次该接口
- 用户点击图片或“刷新验证码”按钮时再次调用
- 登录失败后建议重新拉取一张新验证码
- 登录时要把 `captchaId` 和用户输入的 `captchaCode` 一起传给登录接口

---

## 2. 登录接口

### 接口信息

- 接口名称：管理员登录
- 请求方式：`POST`
- 请求地址：`/admin/auth/login`
- 接口说明：使用用户名、密码和图形验证码完成登录

### 请求体

```json
{
  "username": "admin",
  "password": "123456",
  "captchaId": "9f7dc5db6e0e4bc88d4a36f7f7d15b0d",
  "captchaCode": "A7K3"
}
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| captchaId | String | 是 | 图形验证码 ID，来自获取验证码接口 |
| captchaCode | String | 是 | 用户输入的图形验证码 |

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

### 最外层返回参数

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| success | boolean | 是否成功 |
| message | String | 返回消息 |
| data | AdminLoginResponse | 登录成功后的用户信息和令牌 |

### data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| token | String | 登录令牌，后续请求放到 `Authorization: Bearer {token}` |
| userId | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| roleCode | String | 角色编码，常见值：`ADMIN`、`USER` |
| balance | BigDecimal | 当前账户余额 |

---

## 3. 登录失败响应示例

### 图形验证码错误

```json
{
  "success": false,
  "message": "图形验证码不正确",
  "data": null
}
```

### 图形验证码过期

```json
{
  "success": false,
  "message": "图形验证码已过期，请刷新后重试",
  "data": null
}
```

### 用户名或密码错误

```json
{
  "success": false,
  "message": "用户名或密码错误",
  "data": null
}
```

### 账号被禁用

```json
{
  "success": false,
  "message": "账号已被禁用",
  "data": null
}
```

### 参数缺失

```json
{
  "success": false,
  "message": "captchaCode: 图形验证码不能为空",
  "data": null
}
```

---

## 4. 前端接入流程建议

1. 页面加载时调用 `GET /admin/auth/login/captcha`
2. 将返回的 `imageBase64` 直接赋值给图片组件的 `src`
3. 将返回的 `captchaId` 暂存到表单或页面状态
4. 用户输入用户名、密码、图形验证码后，调用 `POST /admin/auth/login`
5. 登录失败时，清空验证码输入框，并重新请求一张新的图形验证码
6. 登录成功后，保存 `token`
