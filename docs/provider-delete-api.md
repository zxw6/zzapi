# 渠道删除接口文档

## 接口信息

- 请求方式：`DELETE`
- 请求路径：`/admin/providers/{id}`
- 接口说明：删除指定渠道
- 权限要求：管理员登录
- 认证方式：请求头携带后台登录 token

```http
Authorization: Bearer <token>
```

## 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `Long` | 是 | 渠道 ID |

## 请求示例

```bash
curl -X DELETE "http://localhost:9988/admin/providers/3" \
  -H "Authorization: Bearer <token>"
```

## 成功返回

删除成功时，HTTP 状态码为 `200`。

```json
{
  "success": true,
  "message": "渠道删除成功",
  "data": null
}
```

## 返回参数说明

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功，成功时为 `true` |
| `message` | `string` | 返回提示，成功时固定为 `渠道删除成功` |
| `data` | `null` | 无返回业务数据，固定为 `null` |

## 失败返回

### 1. 渠道不存在

HTTP 状态码：`404`

```json
{
  "success": false,
  "message": "渠道不存在",
  "data": null
}
```

### 2. 渠道删除失败

HTTP 状态码：`400`

```json
{
  "success": false,
  "message": "渠道删除失败",
  "data": null
}
```

### 3. 未登录或 token 无效

HTTP 状态码通常为 `401`

```json
{
  "success": false,
  "message": "请先登录",
  "data": null
}
```

### 4. 非管理员无权限

HTTP 状态码通常为 `403`

```json
{
  "success": false,
  "message": "Admin only",
  "data": null
}
```

## 实际删除行为

这个接口当前是“逻辑删除 + 关联清理”，不是单纯物理删一条渠道记录：

1. 删除该渠道关联的模型路由记录：`model_routes`
2. 逻辑删除该渠道下的令牌记录：`provider_tokens`
3. 逻辑删除渠道主记录：`providers`

其中：

- `providers` 会更新为：
  - `status = DISABLED`
  - `deleted = 1`
- `provider_tokens` 会更新为：
  - `status = DISABLED`
  - `deleted = 1`
- `model_routes` 会按 `provider_id` 直接删除

## 代码位置

- Controller: [AdminProviderController.java](E:/project-ai/Test/src/main/java/com/zxw/modules/provider/controller/AdminProviderController.java)
- Service: [AdminProviderService.java](E:/project-ai/Test/src/main/java/com/zxw/modules/provider/service/AdminProviderService.java)
- Provider Mapper: [ProviderMapper.java](E:/project-ai/Test/src/main/java/com/zxw/persistence/mapper/ProviderMapper.java)
- Provider Token Mapper: [ProviderTokenMapper.java](E:/project-ai/Test/src/main/java/com/zxw/persistence/mapper/ProviderTokenMapper.java)
- Model Route Mapper: [ModelRouteMapper.java](E:/project-ai/Test/src/main/java/com/zxw/persistence/mapper/ModelRouteMapper.java)
