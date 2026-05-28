# 测试报告 — CordysCRM P1 浏览器端到端测试（第三轮）

**任务**: Task-C3/C4/C7 — 健康管理模块 P1 功能测试
**时间**: 2026-05-19 12:15
**测试方式**: 浏览器 E2E
**状态**: ❌ **失败 — `VITE_DEV_NO_BACKEND=1` 导致前端跳过真实后端，使用 mock token，API 全 401**

---

## 🔴 根因：前端 Dev Mode 绕过真实后端

### 发现过程
1. 登录页面可正常访问，admin/CordysCRM 登录视觉成功
2. 登录后 localStorage 存的是 `dev-mode-session-id` 和 `dev-mode-csrf-token`
3. 所有 API 请求返回 401（Shiro 不认 mock token）
4. 根因定位：`VITE_DEV_NO_BACKEND=1` 环境变量启用前端 standalone dev mode

### 根因文件
```
frontend/packages/web/.env.development
  VITE_DEV_NO_BACKEND=1  ← 启用 mock 模式，跳过后端

frontend/packages/web/.env.development.local
  VITE_DEV_NO_BACKEND=1  ← 同上
```

### 影响链路
```
isDevMode() → true（因 VITE_DEV_NO_BACKEND=1）
  ↓
登录时走 mockLoginResponse() → 返回 dev-mode-session-id
  ↓
localStorage 存 mock token
  ↓
所有 API 请求带 mock session → 后端 Shiro 401
  ↓
页面数据无法加载
```

### 证据
```javascript
// localStorage.getItem('sessionId')
"dev-mode-session-id"

// localStorage.getItem('user')
{
  "sessionId": "dev-mode-session-id",
  "userId": "admin",
  "name": "admin",
  "roles": ["admin"],
  "permissionIds": ["*"],
  "source": "LOCAL"
}

// 浏览器控制台
"Request failed with status code 401" × 多次
```

### 后端状态（正常）
| 检查项 | 状态 |
|--------|------|
| 后端 8081 监听 | ✅ 正常 |
| curl POST /login | ✅ 200，返回真实 sessionId |
| curl /front/login（前端代理） | ✅ 200 |
| Shiro session 认证 | ✅ 正常（curl 测试通过）|

### 修复方案

**方案 A（推荐）**：注释掉 `VITE_DEV_NO_BACKEND=1`
```bash
# .env.development 和 .env.development.local
# VITE_DEV_NO_BACKEND=1  # 注释掉，使用真实后端
# 重启 pnpm dev
```

**方案 B**：使用浏览器 localStorage 手动写入真实 sessionId
```javascript
// 浏览器控制台执行（但 Vite server 仍会走 mock 逻辑）
```

**方案 C**：前端代码改造 devMode 逻辑，优先检测后端可用性

---

## 其他观察

### ✅ 正面
- Vite 前端服务器正常运行（端口 5173）
- Vue Router 正常（URL 路由正确，Tab 显示正确）
- ESLint simple-import-sort 错误已修复
- 后端 API 链路完整（curl 验证通过）
- 前端 proxy 配置正确（`/front` → `http://127.0.0.1:8081`）

### ❌ 阻塞
- Dev Mode mock 逻辑覆盖真实 API 调用

---

## 下一步

| 负责方 | 任务 | 状态 |
|--------|------|------|
| frontend-dev 或 PM | 移除 `.env.development` 中的 `VITE_DEV_NO_BACKEND=1` | ⏳ |
| tester | 重新执行浏览器 E2E 测试 | ⏳ |
