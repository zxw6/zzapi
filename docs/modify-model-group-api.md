# 修改套餐接口文档

## 接口信息

- 接口名称：修改套餐分组
- 请求方式：`PUT`
- 请求地址：`/admin/model-access/groups/{groupId}`
- 接口说明：管理员修改指定套餐分组的编码、名称、价格、额度、有效期和备注信息

## 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| groupId | Long | 是 | 套餐分组 ID |

## 请求体

```json
{
  "groupCode": "gpt-5-5",
  "groupName": "GPT 5.5 套餐",
  "salePrice": 15.00,
  "packageDays": 30,
  "dailyQuota": 60.00,
  "weeklyQuota": 420.00,
  "monthlyQuota": 1800.00,
  "remark": "管理员修改后的套餐说明"
}
```

## 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| groupCode | String | 是 | 套餐分组编码，不能为空，不能重复 |
| groupName | String | 是 | 套餐分组名称 |
| salePrice | BigDecimal | 是 | 套餐售价，不能小于 0 |
| packageDays | Integer | 是 | 套餐有效天数 |
| dailyQuota | BigDecimal | 是 | 日额度，不能小于 0 |
| weeklyQuota | BigDecimal | 是 | 周额度，不能小于 0 |
| monthlyQuota | BigDecimal | 是 | 月额度，不能小于 0 |
| remark | String | 否 | 备注说明 |

## 成功响应

```json
{
  "success": true,
  "message": "套餐修改成功",
  "data": {
    "packageRestrictionEnabled": true,
    "packageStatus": "ACTIVE",
    "packageStatusText": "使用中",
    "activeGroupId": 1,
    "activeGroupCode": "gpt-5-5",
    "activeGroupName": "GPT 5.5 套餐",
    "packagePrice": 15.00,
    "dailyQuota": 60.00,
    "weeklyQuota": 420.00,
    "monthlyQuota": 1800.00,
    "dailyUsed": 0.00,
    "weeklyUsed": 0.00,
    "monthlyUsed": 0.00,
    "expiresAt": "2026-05-29T10:00:00",
    "remainingDays": 30,
    "groups": [
      {
        "id": 1,
        "groupCode": "gpt-5-5",
        "groupName": "GPT 5.5 套餐",
        "salePrice": 15.00,
        "packageDays": 30,
        "dailyQuota": 60.00,
        "weeklyQuota": 420.00,
        "monthlyQuota": 1800.00,
        "modelCount": 12,
        "purchased": true,
        "active": true,
        "expiresAt": "2026-05-29T10:00:00",
        "remainingDays": 30,
        "dailyUsed": 0.00,
        "weeklyUsed": 0.00,
        "monthlyUsed": 0.00,
        "packageStatus": "ACTIVE",
        "packageStatusText": "使用中",
        "remark": "管理员修改后的套餐说明",
        "systemPreset": false
      }
    ]
  }
}
```

## 最外层返回参数

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| success | boolean | 是否成功 |
| message | String | 返回消息 |
| data | ModelAccessSummaryResponse | 最新套餐概览数据 |

## data 返回参数说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| packageRestrictionEnabled | boolean | 是否开启套餐限制 |
| packageStatus | String | 当前套餐状态 |
| packageStatusText | String | 当前套餐状态说明 |
| activeGroupId | Long | 当前激活套餐分组 ID |
| activeGroupCode | String | 当前激活套餐分组编码 |
| activeGroupName | String | 当前激活套餐分组名称 |
| packagePrice | BigDecimal | 当前套餐价格 |
| dailyQuota | BigDecimal | 日额度 |
| weeklyQuota | BigDecimal | 周额度 |
| monthlyQuota | BigDecimal | 月额度 |
| dailyUsed | BigDecimal | 今日已用额度 |
| weeklyUsed | BigDecimal | 本周已用额度 |
| monthlyUsed | BigDecimal | 本月已用额度 |
| expiresAt | LocalDateTime | 到期时间 |
| remainingDays | Long | 剩余天数 |
| groups | List<ModelGroupOptionResponse> | 当前账号可见的套餐分组列表 |

## groups 数组元素说明

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 套餐分组 ID |
| groupCode | String | 套餐分组编码 |
| groupName | String | 套餐分组名称 |
| salePrice | BigDecimal | 售价 |
| packageDays | Integer | 有效天数 |
| dailyQuota | BigDecimal | 日额度 |
| weeklyQuota | BigDecimal | 周额度 |
| monthlyQuota | BigDecimal | 月额度 |
| modelCount | Integer | 套餐下模型数量 |
| purchased | boolean | 当前账号是否已购买 |
| active | boolean | 当前账号是否正在使用中 |
| expiresAt | LocalDateTime | 到期时间 |
| remainingDays | Long | 剩余天数 |
| dailyUsed | BigDecimal | 今日已用额度 |
| weeklyUsed | BigDecimal | 本周已用额度 |
| monthlyUsed | BigDecimal | 本月已用额度 |
| packageStatus | String | 套餐状态 |
| packageStatusText | String | 套餐状态说明 |
| remark | String | 备注 |
| systemPreset | boolean | 是否系统预置套餐 |

## 失败场景

| 场景 | 说明 |
| --- | --- |
| 套餐分组不存在 | `groupId` 对应数据不存在 |
| 套餐编码为空 | `groupCode` 为空或格式化后为空 |
| 套餐编码重复 | 与其他套餐分组编码重复 |
| 系统预置套餐编码被修改 | 系统默认套餐不允许改成其他编码 |
| 非管理员调用 | 无权限修改套餐 |

## 备注

- 该接口修改的是套餐模板配置
- 不会直接改写历史购买记录快照
- 修改成功后，前端可以直接使用返回的 `data` 刷新套餐页面
