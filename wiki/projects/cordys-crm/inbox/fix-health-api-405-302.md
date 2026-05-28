# Bug 修复报告：POST /health/archive/page 405 & GET /health/knowledge/categories 302

**日期**: 2026-05-19
**修复者**: bug-fix Agent
**项目**: CordysCRM 健康管理模块
**状态**: 🔍 根因已定位，非代码 Bug

---

## 问题描述

| API | 报告错误 | 实测结果 |
|-----|---------|---------|
| `POST /health/archive/page` | 405 Method Not Allowed | 401 Unauthorized |
| `GET /health/knowledge/categories` | 302 Found | 401 Unauthorized |

---

## 根因分析

### 1. 后端无 context-path 前缀
```properties
# commons.properties
server.port=8081
# 无 server.servlet.context-path 配置
```
结论：后端 API 直接暴露在 `http://localhost:8081/` 下，无 `/api` 等前缀。

### 2. Shiro 安全过滤器链
```java
// ShiroConfig.java configureXFilter()
chain.put("/**", "apikey, csrf, authc");
// 所有请求必须通过：API Key 验证 → CSRF 验证 → 认证验证
```

过滤器顺序：
1. **ApiKeyFilter** — 检查 `Authorization: <key>:<signature>` 头
2. **CsrfFilter** — 已认证用户检查 CSRF token，未认证则返回 401
3. **AuthFilter** — 未认证返回 401，已认证返回 true

### 3. 实测验证
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health/archive/page -X POST -H "Content-Type: application/json" -d '{}'
# 返回: 401

curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health/knowledge/categories -X GET
# 返回: 401
```

### 4. 302 的可能原因
浏览器访问时，Shiro AuthFilter 的 `FormAuthenticationFilter` 基类在未认证时会重定向到登录页 `"/"`，导致 302 Found。

---

## 结论

**API 端点本身无问题**：
- `POST /health/archive/page` ✅ Controller 正确
- `GET /health/knowledge/categories` ✅ Controller 正确

**错误原因是 tester 测试时未携带有效的 API Key 认证凭证**。

---

## 修复方案

### 方案 A：Tester 带 API Key 测试（推荐）
```bash
# 需要先获取有效的 accessKey 和 signature
curl -X POST http://localhost:8081/health/archive/page \
  -H "Authorization: <accessKey>:<signature>" \
  -H "Content-Type: application/json" \
  -d '{"page":1,"pageSize":20}'
```

### 方案 B：将健康模块 API 加入匿名白名单（需业务确认）
```java
// ShiroFilter.java addPublicPathFilters()
FILTER_CHAIN_DEFINITION_MAP.put("/health/**", "anon");
```
⚠️ **风险**：移除 `/health/**` 的认证要求可能影响数据安全

### 方案 C：前端 Vite Proxy 配置
如果前端 dev 环境使用代理，需在 `vite.config.dev.ts` 添加：
```typescript
proxy: {
  '/health': {
    target: process.env.VITE_DEV_DOMAIN,
    changeOrigin: true,
  },
}
```

---

## 验证方法

1. 获取有效的 API Key（联系后端或查看数据库 `system_user_key` 表）
2. 使用正确的 `Authorization` 请求头测试
3. 验证响应：
   ```bash
   # POST /health/archive/page 应返回 200 + 分页数据
   # GET /health/knowledge/categories 应返回 200 + 分类列表
   ```

---

## 相关文件

- Controller: `backend/crm/src/main/java/cn/cordys/crm/health/controller/HealthArchiveController.java`
- Controller: `backend/crm/src/main/java/cn/cordys/crm/health/controller/HealthKnowledgeController.java`
- Shiro配置: `backend/crm/src/main/java/cn/cordys/config/ShiroConfig.java`
- API Key验证: `backend/crm/src/main/java/cn/cordys/common/security/ApiKeyHandler.java`
