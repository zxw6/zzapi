# Codex 使用教程

## 1. 安装 Node.js

直接访问 [Node.js 官网](https://nodejs.org/zh-cn) 下载并安装最新 LTS 版本。

安装时一直点击“下一步（Next）”即可完成安装。

## 2. 安装 Codex

打开终端（Terminal），运行：

```shell
npm install -g @openai/codex
```

如果系统提示权限不足，可以使用管理员权限终端后再执行。

## 3. 验证安装

安装完成后，打开终端执行：

```shell
codex --version
```

如果能够正常输出版本号，就说明安装成功。

## 4. 配置文件

进入当前用户目录下的 `.codex` 文件夹中。

Windows 常见路径示例：

```text
C:\Users\你的用户名\.codex
```

如果没有 `.codex` 文件夹，请手动创建该文件夹。

然后在 `.codex` 文件夹中创建以下两个文件：

- `config.toml`
- `auth.json`

如果这两个文件已经存在，就直接修改即可，不需要重复创建。

### 4.1 配置 `auth.json`

将下面内容写入 `auth.json`：

```json
{"OPENAI_API_KEY": "sk-live-your-full-api-key"}
```

说明：

- 请将 `xxx` 替换成你的实际令牌

### 4.2 配置 `config.toml`

将下面内容直接粘贴到 `config.toml` 中即可：

```toml
model_provider = "zxw"
model = "gpt-5.4"
model_reasoning_effort = "high"
disable_response_storage = true
service_tier = "fast"

approval_policy = "on-request"
sandbox_mode = "workspace-write"

[model_providers.zxw]
name = "zxw"
base_url = "http://127.0.0.1:9988/v1"
wire_api = "responses"
requires_openai_auth = true
```

### 4.3 参数说明

`model_reasoning_effort` 可选值：

- `high`
- `medium`
- `low`

分别表示模型思考强度为高、中、低。

请记得根据自己的实际情况替换 `base_url`。

## 5. 启动使用

配置完成后，重新打开终端，直接运行：

```shell
codex
```

如果配置没有问题，Codex 就可以正常启动并连接到你配置的模型服务。

## 6. 常见问题

### 6.1 终端提示找不到 `codex`

说明全局安装没有生效，可以重新执行：

```shell
npm install -g @openai/codex
```

然后重新打开终端再试一次。

### 6.2 `codex --version` 没有输出版本号

请先确认下面两个命令能正常执行：

```shell
node -v
npm -v
```

如果都能输出版本号，再重新安装 Codex。

### 6.3 `.codex` 文件夹不存在

可以手动创建，不影响使用。

只要 `.codex` 目录下最终包含以下两个文件即可：

- `config.toml`
- `auth.json`
