# 快速启动

## 1. 运行要求

- JDK 17
- MySQL 8.x

注意：当前项目使用的是 `Spring Boot 3.x`，不能用 JDK 8 启动。

## 2. 数据库初始化

把下面这个 SQL 文件执行到你的 MySQL 数据库里：

- `src/main/resources/db/schema-mysql.sql`

数据库默认连接配置在：

- `src/main/resources/application.yml`

也可以通过环境变量覆盖：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `GATEWAY_CRYPTO_SECRET`

## 3. 启动命令

Windows PowerShell 示例：

```powershell
$env:JAVA_HOME='D:\JAVA\JDK\jdk17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
./mvnw spring-boot:run
```

## 4. 后台地址

- 管理台前端：`http://localhost:9988/console/index.html`
- 后台接口健康检查：`http://localhost:9988/admin/system/health`
- OpenAI 兼容接口：`http://localhost:9988/v1/chat/completions`

## 5. 默认管理员

- 用户名：`admin`
- 密码：`admin123456`

应用启动时，如果数据库里没有 `admin`，系统会自动创建。

## 6. 最简使用顺序

1. 创建普通用户
2. 给用户充值
3. 创建用户 API Key
4. 配置上游渠道和 Token
5. 创建平台模型并绑定上游模型
6. 用用户 API Key 调用 `/v1/chat/completions`
