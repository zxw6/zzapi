# AI 缃戝叧椤圭洰娓愯繘寮忛噸鏋勬柟妗?
> 鐩爣:鍦ㄤ笉鐮村潖鐜版湁鍔熻兘鐨勫墠鎻愪笅,鎶婇」鐩崌绾у埌鐜颁唬鍖栨妧鏈爤銆?> 鍘熷垯:**姣忓畬鎴愪竴涓樁娈?椤圭洰蹇呴』鑳借窇璧锋潵銆佽兘鍥炴粴銆?* 涓嶅厑璁稿嚭鐜?鏀逛竴鍗婅窇涓嶈捣鏉?鐨勪腑闂寸姸鎬併€?>
> 閫傜敤鐗堟湰:Spring Boot 3.5.13 鈫?3.5.x(淇濇寔褰撳墠涓荤増鏈?,Java 17 淇濈暀(鍙€夊崌 21)銆?
---

## 0. 褰撳墠鏋舵瀯鐩樼偣

| 椤?| 鐜扮姸 | 鐥涚偣 |
| --- | --- | --- |
| Java | 17 | 鍙繚鐣?鍗?21 鎵嶈兘鐢ㄨ櫄鎷熺嚎绋?鍙€? |
| ORM | 绾?JdbcTemplate(15 涓?Service 鍏ㄦ槸鎵嬪啓 SQL) | 绠€鍗?CRUD 閲嶅鍔冲姩澶?|
| HTTP 瀹㈡埛绔?| OkHttp + JDK HttpClient 娣风敤([GatewayChatService.java:128-131](src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java#L128)) | 鎵嬪啓 SSE 杞崲銆佽秴鏃堕厤缃暎涔?|
| 鍗忚閫傞厤 | [GatewayGeminiController.java](src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java) 鎵嬫捀 Gemini 鈫?OpenAI 鍗忚浜掕浆 | 閲嶉€犺疆瀛?姣忓姞涓€涓笂娓稿氨瑕佸啓涓€閬?|
| 閴存潈 | 鑷繁鍐?JWT Filter + Interceptor + ThreadLocal | 鑳界敤,鏃犻渶鏀?|
| 闄愭祦/鐔旀柇 | **鏃?* | 涓婃父鎸備簡鑳芥嫋鍨暣涓綉鍏?|
| 鍙娴?| 鑷缓 request_logs 琛?| 娌℃湁鎸囨爣鎺ュ彛銆佹病鏈夊仴搴锋鏌ャ€佹病鏈?Tracing |
| API 鏂囨。 | **鏃?* | 鍓嶇/瀵规帴鏂归潬鐚?|
| 娴嬭瘯 | 1 涓┖ SpringBootTest | 鏃犲洖褰掍繚闅?|
| 閮ㄧ讲 | 鍑犱釜 .cmd 鑴氭湰 | 鏃?Docker銆佹棤 CI |

---

## 1. 鐩爣鏋舵瀯 + 鎬讳綋璺嚎鍥?
```
                     鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?                     鈹?        鍓嶇(鏆備笉鍔?                鈹?                     鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?                                       鈫?HTTP
   鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?   鈹? Spring Boot 3.5 + Java 21 (铏氭嫙绾跨▼寮€鍚?                     鈹?   鈹? 鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹? 鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹? 鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹? 鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?      鈹?   鈹? 鈹?OpenAPI  鈹? 鈹?Actuator 鈹? 鈹? CORS    鈹? 鈹? JWT     鈹?      鈹?   鈹? 鈹?/swagger 鈹? 鈹?/metrics 鈹? 鈹? Filter  鈹? 鈹? Filter  鈹?      鈹?   鈹? 鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹? 鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹? 鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹? 鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?      鈹?   鈹?                                                                鈹?   鈹? 鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€ Gateway 灞?鏍稿績鏀归€?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?鈹?   鈹? 鈹? Controller 鈫?ChatService 鈫?Spring AI ChatClient          鈹?鈹?   鈹? 鈹?                             鈫?                            鈹?鈹?   鈹? 鈹?                        Resilience4j(闄愭祦/鐔旀柇/閲嶈瘯)     鈹?鈹?   鈹? 鈹?                             鈫?                            鈹?鈹?   鈹? 鈹?                        RestClient(鏇挎崲 OkHttp)          鈹?鈹?   鈹? 鈹?                             鈫?                            鈹?鈹?   鈹? 鈹?                        涓婃父(OpenAI/Gemini/Anthropic)     鈹?鈹?   鈹? 鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?鈹?   鈹?                                                                鈹?   鈹? 鈹屸攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€ 鏁版嵁灞?鎸夐渶鏀归€?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹愨攤
   鈹? 鈹? Service 鈹€鈹攢鈫?JdbcTemplate(鑰佷唬鐮?鍏堜繚鐣?              鈹傗攤
   鈹? 鈹?          鈹斺攢鈫?MyBatis-Plus Mapper(鏂颁唬鐮佺敤,娓愯繘杩佺Щ)   鈹傗攤
   鈹? 鈹?          鈹斺攢鈫?Caffeine(妯″瀷璺敱銆丄PI Key 缂撳瓨)          鈹傗攤
   鈹? 鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹樷攤
   鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?                                       鈫?                  MySQL(鏃犲彉鍖?        Redis(鏃犲彉鍖?
                                       鈫?                     Prometheus + Grafana(鏂板,鍙€?
```

### 璺嚎鍥?鎸夐闄╀粠浣庡埌楂?

| 闃舵 | 鍐呭 | 宸ヤ綔閲?| 椋庨櫓 | 鏄惁鍙€?|
| --- | --- | --- | --- | --- |
| 1 | Java 21 + 铏氭嫙绾跨▼ | 0.5 澶?| 鏋佷綆 | **鍙€?*(鐣?17 涔熻) |
| 2 | Springdoc-OpenAPI(Swagger UI) | 0.5 澶?| 鏋佷綆 | 蹇呭仛 |
| 3 | Actuator + Micrometer + Prometheus | 0.5 澶?| 鏋佷綆 | 蹇呭仛 |
| 4 | RestClient 鏇挎崲 OkHttp | 1-2 澶?| 浣?| 蹇呭仛(涓洪樁娈?5銆? 閾鸿矾) |
| 5 | **Sentinel**(闄愭祦/鐔旀柇/闄嶇骇) | 1-1.5 澶?| 浣?| 蹇呭仛(缃戝叧鍒氶渶) |
| 6 | Spring AI 閲嶅啓鍗忚閫傞厤灞?| 3-5 澶?| 涓?| 寮虹儓鎺ㄨ崘 |
| 7 | MyBatis-Plus 娓愯繘杩佺Щ | 3-7 澶?| 涓?| 鍙€?|
| 8 | Caffeine 缂撳瓨 | 0.5 澶?| 浣?| 鍙€?|
| 9 | Testcontainers + 闆嗘垚娴嬭瘯 | 2-3 澶?| 浣?| 鍙€?|
| 10 | Docker + GitHub Actions | 1 澶?| 浣?| 鍙€?|

**鎬昏:蹇呭仛閮ㄥ垎绾?4-5 澶?鍔犱笂 Spring AI 鏀归€犵害 8-10 澶┿€?*

---

## 闃舵 0:鍑嗗宸ヤ綔(寮€濮嬩换浣曟敼閫犲墠閮借鍋?

```bash
# 1. 鏂板缓鍒嗘敮
git checkout -b refactor/modernize

# 2. 澶囦唤鏁版嵁搴?schema
mysqldump -u root -p zhongzhuan --no-data > backup-schema-$(date +%Y%m%d).sql

# 3. 鎶婄幇鏈?.out.log 娓呭共鍑€,鏂逛究鍚庣画鐪嬫柊鏃ュ織
git rm run-app.*.log run-packaged.*.log run-gateway.out.log 2>/dev/null
echo "run-*.log" >> .gitignore
```

**姣忎釜闃舵瀹屾垚鍚庡繀鍋?**
1. 鍚姩搴旂敤,璺戜竴閬嶆牳蹇冨満鏅?鐧诲綍 + 涓€娆?chat completion)
2. `git commit -m "phase X: ..."` 鍗曠嫭鎻愪氦
3. 鍑洪棶棰樼珛鍒?`git revert <commit>`,涓嶈鍦ㄧ儌鎽婂瓙涓婄户缁敼

---

## 闃舵 1(鍙€?:鍗囩骇鍒?Java 21 + 铏氭嫙绾跨▼

> 鈿狅笍 **姝ら樁娈靛彲璺宠繃銆?* Spring Boot 3.5 瀹屾暣鏀寔 Java 17,鐣欏湪 17 瀹屽叏娌￠棶棰樸€?> 鍞竴鎹熷け:铏氭嫙绾跨▼(Loom)闇€瑕?Java 21銆傚鏋滀綘鐜版湁绾夸笂鏄?17 鐨勮繍缁寸幆澧?**涓嶅繀鍗囩骇**,鍚庣画鎵€鏈夐樁娈甸兘鍏煎 17銆?> 濡傛灉 OK 鍗囩骇,缁х画寰€涓?鍚﹀垯**鐩存帴璺冲埌闃舵 2**銆?
### 鐩爣
- Java 17 鈫?21
- 寮€鍚櫄鎷熺嚎绋?璁?I/O 瀵嗛泦鍨嬬殑缃戝叧鍚炲悙閲忕炕鍊?
### 淇敼 [pom.xml](pom.xml)

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
    <!-- 鍏朵粬淇濇寔涓嶅彉 -->
</properties>
```

`maven-compiler-plugin` 閲?3 澶?`<release>17</release>` 涔熷叏鏀规垚 `21`銆?
### 淇敼 [application.yml](src/main/resources/application.yml)

鍦?`spring:` 涓嬪鍔?

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 楠岃瘉

```bash
./mvnw clean package -DskipTests
java --version    # 纭鏈満 JDK 21
java -jar target/Test-0.0.1-SNAPSHOT.jar
```

鍚姩鏃ュ織閲屽簲璇ヨ兘鐪嬪埌 Tomcat 鐢ㄨ櫄鎷熺嚎绋嬫睜銆傛墦涓€娆?chat completions 鎺ュ彛纭鑳借窇閫氥€?
### 鍥炴粴

`git revert HEAD` 鍗冲彲銆?
### 涓嶅崌绾?21 鐨勬浛浠ｆ柟妗?
濡傛灉鐣欏湪 JDK 17,缃戝叧鍚炲悙閲忎紭鍖栧彲浠ラ潬杩欎袱涓墜娈?
1. 璋冨ぇ Tomcat 绾跨▼姹?`server.tomcat.threads.max=400`(榛樿 200)
2. 娴佸紡鎺ュ彛鐢?`StreamingResponseBody`(椤圭洰宸茬粡鍦ㄧ敤)+ Servlet 3.0 寮傛,涓嶄細闃诲涓荤嚎绋?
---

## 闃舵 2:鍔?Springdoc-OpenAPI(Swagger UI)

### 鐩爣
鎵€鏈?Controller 鑷姩鐢熸垚鍦ㄧ嚎 API 鏂囨。,璁块棶 `http://localhost:9988/swagger-ui.html`銆?
### pom.xml 澧炲姞渚濊禆

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### application.yml 澧炲姞閰嶇疆

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

### 淇敼 [WebMvcConfig.java](src/main/java/com/zxw/config/WebMvcConfig.java)

鎶?Swagger 璺緞鍔犲埌 `AdminAuthInterceptor` 鐨勬帓闄ゅ垪琛ㄩ噷:

```java
.excludePathPatterns(
    "/admin/auth/login",
    "/admin/auth/register",
    "/admin/auth/health",
    "/swagger-ui/**",       // 鏂板
    "/swagger-ui.html",     // 鏂板
    "/v3/api-docs/**"       // 鏂板
)
```

### 缁?Controller 鍔犳敞瑙?鍙€?鏂囨。鏇村弸濂?

鍙鏍稿績 Controller 鍔?鍏朵粬鍙互鍚庣画鎱㈡參琛?

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Gateway - Chat", description = "OpenAI 鍏煎鐨?chat completions 鎺ュ彛")
public class GatewayChatController {

    @Operation(summary = "Chat Completions(鏀寔娴佸紡)")
    @PostMapping(...)
    public ... chatCompletions(...) { ... }
}
```

### 楠岃瘉

鍚姩鍚庤闂?`http://localhost:9988/swagger-ui.html`,搴旇鑳界湅鍒版墍鏈夋帴鍙ｅ垪琛ㄣ€?
---

## 闃舵 3:鍔?Actuator + Micrometer + Prometheus

### 鐩爣
- 鍋ュ悍妫€鏌?`/actuator/health`
- JVM/Tomcat 鎸囨爣 `/actuator/prometheus`
- 鍚庣画鍙互鎺?Grafana 鐪嬪ぇ鐩?
### pom.xml 澧炲姞渚濊禆

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

### application.yml 澧炲姞閰嶇疆

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

### 淇敼 [WebMvcConfig.java](src/main/java/com/zxw/config/WebMvcConfig.java)

鎶?`/actuator/**` 鍔犲叆鎺掗櫎鍒楄〃(鍚屼笂)銆?
### 鑷畾涔変笟鍔℃寚鏍?鍙€?

鍦?`GatewayChatService` 閲屾敞鍏?`MeterRegistry`,璁板綍涓婃父鑰楁椂:

```java
private final Timer upstreamTimer;

public GatewayChatService(MeterRegistry registry, ...) {
    this.upstreamTimer = Timer.builder("gateway.upstream.duration")
        .description("涓婃父 AI 鎺ュ彛鑰楁椂")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);
}

// 璋冪敤涓婃父鏃?
Timer.Sample sample = Timer.start(registry);
try {
    // 璋冧笂娓?} finally {
    sample.stop(upstreamTimer.tags("provider", route.providerCode(), "model", model));
}
```

### 楠岃瘉

```bash
curl http://localhost:9988/actuator/health
curl http://localhost:9988/actuator/prometheus | head -50
```

---

## 闃舵 4:鐢?RestClient 鏇挎崲 OkHttp

### 鐩爣
- 骞叉帀 OkHttp 渚濊禆
- 鐢?Spring 6 鐨?`RestClient`(鍚屾)缁熶竴鎵€鏈?HTTP 璋冪敤
- 涓哄悗缁?Spring AI 鏀归€犻摵璺?Spring AI 搴曞眰灏辨槸 RestClient)

### 姝ラ

#### 4.1 鏂板缓 RestClient 閰嶇疆

鍒涘缓 [src/main/java/com/zxw/config/HttpClientConfig.java](src/main/java/com/zxw/config/HttpClientConfig.java)

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
        factory.setReadTimeout(Duration.ofMinutes(5));   // 娴佸紡闇€瑕侀暱涓€鐐?
        return RestClient.builder()
            .requestFactory(factory)
            .build();
    }
}
```

#### 4.2 娓愯繘鏇挎崲 GatewayChatService 閲岀殑璋冪敤

**鍘熷垯:涓€娆″彧鏀逛竴涓柟娉?鏀瑰畬璺戜竴閬?纭娌″潖銆?*

鍘熶唬鐮?浼唬鐮?:
```java
Request req = new Request.Builder()
    .url(url).headers(...).post(body).build();
try (Response resp = okHttpClient.newCall(req).execute()) {
    return resp.body().string();
}
```

鏀逛负:
```java
String resp = upstreamRestClient.post()
    .uri(url)
    .headers(h -> h.setBearerAuth(token))
    .contentType(MediaType.APPLICATION_JSON)
    .body(body)
    .retrieve()
    .body(String.class);
```

#### 4.3 娴佸紡璋冪敤

OpenAI 娴佸紡杩斿洖 SSE,RestClient 鏀寔娴佸紡:
```java
upstreamRestClient.post()
    .uri(url)
    .headers(h -> h.setBearerAuth(token))
    .body(body)
    .exchange((req, resp) -> {
        try (var stream = resp.getBody()) {
            // 鎶?stream 鍐欏埌 StreamingResponseBody 杈撳嚭
        }
        return null;
    });
```

鎴栬€呯敤 WebClient(鍝嶅簲寮?澶勭悊娴佸紡鏇翠紭闆?瑙佸彲閫夐」)銆?
#### 4.4 鍒犳帀 OkHttp

鎵€鏈夎皟鐢ㄦ浛鎹㈠畬,纭鍏ㄥ姛鑳借窇閫氬悗,浠?pom.xml 鍒犻櫎:
```xml
<!-- 鍒犻櫎杩欎釜 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
```

### 楠岃瘉

闈炴祦寮?+ 娴佸紡鍚勮窇涓€娆?chat completions,纭杈撳嚭涓€鑷淬€?
### 鍥炴粴

濡傛灉鏌愪釜 endpoint 鍑洪棶棰?**鍙洖婊氶偅涓€涓柟娉?*,鍏朵粬淇濇寔鏂颁唬鐮併€?
---

## 闃舵 5:鍔?Sentinel(闄愭祦 + 鐔旀柇 + 闄嶇骇)

> 閫?Sentinel 鑰屼笉鏄?Resilience4j 鐨勫師鍥?**鑷甫鍙鍖栨帶鍒跺彴 + 鐑偣鍙傛暟闄愭祦(鎸?API Key)鏄潃鎵嬮攺**,涓斾腑鏂囨枃妗ｅ畬鍠勩€?
### 鐩爣
- 姣忎釜涓婃父 provider 涓€涓?CircuitBreaker(鎸備簡鑳界啍鏂?涓嶆嫋鍨暣浣?
- **鎸?API Key 缁村害鐑偣鍙傛暟闄愭祦**(闃叉鍗曚釜 key 鍒锋帴鍙?
- 涓婃父 5xx 鑷姩闄嶇骇鍒?fallback 鏂规硶
- 娴忚鍣ㄥ疄鏃剁湅姣忎釜鎺ュ彛 QPS銆佸姩鎬佽皟闃堝€?鏃犻渶閲嶅惎

### 5.1 pom.xml 澧炲姞渚濊禆

```xml
<!-- 鏍稿績 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- @SentinelResource 娉ㄨВ鏀寔 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-annotation-aspectj</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- 鐑偣鍙傛暟闄愭祦(鎸?API Key) -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-parameter-flow-control</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- 涓?Sentinel 鎺у埗鍙伴€氫俊 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-transport-simple-http</artifactId>
    <version>1.8.8</version>
</dependency>
<!-- AOP 鏀寔(娉ㄨВ蹇呴渶) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

> **娉ㄦ剰**:涓嶈寮?`spring-cloud-starter-alibaba-sentinel`,閭ｄ釜浼氭媺涓€鍫?Spring Cloud 渚濊禆,浣犺繖涓」鐩敤涓嶅埌銆傜洿鎺ョ敤涓婇潰 4 涓?starter 鍗冲彲銆?
### 5.2 娉ㄥ唽 Sentinel AOP 鍒囬潰

鏂板缓 [src/main/java/com/zxw/config/SentinelConfig.java](src/main/java/com/zxw/config/SentinelConfig.java)

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

### 5.3 鍚姩鍙傛暟鍔犱笂鎺у埗鍙板湴鍧€

淇敼 [_start-app.cmd](../_start-app.cmd) 鎴栧惎鍔ㄨ剼鏈?鍔?JVM 鍙傛暟:

```bash
java \
  -Dcsp.sentinel.dashboard.server=localhost:8080 \
  -Dproject.name=ai-gateway \
  -Dcsp.sentinel.api.port=8719 \
  -jar target/Test-0.0.1-SNAPSHOT.jar
```

| 鍙傛暟 | 浣滅敤 |
| --- | --- |
| `csp.sentinel.dashboard.server` | 鎺у埗鍙板湴鍧€(涓嬮潰浼氳) |
| `project.name` | 鍦ㄦ帶鍒跺彴閲屾樉绀虹殑鏈嶅姟鍚?|
| `csp.sentinel.api.port` | 搴旂敤鏆撮湶缁欐帶鍒跺彴鎷夊彇鏁版嵁鐨勭鍙?榛樿 8719) |

### 5.4 缁?Gateway 鏍稿績鏂规硶鍔犱繚鎶ゆ敞瑙?
淇敼 [GatewayChatService.java](src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java) 鐨?`chatCompletions()`:

```java
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

@SentinelResource(
    value = "gateway:chat-completions",
    blockHandler = "chatBlocked",       // 闄愭祦/鐔旀柇鏃惰皟
    fallback = "chatFallback"            // 涓氬姟寮傚父鏃惰皟
)
public ResponseEntity<?> chatCompletions(String authorization, String body, HttpServletRequest req) {
    // 鐜版湁瀹炵幇淇濇寔涓嶅姩
}

// 闄愭祦/鐔旀柇澶勭悊(鍙傛暟鍒楄〃蹇呴』璺熷師鏂规硶涓€鑷?+ 鏈熬鍔?BlockException)
public ResponseEntity<?> chatBlocked(String authorization, String body, HttpServletRequest req, BlockException ex) {
    return ResponseEntity.status(429)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"error\":{\"message\":\"璇锋眰杩囦簬棰戠箒,璇风◢鍚庨噸璇昞",\"type\":\"rate_limit_exceeded\"}}");
}

// 涓氬姟寮傚父 fallback(鍙傛暟鍒楄〃蹇呴』璺熷師鏂规硶涓€鑷?+ 鍙€夋湯灏惧姞 Throwable)
public ResponseEntity<?> chatFallback(String authorization, String body, HttpServletRequest req, Throwable t) {
    return ResponseEntity.status(503)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"error\":{\"message\":\"涓婃父鏈嶅姟鏆備笉鍙敤\",\"type\":\"upstream_unavailable\"}}");
}
```

### 5.5 鎸?API Key 鐑偣鍙傛暟闄愭祦(鏍稿績闇€姹?

濡傛灉鎯虫寜 API Key 缁村害闄愭祦(涓嶅悓 key 鍚勮嚜鐙珛璁℃暟),闇€瑕佹妸 key 浣滀负鍙傛暟浼犺繘璧勬簮:

```java
@SentinelResource(
    value = "gateway:chat-completions:per-key",
    blockHandler = "chatBlocked"
)
public ResponseEntity<?> chatCompletions(
    @SentinelResource.Param String apiKey,   // 绗竴涓弬鏁颁細浣滀负鐑偣鍙傛暟绱㈠紩 0
    String body,
    HttpServletRequest req
) { ... }
```

> Sentinel 榛樿浼氭妸鏂规硶鍙傛暟渚濇浣滀负鐑偣鍙傛暟绱㈠紩(0, 1, 2...),`apiKey` 鏄 0 涓?鍦ㄦ帶鍒跺彴閰嶈鍒欐椂濉?`paramIdx=0` 鍗冲彲銆?
鐒跺悗鍦ㄦ帶鍒跺彴 鈫?銆岀儹鐐硅鍒欍€嶉噷缁?`gateway:chat-completions:per-key` 閰?
- `paramIdx`: 0(瀵?apiKey 杩欎釜鍙傛暟闄愭祦)
- `count`: 60(姣忕 60 娆?
- `durationInSec`: 1
- 杩樺彲浠ヤ负鏌愪釜鐗瑰畾 key 閰嶃€屼緥澶栭」銆?濡傚唴閮?key 涓嶉檺娴?

### 5.6 鍚姩 Sentinel 鎺у埗鍙?鍙鍖栧ぇ鐩?

```bash
# 涓嬭浇鎺у埗鍙?jar(涔熷彲鎸傚湪鍐呯綉鏌愬彴鏈哄櫒涓婇暱鏈熻繍琛?
wget https://github.com/alibaba/Sentinel/releases/download/1.8.8/sentinel-dashboard-1.8.8.jar

# 鍚姩(绔彛 8080,璺熶綘 Spring Boot 9988 涓嶅啿绐?
java -Dserver.port=8080 \
     -Dcsp.sentinel.dashboard.server=localhost:8080 \
     -jar sentinel-dashboard-1.8.8.jar
```

娴忚鍣ㄦ墦寮€ `http://localhost:8080`,榛樿璐﹀彿 `sentinel` / `sentinel`銆?
搴旂敤鍚姩鍚?**鍏堟墦涓€娆℃帴鍙ｈ Sentinel 娉ㄥ唽璧勬簮**,鐒跺悗鎺у埗鍙板乏渚т細鍑虹幇 `ai-gateway` 鏈嶅姟鑺傜偣,鍙互鐪嬪埌:
- 瀹炴椂 QPS銆佸搷搴旀椂闂淬€佸紓甯哥巼
- 璧勬簮閾捐矾瑙嗗浘
- 鍦ㄧ嚎閰嶇疆銆屾祦鎺ц鍒欍€嶃€岀啍鏂鍒欍€嶃€岀儹鐐硅鍒欍€嶃€岄檷绾ц鍒欍€?
### 5.7 鎺ㄨ崘鐨勫垵濮嬭鍒?鍦ㄦ帶鍒跺彴閰?涓嶅啓姝讳唬鐮?

| 璧勬簮 | 瑙勫垯绫诲瀷 | 闃堝€?| 璇存槑 |
| --- | --- | --- | --- |
| `gateway:chat-completions:per-key` | 鐑偣鍙傛暟 | paramIdx=0, count=60/s | 姣忎釜 API Key 60 QPS |
| `gateway:chat-completions` | 娴佹帶 | count=500/s | 鍏ㄥ眬 500 QPS 涓婇檺 |
| `gateway:chat-completions` | 鐔旀柇 | 鎱㈣皟鐢ㄦ瘮渚?50%, 鏃堕暱>10s | 涓婃父鎱㈠氨鐔旀柇 |
| `gateway:chat-completions` | 鐔旀柇 | 寮傚父姣斾緥>50% | 涓婃父鎶ラ敊鐜囬珮灏辩啍鏂?|

### 楠岃瘉

```bash
# 1. 鏁呮剰蹇€熸墦 70 娆℃帴鍙?纭绗?61 娆″紑濮嬭繑鍥?429
for i in {1..70}; do
    curl -s -o /dev/null -w "%{http_code}\n" \
      -X POST http://localhost:9988/v1/chat/completions \
      -H "Authorization: Bearer <api-key>" \
      -H "Content-Type: application/json" \
      -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"x"}]}'
done

# 2. 鎶婁笂娓?URL 鏀归敊,杩炵画鎵?20 娆?鐪嬫槸鍚﹁Е鍙戠啍鏂?fallback 杩斿洖 503
# 3. 鎺у埗鍙扮湅瀹炴椂 QPS 鏇茬嚎
```

### 鍥炴粴

娉ㄨВ鍘绘帀(鎴栨敼鎴愮┖娉ㄨВ)鍗冲彲鎭㈠鍘熻涓?渚濊禆鐣欑潃涓嶅奖鍝嶄笟鍔°€?
---

## 闃舵 6:鐢?Spring AI 閲嶅啓鍗忚閫傞厤灞?鏍稿績鏀归€?

### 鐩爣
骞叉帀 [GatewayGeminiController.java](src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java) 閲屾墜鍐欑殑 Gemini 鈫?OpenAI 鍗忚浜掕浆,浠ュ強 [GatewayChatService.anthropicMessages()](src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java) 閲岀殑 Anthropic 閫傞厤,**鍏ㄩ儴浜ょ粰 Spring AI 缁熶竴灏佽**銆?
### 涓轰粈涔堣繖鏄渶澶ф敹鐩?
| 褰撳墠浠ｇ爜 | Spring AI 鏂瑰紡 |
| --- | --- |
| 鎵嬪啓 OpenAI / Gemini / Anthropic JSON 杞崲 | `ChatClient` 缁熶竴 API,鑷姩閫傞厤 |
| 鎵嬪啓 SSE 瑙ｆ瀽 | `.stream().chatResponse()` 杩斿洖 Flux |
| 鍔犳柊涓婃父灏辫鍐欎竴濂?Controller | 鍔犱釜 `ChatModel` Bean 灏辫 |
| 娌℃湁 Function Calling 鎶借薄 | 鍐呯疆 Function Calling銆乀ool 璋冪敤 |

### pom.xml 澧炲姞渚濊禆

```xml
<!-- 浠撳簱:Spring AI 杩樻病鍦?Maven Central 鍏ㄩ噺鍙戝竷,闇€瑕?milestone 浠撳簱 -->
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
    <!-- OpenAI 鍏煎 -->
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

> 娉?Spring AI 1.0.0 宸?GA,浼樺厛鐢ㄧǔ瀹氱増;鍏蜂綋鐗堟湰鍙疯鐢?[Resolve Library ID + Query Docs](#) 宸ュ叿鏍稿褰撳墠鏈€鏂?GA 鐗堛€?
### 鏀归€犳€濊矾

**鏂板** `UpstreamChatClientFactory`:鏍规嵁 `route.providerCode()` 鍔ㄦ€佹瀯閫犱竴涓?`ChatClient`銆?
```java
@Service
public class UpstreamChatClientFactory {

    public ChatClient build(GatewayRoute route) {
        return switch (route.providerCode().toLowerCase()) {
            case "openai" -> openAiClient(route);
            case "anthropic" -> anthropicClient(route);
            case "gemini" -> geminiClient(route);
            default -> throw new BusinessException(400, "鏈煡 provider: " + route.providerCode());
        };
    }

    private ChatClient openAiClient(GatewayRoute route) {
        OpenAiApi api = new OpenAiApi(route.baseUrl(), route.apiKey());
        OpenAiChatModel model = new OpenAiChatModel(api);
        return ChatClient.create(model);
    }
    // anthropic / gemini 鍚岀悊
}
```

**Controller 涓嶅姩**,浣?Service 鏀规垚:

```java
public StreamingResponseBody chatCompletions(...) {
    GatewayRoute route = routeService.resolve(model);
    ChatClient client = factory.build(route);

    Flux<ChatResponse> flux = client.prompt()
        .messages(buildMessages(requestBody))   // 鎶?OpenAI 鏍煎紡杞?Spring AI Message
        .stream()
        .chatResponse();

    return outputStream -> flux.doOnNext(chunk -> {
        // 鎶?Spring AI ChatResponse 杞洖 OpenAI SSE 鏍煎紡杈撳嚭
        outputStream.write(toOpenAiSse(chunk).getBytes());
        outputStream.flush();
    }).blockLast();
}
```

### 鏀归€犳楠?
1. **绗竴姝?*:鏂板 `UpstreamChatClientFactory`,鍦ㄦ柊鎺ュ彛 `/v2/chat/completions` 涓嬬敤 Spring AI 瀹炵幇,**鑰佹帴鍙ｄ繚鐣欎笉鍔?*
2. **绗簩姝?*:璺?A/B 娴嬭瘯,鏂版帴鍙ｇ‘璁ょǔ瀹?3. **绗笁姝?*:鎶婅€?Controller 鍐呴儴瀹炵幇鍒囧埌鏂伴€昏緫,鏃х殑鍗忚杞崲浠ｇ爜鍒犻櫎
4. **绗洓姝?*:鍒犻櫎 [GatewayGeminiController.java](src/main/java/com/zxw/modules/gateway/controller/GatewayGeminiController.java) 閲屾墍鏈?`buildChatCompletionsRequest` / `convertChatCompletionsSseToGemini` 绛夋墜鍐欏嚱鏁?
### 楠岃瘉(姣忎竴姝ラ兘瑕?

- OpenAI 瀹㈡埛绔?curl): `POST /v1/chat/completions` 娴佸紡 + 闈炴祦寮?- Gemini 瀹㈡埛绔? `POST /v1beta/models/gemini-pro:streamGenerateContent`
- Anthropic 瀹㈡埛绔? `POST /v1/messages`
- 涓変釜 SDK(openai-python / anthropic-python / google-genai)鍚勮窇涓€娆?
### 鍥炴粴

姣忎竴姝ョ嫭绔?commit,鍑洪棶棰橀€愪釜 revert銆傛渶鍧忔儏鍐靛垏鍥炶€佷唬鐮?鍙崯澶遍樁娈?6 鐨勫伐浣溿€?
---

## 闃舵 7(鍙€?:MyBatis-Plus 娓愯繘杩佺Щ

### 鏄惁瑕佸仛?
**涓嶅己鍒躲€?* 濡傛灉浣犲彧鎯?鐢ㄤ笂鏂版妧鏈?涓斾笉鎯虫帹缈诲凡缁忚兘璺戠殑浠ｇ爜,鍙互**鍙湪鏂版ā鍧楃敤 MP**,鑰佹ā鍧楃户缁?JdbcTemplate銆備袱鑰呭苟瀛橀浂鍐茬獊銆?
### 鏀归€犵瓥鐣?
**鉂?涓嶈鍋氱殑**:鎶?15 涓?Service 涓€娆℃€у叏鏀瑰畬銆?
**鉁?鎺ㄨ崘鍋氭硶**:
1. **绠€鍗?CRUD 浼樺厛**:浠?`AdminUserService.getUser()`銆乣AdminApiKeyService.listApiKeys()` 杩欑绾煡璇㈠紑濮?2. **澶嶆潅 JOIN 淇濈暀**:[GatewayRouteService.findRoutes()](src/main/java/com/zxw/modules/gateway/service/GatewayRouteService.java)(4 琛?JOIN + 鎺掑簭)缁х画鐢?JdbcTemplate,MP 鍙嶈€屾洿鍟板棪
3. **DDL 妫€鏌ヤ繚鐣?*:[BootstrapDataInitializer.java](src/main/java/com/zxw/config/BootstrapDataInitializer.java) 閲岀殑 `hasTable()` / `hasColumn()` 涓嶈鍔?4. **閫昏緫鍒犻櫎缁熶竴**:鐢?MP 鐨?`@TableLogic` 娉ㄨВ澶勭悊 `deleted` 瀛楁,浣嗚淇濊瘉鑰佷唬鐮佷篃璁よ繖涓涔?
### pom.xml 澧炲姞渚濊禆

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```

### 閰嶇疆

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

### 绀轰緥:杩佺Щ涓€涓?Service

鏂板缓 `entity/User.java`:
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

鏂板缓 `mapper/UserMapper.java`:
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

`AdminUserService` 閲?*鏂板**(涓嶆浛鎹?瀛楁:
```java
private final UserMapper userMapper;   // 鏂板
private final JdbcTemplate jdbcTemplate;  // 淇濈暀

// 绠€鍗曟煡璇㈢敤 MP
public User getUser(Long id) {
    return userMapper.selectById(id);
}

// 澶嶆潅鏌ヨ缁х画 JdbcTemplate
public List<UserListItemResponse> listUsers(...) {
    // 鍘熶唬鐮佷笉鍔?}
```

---

## 闃舵 8(鍙€?:Caffeine 鏈湴缂撳瓨

### 鐥涚偣
[GatewayRouteService.findRoutes()](src/main/java/com/zxw/modules/gateway/service/GatewayRouteService.java) 姣忔璇锋眰閮芥煡 4 琛?JOIN,妯″瀷璺敱鍏跺疄鍙樺寲涓嶉绻?鍙互缂撳瓨銆?
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

### 鍚姩绫诲姞 `@EnableCaching`

```java
@EnableCaching
@SpringBootApplication
public class TestApplication { ... }
```

### 閰嶇疆

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=60s
```

### 浣跨敤

```java
@Cacheable(cacheNames = "model-routes", key = "#modelCode")
public List<GatewayRoute> findRoutes(String modelCode) { ... }

// 妯″瀷/璺敱鍙樻洿鍚庢竻缂撳瓨
@CacheEvict(cacheNames = "model-routes", allEntries = true)
public void updateModel(...) { ... }
```

---

## 闃舵 9(鍙€?:Testcontainers + 闆嗘垚娴嬭瘯

### 鐩爣
鐢?Docker 鍚湡瀹?MySQL + Redis 璺戞祴璇?鑰屼笉鏄?H2 + 宓屽叆 Redis(鍚庤€呰窡鐢熶骇宸紓澶ぇ)銆?
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

### 鍐欎竴涓泦鎴愭祴璇曞熀绫?
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

鍚庣画鎵€鏈夐泦鎴愭祴璇曠户鎵胯繖涓被,**淇濊瘉姣忔娴嬭瘯璺戝湪鐪熷疄 MySQL 涓?*銆?
---

## 闃舵 10(鍙€?:Docker + GitHub Actions

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/Test-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9988
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml(鏈湴寮€鍙?

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: <DB_PASSWORD>
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

## 楠岃瘉娓呭崟(姣忎釜闃舵瀹屾垚鍚庤窇涓€閬?

```bash
# 1. 鍚姩姝ｅ父
./mvnw spring-boot:run

# 2. 鍋ュ悍妫€鏌?curl http://localhost:9988/actuator/health

# 3. 鐧诲綍
curl -X POST http://localhost:9988/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 4. 鍒楁ā鍨?鐢ㄤ笂涓€姝ユ嬁鍒扮殑 token)
curl http://localhost:9988/v1/models -H "Authorization: Bearer <api-key>"

# 5. 闈炴祦寮?chat
curl -X POST http://localhost:9988/v1/chat/completions \
  -H "Authorization: Bearer <api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}'

# 6. 娴佸紡 chat(纭 SSE 鏍煎紡姝ｅ父)
curl -X POST http://localhost:9988/v1/chat/completions \
  -H "Authorization: Bearer <api-key>" \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o-mini","stream":true,"messages":[{"role":"user","content":"hi"}]}'

# 7. Gemini 鍗忚
curl -X POST "http://localhost:9988/v1beta/models/gemini-pro:streamGenerateContent?alt=sse" \
  -H "x-goog-api-key: <api-key>" \
  -d '{"contents":[{"role":"user","parts":[{"text":"hi"}]}]}'

# 8. Anthropic 鍗忚
curl -X POST http://localhost:9988/v1/messages \
  -H "x-api-key: <api-key>" \
  -d '{"model":"claude-3-5-sonnet","max_tokens":100,"messages":[{"role":"user","content":"hi"}]}'
```

8 涓叏杩?= 褰撳墠闃舵楠屾敹閫氳繃銆?
---

## 鍥炴粴绛栫暐

| 闃舵 | 鍑洪棶棰樻€庝箞鍔?|
| --- | --- |
| 1 (Java 21,鍙€? | `git revert`,JDK 鍒囧洖 17;鎴栫洿鎺ヨ烦杩囨湰闃舵 |
| 2 (OpenAPI) | `git revert`,鏂囨。鍔熻兘涓㈠け浣嗕笟鍔′笉鍙楀奖鍝?|
| 3 (Actuator) | `git revert`,鐩戞帶涓㈠け浣嗕笟鍔′笉鍙楀奖鍝?|
| 4 (RestClient) | 鍗曟柟娉曞洖婊?璺?OkHttp 鍏卞瓨鏈熷彲淇濈暀鍙屽疄鐜?|
| 5 (Sentinel) | 娉ㄨВ鍘绘帀鍗虫仮澶嶅師琛屼负;渚濊禆鍙繚鐣?|
| 6 (Spring AI) | 鑰?Controller 淇濈暀鏈熼棿鍙灛闂村垏鍥?鍏ㄥ垏瀹屽悗鍥炴粴瑕?revert 澶氫釜 commit |
| 7 (MyBatis-Plus) | 涓嶆浛鎹㈣€佷唬鐮?鍙湪鏂颁唬鐮佺敤,澶╃劧闆跺洖婊氭垚鏈?|
| 8 (Caffeine) | `@Cacheable` 鍘绘帀鍗虫仮澶?|
| 9-10 | 娴嬭瘯/閮ㄧ讲鐩稿叧,涓嶅奖鍝嶇嚎涓?|

---

## 鎺ㄨ崘鎵ц鑺傚

**绗?1 鍛?蹇呭仛)**:闃舵 0銆?銆?銆?(闃舵 1 璺宠繃鎴栧苟鍏?
- 鍛ㄤ竴:闃舵 0(鍒嗘敮 + 澶囦唤)+ 闃舵 2(OpenAPI)
- 鍛ㄤ簩:闃舵 3(Actuator)
- 鍛ㄤ笁-鍛ㄤ簲:闃舵 4(RestClient 鏇挎崲 OkHttp)鈥?鎱㈡參鏇挎崲,姣忓ぉ鏇夸竴涓や釜鏂规硶

**绗?2 鍛?蹇呭仛)**:闃舵 5
- 闃舵 5(Sentinel)鈥?瑁呮帶鍒跺彴 + 鍔犳敞瑙?+ 閰嶈鍒?1-1.5 澶?
**绗?3-4 鍛?寮虹儓鎺ㄨ崘)**:闃舵 6
- 闃舵 6(Spring AI)鈥?杩欐槸鏈€澶ф敹鐩?浣嗕篃鏈€闇€瑕佸皬蹇?
**涔嬪悗(鐪嬪績鎯?**:闃舵 7-10

---

## 浣犳渶璇ュ厛鍔ㄧ殑:闃舵 1-5(绾?5 涓伐浣滄棩)

杩?5 涓樁娈靛仛瀹?浣犵殑椤圭洰灏变粠"鑳借窇"鍙樻垚"鐜颁唬鍖栥€佸彲瑙傛祴銆佹湁寮规€?,**娌℃湁浠讳綍涓€澶勬帹鍊掗噸鏉?*銆?
闃舵 6(Spring AI)鏄湡姝ｇ殑"鐢ㄦ柊鎶€鏈?,浣嗚棰勭暀鍏呭垎鏃堕棿銆?
濡傛灉浣犳兂鍏堝仛鍝竴涓?鍛婅瘔鎴?鎴戝彲浠ュ啀鍐欏緱鏇寸粏(姣斿鎶?RestClient 鏇挎崲鐨勬瘡涓€琛屽師浠ｇ爜 鈫?鏂颁唬鐮佸垪鍑烘潵,鎴栬€呮妸 Spring AI 鏀归€犵殑瀹屾暣浠ｇ爜绀轰緥缁欎綘)銆?