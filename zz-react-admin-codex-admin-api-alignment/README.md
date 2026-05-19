# 桌面版样式设计

这是由 Figma 导出的前端工程，现已升级为可对接真实后端的生产化前端骨架：
- 统一 HTTP 客户端（超时、重试、标准错误、401 自动刷新）
- Access/Refresh Token 会话机制
- React Query 请求状态管理
- Mock API 代理（开发阶段可全量离线联调）

原始设计文件：
<https://www.figma.com/design/Wbz52IozRdihnYjnaAvkwA/%E6%A1%8C%E9%9D%A2%E7%89%88%E6%A0%B7%E5%BC%8F%E8%AE%BE%E8%AE%A1>

## 环境要求

- Node.js: `22.13.0`（见 `.nvmrc`）
- npm: `10+`

## 环境变量

复制 `.env.example` 为 `.env` 并按需修改：

- `VITE_API_BASE_URL`: API 根地址
- `VITE_API_MODE`: `mock` 或 `real`
- `VITE_APP_NAME`: 应用名称

> 默认推荐 `VITE_API_MODE=mock`，便于前后端未联调时先跑通完整流程。

## 本地开发

```bash
npm i
npm run dev
```

## 质量门禁

```bash
npm run typecheck
npm run lint
npm run test
npm run build
```

## 常用脚本

- `npm run dev`: 启动开发服务器
- `npm run build`: 生产构建
- `npm run preview`: 预览构建产物
- `npm run typecheck`: TypeScript 类型检查
- `npm run lint`: ESLint 检查
- `npm run lint:fix`: ESLint 自动修复
- `npm run format`: Prettier 格式化
- `npm run format:check`: Prettier 格式检查
- `npm run test`: 运行 Vitest 测试
- `npm run test:watch`: 监听模式运行测试
- `npm run test:coverage`: 生成覆盖率报告

## 核心目录

- `src/lib/http/*`: 请求客户端、错误模型、请求类型
- `src/app/auth/*`: 会话存储、鉴权上下文、鉴权 API
- `src/app/api/*`: 业务 API 类型与 Query hooks
- `src/mocks/server.ts`: Mock API 代理

## CI

GitHub Actions 已配置 `typecheck + lint + test + build` 流水线：
- 文件：`.github/workflows/ci.yml`

## 改造计划

- 第一周改造清单与文件映射见：`docs/week1-plan.md`
