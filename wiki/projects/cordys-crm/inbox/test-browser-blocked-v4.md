# 测试报告 — CordysCRM 健康管理模块浏览器端到端测试（第四轮）

**时间**: 2026-05-19 14:20  
**测试者**: tester Agent  
**项目**: CordysCRM  
**目标**: 登录后测试 /health/archive、/health/examination、/health/ai 三个页面

---

## 测试配置

| 服务 | 状态 | 端口 | PID |
|------|------|------|-----|
| 后端 Java | ✅ 运行中 | 8081 | 57181 |
| 前端 Vite (旧) | ✅ 运行中 | 5173 | 64569 |
| 前端 Vite (新) | ❌ 被 ESLint 错误终止 | — | — |

**修复历史**:
1. `.env.development` — `VITE_DEV_NO_BACKEND=1` 已注释 ✅
2. `.env.development.local` — `VITE_DEV_NO_BACKEND=1` 已注释 ✅
3. Vite 硬重启（kill 61439 → `pnpm --filter web dev`）✅
4. `eslint.config.cjs` simple-import-sort 已修复 ✅

---

## 测试步骤

### Step 1: 登录
- [✅] 清除 localStorage
- [✅] 访问 http://127.0.0.1:5173
- [✅] 输入 admin / CordysCRM
- [✅] 登录成功
- [✅] localStorage 验证：sessionId = `f0daa377-581a-4638-93cd-748588681275`（真实 Shiro sessionId）

### Step 2: Dashboard
- [✅] 仪表板正常渲染
- [✅] 左侧菜单显示「健康管理」

### Step 3: 健康档案页 /health/archive
- [❌] **BLOCKED** — ESLint errors in healthArchiveTable.vue

### Step 4: 体检同步页 /health/examination
- [⏸] 未测试（Step 3 阻塞）

### Step 5: AI 解读页 /health/ai
- [⏸] 未测试（Step 3 阻塞）

---

## 阻塞详情：healthArchiveTable.vue ESLint Errors

```
✖ 13 problems (8 errors, 5 warnings)
  Plugin: vite-plugin-eslint

Files with ESLint errors blocking Vite transform:

1. src/views/health/components/healthArchiveTable.vue
   - simple-import-sort/imports        (line 61)
   - no-use-before-define ×6          (lines 121, 122, 215, 235, 282, 312)
   - no-nested-ternary                (line 297)
   - no-console ×5                    (warnings, non-blocking)

2. src/views/health/components/healthSyncPanel.vue
   - no-use-before-define ×2          (lines 192, 195)
   - no-console ×5                    (warnings, non-blocking)

3. Other health components likely similar pattern
```

**vite-plugin-eslint 配置**: `throwOnError: true`（见 `vite.config.dev.ts`）  
**影响**: ESLint error → transform 失败 → 动态 import 404 → 页面白屏

---

## Dev Mock 解除验证

```javascript
// localStorage 验证（已确认为真实 sessionId）
{
  sessionId: "f0daa377-581a-4638-93cd-748588681275",
  csrfToken: "CYkmuI1mLdlY2AWnqenHEgn4UANqI890+..."
}
// ✅ Dev mock 已解除
```

---

## API 基础验证

```bash
# 后端健康
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8081/health/archive/page
# → 需登录认证，未测试

# 登录 API
curl -s -X POST http://127.0.0.1:8081/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"CordysCRM","platform":"web","authenticate":"LOCAL"}'
# → 200 ✅
```

---

## 修复方案

Frontend-dev 需要修复以下文件的 ESLint errors：

```bash
cd /home/wyhubuntu/project/CordysCRM-main/frontend

# 自动修复 simple-import-sort
pnpm eslint:fix --ext .vue packages/web/src/views/health/components/

# 手动修复 no-use-before-define（函数/变量需在使用前定义）
# 主要问题：setup() 函数中调用了函数但函数定义在后面
# 解决：在 setup() 顶部先定义所有函数，或使用 <script setup> 中 ref 自动提升的特性
```

受影响文件：
- `src/views/health/components/healthArchiveTable.vue`
- `src/views/health/components/healthSyncPanel.vue`
- `src/views/health/components/healthAiPanel.vue`（推测）

---

## 测试结论

| 检查项 | 状态 |
|--------|------|
| Dev mock 解除 | ✅ 通过 |
| 登录流程 | ✅ 通过 |
| Dashboard | ✅ 通过 |
| /health/archive | ❌ 阻塞（ESLint errors） |
| /health/examination | ⏸ 未测 |
| /health/ai | ⏸ 未测 |

**下一步**: 等 frontend-dev 修复 health 组件的 ESLint errors，重启 Vite 后重新测试

---

## 关联报告

- `inbox/test-browser-blocked-v3.md` — Dev mock 根因发现
- `inbox/test-browser-blocked-v2.md` — simple-import-sort 修复
- `inbox/test-browser-blocked.md` — ESLint 覆盖层发现
