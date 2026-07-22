# GPT-5.6 / Codex Responses API 兼容修改说明

本文档只说明 `GatewayChatService.java` 应该怎么改，不直接修改源码。

当前文件：

`G:\project-ai\Test\src\main\java\com\zxw\modules\gateway\service\GatewayChatService.java`

## 结论

要让 Codex 使用 `gpt-5.6-sol` 时能显示推理过程、自动读取文件、调用 shell、调用图片等工具，中转站必须把 `/v1/responses` 当成原生 Responses API 透传。

不能把 5.6 的 Responses 请求转换成 `/chat/completions`，也不能把 Responses SSE 聚合成普通文本后再重新组装。否则 Codex 客户端拿不到这些关键协议字段：

- `tools`
- `tool_choice`
- `previous_response_id`
- `response.output_item.added`
- `response.output_item.done`
- `function_call`
- `function_call_output`
- `call_id`
- reasoning 相关事件或 output item
- `response.completed`

一句话：5.6/Codex 要的是“事件流和工具调用协议”，不是普通聊天文本。

## 当前代码里的风险点

### 1. Responses 入口会走本地 agent 分支

位置：`GatewayChatService.java` 约第 297 行：

```java
if (input instanceof ObjectNode objectInput && shouldHandleResponsesAsAgent(route.modelCode(), objectInput)) {
    if (stream) {
        return closeAndReturn(streamAgentResponses(auth, route, requestId, effectiveRequestBody, objectInput,
                servletRequest, startTime), concurrencyPermit);
    }
    return closeAndReturn(normalAgentResponses(auth, route, requestId, effectiveRequestBody, objectInput,
            servletRequest, startTime), concurrencyPermit);
}
```

这个分支会让网关自己尝试当 agent。但你的代码里约第 3645 行已经明确关闭了服务器本地工具：

```java
throw new BusinessException(400, "当前服务已切换为纯网关模式，不再在服务器上扫描、读取、修改或执行本地文件。请在你自己的电脑上安装 Codex，并通过本网关 API 使用你的本地工具。");
```

所以对 Codex 桌面端来说，这个分支不应该接管。Codex 的工具应该由本地 Codex 客户端执行，中转站只负责把模型的 `function_call` 事件透传回客户端。

建议改法：对 5.6/Codex 原生 Responses 模型，直接跳过本地 agent 分支。

```java
if (input instanceof ObjectNode objectInput
        && !shouldKeepNativeResponsesRouting(route)
        && shouldHandleResponsesAsAgent(route.modelCode(), objectInput)) {
    if (stream) {
        return closeAndReturn(streamAgentResponses(auth, route, requestId, effectiveRequestBody, objectInput,
                servletRequest, startTime), concurrencyPermit);
    }
    return closeAndReturn(normalAgentResponses(auth, route, requestId, effectiveRequestBody, objectInput,
            servletRequest, startTime), concurrencyPermit);
}
```

### 2. 5.6 不能走 Responses -> Chat Completions 兼容转换

位置：`normalResponses()` 约第 745 行，`streamResponses()` 约第 770 行。

当前代码存在这些兼容转换：

```java
shouldUseResponsesCompatibility(route)
executeResponsesCompatibility(route, upstreamBody)
executeResponsesCompatibilityStream(route, upstreamBody)
buildChatCompletionsRequestFromResponses(...)
buildResponsesFromChatCompletion(...)
```

这些方法适合“只想让普通聊天能通”的旧模型或非 Responses 上游，不适合 Codex/5.6。

建议原则：

- `gpt-5.6-sol`
- `gpt-5.6-*`
- `gpt-5-*`
- `codex-*`

这些模型必须走 `/responses` 原生转发，不走 `/chat/completions` 兼容层。

建议确认或改成：

```java
private boolean shouldKeepNativeResponsesRouting(GatewayRouteService.RouteDefinition route) {
    if (route == null) {
        return false;
    }
    String publicModel = route.modelCode() == null ? "" : route.modelCode().toLowerCase(Locale.ROOT);
    String upstreamModel = route.upstreamModel() == null ? "" : route.upstreamModel().toLowerCase(Locale.ROOT);
    return isNativeResponsesModel(publicModel) || isNativeResponsesModel(upstreamModel);
}

private boolean isNativeResponsesModel(String model) {
    if (model == null) {
        return false;
    }
    String normalized = model.toLowerCase(Locale.ROOT);
    return normalized.startsWith("gpt-5")
            || normalized.contains("gpt-5.6")
            || normalized.contains("5.6-sol")
            || normalized.contains("codex");
}
```

然后让兼容模式明确排除这些模型：

```java
private boolean shouldUseResponsesCompatibility(GatewayRouteService.RouteDefinition route) {
    return route != null
            && !isAnthropicRoute(route)
            && !isOfficialOpenAiRoute(route)
            && !shouldKeepNativeResponsesRouting(route)
            && !shouldAggregateCodexStream(route);
}
```

### 3. `shouldAggregateCodexStream()` 不要拦截 5.6

位置：约第 1363 行：

```java
private boolean shouldAggregateCodexStream(GatewayRouteService.RouteDefinition route) {
    return !isOfficialOpenAiRoute(route)
            && route.upstreamModel() != null
            && route.upstreamModel().toLowerCase(Locale.ROOT).contains("codex");
}
```

这个分支会把 chat stream 聚合成完整 JSON。对 5.6/Codex Responses 是危险的。

建议改成：

```java
private boolean shouldAggregateCodexStream(GatewayRouteService.RouteDefinition route) {
    return !shouldKeepNativeResponsesRouting(route)
            && !isOfficialOpenAiRoute(route)
            && route.upstreamModel() != null
            && route.upstreamModel().toLowerCase(Locale.ROOT).contains("codex");
}
```

### 4. `buildResponsesFromSse()` 不能用于给客户端生成 5.6 结果

位置：约第 1520 行。

这个方法现在只收集：

```java
response.output_text.delta
response.output_text.done
message item
usage
```

它最终会重新构造一个普通 `message` 输出。这样会丢掉：

- `function_call`
- `function_call_output`
- reasoning item
- tool call 的 `call_id`
- 多个 output item 的顺序
- 原始 SSE 事件类型

建议：这个方法只能用于日志和计费解析，不要用于返回给 Codex 客户端。

流式请求应该使用已有的 `streamOpenAiEventStream(...)` 直接透传：

```java
return streamOpenAiEventStream(auth, route, requestId, originalRequestBody,
        upstreamBody, servletRequest, startTime, "/responses", true, requestPermit);
```

非流式请求应该直接返回上游 `/responses` JSON，不要转 Chat：

```java
UpstreamTextResponse response = executeOpenAiTextRequest(route, "/responses", upstreamBody);
String responseBody = rewriteClientFacingModel(route,
        normalizeUpstreamResponseBody(route, response.statusCode(), response.body()));
return ResponseEntity.status(response.statusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(responseBody);
```

### 5. 不要删除 Codex 的工具字段

位置：约第 2350 行的 `optimizeResponsesInputForSimpleQuery(...)`。

你现在已经有保护：

```java
if (hasRequestTools(input) || hasWorkspaceHint(input) || isExplicitGatewayAgentRequest(input)) {
    return input;
}
```

这个方向是对的。建议进一步收紧：只要是 5.6/Codex 原生 Responses 模型，就完全不要做瘦身优化。

示例：

```java
private ObjectNode optimizeResponsesInputForSimpleQuery(ObjectNode input, String modelCode) {
    if (input == null || isNativeResponsesModel(modelCode)) {
        return input;
    }
    // 保留你原来的逻辑
}
```

原因：Codex 的请求里可能会包含历史、工具配置、上下文引用、`previous_response_id`。这些字段不是“臃肿上下文”，而是工具链状态。

## 推荐的最小修改路线

### 第一步：增加原生 Responses 判断

放在 `GatewayChatService.java` 的工具判断方法附近，例如 `shouldKeepNativeResponsesRouting(...)` 附近：

```java
private boolean isNativeResponsesModel(String model) {
    if (model == null) {
        return false;
    }
    String normalized = model.toLowerCase(Locale.ROOT);
    return normalized.startsWith("gpt-5")
            || normalized.contains("gpt-5.6")
            || normalized.contains("5.6-sol")
            || normalized.contains("codex");
}
```

### 第二步：让 5.6 保持原生 `/responses`

```java
private boolean shouldKeepNativeResponsesRouting(GatewayRouteService.RouteDefinition route) {
    if (route == null) {
        return false;
    }
    String publicModel = route.modelCode() == null ? "" : route.modelCode().toLowerCase(Locale.ROOT);
    String upstreamModel = route.upstreamModel() == null ? "" : route.upstreamModel().toLowerCase(Locale.ROOT);
    return isNativeResponsesModel(publicModel) || isNativeResponsesModel(upstreamModel);
}
```

### 第三步：本地 agent 分支排除 5.6

把 Responses 入口里的判断从：

```java
if (input instanceof ObjectNode objectInput && shouldHandleResponsesAsAgent(route.modelCode(), objectInput)) {
```

改成：

```java
if (input instanceof ObjectNode objectInput
        && !shouldKeepNativeResponsesRouting(route)
        && shouldHandleResponsesAsAgent(route.modelCode(), objectInput)) {
```

### 第四步：5.6 流式请求直接透传

确认 `streamResponses(...)` 对 5.6 最终走这里：

```java
return streamOpenAiEventStream(auth, route, requestId, originalRequestBody,
        upstreamBody, servletRequest, startTime, "/responses", true, requestPermit);
```

不要走：

```java
executeResponsesCompatibilityStream(...)
streamCodexResponses(...)
executeCodexResponsesByStreaming(...)
buildResponsesEventStreamFromChatCompletion(...)
```

### 第五步：5.6 非流式请求直接转发 `/responses`

确认 `normalResponses(...)` 对 5.6 最终走：

```java
executeOpenAiTextRequest(route, "/responses", upstreamBody)
```

不要走：

```java
executeResponsesCompatibility(...)
executeCodexResponsesByStreaming(...)
buildResponsesFromChatCompletion(...)
```

## 验证方法

### 验证 1：普通流式 Responses

请求：

```bash
curl -N http://你的网关地址/v1/responses \
  -H "Authorization: Bearer 你的key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-5.6-sol",
    "stream": true,
    "input": "说一句中文"
  }'
```

期望看到类似事件：

```text
data: {"type":"response.created",...}
data: {"type":"response.output_text.delta",...}
data: {"type":"response.completed",...}
data: [DONE]
```

### 验证 2：工具调用链路

用一个最小 function tool 测：

```bash
curl -N http://你的网关地址/v1/responses \
  -H "Authorization: Bearer 你的key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-5.6-sol",
    "stream": true,
    "tools": [
      {
        "type": "function",
        "name": "get_time",
        "description": "Get current time",
        "parameters": {
          "type": "object",
          "properties": {},
          "additionalProperties": false
        }
      }
    ],
    "tool_choice": "auto",
    "input": "现在几点了？"
  }'
```

期望中转站原样返回类似事件：

```text
data: {"type":"response.output_item.added","item":{"type":"function_call",...}}
data: {"type":"response.output_item.done","item":{"type":"function_call","call_id":"...","name":"get_time","arguments":"{}"}}
```

重点检查 `call_id` 是否存在。没有 `call_id`，Codex 客户端就无法把工具结果和模型请求对应起来。

### 验证 3：Codex 桌面端

Codex 配置里继续使用：

```toml
wire_api = "responses"
model = "gpt-5.6-sol"
```

然后问：

```text
现在几点了
```

或者：

```text
读取当前项目的 pom.xml
```

正常情况：

- 模型先输出或触发工具调用事件
- Codex 本地执行工具
- Codex 把 `function_call_output` 带着原 `call_id` 发回 `/v1/responses`
- 模型继续生成最终回答

## 最容易踩的坑

1. 只返回 `choices[].delta.content`

这会让聊天能通，但 Codex 工具不可用。

2. 把 `/responses` 转成 `/chat/completions`

这会损坏 5.6 的工具协议和 reasoning 事件。

3. 生成假的 Responses SSE

自己拼 `response.output_text.delta` 可以让前端看到字，但不能让 Codex 正常使用工具。

4. 删除 `tools`、`tool_choice`、`previous_response_id`

这会让多轮工具调用断掉。

5. 服务器端自己执行本地文件工具

你的网关已经是纯网关模式，服务器不该读用户电脑文件。文件、shell、图片等工具应该由 Codex 桌面端执行。

## 推荐最终策略

对 `gpt-5.6-sol`：

- `/v1/responses` 请求体：只改 `model` 为上游模型名，其它字段尽量原样保留。
- `/v1/responses` SSE：按字节或按 chunk 原样透传。
- 日志计费：可以旁路解析，但解析失败不能影响客户端流。
- 不做 Chat Completions 转换。
- 不做本地 agent 接管。
- 不做简单问题瘦身。

这样 5.6 才能在 Codex 里表现得像真正的 agent 模型，而不是一个普通聊天模型。

## 5.6 家族是否都要兼容

不是所有 5.6 模型都要“同一种兼容”，但所有准备用在 Codex、IDE agent、工具调用、多轮 Responses 状态里的 5.6 模型，都必须走原生 `/v1/responses` 透传。

建议按模型角色分：

| 模型角色 | 示例 | 是否必须原生 Responses 透传 | 原因 |
| --- | --- | --- | --- |
| Codex / IDE agent / 自动工具模型 | `gpt-5.6-sol`、你暴露给 Codex 的 5.6 路由 | 必须 | Codex 需要 `function_call`、`call_id`、`function_call_output`、reasoning 和 output item 事件 |
| 高质量旗舰聊天 | `gpt-5.6-sol` | 建议 | 即使暂时只聊天，保留 Responses 能避免后面接工具时再改一遍 |
| 平衡/低成本模型 | 例如 Terra 类路由 | 看用途 | 如果只是普通聊天可以兼容 Chat；如果要工具或 Codex，就必须 Responses |
| 极低成本/分类/简单抽取模型 | 例如 Luna 类路由 | 看用途 | 普通分类可以走简单接口；工具链、多轮状态、reasoning 则必须 Responses |
| 旧模型或非 OpenAI 兼容模型 | 第三方 chat-only 模型 | 不强制 | 这些模型可能没有原生 Responses，只能走兼容转换 |

换句话说，兼容策略不要按“名字里有 5.6 就全部一样处理”，而要按“这个路由是否承载 Codex/工具协议”处理。

## 推荐的模型判断规则

如果你的中转站要暴露多个 5.6 模型，建议把判断拆成两个概念：

1. `isGpt56Family(...)`：识别 5.6 家族。
2. `requiresNativeResponses(...)`：判断这个路由是否必须原生 Responses。

示例：

```java
private boolean isGpt56Family(String model) {
    if (model == null) {
        return false;
    }
    String normalized = model.toLowerCase(Locale.ROOT);
    return normalized.startsWith("gpt-5.6")
            || normalized.contains("5.6-sol")
            || normalized.contains("5.6-terra")
            || normalized.contains("5.6-luna");
}

private boolean requiresNativeResponses(GatewayRouteService.RouteDefinition route) {
    if (route == null) {
        return false;
    }
    String publicModel = route.modelCode() == null ? "" : route.modelCode().toLowerCase(Locale.ROOT);
    String upstreamModel = route.upstreamModel() == null ? "" : route.upstreamModel().toLowerCase(Locale.ROOT);

    return publicModel.contains("codex")
            || upstreamModel.contains("codex")
            || isGpt56Family(publicModel)
            || isGpt56Family(upstreamModel)
            || isAgentCapableOpenAiModel(publicModel)
            || isAgentCapableOpenAiModel(upstreamModel);
}
```

如果你想更稳，不靠模型名猜，可以在数据库的模型路由表里加一个能力字段，例如：

```text
native_responses_required = true
supports_tools = true
supports_reasoning = true
```

然后优先读数据库能力字段，模型名判断只作为兜底。

## 你的场景建议

你现在是 Codex 桌面端通过中转站使用：

```toml
wire_api = "responses"
model = "gpt-5.6-sol"
```

所以至少这些路由要强制原生 Responses：

- `gpt-5.6-sol`
- 任何你打算给 Codex 用的 `gpt-5.6-*`
- 任何上游模型名或公开模型名里包含 `codex` 的路由
- 任何带 `tools`、`tool_choice`、`previous_response_id`、`function_call_output` 的请求

这些请求都不能进 Chat 兼容转换，不能删除字段，不能重新拼简化 SSE。
