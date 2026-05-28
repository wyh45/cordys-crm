# P0 前端产出报告

**任务**: Task-C0c (API契约对齐) + Task-C2 (健康菜单+子页面)
**执行者**: frontend-dev
**时间**: 2026-05-18
**状态**: Task-C0c ✅ + Task-C2 ✅ 完成，待审查

---

## 一、Task-C0c — API契约对齐

### 改动文件（3个）

#### 1. `packages/lib-shared/api/requrls/health.ts`

| 前端变量 | 旧URL | 新URL | 说明 |
|---------|-------|-------|------|
| `HealthSyncUrl` | `/health/sync/trigger` | `/health/sync/sync` | ✅ |
| `HealthSyncDayUrl` | _(不存在)_ | `/health/sync/sync-day` | ✅ 新增 |
| `SaveHealthFollowUrl` | _(原Add+Update两个)_ | `/health/follow/save` | ✅ 合并 |
| `DeleteHealthFollowUrl` | body:id | `/health/follow/delete/{id}` (path) | ✅ |
| `SaveHealthKnowledgeUrl` | _(原Add+Update两个)_ | `/health/knowledge/save` | ✅ 合并 |
| `DeleteHealthKnowledgeUrl` | body:id | `/health/knowledge/delete/{id}` (path) | ✅ |
| `GetHealthKnowledgeCategoriesUrl` | _(不存在)_ | `/health/knowledge/categories` | ✅ 新增 |

#### 2. `packages/lib-shared/api/modules/health.ts`

**类型修复**:
- `HealthSyncParams`: `{customerId, syncType}` → `{startDate, endDate}` (query param)
- 新增 `HealthSyncDayParams`: `{date: string}`

**函数修复**:
| 函数 | 旧行为 | 新行为 |
|------|-------|-------|
| `triggerHealthSync()` | `data: HealthSyncParams` | `params: HealthSyncParams` (axios query) |
| `triggerHealthSyncDay()` | _(不存在)_ | `params: {date}` ✅ |
| `addHealthFollow()` | `/health/follow/add` | **删除，合并为** `saveHealthFollow()` |
| `updateHealthFollow()` | `/health/follow/update` | **删除，合并为** `saveHealthFollow()` |
| `deleteHealthFollow()` | `body: {id}` | `path: /delete/{id}` |
| `addHealthKnowledge()` | `/health/knowledge/add` | **删除，合并为** `saveHealthKnowledge()` |
| `updateHealthKnowledge()` | `/health/knowledge/update` | **删除，合并为** `saveHealthKnowledge()` |
| `deleteHealthKnowledge()` | `body: {id}` | `path: /delete/{id}` |
| `getHealthKnowledgeCategories()` | _(不存在)_ | GET `/health/knowledge/categories` ✅ |

**导出修复**: `return` 对象中 `addHealthFollow/updateHealthFollow` → `saveHealthFollow`; `addHealthKnowledge/updateHealthKnowledge` → `saveHealthKnowledge`

#### 3. `packages/web/src/api/modules/index.ts`

重新导出调整后的函数集合（`saveHealthFollow`/`saveHealthKnowledge`/`triggerHealthSyncDay`/`getHealthKnowledgeCategories`）

### API对齐对照表（修正版）

| 端点 | 方法 | 状态 | 前端文件 |
|------|------|------|---------|
| `/health/archive/page` | POST | ✅ 已匹配 | requrls |
| `/health/archive/get/{id}` | GET | ✅ 已匹配 | modules |
| `/health/archive/add` | POST | ✅ 已匹配 | requrls |
| `/health/archive/update` | POST | ✅ 已匹配 | requrls |
| `/health/archive/delete` | POST body | ✅ 已匹配 | modules |
| `/health/sync/sync` | POST query | ✅ 已修复 | requrls+modules |
| `/health/sync/sync-day` | POST query | ✅ 已新增 | requrls+modules |
| `/health/sync/status/{id}` | GET | ⚠️ **后端缺失** — 待 Task-C5 后端补端点 |
| `/health/follow/page` | POST | ✅ 已匹配 | requrls |
| `/health/follow/get/{id}` | GET | ✅ 已匹配 | modules |
| `/health/follow/save` | POST | ✅ 已修复 | modules |
| `/health/follow/delete/{id}` | POST | ✅ 已修复 | modules |
| `/health/follow/mark-contacted/{archiveId}` | POST | ✅ 后端有，前端未调用 | — |
| `/health/follow/by-archive/{archiveId}` | GET | ✅ 后端有，前端未调用 | — |
| `/health/rule/page` | POST | ✅ 已匹配 | modules |
| `/health/rule/get/{id}` | GET | ✅ 已匹配 | modules |
| `/health/rule/save` | POST | ✅ 已匹配 | modules |
| `/health/rule/delete/{id}` | POST | ✅ 已修复 | modules |
| `/health/rule/enable/{id}` | POST | ✅ 已匹配 | modules |
| `/health/rule/disable/{id}` | POST | ✅ 已匹配 | modules |
| `/health/knowledge/page` | POST | ✅ 已匹配 | requrls |
| `/health/knowledge/get/{id}` | GET | ✅ 已匹配 | modules |
| `/health/knowledge/save` | POST | ✅ 已修复 | modules |
| `/health/knowledge/delete/{id}` | POST | ✅ 已修复 | modules |
| `/health/knowledge/categories` | GET | ✅ 已新增 | modules |
| `/health/ai/interpret` | POST | ✅ 后端有，前端未调用 | — |
| `/health/push/send` | POST | ✅ 后端有，前端未调用 | — |
| `/health/push/page` | POST | ✅ 后端有，前端未调用 | — |
| `/health/push/customer/{customerId}` | GET | ✅ 后端有，前端未调用 | — |
| `/health/push/knowledge/{knowledgeId}` | GET | ✅ 后端有，前端未调用 | — |
| `/health/archive/examination/{archiveId}` | GET | ✅ 后端有，前端未调用 | — |
| `/health/archive/allergy/save` | POST | ✅ 后端有，前端未调用 | — |
| `/health/archive/allergy/{archiveId}` | GET | ✅ 后端有，前端未调用 | — |
| `/health/archive/history/save` | POST | ✅ 后端有，前端未调用 | — |
| `/health/archive/history/{archiveId}` | GET | ✅ 后端有，前端未调用 | — |
| `/health/archive/vaccination/save` | POST | ✅ 后端有，前端未调用 | — |
| `/health/archive/vaccination/{archiveId}` | GET | ✅ 后端有，前端未调用 | — |

---

## 二、Task-C2 — 健康管理菜单+子页面

### CordysCRM菜单机制说明

CordysCRM 的前端菜单**从路由配置自动推导**（`router/app-menus/index.ts`），无需手动配置数据库菜单。只需在路由中注册 route + locale key，菜单自动出现。

### 改动文件（5个）

#### 1. `packages/web/src/enums/routeEnum.ts`

```typescript
export enum HealthRouteEnum {
  HEALTH = 'health',
  HEALTH_ARCHIVE = 'healthArchive',     // ✅ 新增
  HEALTH_EXAMINATION = 'healthExamination', // ✅ 新增
  HEALTH_FOLLOW = 'healthFollow',       // ✅ 新增
  HEALTH_KNOWLEDGE = 'healthKnowledge',  // ✅ 新增
  HEALTH_PUSH = 'healthPush',           // ✅ 新增
  HEALTH_RULE = 'healthRule',           // ✅ 新增
  HEALTH_AI = 'healthAi',              // ✅ 新增
}
```

#### 2. `packages/web/src/router/routes/modules/health.ts`

**旧结构**: 单一 `index` 子路由，Tab 布局
**新结构**: 7 个独立子路由，默认 redirect → `/health/archive`

| 路由路径 | 子菜单名称 | 功能 |
|---------|-----------|------|
| `/health/archive` | 健康档案 | 档案列表 + 体检同步 |
| `/health/examination` | 体检管理 | 异常指标统计 |
| `/health/follow` | 随访记录 | 随访列表 + 随访规则 |
| `/health/knowledge` | 健康知识 | 知识库列表 |
| `/health/push` | 健康推送 | 推送面板 |
| `/health/rule` | 随访规则 | 规则列表 |
| `/health/ai` | AI解读 | AI解读（占位页） |

#### 3. 新建 7 个子页面目录和 Vue 文件

| 页面 | 路径 | 说明 |
|------|------|------|
| archive | `views/health/archive/index.vue` | Tab: 档案列表 + 体检同步 |
| examination | `views/health/examination/index.vue` | 占位页，TODO: 接入异常指标统计 |
| follow | `views/health/follow/index.vue` | Tab: 随访列表 + 随访规则 |
| knowledge | `views/health/knowledge/index.vue` | 知识库列表 |
| push | `views/health/push/index.vue` | 推送面板 |
| rule | `views/health/rule/index.vue` | 随访规则 |
| ai | `views/health/ai/index.vue` | AI解读占位页 |

#### 4. `views/health/locale/zh-CN.ts` — 新增 locale key

```typescript
'menu.health': '健康管理',
'menu.healthArchive': '健康档案',
'menu.healthExamination': '体检管理',
'menu.healthFollow': '随访记录',
'menu.healthKnowledge': '健康知识',
'menu.healthPush': '健康推送',
'menu.healthRule': '随访规则',
'menu.healthAi': 'AI解读',
'health.aiPlaceholder': 'AI健康解读功能开发中...',
```

#### 5. `views/health/locale/en-US.ts` — 新增 locale key

同上（中→英）

### 旧 `index.vue` 处理

原 `views/health/index.vue` 保留（Tab布局完整），未来如有需要可废弃。

---

## 三、遗留问题（需后端/其他任务处理）

| 问题 | 位置 | 影响 | 优先级 |
|------|------|------|--------|
| `GET /health/sync/status/{id}` 后端缺失 | Task-C5 | healthSyncPanel 无法轮询状态 | P1 |
| 档案详情页增强（子表字段） | Task-C3 | 过敏史/病史/疫苗需用子表API | P1 |
| healthFollowTable.vue 未使用 `saveHealthFollow` | Task-C3/C9 | 随访表单提交未对接API | P1 |
| healthKnowledgeTable.vue 未使用 `saveHealthKnowledge` | Task-C3/C11 | 知识库表单提交未对接API | P1 |
| healthFollowRuleTable.vue 仍用 mock 数据 | Task-C10 | 需接真实 API | P2 |

---

## 四、文件变更清单

| 文件 | 动作 | 变更内容 |
|------|------|---------|
| `packages/lib-shared/api/requrls/health.ts` | 修改 | URL 常量对齐后端 |
| `packages/lib-shared/api/modules/health.ts` | 修改 | 函数签名 + 新增3个函数 |
| `packages/web/src/api/modules/index.ts` | 修改 | 重导出更新 |
| `packages/web/src/views/health/components/healthSyncPanel.vue` | 修改 | 参数从body改query，UI改为日期选择 |
| `packages/web/src/views/health/locale/zh-CN.ts` | 修改 | 新增12个locale key |
| `packages/web/src/views/health/locale/en-US.ts` | 修改 | 新增12个locale key |
| `packages/web/src/enums/routeEnum.ts` | 修改 | 新增6个HealthRouteEnum |
| `packages/web/src/router/routes/modules/health.ts` | 修改 | 7子路由替换单index路由 |
| `views/health/archive/index.vue` | 新建 | 档案+同步Tab页 |
| `views/health/examination/index.vue` | 新建 | 体检管理占位页 |
| `views/health/follow/index.vue` | 新建 | 随访+规则Tab页 |
| `views/health/knowledge/index.vue` | 新建 | 知识库页 |
| `views/health/push/index.vue` | 新建 | 推送页 |
| `views/health/rule/index.vue` | 新建 | 随访规则页 |
| `views/health/ai/index.vue` | 新建 | AI解读占位页 |
