# zzapi

zzapi 是一个面向 AI 应用的 API 网关系统，提供 OpenAI / Anthropic / Gemini 兼容接口、模型路由、供应商管理、API Key 鉴权、用户套餐、余额扣费、请求日志和图片生成结果 URL 化能力。

项目目标是把多个上游模型供应商统一封装成一套稳定的 API，同时提供后台管理能力，方便维护模型、渠道、用户额度和调用记录。

## 核心能力

- OpenAI 兼容 API
  - Chat Completions
  - Responses
  - Images Generations
  - Models
- Anthropic Messages 兼容接口
- Gemini Generate Content 兼容接口
- 多供应商渠道管理
- 上游模型绑定与路由管理
- API Key 鉴权
- 用户套餐与模型组权限
- 钱包余额扣费
- 请求日志与用量统计
- 首 token 延迟记录
- 图片生成结果本地保存并返回 URL
- 管理端与用户端接口隔离
- 用户端隐藏内部倍率、成本和供应商密钥

## 技术栈

- Java 17
- Spring Boot 3.5
- MyBatis Plus
- MySQL
- Redis
- Maven
- OkHttp
- JWT

## 接口概览

### 用户端网关接口

这些接口用于给用户、客户端或第三方 SDK 调用。

```txt
GET  /v1/models
GET  /models

POST /v1/chat/completions
POST /chat/completions

POST /v1/responses
POST /responses

POST /v1/images/generations
POST /images/generations

POST /v1/messages
POST /messages
Gemini 兼容接口：
POST /v1beta/models/{model}:generateContent
POST /v1/models/{model}:generateContent

POST /v1beta/models/{model}:streamGenerateContent
POST /v1/models/{model}:streamGenerateContent
管理端接口
管理端接口用于后台维护模型、供应商、API Key、用户、套餐和日志。
模型管理：
GET    /admin/models
POST   /admin/models
PUT    /admin/models/{id}
DELETE /admin/models/{id}
POST   /admin/models/import
供应商管理：
GET    /admin/providers
POST   /admin/providers
PUT    /admin/providers/{id}/status
DELETE /admin/providers/{id}
API Key 管理：
GET    /admin/api-keys
POST   /admin/api-keys
PUT    /admin/api-keys/{id}/status
DELETE /admin/api-keys/{id}
用户与钱包：
GET    /admin/users
POST   /admin/users
PUT    /admin/users/{userId}
DELETE /admin/users/{userId}
POST   /admin/users/recharge
套餐与权限：
GET    /admin/model-access/summary
POST   /admin/model-access/groups
PUT    /admin/model-access/groups/{groupId}
POST   /admin/model-access/purchase
GET    /admin/model-access/purchases
GET    /admin/model-access/wallet-transactions
请求日志：
GET    /admin/request-logs
DELETE /admin/request-logs
计费说明
系统按模型价格和用户侧规则计算费用。
用户端可见价格字段：
promptPrice
cachedPromptPrice
cacheWritePromptPrice
completionPrice
含义：
promptPrice            输入价格
cachedPromptPrice      缓存读取价格
cacheWritePromptPrice  缓存写入价格
completionPrice        输出价格
用户端不会返回以下内部字段：
multiplier
requestPrice
costAmount
说明：
multiplier 是后端内部倍率规则，管理员可配置，普通用户端不可见。
requestPrice 已不再作为前端计费字段使用。
用户端看到的是后端处理后的用户侧价格，不直接暴露倍率。
请求日志中普通用户可以看到首 token 延迟、token 用量和用户侧扣费，但不会看到内部成本和倍率。
图片生成
图片生成接口兼容 OpenAI Images API：
POST /v1/images/generations
当上游返回 b64_json 或 base64 图片结果时，后端会将图片保存到本地目录，并把响应转换为 URL，避免前端和日志中保存大段 base64。
生成图片访问路径示例：
/generated-images/{fileName}
本地开发
环境要求
JDK 17+
MySQL
Redis
Maven 或项目自带 Maven Wrapper
配置文件
主要配置文件：
src/main/resources/application.yml
需要根据本地环境配置：
服务端口
MySQL 地址、用户名、密码
Redis 地址
JWT 密钥
CORS 跨域配置
网关上游超时时间
数据库初始化
数据库结构文件：
src/main/resources/db/schema-mysql.sql
首次部署前请先创建数据库并导入该 SQL 文件。
编译与打包
Windows：
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd -DskipTests package
Linux / macOS：
./mvnw -DskipTests compile
./mvnw -DskipTests package
打包产物：
target/zzapi.jar
运行：
java -jar target/zzapi.jar
项目结构
src/main/java/com/zxw
├── common
│   ├── api          通用响应结构
│   ├── exception    业务异常与全局异常处理
│   └── security     JWT、鉴权上下文、密码与加密服务
├── config           Spring Boot、CORS、并发、上游连接配置
├── modules
│   ├── auth         登录、注册、验证码、密码重置
│   ├── apikey       API Key 管理与鉴权
│   ├── gateway      网关协议适配、上游转发、扣费与日志
│   ├── model        模型、路由、上游模型管理
│   ├── provider     供应商渠道管理
│   ├── access       套餐、模型组、额度、余额
│   ├── request      请求日志
│   ├── user         用户与钱包管理
│   ├── usage        API Key 套餐用量
│   └── dashboard    仪表盘统计
└── persistence
    ├── entity       数据库实体
    ├── mapper       MyBatis Mapper
    └── model        查询视图对象
安全建议
不要在前端保存或暴露供应商 API Key
不要向普通用户返回倍率、成本、供应商 Token 等内部字段
生产环境必须更换默认 JWT 密钥
生产环境必须使用强数据库密码
建议开启 HTTPS
建议对管理端接口增加更严格的访问控制
建议定期清理请求日志和生成图片文件
部署建议
推荐生产部署方式：
Nginx / HTTPS
        ↓
zzapi.jar
        ↓
MySQL + Redis
可以使用 systemd、Docker、宝塔、1Panel 或其他进程管理工具运行 zzapi.jar。
启动示例：
java -jar target/zzapi.jar
后台运行时建议将日志输出到独立文件，并配置日志轮转
