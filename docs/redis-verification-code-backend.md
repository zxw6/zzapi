# Redis 验证码后端实现说明

## 1. 概述

本次改动为项目新增了“注册验证码写入 Redis 并在注册时校验”的后端能力。

当前实现目标：

- 提供发送注册验证码接口
- 将验证码存入 Redis
- 设置验证码过期时间
- 设置发送冷却时间，避免频繁请求
- 用户注册时校验验证码
- 校验成功后删除 Redis 中的验证码

当前范围仅包含后端生成、存储、校验逻辑，不包含真实邮件或短信发送。

## 2. 本次改动的文件

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/com/zxw/config/WebMvcConfig.java`
- `src/main/java/com/zxw/modules/auth/controller/AdminAuthController.java`
- `src/main/java/com/zxw/modules/auth/service/AdminAuthService.java`
- `src/main/java/com/zxw/modules/auth/service/VerificationCodeService.java`
- `src/main/java/com/zxw/modules/auth/dto/UserRegisterRequest.java`
- `src/main/java/com/zxw/modules/auth/dto/VerificationCodeSendRequest.java`
- `src/main/java/com/zxw/modules/auth/dto/VerificationCodeSendResponse.java`

## 3. 依赖说明

项目新增了 Redis 依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

这样 Spring Boot 会自动装配 `StringRedisTemplate`，供验证码服务直接使用。

## 4. 配置说明

在 `application.yml` 中新增了 Redis 配置：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: ${REDIS_TIMEOUT:5s}
```

同时新增了验证码业务配置：

```yaml
app:
  auth:
    verification-code:
      expire-seconds: ${VERIFICATION_CODE_EXPIRE_SECONDS:300}
      resend-interval-seconds: ${VERIFICATION_CODE_RESEND_INTERVAL_SECONDS:60}
      return-code-in-response: ${VERIFICATION_CODE_RETURN_IN_RESPONSE:true}
```

字段说明：

- `expire-seconds`
  - 验证码有效期，默认 `300` 秒
- `resend-interval-seconds`
  - 同一邮箱再次请求验证码的冷却时间，默认 `60` 秒
- `return-code-in-response`
  - 是否直接在接口响应中返回验证码
  - 默认是 `true`，方便联调
  - 生产环境建议改为 `false`

## 5. Redis Key 设计

### 5.1 验证码 Key

```text
auth:verify:register:{email}
```

示例：

```text
auth:verify:register:123456@qq.com
```

用途：

- 保存某个邮箱当前的注册验证码
- 自动过期

### 5.2 发送冷却 Key

```text
auth:verify:register:cooldown:{email}
```

示例：

```text
auth:verify:register:cooldown:123456@qq.com
```

用途：

- 控制同一邮箱在短时间内重复发送验证码

## 6. 接口设计

### 6.1 发送注册验证码

接口：

```http
POST /admin/auth/register/code
Content-Type: application/json
```

说明：

- 不需要 JWT
- 已在拦截器白名单中放开

请求体：

```json
{
  "email": "123456@qq.com"
}
```

成功响应示例：

```json
{
  "success": true,
  "message": "Verification code generated",
  "data": {
    "email": "123456@qq.com",
    "expireSeconds": 300,
    "code": "123456"
  }
}
```

注意：

- 当 `return-code-in-response=false` 时，`data.code` 会是 `null`
- 如果发送过于频繁，会返回 `429`

### 6.2 注册接口

接口：

```http
POST /admin/auth/register
Content-Type: application/json
```

请求体：

```json
{
  "username": "testuser",
  "password": "123456",
  "nickname": "demo",
  "email": "123456@qq.com",
  "phone": "13800138000",
  "verificationCode": "123456"
}
```

新增字段：

- `verificationCode`
  - 必填
  - 必须是 6 位数字

成功后行为：

- 创建用户
- 初始化钱包
- 自动签发 JWT

失败场景：

- 验证码为空
- 验证码过期
- 验证码错误
- 用户名已存在
- QQ 邮箱已存在

## 7. 后端处理流程

### 7.1 发送验证码流程

1. 校验邮箱不能为空且必须是 `@qq.com`
2. 检查冷却 Key 是否存在
3. 若存在，拒绝发送并返回 `429`
4. 生成 6 位随机验证码
5. 写入 Redis 验证码 Key
6. 写入 Redis 冷却 Key
7. 返回邮箱、有效期，以及是否返回验证码本体

### 7.2 注册流程

1. 校验邮箱格式
2. 校验验证码格式
3. 从 Redis 中读取对应邮箱的验证码
4. 若验证码不存在，视为过期
5. 若验证码不一致，视为错误
6. 校验成功后删除验证码 Key
7. 继续执行原有注册逻辑

## 8. 关键实现类说明

### 8.1 `VerificationCodeService`

职责：

- 生成验证码
- 写入 Redis
- 控制冷却时间
- 校验验证码
- 校验成功后删除缓存

### 8.2 `AdminAuthService`

新增能力：

- 提供 `sendRegisterCode(...)`
- 在 `register(...)` 中调用验证码校验逻辑

### 8.3 `AdminAuthController`

新增接口：

```http
POST /admin/auth/register/code
```

## 9. 安全与生产建议

当前实现适合开发联调和第一版业务落地，但上线前建议做以下增强：

- 将 `VERIFICATION_CODE_RETURN_IN_RESPONSE` 改为 `false`
- 接入真实邮件服务或短信服务
- 增加图形验证码或行为验证码，防止刷接口
- 增加 IP 级别限流
- 增加邮箱维度的日发送上限
- 对错误次数进行限制
- 对验证码进行脱敏日志处理，不要写入业务日志

## 10. 前端对接建议

推荐前端流程：

1. 用户输入 QQ 邮箱
2. 调用 `POST /admin/auth/register/code`
3. 用户输入收到的验证码
4. 调用 `POST /admin/auth/register`
5. 注册成功后保存返回的 JWT

如果当前还是前后端联调阶段，可以先直接读取发送验证码接口返回的 `data.code` 做联调。

## 11. 当前限制

当前版本有这些限制：

- 仅支持 QQ 邮箱
- 仅支持注册场景验证码
- 未接入真实邮件发送
- 未额外增加数据库表
- 验证码只存在 Redis 中，不做持久化

## 12. 后续可扩展方向

后续可以扩展为：

- 登录验证码
- 找回密码验证码
- 修改邮箱验证码
- 短信验证码
- 邮件模板发送
- 多场景统一验证码中心服务

## 13. 说明

本次仅完成代码与文档落地，未按本地运行方式做启动验证。
