# 国外 Token 分流设计文档

## 1. 背景

当前网关已经具备上游渠道和 token 池的基础结构：

- `providers`：上游渠道，例如 OpenAI 官方、海外代理、香港节点、新加坡节点。
- `provider_tokens`：某个渠道下的多个 token。
- `models`：平台对外暴露的模型。
- `model_routes`：平台模型到上游渠道和上游模型的映射。

现在的问题是：虽然一个 `provider` 下可以配置多个 token，但当前模型路由查询使用 `limit 1`，并且按 `weight_no desc, t.id asc` 排序，所以实际请求会长期优先命中同一个 token，不能做到真正的请求分流和限流保护。

本文目标是设计一套适合国外 token 的分流方案，让多个海外 token 能够按权重、限额、失败状态自动分摊请求。

## 2. 目标

### 2.1 功能目标

- 支持一个模型绑定一个国外 provider，并在该 provider 下多个 token 间自动分流。
- 支持按 token 权重分流，权重越高承担越多请求。
- 支持按 `rpm_limit` 和 `tpm_limit` 做分钟级保护。
- 支持 token 失败冷却，避免持续打到异常 token。
- 支持上游 `429`、`401`、`5xx` 等状态的差异化处理。
- 支持 Redis 记录实时计数，避免每次请求查询数据库。
- 尽量兼容现有表结构，减少数据库改动。

### 2.2 性能目标

- 路由解析不再每次访问数据库。
- token 选择不阻塞主请求链路。
- Redis 操作控制在少量 key 内，避免额外延迟。
- 高并发下减少某一个 token 被打爆的概率。

### 2.3 非目标

- 本方案不直接处理用户侧 API Key 鉴权缓存，鉴权缓存已单独优化。
- 本方案不负责上游账号余额自动充值。
- 本方案不保证严格意义上的全局精准 token 额度，因为实际 token 消耗通常需要上游响应后才能知道。

## 3. 当前实现分析

当前 `ModelRouteMapper.selectRoutesByModelCode` 逻辑类似：

```sql
select ...
from models m
join model_routes r on r.model_id = m.id and r.status = 'ACTIVE'
join providers p on p.id = r.provider_id and p.deleted = 0 and p.status = 'ACTIVE'
join provider_tokens t on t.provider_id = p.id
    and t.deleted = 0
    and t.status = 'ACTIVE'
    and (r.provider_token_id is null or r.provider_token_id = t.id)
where m.model_code = #{modelCode}
order by r.priority_no asc,
         case when r.provider_token_id = t.id then 0 else 1 end,
         t.weight_no desc,
         t.id asc
limit 1
```

这会带来两个现象：

- 如果 `model_routes.provider_token_id` 有值：固定使用指定 token。
- 如果 `model_routes.provider_token_id` 为空：理论上是 token 池，但因为 `limit 1`，实际上总是选中排序最靠前的 token。

所以当前能力是“优先级选择”，不是“负载分流”。

## 4. 推荐总体方案

推荐采用：

```text
数据库保存静态配置
本地内存缓存路由和 token 候选列表
Redis 保存实时用量、冷却状态和轮询游标
代码使用加权最小负载算法选择 token
```

整体链路：

```text
请求进入
  -> API Key 鉴权
  -> 解析平台模型
  -> 读取路由候选列表
  -> 根据 provider/token 状态选择一个 token
  -> 调用国外上游
  -> 根据响应更新 token 使用量和健康状态
  -> 记录日志、扣费、统计
```

## 5. 配置方式

### 5.1 Provider 配置

建议按上游地域或线路拆分 provider：

| provider_code | 用途 |
| --- | --- |
| `openai_us` | OpenAI 国外官方接口 |
| `openai_hk_proxy` | 香港代理线路 |
| `openai_sg_proxy` | 新加坡代理线路 |

如果只是同一个 base_url 下多个 token，不需要建多个 provider，直接在同一个 provider 下配置多个 token 即可。

### 5.2 Token 配置

`provider_tokens` 中建议这样配置：

| 字段 | 说明 |
| --- | --- |
| `provider_id` | 所属上游渠道 |
| `token_name` | token 名称，例如 `us-token-1` |
| `token_value_encrypted` | 加密后的真实 token |
| `status` | `ACTIVE` 才参与分流 |
| `weight_no` | 分流权重 |
| `rpm_limit` | 每分钟最大请求数，0 表示不限制 |
| `tpm_limit` | 每分钟最大 token 数，0 表示不限制 |
| `current_balance` | 可选，用于展示余额 |

### 5.3 Model Route 配置

需要 token 池分流时：

```text
model_routes.provider_token_id = null
```

需要固定某个 token 时：

```text
model_routes.provider_token_id = 指定 token id
```

这样可以同时支持两种模式：

- 固定 token：适合专线客户、专属 key。
- token 池：适合普通国外请求分流。

## 6. 分流算法

### 6.1 候选 token 过滤

每次选择 token 前，先过滤候选列表：

```text
status = ACTIVE
未处于冷却状态
未超过 rpm_limit
未超过 tpm_limit
```

如果所有 token 都被过滤掉，可以按降级策略处理。

### 6.2 加权最小负载

推荐使用加权最小负载：

```text
score = 当前分钟请求数 / max(weight_no, 1)
选择 score 最低的 token
```

示例：

| token | weight_no | 当前分钟请求数 | score |
| --- | ---: | ---: | ---: |
| token-1 | 100 | 20 | 0.20 |
| token-2 | 50 | 20 | 0.40 |
| token-3 | 100 | 10 | 0.10 |

最终选择 `token-3`。

这种方式比简单轮询更适合高并发，因为它会自动避开当前压力更大的 token。

### 6.3 兜底轮询

如果 Redis 读取失败，可以退化为本地加权轮询：

```text
按 token weight_no 展开或计算游标
选择下一个可用 token
```

兜底逻辑只保证可用，不保证全局精确限流。

## 7. Redis Key 设计

建议 Redis key 统一使用 `gateway:upstream` 前缀。

### 7.1 请求数计数

```text
gateway:upstream:token:rpm:{tokenId}:{yyyyMMddHHmm}
```

值：

```text
当前分钟请求数
```

TTL：

```text
90 秒
```

### 7.2 Token 数计数

```text
gateway:upstream:token:tpm:{tokenId}:{yyyyMMddHHmm}
```

值：

```text
当前分钟 token 消耗数
```

TTL：

```text
90 秒
```

### 7.3 冷却状态

```text
gateway:upstream:token:cooldown:{tokenId}
```

值：

```text
冷却原因，例如 429、5xx、network_error
```

TTL：

```text
429：30 到 120 秒
5xx：10 到 60 秒
网络错误：5 到 30 秒
401：建议不只冷却，应告警或禁用
```

### 7.4 失败次数

```text
gateway:upstream:token:fail:{tokenId}
```

值：

```text
短时间连续失败次数
```

TTL：

```text
5 分钟
```

连续失败次数超过阈值后，可以自动进入更长冷却。

## 8. 上游响应处理策略

| 上游结果 | 建议处理 |
| --- | --- |
| `2xx` | 正常记录 rpm/tpm，清理短失败状态 |
| `401` | token 可能失效，记录告警，建议长冷却或禁用 |
| `429` | token 限流，设置冷却，重试其他 token |
| `5xx` | 上游异常，短冷却，允许切换 token 或 provider |
| 连接超时 | 短冷却，重试其他 token |
| 读超时 | 对非流式请求可重试，流式请求谨慎重试 |

注意：流式请求一旦已经向客户端输出内容，不建议再切换 token 重试，否则会造成客户端收到混合响应。

## 9. 重试策略

推荐配置化：

```yaml
gateway:
  upstream-routing:
    max-token-retries: 2
    retry-on-statuses: 429,500,502,503,504
    token-cooldown-seconds: 30
    fail-cooldown-seconds: 10
```

建议默认：

- 非流式请求：最多换 token 重试 2 次。
- 流式请求：上游响应头未返回前可以换 token；已经开始输出后不重试。
- `401` 不建议自动重试太多次，因为大概率是 token 配置错误。

## 10. 代码改造点

### 10.1 Mapper 改造

新增查询方法：

```text
selectRouteCandidatesByModelCode(modelCode)
```

要求：

- 不再 `limit 1`。
- 返回同一模型下所有可用 route 和 token。
- 保留 `priority_no`、`weight_no`、`rpm_limit`、`tpm_limit`。

### 10.2 RouteDefinition 扩展

当前 `GatewayRouteService.RouteDefinition` 只表示最终选中的一个 token。

建议新增：

```text
RouteCandidate
TokenCandidate
```

或者让 `GatewayRouteService` 返回：

```text
ResolvedRoute {
  model 信息
  provider 信息
  upstream_model
  tokenCandidates
}
```

最终由 token 选择器选出具体 token，再构造成请求使用的 `RouteDefinition`。

### 10.3 新增 Token 选择服务

建议新增：

```text
GatewayUpstreamTokenSelector
```

职责：

- 根据候选 token 列表选择一个 token。
- 读取 Redis rpm/tpm/cooldown。
- 请求开始前预占 rpm。
- 请求完成后补记 tpm。
- 请求失败后设置冷却。

### 10.4 GatewayChatService 调整

需要调整的位置：

- 解析模型路由时，不直接拿第一个 token。
- 调用上游前，先通过 `GatewayUpstreamTokenSelector` 选择 token。
- 上游调用成功后，记录 token 用量。
- 上游调用失败后，通知 selector 做冷却和失败计数。
- 非流式请求支持换 token 重试。

## 11. 数据库是否需要改表

### 11.1 轻量版

轻量版不需要改表，直接复用现有字段：

- `provider_tokens.weight_no`
- `provider_tokens.rpm_limit`
- `provider_tokens.tpm_limit`
- `provider_tokens.status`
- `model_routes.provider_token_id`

这是推荐第一阶段。

### 11.2 增强版

后续如果需要更细分，可以增加字段：

```sql
alter table provider_tokens
    add column region_code varchar(32) null comment '区域，例如 us、hk、sg',
    add column failure_policy varchar(32) not null default 'AUTO_COOLDOWN',
    add column max_consecutive_failures int not null default 5;
```

也可以给 `model_routes` 增加路由组概念：

```sql
alter table model_routes
    add column route_group varchar(64) null comment '路由组，例如 foreign、domestic、vip';
```

不过第一阶段不建议加，先把分流跑通。

## 12. 管理后台展示建议

国外 token 分流上线后，后台 provider 页面建议展示：

- token 当前状态。
- 最近一分钟请求数。
- 最近一分钟 token 数。
- 是否处于冷却。
- 最近失败原因。
- 权重。
- rpm/tpm 限额。

这样排查“为什么某个 token 没有流量”会更直观。

## 13. 上线步骤

建议分三期：

### 第一期：兼容现有表的 token 池分流

- Mapper 返回候选 token 列表。
- 新增 Redis 选择器。
- 支持加权最小负载。
- 支持 rpm 限流。
- 支持 429/5xx 冷却。

### 第二期：补充 tpm 和重试

- 根据上游 usage 补记 tpm。
- 非流式请求支持换 token 重试。
- 流式请求只在响应开始前重试。
- 增加配置项控制重试次数和冷却时间。

### 第三期：后台可观测性

- 后台展示 token 实时负载。
- 展示冷却原因。
- 支持手动禁用异常 token。
- 支持清理冷却状态。

## 14. 风险和注意事项

- Redis 不可用时，应允许降级到本地选择，避免整个网关不可用。
- token 用量中的 tpm 只能在响应后准确知道，请求前只能预估。
- 高并发场景下，Redis 计数需要使用原子递增。
- 流式请求不适合中途换 token 重试。
- 如果多个应用实例部署，本地缓存要控制 TTL，配置变更后需要支持主动清理缓存。
- `401` 应视为配置错误或 token 失效，不建议无限重试。

## 15. 推荐默认参数

```yaml
gateway:
  upstream-routing:
    route-cache-ttl-ms: 300000
    token-state-cache-ttl-ms: 1000
    max-token-retries: 2
    rpm-window-seconds: 60
    redis-counter-ttl-seconds: 90
    default-cooldown-seconds: 30
    rate-limit-cooldown-seconds: 60
    server-error-cooldown-seconds: 15
    network-error-cooldown-seconds: 10
```

## 16. 最终推荐

当前项目最适合先做轻量版：

```text
不改表
model_routes.provider_token_id 为空表示 token 池
Mapper 返回同 provider 下全部 ACTIVE token
Redis 做 rpm/tpm/cooldown
代码按加权最小负载选择 token
```

这样改动范围可控，能直接解决国外 token 被单个打满的问题，也方便后续继续扩展到多 provider、跨区域和后台可观测。
