# 站点信息后端实现说明

## 1. 页面字段

根据你提供的页面图，本次后端支持以下字段：

- 站点名称
- 管理员邮箱
- 站点描述
- 接口基础地址 `Base URL`
- 页脚文字
- 界面主题

---

## 2. 数据库设计

新增数据表：`site_settings`

```sql
CREATE TABLE IF NOT EXISTS site_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    settings_key VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
    site_name VARCHAR(128) NOT NULL,
    admin_email VARCHAR(128) NOT NULL,
    site_description VARCHAR(255) NULL,
    base_url VARCHAR(255) NOT NULL,
    footer_text VARCHAR(255) NULL,
    theme_mode VARCHAR(16) NOT NULL DEFAULT 'LIGHT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_settings_key (settings_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

说明：

- 这是一个单条配置表
- 使用 `settings_key = DEFAULT` 作为唯一配置记录
- 后续如果还要扩展别的站点级配置，也可以继续沿用这张表

---

## 3. 默认数据

系统启动时，如果表里还没有默认记录，会自动创建一条：

```text
site_name        = API Hub 中转站
admin_email      = admin@apihub.io
site_description = 企业级 AI API 中转管理平台
base_url         = https://api.yourdomain.com
footer_text      = Powered by API Hub
theme_mode       = LIGHT
```

这样前端第一次打开页面就能直接获取数据。

---

## 4. 接口列表

### 4.1 获取站点信息

```http
GET /admin/system/site-settings
```

响应示例：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1,
    "siteName": "API Hub 中转站",
    "adminEmail": "admin@apihub.io",
    "siteDescription": "企业级 AI API 中转管理平台",
    "baseUrl": "https://api.yourdomain.com",
    "footerText": "Powered by API Hub",
    "themeMode": "LIGHT",
    "updatedAt": "2026-04-22T21:00:00"
  }
}
```

### 4.2 保存站点信息

```http
PUT /admin/system/site-settings
```

请求体示例：

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

成功响应：

```json
{
  "success": true,
  "message": "站点信息保存成功",
  "data": null
}
```

---

## 5. 参数校验

保存接口已支持以下校验：

- `siteName`：必填，最大 `128` 字符
- `adminEmail`：必填，必须为合法邮箱
- `siteDescription`：可选，最大 `255` 字符
- `baseUrl`：必填，且必须以 `http://` 或 `https://` 开头
- `footerText`：可选，最大 `255` 字符
- `themeMode`：必填，仅允许 `LIGHT` 或 `DARK`

后端还会自动处理：

- 去掉字符串首尾空格
- 去掉 `baseUrl` 末尾多余的 `/`
- 将 `themeMode` 转为大写

---

## 6. 代码位置

- 表结构：`src/main/resources/db/schema-mysql.sql`
- 控制器：`src/main/java/com/zxw/modules/system/controller/AdminSystemController.java`
- 服务类：`src/main/java/com/zxw/modules/system/service/AdminSiteSettingsService.java`
- 请求 DTO：`src/main/java/com/zxw/modules/system/dto/SiteSettingsUpdateRequest.java`
- 响应 DTO：`src/main/java/com/zxw/modules/system/dto/SiteSettingsResponse.java`
- 默认初始化：`src/main/java/com/zxw/config/BootstrapDataInitializer.java`

---

## 7. 前端对接方式

页面初始化：

1. 调用 `GET /admin/system/site-settings`
2. 将接口返回值填充到表单

点击保存：

1. 将表单内容组装成 JSON
2. 调用 `PUT /admin/system/site-settings`
3. 保存成功后提示用户

主题值建议前端直接使用：

- 浅色模式：`LIGHT`
- 深色模式：`DARK`

---

## 8. 当前实现范围

这次只实现了你截图里“站点基本信息”区域的后端，不包含：

- 安全管理
- Webhooks / 回调

如果你继续做下一块，我可以直接把这两个模块的后端也接着补上。
