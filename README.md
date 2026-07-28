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
