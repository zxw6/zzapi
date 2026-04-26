# AI 网关项目渐进式重构方案

> 目标:在不破坏现有功能的前提下,把项目升级到现代化技术栈。
> 原则:**每完成一个阶段,项目必须能跑起来、能回滚。** 不允许出现"改一半跑不起来"的中间状态。
>
> 适用版本:Spring Boot 3.5.13 → 3.5.x(保持当前主版本),Java 17 保留(可选升 21)。

---

## 0. 当前架构盘点

| 项 | 现状 | 痛点 |
| --- | --- | --- |
| Java | 17 | 可保留;升 21 才能用虚拟线程(可选) |
| ORM | 纯 JdbcTemplate(15 个 Service 全是手写 SQL) | 简单 CRUD 重复劳动多 |
| HTTP 客户端 | OkHttp + JDK HttpClient 混用([GatewayChatService.java:128-131](src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java#L128)) | 手写 SSE 转换、超时配置散乱 |
| 协议适配 | [GatewayGeminiController.java](src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java) 手撸 Gemini ↔ OpenAI 协议互转 | 重造轮子,每加一个上游就要写一遍 |
| 鉴权 | 自己写 JWT Filter + Interceptor + ThreadLocal | 能用,无需改 |
| 限流/熔断 | **无** | 上游挂了能拖垮整个网关 |
| 可观测 | 自建 request_logs 表 | 没有指标接口、没有健康检查、没有 Tracing |
| API 文档 | **无** | 前端/对接方靠猜 |
| 测试 | 1 个空 SpringBootTest | 无回归保障 |
| 部署 | 几个 .cmd 脚本 | 无 Docker、无 CI |

---

## 1. 目标架构 + 总体路线图

```
                     ┌─────────────────────────────────────┐
                     │         前端(暂不动)                │
                     └─────────────────────────────────────┘
                                       ↓ HTTP
   ┌────────────────────────────────────────────────────────────────┐
   │  Spring Boot 3.5 + Java 21 (虚拟线程开启)                     │
   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
   │  │ OpenAPI  │  │ Actuator │  │  CORS    │  │  JWT     │       │
   │  │ /swagger │  │ /metrics │  │  Filter  │  │  Filter  │       │
   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
   │                                                                 │
   │  ┌────────────────── Gateway 层(核心改造)──────────────────┐ │
   │  │  Controller → ChatService → Spring AI ChatClient          │ │
   │  │                              ↓                             │ │
   │  │                         Resilience4j(限流/熔断/重试)     │ │
   │  │                              ↓                             │ │
   │  │                         RestClient(替换 OkHttp)          │ │
   │  │                              ↓                             │ │
   │  │                         上游(OpenAI/Gemini/Anthropic)     │ │
   │  └───────────────────────────────────────────────────────────┘ │
   │                                                                 │
   │  ┌─────────────── 数据层(按需改造)──────────────────────────┐│
   │  │  Service ─┬─→ JdbcTemplate(老代码,先保留)              ││
   │  │           └─→ MyBatis-Plus Mapper(新代码用,渐进迁移)   ││
   │  │           └─→ Caffeine(模型路由、API Key 缓存)          ││
   │  └───────────────────────────────────────────────────────────┘│
   └────────────────────────────────────────────────────────────────┘
                                       ↓
                  MySQL(无变化)        Redis(无变化)
                                       ↓
                     Prometheus + Grafana(新增,可选)
```

### 路线图(按风险从低到高)

| 阶段 | 内容 | 工作量 | 风险 | 是否可选 |
| --- | --- | --- | --- | --- |
| 1 | Java 21 + 虚拟线程 | 0.5 天 | 极低 | **可选**(留 17 也行) |
| 2 | Springdoc-OpenAPI(Swagger UI) | 0.5 天 | 极低 | 必做 |
| 3 | Actuator + Micrometer + Prometheus | 0.5 天 | 极低 | 必做 |
| 4 | RestClient 替换 OkHttp | 1-2 天 | 低 | 必做(为阶段 5、6 铺路) |
| 5 | **Sentinel**(限流/熔断/降级) | 1-1.5 天 | 低 | 必做(网关刚需) |
| 6 | Spring AI 重写协议适配层 | 3-5 天 | 中 | 强烈推荐 |
| 7 | MyBatis-Plus 渐进迁移 | 3-7 天 | 中 | 可选 |
| 8 | Caffeine 缓存 | 0.5 天 | 低 | 可选 |
| 9 | Testcontainers + 集成测试 | 2-3 天 | 低 | 可选 |
| 10 | Docker + GitHub Actions | 1 天 | 低 | 可选 |

**总计:必做部分约 4-5 天,加上 Spring AI 改造约 8-10 天。**

---

## 阶段 0:准备工作(开始任何改造前都要做)

```bash
# 1. 新建分支
git checkout -b refactor/modernize

# 2. 备份数据库 schema
mysqldump -u root -p zhongzhuan --no-data > backup-schema-$(date +%Y%m%d).sql

# 3. 把现有 .out.log 清干净,方便后续看新日志
git rm run-app.*.log run-packaged.*.log run-gateway.out.log 2>/dev/null
echo "run-*.log" >> .gitignore
```

**每个阶段完成后必做:**
1. 启动应用,跑一遍核心场景(登录 + 一次 chat completion)
2. `git commit -m "phase X: ..."` 单独提交
3. 出问题立刻 `git revert <commit>`,不要在烂摊子上继续改

---

## 阶段 1(可选):升级到 Java 21 + 虚拟线程

> ⚠️ **此阶段可跳过。** Spring Boot 3.5 完整支持 Java 17,留在 17 完全没问题。
> 唯一损失:虚拟线程(Loom)需要 Java 21。如果你现有线上是 17 的运维环境,**不必升级**,后续所有阶段都兼容 17。
> 如果 OK 升级,继续往下;否则**直接跳到阶段 2**。

### 目标
- Java 17 → 21
- 开启虚拟线程,让 I/O 密集型的网关吞吐量翻倍

### 修改 [pom.xml](pom.xml)

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
    <!-- 其他保持不变 -->
</properties>
```

`maven-compiler-plugin` 里 3 处 `<release>17</release>` 也全改成 `21`。

### 修改 [application.yml](src/main/resources/application.yml)

在 `spring:` 下增加:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 验证

```bash
./mvnw clean package -DskipTests
java --version    # 确认本机 JDK 21
java -jar target/Test-0.0.1-SNAPSHOT.jar
```

启动日志里应该能看到 Tomcat 用虚拟线程池。打一次 chat completions 接口确认能跑通。

### 回滚

`git revert HEAD` 即可。

### 不升级 21 的替代方案

如果留在 JDK 17,网关吞吐量优化可以靠这两个手段:
1. 调大 Tomcat 线程池:`server.tomcat.threads.max=400`(默认 200)
2. 流式接口用 `StreamingResponseBody`(项目已经在用)+ Servlet 3.0 异步,不会阻塞主线程

---

## 阶段 2:加 Springdoc-OpenAPI(Swagger UI)

### 目标
所有 Controller 自动生成在线 API 文档,访问 `http://localhost:9988/swagger-ui.html`。

### pom.xml 增加依赖

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### application.yml 增加配置

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: method
  packages-to-scan: com.zxw
```

### 修改 [WebMvcConfig.java](src/main/java/com/zxw/config/WebMvcConfig.java)

把 Swagger 路径加到 `AdminAuthInterceptor` 的排除列表里:

```java
.excludePathPatterns(
    "/admin/auth/login",
    "/admin/auth/register",
    "/admin/auth/health",
    "/swagger-ui/**",       // 新增
    "/swagger-ui.html",     // 新增
    "/v3/api-docs/**"       // 新增
)
```

### 给 Controller 加注解(可选,文档更友好)

只对核心 Controller 加,其他可以后续慢慢补:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Gateway - Chat", description = "OpenAI 兼容的 chat completions 接口")
public class GatewayChatController {

    @Operation(summary = "Chat Completions(支持流式)")
    @PostMapping(...)
    public ... chatCompletions(...) { ... }
}
```

### 验证

启动后访问 `http://localhost:9988/swagger-ui.html`,应该能看到所有接口列表。

---

## 阶段 3:加 Actuator + Micrometer + Prometheus

### 目标
- 健康检查 `/actuator/health`
- JVM/Tomcat 指标 `/actuator/prometheus`
- 后续可以接 Grafana 看大盘

### pom.xml 增加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### application.yml 增加配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
    tags:
      application: ${spring.application.name}
```

### 修改 [WebMvcConfig.java](src/main/java/com/zxw/config/WebMvcConfig.java)

把 `/actuator/**` 加入排除列表(同上)。

### 自定义业务指标(可选)

在 `GatewayChatService` 里注入 `MeterRegistry`,记录上游耗时:

```java
private final Timer upstreamTimer;

public GatewayChatService(MeterRegistry registry, ...) {
    this.upstreamTimer = Timer.builder("gateway.upstream.duration")
        .description("上游 AI 接口耗时")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);
}

// 调用上游时:
Timer.Sample sample = Timer.start(registry);
try {
    // 调上游
} finally {
    sample.stop(upstreamTimer.tags("provider", route.providerCode(), "model", model));
}
```

### 验证

```bash
curl http://localhost:9988/actuator/health
curl http://localhost:9988/actuator/prometheus | head -50
```

---

## 阶段 4:用 RestClient 替换 OkHttp

### 目标
- 干掉 OkHttp 依赖
- 用 Spring 6 的 `RestClient`(同步)统一所有 HTTP 调用
- 为后续 Spring AI 改造铺路(Spring AI 底层就是 RestClient)

### 步骤

#### 4.1 新建 RestClient 配置

创建 [src/main/java/com/zxw/config/HttpClientConfig.java](src/main/java/com/zxw/config/HttpClientConfig.java)

```java
package com.zxw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient upstreamRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .version(HttpClient.Version.HTTP_2)
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMinutes(5));   // 流式需要长一点

        return RestClient.builder()
            .requestFactory(factory)
            .build();
    }
}
```

#### 4.2 渐进替换 GatewayChatService 里的调用

**原则:一次只改一个方法,改完跑一遍,确认没坏。**

原代码(伪代码):
```java
Request req = new Request.Builder()
    .url(url).headers(...).post(body).build();
try (Response resp = okHttpClient.newCall(req).execute()) {
    return resp.body().string();
}
```

改为:
```java
String resp = upstreamRestClient.post()
    .uri(url)
    .headers(h -> h.setBearerAuth(token))
    .contentType(MediaType.APPLICATION_JSON)
    .body(body)
    .retrieve()
    .body(String.class);
```

#### 4.3 流式调用

OpenAI 流式返回 SSE,RestClient 支持流式:
```java
upstreamRestClient.post()
    .uri(url)
    .headers(h -> h.setBearerAuth(token))
    .body(body)
    .exchange((req, resp) -> {
        try (var stream = resp.getBody()) {
            // 把 stream 写到 StreamingResponseBody 输出
        }
        return null;
    });
```

或者用 WebClient(响应式)处理流式更优雅(见可选项)。

#### 4.4 删掉 OkHttp

所有调用替换完,确认全功能跑通后,从 pom.xml 删除:
```xml
<!-- 删除这个 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
```

### 验证

非流式 + 流式各跑一次 chat completions,确认输出一致。

### 回滚

如果某个 endpoint 出问题,**只回滚那一个方法**,其他保持新代码。

---

## 阶段 5:加 Sentinel(限流 + 熔断 + 降级)

> 选 Sentinel 而不是 Resilience4j 的原因:**自带可视化控制台 + 热点参数限流(按 API Key)是杀手锏**,且中文文档完善。

### 目标
- 每个上游 provider 一个 CircuitBreaker(挂了能熔断,不拖垮整体)
- **按 API Key 维度热点参数限流**(防止单个 key 刷接口)
- 上游 5xx 自动降级到 fallback 方法
- 浏览器实时看每个接口 QPS、动态调阈值,无需重启

### 5.1 pom.xml 增加依赖

```xml
<!-- 核心 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- @SentinelResource 注解支持 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-annotation-aspectj</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- 热点参数限流(按 API Key) -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-parameter-flow-control</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- 与 Sentinel 控制台通信 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-transport-simple-http</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- AOP 支持(注解必需) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

> **注意**:不要引 `spring-cloud-starter-alibaba-sentinel`,那个会拉一堆 Spring Cloud 依赖,你这个项目用不到。直接用上面 4 个 starter 即可。

### 5.2 注册 Sentinel AOP 切面

新建 [src/main/java/com/zxw/config/SentinelConfig.java](src/main/java/com/zxw/config/SentinelConfig.java)

```java
package com.zxw.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

### 5.3 启动参数加上控制台地址

修改 [_start-app.cmd](../_start-app.cmd) 或启动脚本,加 JVM 参数:

```bash
java \
  -Dcsp.sentinel.dashboard.server=localhost:8080 \
  -Dproject.name=ai-gateway \
  -Dcsp.sentinel.api.port=8719 \
  -jar target/Test-0.0.1-SNAPSHOT.jar
```

| 参数 | 作用 |
| --- | --- |
| `csp.sentinel.dashboard.server` | 控制台地址(下面会装) |
| `project.name` | 在控制台里显示的服务名 |
| `csp.sentinel.api.port` | 应用暴露给控制台拉取数据的端口(默认 8719) |

### 5.4 给 Gateway 核心方法加保护注解

修改 [GatewayChatService.java](src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java) 的 `chatCompletions()`:

```java
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

@SentinelResource(
    value = "gateway:chat-completions",
    blockHandler = "chatBlocked",       // 限流/熔断时调
    fallback = "chatFallback"            // 业务异常时调
)
public ResponseEntity<?> chatCompletions(String authorization, String body, HttpServletRequest req) {
    // 现有实现保持不动
}

// 限流/熔断处理(参数列表必须跟原方法一致 + 末尾加 BlockException)
public ResponseEntity<?> chatBlocked(String authorization, String body, HttpServletRequest req, BlockException ex) {
    return ResponseEntity.status(429)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"error\":{\"message\":\"请求过于频繁,请稍后重试\",\"type\":\"rate_limit_exceeded\"}}");
}

// 业务异常 fallback(参数列表必须跟原方法一致 + 可选末尾加 Throwable)
public ResponseEntity<?> chatFallback(String authorization, String body, HttpServletRequest req, Throwable t) {
    return ResponseEntity.status(503)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"error\":{\"message\":\"上游服务暂不可用\",\"type\":\"upstream_unavailable\"}}");
}
```

### 5.5 按 API Key 热点参数限流(核心需求)

如果想按 API Key 维度限流(不同 key 各自独立计数),需要把 key 作为参数传进资源:

```java
@SentinelResource(
    value = "gateway:chat-completions:per-key",
    blockHandler = "chatBlocked"
)
public ResponseEntity<?> chatCompletions(
    @SentinelResource.Param String apiKey,   // 第一个参数会作为热点参数索引 0
    String body,
    HttpServletRequest req
) { ... }
```

> Sentinel 默认会把方法参数依次作为热点参数索引(0, 1, 2...),`apiKey` 是第 0 个,在控制台配规则时填 `paramIdx=0` 即可。

然后在控制台 → 「热点规则」里给 `gateway:chat-completions:per-key` 配:
- `paramIdx`: 0(对 apiKey 这个参数限流)
- `count`: 60(每秒 60 次)
- `durationInSec`: 1
- 还可以为某个特定 key 配「例外项」(如内部 key 不限流)

### 5.6 启动 Sentinel 控制台(可视化大盘)

```bash
# 下载控制台 jar(也可挂在内网某台机器上长期运行)
wget https://github.com/alibaba/Sentinel/releases/download/1.8.8/sentinel-dashboard-1.8.8.jar

# 启动(端口 8080,跟你 Spring Boot 9988 不冲突)
java -Dserver.port=8080 \
     -Dcsp.sentinel.dashboard.server=localhost:8080 \
     -jar sentinel-dashboard-1.8.8.jar
```

浏览器打开 `http://localhost:8080`,默认账号 `sentinel` / `sentinel`。

应用启动后,**先打一次接口让 Sentinel 注册资源**,然后控制台左侧会出现 `ai-gateway` 服务节点,可以看到:
- 实时 QPS、响应时间、异常率
- 资源链路视图
- 在线配置「流控规则」「熔断规则」「热点规则」「降级规则」

### 5.7 推荐的初始规则(在控制台配,不写死代码)

| 资源 | 规则类型 | 阈值 | 说明 |
| --- | --- | --- | --- |
| `gateway:chat-completions:per-key` | 热点参数 | paramIdx=0, count=60/s | 每个 API Key 60 QPS |
| `gateway:chat-completions` | 流控 | count=500/s | 全局 500 QPS 上限 |
| `gateway:chat-completions` | 熔断 | 慢调用比例>50%, 时长>10s | 上游慢就熔断 |
| `gateway:chat-completions` | 熔断 | 异常比例>50% | 上游报错率高就熔断 |

### 验证

```bash
# 1. 故意快速打 70 次接口,确认第 61 次开始返回 429
for i in {1..70}; do
    curl -s -o /dev/null -w "%{http_code}\n" \
      -X POST http://localhost:9988/v1/chat/completions \
      -H "Authorization: Bearer <api-key>" \
      -H "Content-Type: application/json" \
      -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"x"}]}'
done

# 2. 把上游 URL 改错,连续打 20 次,看是否触发熔断,fallback 返回 503
# 3. 控制台看实时 QPS 曲线
```

### 回滚

注解去掉(或改成空注解)即可恢复原行为,依赖留着不影响业务。

---

## 阶段 6:用 Spring AI 重写协议适配层(核心改造)

### 目标
干掉 [GatewayGeminiController.java](src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java) 里手写的 Gemini ↔ OpenAI 协议互转,以及 [GatewayChatService.anthropicMessages()](src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java) 里的 Anthropic 适配,**全部交给 Spring AI 统一封装**。

### 为什么这是最大收益

| 当前代码 | Spring AI 方式 |
| --- | --- |
| 手写 OpenAI / Gemini / Anthropic JSON 转换 | `ChatClient` 统一 API,自动适配 |
| 手写 SSE 解析 | `.stream().chatResponse()` 返回 Flux |
| 加新上游就要写一套 Controller | 加个 `ChatModel` Bean 就行 |
| 没有 Function Calling 抽象 | 内置 Function Calling、Tool 调用 |

### pom.xml 增加依赖

```xml
<!-- 仓库:Spring AI 还没在 Maven Central 全量发布,需要 milestone 仓库 -->
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
</repositories>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- OpenAI 兼容 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    <!-- Anthropic -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-anthropic</artifactId>
    </dependency>
    <!-- Vertex AI Gemini -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-vertex-ai-gemini</artifactId>
    </dependency>
</dependencies>
```

> 注:Spring AI 1.0.0 已 GA,优先用稳定版;具体版本号请用 [Resolve Library ID + Query Docs](#) 工具核对当前最新 GA 版。

### 改造思路

**新增** `UpstreamChatClientFactory`:根据 `route.providerCode()` 动态构造一个 `ChatClient`。

```java
@Service
public class UpstreamChatClientFactory {

    public ChatClient build(GatewayRoute route) {
        return switch (route.providerCode().toLowerCase()) {
            case "openai" -> openAiClient(route);
            case "anthropic" -> anthropicClient(route);
            case "gemini" -> geminiClient(route);
            default -> throw new BusinessException(400, "未知 provider: " + route.providerCode());
        };
    }

    private ChatClient openAiClient(GatewayRoute route) {
        OpenAiApi api = new OpenAiApi(route.baseUrl(), route.apiKey());
        OpenAiChatModel model = new OpenAiChatModel(api);
        return ChatClient.create(model);
    }
    // anthropic / gemini 同理
}
```

**Controller 不动**,但 Service 改成:

```java
public StreamingResponseBody chatCompletions(...) {
    GatewayRoute route = routeService.resolve(model);
    ChatClient client = factory.build(route);

    Flux<ChatResponse> flux = client.prompt()
        .messages(buildMessages(requestBody))   // 把 OpenAI 格式转 Spring AI Message
        .stream()
        .chatResponse();

    return outputStream -> flux.doOnNext(chunk -> {
        // 把 Spring AI ChatResponse 转回 OpenAI SSE 格式输出
        outputStream.write(toOpenAiSse(chunk).getBytes());
        outputStream.flush();
    }).blockLast();
}
```

### 改造步骤

1. **第一步**:新增 `UpstreamChatClientFactory`,在新接口 `/v2/chat/completions` 下用 Spring AI 实现,**老接口保留不动**
2. **第二步**:跑 A/B 测试,新接口确认稳定
3. **第三步**:把老 Controller 内部实现切到新逻辑,旧的协议转换代码删除
4. **第四步**:删除 [GatewayGeminiController.java](src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java) 里所有 `buildChatCompletionsRequest` / `convertChatCompletionsSseToGemini` 等手写函数

### 验证(每一步都要)

- OpenAI 客户端(curl): `POST /v1/chat/completions` 流式 + 非流式
- Gemini 客户端: `POST /v1beta/models/gemini-pro:streamGenerateContent`
- Anthropic 客户端: `POST /v1/messages`
- 三个 SDK(openai-python / anthropic-python / google-genai)各跑一次

### 回滚

每一步独立 commit,出问题逐个 revert。最坏情况切回老代码,只损失阶段 6 的工作。

---

## 阶段 7(可选):MyBatis-Plus 渐进迁移

### 是否要做?
**不强制。** 如果你只想"用上新技术"且不想推翻已经能跑的代码,可以**只在新模块用 MP**,老模块继续 JdbcTemplate。两者并存零冲突。

### 改造策略

**❌ 不要做的**:把 15 个 Service 一次性全改完。

**✅ 推荐做法**:
1. **简单 CRUD 优先**:从 `AdminUserService.getUser()`、`AdminApiKeyService.listApiKeys()` 这种纯查询开始
2. **复杂 JOIN 保留**:[GatewayRouteService.findRoutes()](src/main/java/com/zxw/modules/gateway/service/GatewayRouteService.java)(4 表 JOIN + 排序)继续用 JdbcTemplate,MP 反而更啰嗦
3. **DDL 检查保留**:[BootstrapDataInitializer.java](src/main/java/com/zxw/config/BootstrapDataInitializer.java) 里的 `hasTable()` / `hasColumn()` 不要动
4. **逻辑删除统一**:用 MP 的 `@TableLogic` 注解处理 `deleted` 字段,但要保证老代码也认这个语义

### pom.xml 增加依赖

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```

### 配置

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.zxw.modules.*.entity
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
```

### 示例:迁移一个 Service

新建 `entity/User.java`:
```java
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
```

新建 `mapper/UserMapper.java`:
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

`AdminUserService` 里**新增**(不替换)字段:
```java
private final UserMapper userMapper;   // 新增
private final JdbcTemplate jdbcTemplate;  // 保留

// 简单查询用 MP
public User getUser(Long id) {
    return userMapper.selectById(id);
}

// 复杂查询继续 JdbcTemplate
public List<UserListItemResponse> listUsers(...) {
    // 原代码不动
}
```

---

## 阶段 8(可选):Caffeine 本地缓存

### 痛点
[GatewayRouteService.findRoutes()](src/main/java/com/zxw/modules/gateway/service/GatewayRouteService.java) 每次请求都查 4 表 JOIN,模型路由其实变化不频繁,可以缓存。

### pom.xml

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 启动类加 `@EnableCaching`

```java
@EnableCaching
@SpringBootApplication
public class TestApplication { ... }
```

### 配置

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=60s
```

### 使用

```java
@Cacheable(cacheNames = "model-routes", key = "#modelCode")
public List<GatewayRoute> findRoutes(String modelCode) { ... }

// 模型/路由变更后清缓存
@CacheEvict(cacheNames = "model-routes", allEntries = true)
public void updateModel(...) { ... }
```

---

## 阶段 9(可选):Testcontainers + 集成测试

### 目标
用 Docker 启真实 MySQL + Redis 跑测试,而不是 H2 + 嵌入 Redis(后者跟生产差异太大)。

### pom.xml

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
```

### 写一个集成测试基类

```java
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("zhongzhuan")
        .withInitScript("db/schema-mysql.sql");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }
}
```

后续所有集成测试继承这个类,**保证每次测试跑在真实 MySQL 上**。

---

## 阶段 10(可选):Docker + GitHub Actions

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/Test-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9988
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml(本地开发)

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: zzapi@Admin
      MYSQL_DATABASE: zhongzhuan
    ports: ["3306:3306"]
    volumes: ["./src/main/resources/db:/docker-entrypoint-initdb.d"]
  redis:
    image: redis:7
    ports: ["6379:6379"]
  app:
    build: .
    ports: ["9988:9988"]
    depends_on: [mysql, redis]
    environment:
      DB_URL: jdbc:mysql://mysql:3306/zhongzhuan
      REDIS_HOST: redis
```

### .github/workflows/build.yml

```yaml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./mvnw clean verify
```

---

## 验证清单(每个阶段完成后跑一遍)

```bash
# 1. 启动正常
./mvnw spring-boot:run

# 2. 健康检查
curl http://localhost:9988/actuator/health

# 3. 登录
curl -X POST http://localhost:9988/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 4. 列模型(用上一步拿到的 token)
curl http://localhost:9988/v1/models -H "Authorization: Bearer <api-key>"

# 5. 非流式 chat
curl -X POST http://localhost:9988/v1/chat/completions \
  -H "Authorization: Bearer <api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}'

# 6. 流式 chat(确认 SSE 格式正常)
curl -X POST http://localhost:9988/v1/chat/completions \
  -H "Authorization: Bearer <api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o-mini","stream":true,"messages":[{"role":"user","content":"hi"}]}'

# 7. Gemini 协议
curl -X POST "http://localhost:9988/v1beta/models/gemini-pro:streamGenerateContent?alt=sse" \
  -H "x-goog-api-key: <api-key>" \
  -d '{"contents":[{"role":"user","parts":[{"text":"hi"}]}]}'

# 8. Anthropic 协议
curl -X POST http://localhost:9988/v1/messages \
  -H "x-api-key: <api-key>" \
  -d '{"model":"claude-3-5-sonnet","max_tokens":100,"messages":[{"role":"user","content":"hi"}]}'
```

8 个全过 = 当前阶段验收通过。

---

## 回滚策略

| 阶段 | 出问题怎么办 |
| --- | --- |
| 1 (Java 21,可选) | `git revert`,JDK 切回 17;或直接跳过本阶段 |
| 2 (OpenAPI) | `git revert`,文档功能丢失但业务不受影响 |
| 3 (Actuator) | `git revert`,监控丢失但业务不受影响 |
| 4 (RestClient) | 单方法回滚,跟 OkHttp 共存期可保留双实现 |
| 5 (Sentinel) | 注解去掉即恢复原行为;依赖可保留 |
| 6 (Spring AI) | 老 Controller 保留期间可瞬间切回;全切完后回滚要 revert 多个 commit |
| 7 (MyBatis-Plus) | 不替换老代码,只在新代码用,天然零回滚成本 |
| 8 (Caffeine) | `@Cacheable` 去掉即恢复 |
| 9-10 | 测试/部署相关,不影响线上 |

---

## 推荐执行节奏

**第 1 周(必做)**:阶段 0、2、3、4(阶段 1 跳过或并入)
- 周一:阶段 0(分支 + 备份)+ 阶段 2(OpenAPI)
- 周二:阶段 3(Actuator)
- 周三-周五:阶段 4(RestClient 替换 OkHttp)— 慢慢替换,每天替一两个方法

**第 2 周(必做)**:阶段 5
- 阶段 5(Sentinel)— 装控制台 + 加注解 + 配规则,1-1.5 天

**第 3-4 周(强烈推荐)**:阶段 6
- 阶段 6(Spring AI)— 这是最大收益,但也最需要小心

**之后(看心情)**:阶段 7-10

---

## 你最该先动的:阶段 1-5(约 5 个工作日)

这 5 个阶段做完,你的项目就从"能跑"变成"现代化、可观测、有弹性",**没有任何一处推倒重来**。

阶段 6(Spring AI)是真正的"用新技术",但要预留充分时间。

如果你想先做哪一个,告诉我,我可以再写得更细(比如把 RestClient 替换的每一行原代码 → 新代码列出来,或者把 Spring AI 改造的完整代码示例给你)。
