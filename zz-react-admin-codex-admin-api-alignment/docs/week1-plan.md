# 第一周改造清单（可落地）

## 目标
在不重做 UI 的前提下，把当前项目从“可演示原型”提升到“可持续迭代的工程基线”。

## 周计划（D1-D5）

### D1 工程基座
- 增加 TypeScript 配置（严格类型检查）
- 增加 ESLint + Prettier + EditorConfig
- 增加 `.gitignore`、`.nvmrc`
- 验收：`npm run typecheck && npm run lint && npm run format:check` 可执行

### D2 鉴权闭环（最小可用）
- 增加前端登录态存储与工具函数
- 增加路由守卫：未登录不可访问 `/console/*`
- 登录页提交后落登录态；退出登录清理登录态
- 验收：直接访问 `/console/overview` 会跳回登录页

### D3 测试基线
- 引入 Vitest + Testing Library + jsdom
- 增加 `test` 脚本和 `setup`
- 增加关键路径单测（鉴权/路由守卫）
- 验收：`npm run test` 通过

### D4 CI 门禁
- 增加 GitHub Actions：install、typecheck、lint、test、build
- 验收：PR 自动执行质量门禁

### D5 清理与收敛
- 清理明显未接入页面/依赖（先标记、后逐步删除）
- 补 README：开发、测试、发布前检查
- 验收：文档与脚本一致，团队新人可 10 分钟内跑通

## 文件改动映射（本周）
- `package.json`：新增质量脚本与测试脚本
- `tsconfig.json` / `tsconfig.node.json`：类型检查
- `eslint.config.mjs`：静态检查规则
- `.prettierrc.json` / `.prettierignore` / `.editorconfig`：格式规范
- `.gitignore` / `.nvmrc`：环境一致性
- `vitest.config.ts` / `src/test/setup.ts`：测试基座
- `src/app/auth/auth.ts`：登录态领域工具
- `src/app/routes/guards.tsx` / `src/app/routes.tsx`：路由守卫
- `src/app/pages/user/UserLogin.tsx`：登录写入登录态
- `src/app/components/user/UserLayout.tsx`：退出清理登录态
- `.github/workflows/ci.yml`：CI 门禁
- `README.md`：开发与质量命令说明
