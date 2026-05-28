# 测试报告 — 浏览器端到端测试（阻塞）

**任务**: Task-C3/C4/C7 — 健康管理模块 P1 功能测试
**时间**: 2026-05-19 12:05
**测试方式**: 浏览器（绕过 Shiro session 认证问题）
**状态**: ❌ **测试失败 — ESLint 错误覆盖层阻塞所有 UI 交互**

---

## 🔴 测试失败 — ESLint 错误覆盖层

### 环境
- 前端: Vite dev server (pnpm) on port 5173
- 后端: Spring Boot on port 8081
- 测试账号: admin / CordysCRM

### 复现步骤
1. 打开浏览器 → http://127.0.0.1:5173
2. 页面加载 → 显示登录表单（账号密码输入框）
3. 输入 admin / CordysCRM
4. 点击登录按钮 → **ESLint 错误覆盖层覆盖整个页面**
5. 任何 UI 交互（点击/输入/导航）均被遮挡

### 预期结果
登录成功 → 跳转 Dashboard → 可访问 /health/archive 等页面

### 实际结果
ESLint 错误覆盖层遮住所有内容，登录按钮可见但点击后错误层仍然存在，无法进入任何页面。

### 错误详情

#### Browser Console 错误
```
[Vue Router warn]: uncaught error during route navigation:
TypeError: Failed to fetch dynamically imported module:
  http://127.0.0.1:5173/src/layout/default-layout.vue

[vite-plugin-eslint]: Error: Failed to load config "eslint-plugin-vue" to extend from.
```

#### ESLint 根因分析
- **ESLint v9** (9.39.4) 要求配置文件 `eslint.config.js`
- 项目只有 `.eslintrc-auto-import.json`（ESLint v8 格式）
- `vite-plugin-eslint` 尝试加载配置失败 → 抛出错误 → Vite 在页面渲染错误覆盖层
- 错误覆盖层遮住所有页面内容，无法交互

#### ESLint CLI 验证
```bash
$ ./node_modules/.bin/eslint --version
ESLint: 9.39.4

$ ./node_modules/.bin/eslint packages/web/src/hooks/useFormCreateTable.ts
ESLint couldn't find an eslint.config.(js|mjs|cjs) file.
```

### 影响范围
- **所有前端页面** — ESLint 错误覆盖层影响全站，无法正常使用
- Task-C3（健康档案页面）— 无法测试
- Task-C4（体检同步页面）— 无法测试
- Task-C7（AI 解读页面）— 无法测试

### 临时绕过尝试
| 方法 | 结果 |
|------|------|
| 按 Escape 关闭覆盖层 | ❌ 无效 |
| F5 刷新页面 | ❌ 错误覆盖层重现 |
| 直接导航到 /health/archive URL | ❌ 错误覆盖层仍存在 |
| 隐藏 error-overlay CSS | ❌ 需要 DevTools 操作，测试流程不可行 |

### 修复建议（frontend-dev）
1. **方案 A**: 降级 ESLint 到 v8（`pnpm add -D eslint@^8`）
2. **方案 B**: 创建 `eslint.config.js` 适配 ESLint v9（推荐，参考 https://eslint.org/docs/latest/use/configure/migration-guide）
3. **方案 C**: 临时禁用 vite-plugin-eslint（`vite.config.ts` 中注释掉 plugin）

---

## ✅ API 层验证（独立测试，已通过）

虽然浏览器测试失败，但通过 curl 验证了后端 API 链路：

| API | 方法 | 结果 | 说明 |
|-----|------|------|------|
| `/login` | POST | ✅ 200 | admin/CordysCRM 登录成功，返回 sessionId |
| `/health/sync/sync` | POST | ✅ 200 | 同步触发成功，返回 syncId |
| `/health/sync/status/{id}` | GET | ✅ 200 | syncId 字段正确，enum 大写 |
| `/health/archive/page` | POST | ✅ 200 | 档案分页查询正常 |
| `/health/knowledge/categories` | GET | ✅ 200 | 知识库分类正常 |

详见: `inbox/test-c5-p1.md`

---

## 下一步

| 阻塞项 | 负责方 | 状态 |
|--------|--------|------|
| ESLint 配置修复 | frontend-dev | ⏳ 待修复 |
| Task-C3 功能测试 | tester | ⏳ 等待 ESLint 修复 |
| Task-C4 功能测试 | tester | ⏳ 等待 ESLint 修复 |
| Task-C7 功能测试 | tester | ⏳ 等待 ESLint 修复 |

---

**结论**: 后端 API 链路正常，前端代码质量良好（vue-tsc 已通过），但 ESLint v9 配置缺失导致 Vite 开发服务器渲染错误覆盖层，全站无法使用。修复 ESLint 配置后可继续功能测试。
