# Task-C0c 前端完成报告

**任务**: API 契约对齐（前端改 URL 适配后端 RESTful 风格）
**状态**: ✅ 已完成
**时间**: 2026-05-18

---

## 变更文件清单

### 1. `lib-shared/api/requrls/health.ts`
修正 7 处 URL：
- `HealthSyncUrl`: `/health/sync/trigger` → `/health/sync/sync`
- 新增 `HealthSyncDayUrl`: `/health/sync/sync-day`
- `SaveHealthFollowUrl`: `/health/follow/add+update` → `/health/follow/save`
- `DeleteHealthFollowUrl`: `/health/follow/delete` (body) → `/health/follow/delete/{id}` (path)
- `SaveHealthKnowledgeUrl`: `/health/knowledge/add+update` → `/health/knowledge/save`
- `DeleteHealthKnowledgeUrl`: `/health/knowledge/delete` (body) → `/health/knowledge/delete/{id}` (path)
- 删除：AddHealthFollowUrl、UpdateHealthFollowUrl、AddHealthKnowledgeUrl、UpdateHealthKnowledgeUrl

### 2. `lib-shared/api/modules/health.ts`
- `triggerHealthSync`: body `{customerId, syncType}` → query params `{startDate, endDate}`（格式 YYYY-MM-DD）
- 新增 `triggerHealthSyncDay`: query param `date`
- `deleteFollow/deleteKnowledge`: body → path param `/{id}`
- `addFollow+updateFollow` → `saveFollow`
- `addKnowledge+updateKnowledge` → `saveKnowledge`

### 3. `web/src/api/modules/index.ts`
更新 re-export，导出新函数：`triggerHealthSyncDay`、`saveHealthFollow`、`saveHealthKnowledge`

### 4. `health/components/healthSyncPanel.vue`
- 移除客户选择器（customerOptions）
- 改为日期范围选择器（n-date-picker，type="daterange"）
- `handleSync`: 格式化日期，通过 params 传递 startDate + endDate
- 同步单日模式：使用 `triggerHealthSyncDay`

---

## 后端缺失端点（Task-C5 负责）

| 端点 | 说明 |
|------|------|
| `GET /health/sync/status/{id}` | 同步状态轮询 |

注：`/rule/get/{id}` 和 `/rule/delete/{id}` **后端已存在**，无需新增。

---

## 验证建议

1. 调用 `POST /health/sync/sync?startDate=2026-05-01&endDate=2026-05-18` 验证同步
2. 调用 `POST /health/sync/sync-day?date=2026-05-18` 验证单日同步
3. 随访、知识库 CRUD 操作验证 save/delete 路径参数
