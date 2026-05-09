# MCP 与 Tool 接入改造说明

## 1. 这份文档是干嘛的

这份文档是结合你当前这个项目，说明如果后面要支持 `tool` 和 `MCP`，项目应该怎么改。

这份文档重点不是讲概念，而是讲：

- 你当前项目现在已经有什么能力
- 如果只想支持 `tool`，最少改哪些地方
- 如果还想支持标准 `MCP`，应该再加哪一层
- 推荐的目录结构怎么分
- 哪种改法最稳，不容易把现在项目搞乱

---

## 2. 先说结论

你这个项目 **不是从零开始接 tool**，而是已经有了一个“网关内置工具执行雏形”。

所以最稳的路线不是直接把 `MCP` 硬塞进 `GatewayChatService`，而是：

```text
先把现有 GatewayChatService 里的工具执行逻辑抽成独立 Tool 层
再在 Tool 层外面加一层 MCP 协议适配层
```

一句话理解：

- `Tool` 是功能本身

- `MCP` 是把这些功能标准化暴露出去的协议层

---

## 3. 你当前项目已经具备的基础

从现在代码看，项目已经有下面这些能力。

### 3.1 已经有 Responses / Agent 入口

项目已经支持：

- `POST /v1/responses`
- `POST /responses`

入口位置：

- `src/main/java/com/zxw/modules/gateway/controller/GatewayChatController.java`

这说明你的项目已经有“让模型走响应式对话协议”的入口，不需要从零新开一套 AI 接口。

### 3.2 已经有 tools 参数透传和转换逻辑

在 `GatewayChatService` 里已经有：

- `transformResponsesToolsToChatTools`
- `transformResponsesToolChoiceToChatToolChoice`

说明现在已经有“工具定义结构转换”的逻辑，只是还没有抽象成统一 Tool 框架。

### 3.3 已经有本地 agent/tool 调度逻辑

在 `GatewayChatService` 里已经有这些方法：

- `executeAgentTool`
- `executeListFilesTool`
- `executeReadFileTool`
- `executeSearchCodeTool`
- `executeWriteFileTool`
- `executeReplaceInFileTool`
- `executeRunCommandTool`

也就是说：

你现在已经不是“没有 tool”，而是“tool 逻辑写在了一个超大的 service 里面，还没抽层”。

### 3.4 已经有 agent 会话和工具日志表

数据库里也已经有：

- `agent_sessions`
- `agent_messages`
- `agent_tool_logs`

这说明你后面就算扩展 MCP，也不需要重新设计整套执行记录体系。

---

## 4. 当前项目的核心问题

你现在如果直接继续往 `GatewayChatService` 里加 MCP，会越来越乱。

当前主要问题有这几个：

### 4.1 Tool 执行逻辑和网关转发逻辑耦合太深

现在一个类里同时做了：

- 网关鉴权
- 模型路由
- 请求转发
- token 计费
- 日志记录
- agent 会话
- tool 执行

这样后面一旦再加 MCP，会变成：

- tool 定义也在这里
- MCP 协议解析也在这里
- resource 读取也在这里
- prompt 管理也在这里

那这个类会越来越难维护。

### 4.2 现有 tool 是“写死的 switch 分发”

现在工具执行本质上还是类似：

```text
根据 toolName
-> if / switch
-> 调不同的方法
```

这种方式前期能跑，但后面如果工具越来越多，就会有几个问题：

- 工具注册不统一
- 参数定义不统一
- 权限控制不好扩展
- 不利于做 MCP 的 `listTools`

### 4.3 现有 tool 更像“内置能力”，还不是标准协议能力

也就是说它现在适合网关内部 agent 用，但还不适合：

- 给外部 MCP Client 统一发现
- 按标准格式列出 tools
- 按统一 schema 调用工具
- 后面扩展 resources / prompts

---

## 5. 如果只想先支持 Tool，应该怎么改

如果你第一步只想把项目整理成“标准一点的 tool 架构”，那最小改法是：

```text
不改现有 AI 接口协议
只把 GatewayChatService 里的工具执行部分抽层
```

### 5.1 推荐新增目录

建议在项目里新增：

```text
src/main/java/com/zxw/modules/gateway/tool
├─ dto
├─ executor
├─ registry
└─ support
```

也可以更简单一点：

```text
src/main/java/com/zxw/modules/gateway/tool
├─ ToolDefinition.java
├─ ToolExecutor.java
├─ ToolDispatcher.java
├─ ToolContext.java
├─ ToolResult.java
└─ executor
   ├─ ListFilesToolExecutor.java
   ├─ ReadFileToolExecutor.java
   ├─ SearchCodeToolExecutor.java
   ├─ WriteFileToolExecutor.java
   ├─ ReplaceInFileToolExecutor.java
   └─ RunCommandToolExecutor.java
```

### 5.2 建议抽出的核心类

#### 1. `ToolDefinition`

作用：

- 定义工具名
- 定义工具描述
- 定义参数 schema
- 定义工具是否允许当前场景使用

示意：

```java
public record ToolDefinition(
        String name,
        String description,
        JsonNode inputSchema
) {
}
```

#### 2. `ToolExecutor`

作用：

- 每个工具对应一个执行器
- 真正执行具体逻辑

示意：

```java
public interface ToolExecutor {

    String getToolName();

    ToolDefinition definition();

    ToolResult execute(ToolContext context, JsonNode arguments) throws Exception;
}
```

#### 3. `ToolContext`

作用：

- 给工具传当前会话信息
- 给工具传用户信息
- 给工具传工作目录
- 给工具传 responseId、step 等上下文

这样以后工具执行器就不用直接依赖 `GatewayChatService` 里的各种局部变量。

#### 4. `ToolResult`

作用：

- 统一封装工具执行结果
- 保留模型可见结果
- 保留文件变化摘要
- 保留日志信息

这个类后面不管是给网关 agent 用，还是给 MCP 用，都可以复用。

#### 5. `ToolDispatcher`

作用：

- 根据 `toolName` 找对应执行器
- 做统一异常处理
- 做统一权限拦截

这样就能替代现在 `executeAgentTool` 里面写死的分发逻辑。

### 5.3 现有代码应该怎么迁移

你现在这些方法：

- `executeListFilesTool`
- `executeReadFileTool`
- `executeSearchCodeTool`
- `executeWriteFileTool`
- `executeReplaceInFileTool`
- `executeRunCommandTool`

建议改成：

- 每个方法单独迁移到一个 `ToolExecutor`
- `GatewayChatService` 只保留调度入口

改完后 `GatewayChatService` 的职责应该变成：

```text
1. 判断当前请求是否进入 agent
2. 让模型产出 tool call
3. 调 ToolDispatcher 执行
4. 把结果回灌给模型
5. 记录会话与日志
```

不要再让它负责每一个工具的具体实现细节。

---

## 6. 如果想支持标准 MCP，应该怎么改

如果你后面要支持真正标准化一点的 `MCP`，建议在 Tool 层外面再包一层：

```text
src/main/java/com/zxw/modules/mcp
├─ controller
├─ service
├─ dto
├─ registry
└─ support
```

### 6.1 MCP 层的职责

MCP 层不要直接写业务逻辑，它只做这些事：

- 对外暴露可发现的 tools
- 对外暴露可读取的 resources
- 接收标准化的 tool call 请求
- 调你内部的 ToolDispatcher
- 把结果包装成 MCP 风格返回

也就是说：

```text
MCP 层 = 协议适配层
Tool 层 = 功能执行层
Service 层 = 业务逻辑层
```

### 6.2 推荐最小能力

如果你要做第一版 MCP，建议先只做这四个能力：

#### 1. `listTools`

作用：

- 返回当前系统有哪些工具可以调用

内部数据来源：

- `ToolRegistry`

#### 2. `callTool`

作用：

- 根据工具名和参数执行工具

内部调用：

- `ToolDispatcher`

#### 3. `listResources`

作用：

- 返回当前系统可以读取哪些资源

例如：

- 站点配置说明
- API 文档
- 某些系统说明文档

#### 4. `readResource`

作用：

- 读取某个具体资源内容

例如：

- 读取“站点设置说明”
- 读取“套餐中心说明”
- 读取“当前模型清单”

---

## 7. 适合你这个项目先做成 tool 的能力

你这个项目不建议一上来先做“文件系统工具”给业务管理员用。

最适合先做成业务 tool 的，是你现成就有 service 的这些能力。

### 7.1 站点设置类

- `getSiteSettings`
- `updateSiteSettings`

对应现有 service：

- `AdminSiteSettingsService`

### 7.2 用户类

- `getUserInfo`
- `listUsers`
- `getUserWallet`

对应现有 service：

- `AdminUserService`

### 7.3 模型类

- `listModels`
- `getModelDetail`
- `listModelGroups`

对应现有 service：

- `AdminModelService`
- `UserModelAccessService`

### 7.4 请求统计类

- `getDashboardOverview`
- `getRequestStats`
- `listRequestLogs`

对应现有 service：

- `AdminDashboardService`
- `AdminRequestLogService`

### 7.5 套餐类

- `getCurrentUserPackages`
- `getPackageUsage`
- `listPurchasedPackages`

对应现有 service：

- `UserModelAccessService`

这些更适合先做，因为：

- 现有 service 已经有
- 业务价值高
- 没有本地命令执行那种高风险
- 后面给 MCP 暴露也更自然

---

## 8. 适合后面再做的高风险工具

下面这些能力不建议第一批就开放给业务侧 MCP：

- `runCommand`
- `writeFile`
- `replaceInFile`
- 任意本地文件读取

原因很简单：

- 风险高
- 权限难控
- 参数校验复杂
- 很容易被误调用

这些更适合保留为“内部 agent 专用工具”，而不是一开始就暴露成通用 MCP tool。

---

## 9. 推荐的整体分层

如果要整理成更清晰的结构，我建议按下面分。

```text
controller
  只负责收请求、回响应

service
  只负责业务逻辑

tool
  只负责把已有业务能力包装成模型可调用工具

mcp
  只负责把 tool / resource 按协议暴露出去

mapper
  只负责数据库访问
```

可以理解成：

```text
原来的 Service 是“给前端/后台用的业务能力”
新的 Tool 层是“给模型调用的能力包装层”
新的 MCP 层是“给外部 AI 客户端发现和调用这些能力的协议层”
```

---

## 10. 最小改造顺序

建议你按这个顺序改，不容易乱。

### 第一步

先抽 Tool 层，不接 MCP。

目标：

- 让 `GatewayChatService` 变薄
- 让每个工具独立成类
- 统一工具定义和工具执行方式

### 第二步

把现有内置工具接到 `ToolRegistry`。

目标：

- 工具可注册
- 工具可枚举
- 后面能做 `listTools`

### 第三步

先做业务型 tools。

例如：

- `getSiteSettings`
- `listModels`
- `getRequestStats`

目标：

- 验证“模型调工具 -> 调 service -> 返回结果”这条链路

### 第四步

再加 `modules/mcp` 协议层。

目标：

- 实现 `listTools`
- 实现 `callTool`
- 后面再扩 `resources`

### 第五步

最后再考虑是否把高风险文件类工具也纳入 MCP。

---

## 11. 最小可落地版本建议

如果你只是想先做一个能跑、能演示、结构也不乱的版本，我建议第一版只做下面这些。

### 11.1 第一版 Tool

- `getSiteSettings`
- `listModels`
- `getDashboardOverview`
- `listRequestLogs`

### 11.2 第一版 MCP

- `listTools`
- `callTool`

### 11.3 第一版不做

- 不做 `runCommand`
- 不做任意文件编辑
- 不做复杂资源权限体系
- 不做多租户复杂隔离

这样你可以先把整个链路打通。

---

## 12. 你这个项目后面最合理的形态

后面比较理想的结构应该是这样：

```text
用户请求
-> GatewayChatController / GatewayChatService
-> 模型判断是否调用 tool
-> ToolDispatcher
-> 对应 ToolExecutor
-> 调现有业务 Service
-> 返回 ToolResult
-> 回灌给模型
-> 输出最终回答
```

如果是 MCP 客户端调用：

```text
MCP Client
-> McpController
-> McpService
-> ToolDispatcher
-> ToolExecutor
-> 业务 Service
-> 返回标准 MCP 结果
```

这样：

- 网关逻辑和工具逻辑分开
- 工具逻辑和业务逻辑分开
- MCP 协议和业务逻辑分开

后面维护会轻松很多。

---

## 13. 最后的建议

你这个项目现在最正确的方向不是：

```text
直接在 GatewayChatService 里继续堆 MCP 代码
```

而是：

```text
先把工具能力从 GatewayChatService 抽出来
再决定是否加 MCP 协议层
```

这样做的好处是：

- 不会破坏你现有网关转发逻辑
- 不会让大类继续失控
- 后面既能给内部 agent 用
- 也能给 MCP Client 用

---

## 14. 一句话总结

最稳改法就是：

```text
先做 Tool 层标准化
再做 MCP 层协议化
业务逻辑仍然留在原来的 Service
```

不要把：

- 业务逻辑
- tool 执行
- agent 调度
- MCP 协议

全都继续堆在一个 `GatewayChatService` 里面。
