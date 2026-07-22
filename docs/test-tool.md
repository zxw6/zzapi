# test-tool

## 1. 这个文件是做什么的

这个文件不是正式生产方案，而是给你学习 `tool` 用的内部示例。

目标是让你看明白：

- 什么样的能力适合做成 `tool`
- 你的这个项目里哪些功能可以变成 `tool`
- `tool` 一般怎么定义
- Java 代码大概怎么写

你可以把这里的内容理解成：

> 把你现有项目里的 Service 能力，再包一层，变成大模型可以调用的功能。

---

## 2. 什么叫这个项目里的 Tool

在你这个项目里，`tool` 可以理解成：

- 一个明确的功能
- 有固定入参
- 有固定返回值
- 可以被模型调用

例如：

- 查询站点信息
- 查询用户信息
- 查询模型列表
- 查询请求统计
- 搜索知识库

这些都非常适合做成 `tool`。

---

## 3. 适合你项目的 Tool 清单

下面这些 `tool` 是最适合你当前项目拿来学习的。

### 3.1 getSiteSettings

用途：

- 查询站点基本信息

适合回答的问题：

- 当前站点名称是什么
- 当前站点 baseUrl 是什么
- 当前管理员邮箱是什么

入参：

```json
{}
```

返回值示例：

```json
{
  "siteName": "API Hub 中转站",
  "adminEmail": "admin@apihub.io",
  "siteDescription": "企业级 AI API 中转管理平台",
  "baseUrl": "https://api.yourdomain.com",
  "footerText": "Powered by API Hub",
  "themeMode": "LIGHT"
}
```

---

### 3.2 getUserInfo

用途：

- 查询指定用户信息

适合回答的问题：

- 某个用户状态是什么
- 某个用户余额是多少
- 某个用户是否可用

入参示例：

```json
{
  "userId": 1001
}
```

返回值示例：

```json
{
  "userId": 1001,
  "username": "demo_user",
  "nickname": "测试用户",
  "status": "ACTIVE",
  "balance": 120.5
}
```

---

### 3.3 listModels

用途：

- 查询当前可用模型列表

适合回答的问题：

- 当前有哪些模型可用
- 某个模型是否启用
- 模型价格是多少

入参示例：

```json
{
  "status": "ACTIVE"
}
```

返回值示例：

```json
[
  {
    "modelCode": "gpt-4o-mini",
    "modelName": "GPT-4o Mini",
    "status": "ACTIVE",
    "promptPrice": 0.0015,
    "completionPrice": 0.002
  }
]
```

---

### 3.4 getRequestStats

用途：

- 查询请求统计信息

适合回答的问题：

- 最近 7 天请求量是多少
- 哪个模型调用最多
- 最近失败请求多不多

入参示例：

```json
{
  "days": 7
}
```

返回值示例：

```json
{
  "days": 7,
  "totalRequests": 1200,
  "successRequests": 1180,
  "failedRequests": 20
}
```

---

### 3.5 searchKnowledgeBase

用途：

- 搜索知识库
- 这是最像 RAG 的 `tool`

适合回答的问题：

- 怎么创建 API Key
- 如何重置密码
- 如何查看请求日志

入参示例：

```json
{
  "query": "怎么创建 API Key",
  "topK": 3
}
```

返回值示例：

```json
[
  {
    "content": "进入后台后，打开 API Keys 管理页面，点击创建按钮即可生成新的密钥。",
    "score": 0.89,
    "source": "帮助中心"
  },
  {
    "content": "创建成功后请及时保存 Secret，系统不会再次完整展示。",
    "score": 0.81,
    "source": "帮助中心"
  }
]
```

---

## 4. 一个最简单的 Tool 定义思路

从学习角度，你可以把一个 `tool` 定义成这几部分：

- `name`
- `description`
- `input schema`
- `handler`

例如：

```text
name: getSiteSettings
description: 查询当前站点配置
input schema: 无参数
handler: 调用 AdminSiteSettingsService.getSiteSettings()
```

---

## 5. 学习版 Java 结构建议

如果你要在 Java 项目里练习，可以先写一个简单的工具注册类。

例如：

- `ToolDefinition`
- `ToolRegistry`
- `ToolExecutor`

这样你能先学会“工具是怎么组织的”，再决定后面要不要接真实模型调用。

---

## 6. 一个简单的 ToolDefinition 示例

```java
public record ToolDefinition(
        String name,
        String description
) {
}
```

这个类先最小化，后面你再慢慢加：

- 入参定义
- 出参定义
- 权限控制

---

## 7. 一个简单的 ToolRegistry 示例

```java
import java.util.List;

public class ToolRegistry {

    public List<ToolDefinition> list() {
        return List.of(
                new ToolDefinition("getSiteSettings", "查询当前站点配置"),
                new ToolDefinition("getUserInfo", "根据用户ID查询用户信息"),
                new ToolDefinition("listModels", "查询当前模型列表"),
                new ToolDefinition("getRequestStats", "查询请求统计信息"),
                new ToolDefinition("searchKnowledgeBase", "搜索知识库并返回相似内容")
        );
    }
}
```

这个作用是：

- 先把工具名字和用途列出来
- 让你在脑子里建立“我的系统里有哪些工具”

---

## 8. 一个简单的 ToolExecutor 思路

下面这个版本是学习用的，不追求复杂。

```java
import java.util.Map;

public interface ToolExecutor {

    String getToolName();

    Object execute(Map<String, Object> arguments);
}
```

意思很简单：

- 每个工具有自己的名字
- 每个工具接收一组参数
- 执行后返回结果

---

## 9. getSiteSettingsTool 学习版示例

这个最适合你现在拿来学，因为你的项目已经有站点信息表和接口了。

```java
import com.zxw.modules.system.service.AdminSiteSettingsService;

import java.util.Map;

public class GetSiteSettingsTool implements ToolExecutor {

    private final AdminSiteSettingsService adminSiteSettingsService;

    public GetSiteSettingsTool(AdminSiteSettingsService adminSiteSettingsService) {
        this.adminSiteSettingsService = adminSiteSettingsService;
    }

    @Override
    public String getToolName() {
        return "getSiteSettings";
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        return adminSiteSettingsService.getSiteSettings();
    }
}
```

你可以把它理解成：

```text
大模型不会直接查数据库
而是调用 getSiteSettings
然后 tool 再去调你的 service
```

---

## 10. getUserInfoTool 学习版示例

这个只是示意写法，重点看结构。

```java
import java.util.Map;

public class GetUserInfoTool implements ToolExecutor {

    private final AdminUserService adminUserService;

    public GetUserInfoTool(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Override
    public String getToolName() {
        return "getUserInfo";
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        Object userIdValue = arguments.get("userId");
        Long userId = Long.valueOf(String.valueOf(userIdValue));
        return adminUserService.getUser(userId);
    }
}
```

这类工具适合做：

- 查询用户资料
- 查询钱包信息
- 查询状态

---

## 11. searchKnowledgeBaseTool 学习版示例

这个是你以后做 RAG 最关键的工具。

```java
import java.util.List;
import java.util.Map;

public class SearchKnowledgeBaseTool implements ToolExecutor {

    private final KnowledgeSearchService knowledgeSearchService;

    public SearchKnowledgeBaseTool(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Override
    public String getToolName() {
        return "searchKnowledgeBase";
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        String query = String.valueOf(arguments.get("query"));
        Object topKValue = arguments.getOrDefault("topK", 3);
        int topK = Integer.parseInt(String.valueOf(topKValue));

        return knowledgeSearchService.search(query, topK);
    }
}
```

这个工具的意义是：

- 用户提问
- 模型先调用 `searchKnowledgeBase`
- 工具去查向量库
- 再把结果给模型

---

## 12. 一个简化版调度器示例

如果你只是自己学习，可以先写一个最简单的调度器。

```java
import java.util.List;
import java.util.Map;

public class SimpleToolDispatcher {

    private final List<ToolExecutor> executors;

    public SimpleToolDispatcher(List<ToolExecutor> executors) {
        this.executors = executors;
    }

    public Object call(String toolName, Map<String, Object> arguments) {
        return executors.stream()
                .filter(tool -> tool.getToolName().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("tool not found: " + toolName))
                .execute(arguments);
    }
}
```

你就可以像这样测试：

```java
Object result = dispatcher.call("getSiteSettings", Map.of());
```

或者：

```java
Object result = dispatcher.call("searchKnowledgeBase", Map.of(
        "query", "怎么创建 API Key",
        "topK", 3
));
```

---

## 13. 如果后面要接到大模型里，逻辑是什么

真正接大模型时，流程通常是：

```text
用户问题
-> 模型判断要不要调用 tool
-> 模型输出 toolName 和 arguments
-> 后端调度器执行 tool
-> tool 返回结果
-> 模型根据结果继续回答
```

例如：

```text
用户：当前站点 baseUrl 是什么？
模型：调用 getSiteSettings
工具返回：baseUrl = https://api.yourdomain.com
模型：当前站点接口基础地址是 https://api.yourdomain.com
```

---

## 14. 你这个项目最适合先学哪个 Tool

最推荐你先学这两个：

### 14.1 getSiteSettings

原因：

- 你刚做完这个模块
- 有表
- 有 service
- 有接口
- 最容易验证

### 14.2 searchKnowledgeBase

原因：

- 这是以后做知识库问答的核心
- 学会它，你就知道 `tool + 向量检索 + AI` 是怎么串起来的

---

## 15. 学习阶段不要急着做太复杂

你现在只是学习，所以不需要一上来就做：

- 权限系统
- 工具调用审计
- 参数 JSON Schema 自动生成
- 多工具并发调度
- 重试机制

你只要先学明白这四件事就够了：

1. 工具名是什么
2. 工具有什么参数
3. 工具实际调哪个 service
4. 工具返回什么数据

---

## 16. 一句话总结

这个项目里的 `tool`，本质上就是：

```text
把你已经写好的后端能力
包装成模型可以调用的函数
```

你现在最适合练习的顺序是：

```text
getSiteSettings
-> getUserInfo
-> listModels
-> getRequestStats
-> searchKnowledgeBase
```

这条线学顺了，后面你再接 MCP、Agent、RAG，就会轻松很多。
