# CordysCRM 健康模块 — Review 响应 + tasks.md 修正报告

**响应者**: feature-evaluator
**日期**: 2026-05-18
**审查文件**: `wiki/projects/cordys-crm/inbox/review-cordyscrm-plan.md`

---

## 一、Review 指出的 3 个严重问题 → 全部确认并修正

| # | 问题 | 修正动作 |
|---|------|----------|
| 1 | **gap analysis 严重低估前端进度** | ✅ 已重写 gap analysis，新增「一、现有代码真实状态」节，列出所有 9+ 前端文件及状态 |
| 2 | **遗漏 Task-C0 API 契约对齐** | ✅ 已将 Task-C0 设为最高 P0，Task-C0c 详细列出 7 个 URL 映射规则 + 3 个缺失端点补充 |
| 3 | **未登记 HealthSyncService NPE Bug** | ✅ 已将 Task-C0a 设为最高优先级，精确到 line 171 和 line 350 的修复代码 |

## 二、tasks.md 关键修正

### 修正前（错误）
```
已有 | 前端菜单 | ❌ 缺失
已有 | 前端框架 | ❌ 缺失
已有 | 档案管理页面 | ❌ 缺失（有API但无独立管理页面）
```

### 修正后（真实状态）
```
前端（9+ 个文件）⚠️ 代码质量高，但 API 契约全部不匹配
├── 路由: router/routes/modules/health.ts (32行) ✅
├── 入口页面: views/health/index.vue (56行) ✅ Tab布局
├── 6个Vue组件: syncPanel/archiveTable/followTable/knowledgeTable/pushPanel/followRuleTable ✅
├── API层: lib-shared/api/modules/health.ts (375行) ❌ URL全不匹配
└── 国际化: locale/zh-CN.ts (89行) ✅ i18n key完整
```

### 新增：API 契约不匹配详情（第二节）
完整表格列出 9 个 URL 中 7 个不匹配的原因（后端 `/save` 合用、前端 `trigger` vs 后端 `sync`、path param vs body param）

### 新增：完整任务清单（P0→P1→P2→P3）
- **Task-C0a**: 修复 NPE Bug（最高优先级）
- **Task-C0b**: 创建 requirements/ 目录
- **Task-C0c**: API 契约对齐（7个URL修改 + 3个缺失端点补充）
- **Task-C0d**: 输出 DDL SQL 文件
- **Task-C1~C15**: 原有任务重新排 P0/P1/P2/P3

## 三、context.md 也需更新

tasks.md 审查已同步影响 context.md 的评估，需要更新：
- "前端框架: ❌ 缺失" → "前端框架: ⚠️ 已有 9+ 文件，但 API 契约未对齐"
- "前端页面: ❌ 缺失" → "前端页面: ⚠️ 已有 Tab 入口 + 6 个组件，但字段定义不匹配"

## 四、decisions.md 新增 2 条决策

| 决策 | 内容 |
|------|------|
| 决策 14 | **API 契约对齐策略**：前端改 URL 适配后端（后端 RESTful 更优），不改造后端 |
| 决策 15 | **前端类型修正**：HealthFollowRule.id 从 `number` 改为 `string`（与后端雪花 ID 对齐） |

---

*reviewer 审查已处理完毕，tasks.md 已按审查意见全面重写*