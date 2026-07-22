# AI 中转站 MySQL 版设计

## 目标

基于现有 `Spring Boot + MySQL` 项目，先落一个可运行的中转站后端骨架，优先覆盖：

- OpenAI 兼容接口代理
- 上游供应商管理
- 用户与 API Key 管理
- 仪表盘统计
- 钱包与扣费

## 推荐目录

```text
src/main/java/com/zxw
├─ common                 # 通用返回、异常、工具类
├─ config                 # 配置类、配置属性
├─ modules
│  ├─ auth                # 后台登录、JWT、权限
│  ├─ user                # 用户管理
│  ├─ apikey              # 用户 API Key
│  ├─ provider            # 上游平台和密钥池
│  ├─ model               # 平台模型和路由
│  ├─ gateway             # OpenAI 兼容入口、转发逻辑
│  ├─ request             # 请求日志、用量聚合
│  ├─ wallet              # 钱包、流水、充值
│  ├─ dashboard           # 仪表盘
│  └─ system              # 系统配置、审计日志、健康检查
└─ TestApplication.java
```

## 核心表

- `users`：平台用户、管理员
- `api_keys`：用户访问密钥
- `providers`：上游供应商
- `provider_tokens`：上游 key 池
- `models`：平台展示模型
- `model_routes`：模型映射到哪个上游
- `wallets`：余额
- `transactions`：充值和扣费流水
- `request_logs`：单次请求明细
- `usage_daily`：日报表聚合
- `system_configs`：系统设置
- `audit_logs`：后台操作审计

## 仪表盘第一版指标

- 总用户数
- 可用 API Key 数
- 渠道数
- 上架模型数
- 今日请求数
- 今日充值金额
- 今日消费金额
- 总钱包余额

趋势图建议直接读取 `usage_daily`，不要每次都扫 `request_logs`。

## 第一版接口建议

- `GET /admin/system/health`
- `GET /admin/dashboard/overview`
- `GET /admin/dashboard/trend?days=7`
- `POST /v1/chat/completions`
- `POST /v1/embeddings`
- `GET /admin/providers`
- `GET /admin/models`
- `GET /admin/request-logs`

## 下一步实现顺序

1. 加后台登录和 JWT
2. 加用户、API Key、渠道、模型的增删改查
3. 加 OpenAI 兼容转发入口
4. 记录请求日志并异步汇总到 `usage_daily`
5. 接前端仪表盘页面

## 安全提醒

- 不要在代码里硬编码上游 API Key
- `provider_tokens.token_value_encrypted` 应做加密存储
- `api_keys.secret_hash` 不要明文保存
- 请求日志中的正文建议支持开关，避免隐私数据直接落库
