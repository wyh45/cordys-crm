# CordysCRM — Project Context

## Project Purpose
本项目唯一宗旨：**通过健康管理功能（健康档案、体检同步、AI解读、随访推送）促使客户再到体检中心消费复查**。所有功能设计都应服务于引导客户回访体检中心这一核心目标。

## Tech Stack
- **Frontend**: Vue 3 (Composition API, `<script setup>`), Naive UI, TypeScript, Vite 7, pnpm monorepo
- **Backend**: Java 21, Spring Boot 3.5.11, MyBatis (custom BaseMapper), Apache Shiro, Flyway
- **Database**: MySQL 9.6 (MariaDB), JDBC `root:wyh123@127.0.0.1:3306/cordys_crm`
- **AI**: Dify API at `http://220.163.111.166/v1`, key `app-05l01mm5RV8dfxUJdIxY6ALG`

## Key Paths
```
frontend/packages/lib-shared/api/modules/health.ts   — API 函数 + 类型定义（共享层）
frontend/packages/lib-shared/api/requrls/health.ts    — URL 常量
frontend/packages/web/src/api/modules/index.ts        — Web 层导出（必须手动加新函数）
frontend/packages/web/src/router/routes/modules/health.ts — 路由定义
frontend/packages/web/src/enums/routeEnum.ts          — 路由枚举
frontend/packages/web/src/views/health/               — 页面 + 组件
frontend/packages/web/src/views/health/locale/        — i18n (zh-CN.ts, en-US.ts)
backend/crm/src/main/java/cn/cordys/crm/health/       — 后端健康模块
backend/crm/src/main/resources/migration/             — Flyway 迁移
```

## Critical: BaseMapper Behavior
- `BaseMapper.insert(entity)` 生成 INSERT 包含 **ALL entity fields**，不只是非 null 字段
- `BaseMapper.updateById(entity)` 生成 UPDATE SET **ALL fields**
- **给 entity 加字段但没有 DB column = SQL error "Unknown column"**
- **解决方案**：先创建 Flyway migration 加列 → 再加 entity field → rebuild → restart

## Critical: Flyway Migrations
- DB schema version: **9.0.0.1** (run `grep "Current version" backend log` to verify)
- Migration files ONLY picked up if version > current schema version
- Path: `backend/crm/src/main/resources/migration/<version>/ddl/V<version>_N__desc.sql`
- Example: `migration/9.0.1/ddl/V9.0.1_1__health_lifestyle.sql`
- `spring.flyway.baseline-on-migrate=true`, `spring.flyway.locations=classpath:migration`
- 旧 migration 目录（1.5.0, 1.6.1 等）中的文件不会被 Flyway 执行（版本号低于 9.0.0.1）

## Frontend Architecture
- **API 三层结构**: lib-shared 定义 → web index.ts 重新导出 → 组件 import from `@/api/modules`
- **新 API 函数必须同时加到**：lib-shared/api/modules/health.ts（函数定义 + 返回对象）+ web/api/modules/index.ts（export 解构）
- **路由枚举**：修改 routes/health.ts 后同步更新 enums/routeEnum.ts
- **i18n**：zh-CN.ts 和 en-US.ts 必须同时更新，key 不能漏
- CrmTable 列定义用 `dataKey` 不是 `key`
- CrmFormCreateDrawer 依赖 form-design 后台配置，如果 FormDesignKey 对应的配置为空则抽屉空白

## Backend Architecture
- Controller `@RequestMapping("/health/archive")` + method `@PostMapping("/page")` = `POST /health/archive/page`
- Service 用 `@Resource private BaseMapper<Entity> mapper` 注入，不写 XML
- 分页用 PageHelper：`PageHelper.startPage(page, size)` → `mapper.select(criteria)` → 返回 `Page<Entity>`
- 删除用 `@PostMapping("/delete")` + `@RequestBody Map<String, String> params` 取 `params.get("id")`
- 子资源路径：`/health/archive/allergy/save`、`/health/archive/allergy/{archiveId}` 等

## Known Data Issues
- **Gender 字段**：DB 存的是 "男"/"女"（中文），前端表单发 MALE/FEMALE。编辑加载和详情展示都需要兼容两种值
- **HealthArchive 表字段**：customerName, gender, age, bloodType, height, weight, phone, idcardNo, archiveNo, bloodPressure, heartRate, riskScore, smoking, drinking, sleepQuality, exercise, diet, bloodSugar

## Run Commands
```bash
# Backend rebuild + start
cd /home/wyhubuntu/project/CordysCRM-main
./mvnw -pl backend/app -am package -DskipTests -q
kill $(pgrep -f "app-main.jar") 2>/dev/null
java -jar backend/app/target/app-main.jar --server.port=8081 > /tmp/backend.log 2>&1 &

# Frontend dev server
cd /home/wyhubuntu/project/CordysCRM-main/frontend
pnpm --filter @cordys/web run dev

# Check health
curl -s http://localhost:8081/ && echo "backend OK"
curl -s http://localhost:5173 | head -c 100  # frontend OK
```

## Deleted/Removed Features
- 路由 `push` 和 `rule` 已删除，只保留 5 个子路由：archive, examination, follow, knowledge, ai
- `healthPushPanel.vue`、`healthExamAbnormal.vue` 已删除

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
