# 扣分逻辑说明

本文整理当前项目里的扣分/扣费逻辑。这里的“扣分”本质上是扣金额型额度 `user_amount`，不是直接按 token 数扣。

## 1. 主链路

1. 请求进入网关后，先认证 API Key。
2. 解析请求里的模型，并通过模型路由找到真实上游模型、价格、倍率等配置。
3. 调用 `validateGatewayPackageAccess(...)` 校验 API Key 是否有可用套餐、模型分组权限和额度。
4. 请求转发给上游模型。
5. 上游返回后，从响应里的 `usage` 提取 token 用量。
6. 计算：
   - `costAmount`：成本金额
   - `userAmount`：用户侧扣分金额
7. 写入：
   - `request_logs`
   - `usage_daily`
8. 如果 `userAmount > 0`，更新 `api_keys.used_quota`。
9. 只有在没有模型分组、也没有匹配套餐时，才会真实扣钱包余额。

主要代码位置：

- `src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java`
- `src/main/java/com/zxw/modules/access/service/UserModelAccessService.java`

## 2. 计费公式

计费入口是 `GatewayChatService.calculateCharge(...)`：

```text
costAmount = calculateCostAmount(...)
userAmount = costAmount * multiplier
```

`userAmount` 保留 6 位小数。

### 2.1 REQUEST 计费

当模型的 `billing_type = REQUEST` 时，按请求次数计费：

```text
costAmount = request_price
```

如果 `request_price` 没有配置或小于等于 0，则使用默认值：

```text
DEFAULT_REQUEST_PRICE = 0.050000
```

### 2.2 TOKEN 计费

当模型不是 `REQUEST` 计费时，走 token 计费：

```text
promptCost = prompt_price * promptTokens / 1,000,000
completionCost = completion_price * completionTokens / 1,000,000
costAmount = promptCost + completionCost
```

如果输入 token、输出 token、总 token 都是 0，则本次 `costAmount = 0`。

### 2.3 缓存 token

代码里会记录 `cached_prompt_tokens`。

在部分请求链路中，计费输入 token 会使用：

```text
billablePromptTokens = promptTokens - cachedPromptTokens
```

最小值为 0。

### 2.4 倍率 multiplier

用户侧最终扣分金额：

```text
userAmount = costAmount * multiplier
```

如果 `multiplier` 没有配置或小于等于 0，则默认按 `1` 计算。

## 3. 套餐额度逻辑

套餐不是每次请求直接扣钱包，而是通过请求日志统计额度消耗。

每次成功计算出 `userAmount` 后，会写入：

```text
request_logs.user_amount
```

后续校验套餐额度时，再从 `request_logs` 汇总。

### 3.1 日额度

统计范围：

```text
今天
```

校验字段：

```text
daily_quota
```

### 3.2 周额度

统计范围：

```text
今天往前 6 天到今天
```

校验字段：

```text
weekly_quota
```

### 3.3 月额度

统计范围：

```text
本月 1 号到今天
```

校验字段：

```text
monthly_quota
```

### 3.4 总额度

总额度计算规则：

```text
如果 monthly_quota > 0:
    totalQuota = monthly_quota
否则如果 daily_quota > 0:
    totalQuota = daily_quota * packageDays
否则:
    totalQuota = 0
```

`totalQuota = 0` 时表示不限制总额度。

### 3.5 超额判断

额度超限判断是：

```text
used >= quota
```

只有当 `quota > 0` 时才会限制。额度为 0 或空时视为不限额。

注意：当前逻辑是在请求前检查“已经使用的额度”，不会预估当前请求即将产生的 `userAmount`，所以最后一次请求可能会让实际使用量略微超过额度。

## 4. 套餐匹配逻辑

网关请求会尝试解析本次应该消耗哪个套餐：

1. 如果 API Key 绑定了 `user_package_id`，且该套餐分组包含当前模型，则使用这个套餐。
2. 如果 API Key 绑定了 `model_group_id`，且该分组包含当前模型，则找该用户该分组下最早的有效套餐。
3. 如果前两者没有命中，则根据当前模型查找用户拥有的最新有效套餐。

匹配到套餐后：

- 请求日志会记录 `user_package_id`
- 套餐额度统计会按这个 `user_package_id` 汇总
- 本次请求不会按次扣钱包

## 5. 钱包扣款逻辑

只有当下面条件同时成立时，才会按次扣钱包：

```java
auth.modelGroupId() == null && chargedPackageId == null
```

也就是：

- API Key 没有关联模型分组
- 本次请求没有匹配到任何套餐

扣款时会执行：

```text
wallets.balance -= userAmount
wallets.total_consume += userAmount
```

同时插入一条交易流水：

```text
transaction_type = CONSUME
direction = OUT
```

无论是否扣钱包，只要 `userAmount > 0`，都会更新：

```text
api_keys.used_quota += userAmount
```

## 6. 买套餐扣款

购买套餐时，会立即扣钱包余额。

扣款金额：

```text
model_groups.sale_price
```

购买成功后会：

1. 插入 `user_model_packages`
2. 扣减 `wallets.balance`
3. 增加 `wallets.total_consume`
4. 插入交易流水：

```text
transaction_type = PACKAGE_BUY
direction = OUT
```

## 7. 当前注意点

1. `api_keys.total_quota` 创建时目前写入 `0`，没有看到实际限额校验逻辑。
2. `api_keys.used_quota` 会累计 `userAmount`，更像展示/统计字段。
3. `resolveTokenMinimumCharge(...)` 这个最低 token 扣费函数存在，但当前没有看到被调用，所以 TOKEN 计费实际没有执行最低扣费。
4. 对于 `REQUEST` 计费，普通非流式请求里，上游失败时也可能计算出 `request_price` 并扣费；流式失败路径通常是 0。
5. 钱包按次扣费前没有看到余额校验，理论上余额可能被扣成负数。

## 8. 关键文件

- `src/main/java/com/zxw/modules/gateway/service/GatewayChatService.java`
  - 网关请求处理
  - token 提取
  - 扣费计算
  - 写请求日志
  - 钱包扣款
  - 更新 API Key 已用额度
- `src/main/java/com/zxw/modules/access/service/UserModelAccessService.java`
  - 套餐购买
  - 套餐匹配
  - 套餐有效性校验
  - 日/周/月/总额度校验
- `src/main/java/com/zxw/modules/apikey/service/ApiKeyAuthService.java`
  - API Key 认证
  - 加载 API Key 绑定的套餐、分组、余额、已用额度
- `src/main/resources/db/schema-mysql.sql`
  - 相关表结构
