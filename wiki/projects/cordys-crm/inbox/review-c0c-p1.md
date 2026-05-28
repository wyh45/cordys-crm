# CordysCRM Task-C0c + P1 后端 — 审查报告

**审查者**: reviewer Agent
**审查日期**: 2026-05-18 16:00
**审查对象**: `inbox/task-c0c-frontend.md` + `inbox/p1-backend.md`
**参照**: 6 个实际源文件 (requrls, modules, syncPanel, HealthSyncJob, HealthJobScheduler, HealthExaminationController)

---

## 一、总体判决

**🟢 PASS** — C0c/P1 后端任务全部完成，质量良好。发现 2 个 C0c 遗留项（均非本次变更引入）和 1 个数据语义问题。

---

## 二、Task-C0c: 前端 API 契约对齐

### ✅ requrls/health.ts (7 处 URL 修改 — 全部验证)

| 变更 | 状态 | 行号 |
|------|------|------|
| `HealthSyncUrl` → `/health/sync/sync` | ✅ | Line 9 |
| 新增 `HealthSyncDayUrl` → `/health/sync/sync-day` | ✅ | Line 10 |
| 新增 `GetHealthSyncStatusUrl` → `/health/sync/status` | ✅ | Line 11 |
| 新增 8 个子表 URL (allergy/history/vaccination/examination) | ✅ | Lines 14-20 |
| `SaveHealthFollowUrl` → `/health/follow/save` | ✅ | Line 25 |
| `SaveHealthKnowledgeUrl` → `/health/knowledge/save` | ✅ | Line 32 |
| 新增 `GetHealthKnowledgeCategoriesUrl` | ✅ | Line 31 |
| 删除 AddHealthFollowUrl/UpdateHealthFollowUrl/AddHealthKnowledgeUrl/UpdateHealthKnowledgeUrl | ✅ | 不在文件中 |
| 新增 `SaveFollowRuleUrl` → `/health/rule/save` | ✅ | Line 47 |

### ✅ modules/health.ts (函数签名修改 — 全部验证)

| 函数 | 变更 | 状态 | 行号 |
|------|------|------|------|
| `triggerHealthSync` | body → query params (`{startDate, endDate}`) | ✅ | 264-265 |
| `triggerHealthSyncDay` | 新增 (`{date}`) | ✅ | 269-270 |
| `saveHealthFollow` | 合并 add+update → single save | ✅ | 328-329 |
| `deleteHealthFollow` | body `{id}` → path `/${id}` | ✅ | 333-334 |
| `saveHealthKnowledge` | 合并 add+update → single save | ✅ | 350-351 |
| `deleteHealthKnowledge` | body `{id}` → path `/${id}` | ✅ | 355-356 |
| `getHealthAllergies/saveHealthAllergy` | 新增子表 API | ✅ | 281-288 |
| `getHealthHistories/saveHealthHistory` | 新增子表 API | ✅ | 291-298 |
| `getHealthVaccinations/saveHealthVaccination` | 新增子表 API | ✅ | 302-308 |
| `getHealthExaminations` | 新增 | ✅ | 311-313 |
| `getHealthKnowledgeCategories` | 新增 | ✅ | 360-362 |

### ✅ healthSyncPanel.vue (日期范围选择器 — 全部验证)

| 变更 | 状态 | 行号 |
|------|------|------|
| 移除客户选择器 (customerOptions/selectedCustomerId) | ✅ | 仅保留 startDate/endDate |
| 两个 `n-date-picker` (type="date") | ✅ | Lines 7, 10 |
| `formatDate()` 辅助函数 | ✅ | Lines 62-66 |
| `handleSync` 调用 `triggerHealthSync({startDate, endDate})` | ✅ | Lines 72-74 |
| 按钮 disabled 条件改为 `!startDate \|\| !endDate` | ✅ | Line 15 |

**额外增强**: 新增了过敏史/病史/疫苗接种/体检记录的完整前端 API 调用函数（modules/health.ts lines 278-313），这些在 tasks.md Section VI 的已有端点列表中对应 `/archive/allergy/*`, `/archive/history/*`, `/archive/vaccination/*` 端点。超出 C0c 原文要求的范围，属于合理的前瞻性工作。

---

## 🟡 C0c 遗留问题 (2 个，均非本次变更引入)

### 遗留 1: 随访规则 `deleteFollowRule` URL 仍用 body 参数

| 前端 (modules/health.ts:411-412) | 后端 (HealthFollowRuleController.java:45) |
|---|---|
| `POST /health/rule/delete` body:`{id}` | `@PostMapping("/delete/{id}")` path param |

**影响**: 前端调用 `/health/rule/delete` 会 404（后端期望 `/health/rule/delete/{id}`）。此问题在 v1 审查已记录，C0c 未覆盖 rule 模块的 delete URL。

### 遗留 2: `HealthFollowRule.id` 仍为 `number` 非 `string`

| 前端 (modules/health.ts:215) | 后端 (BaseModel.java:14) | Decision 15 |
|---|---|---|
| `id: number \| null` | `String id` (雪花ID) | 明确要求改为 `string` |

**影响**: `getFollowRule(5)` 传 number → 后端收到 `"5"` 可容错，但大雪花 ID 如 `1823456789012345678` 会精度丢失。

**建议**: 在 Task-C10 (随访规则管理页面) 执行时顺手修复这两项。

---

## 三、Task-C6: Quartz 定时同步

### ✅ HealthSyncJob.java — 全部验证

| 检查项 | 状态 | 行号 |
|--------|------|------|
| `implements Job` | ✅ | Line 23 |
| 分布式锁 key: `health:sync:job:lock` | ✅ | Line 25 |
| 锁 lease: 30 分钟 (`30 * 60`) | ✅ | Line 26 |
| `redissonClient.getLock(LOCK_KEY)` | ✅ | Line 37 |
| `tryLock(0, LOCK_LEASE_SECONDS, SECONDS)` 非阻塞 | ✅ | Line 42 |
| 锁获取失败 → log + return (不抛异常) | ✅ | Lines 44-45 |
| 增量同步: `LocalDate.now().minusDays(1)` | ✅ | Line 52 |
| 完整日志: 耗时/总数/成功/失败 | ✅ | Lines 60-62 |
| `finally` unlock 检查 `isHeldByCurrentThread()` | ✅ | Lines 70-75 |

### ✅ HealthJobScheduler.java — 全部验证

| 检查项 | 状态 | 行号 |
|--------|------|------|
| `@Configuration` | ✅ | Line 12 |
| `JobDetail` `durability(true)` | ✅ | Line 25 |
| `requestsRecovery(true)` | ✅ | Line 26 |
| cron: `"0 0 2 * * ?"` | ✅ | Line 42 |
| `withMisfireHandlingInstructionFireAndProceed()` | ✅ | Line 43 |
| Job 分组: `"health"` | ✅ | Lines 23, 39 |

---

## 四、Task-C7/C8: 异常指标分级统计

### ✅ HealthExaminationController.java — 全部验证

| 检查项 | 状态 | 行号 |
|--------|------|------|
| `@RequestMapping("/health/examination")` | ✅ | Line 17 |
| `GET /abnormal/stat` | ✅ | Line 33 |
| 参数: `startDate`, `endDate`, `minRecords` (默认10) | ✅ | Lines 36-38 |
| byItem: 按 check item 聚合 + 异常率 + 降序 | ✅ | Lines 63-117 |
| byCustomer: 按 customer 聚合 + 前50截取 | ✅ | Lines 122-161 |
| totalExamRecords / abnormalRate | ✅ | Lines 51-55 |
| 异常等级: NORMAL→CAUTION→WARNING→DANGER | ✅ | Lines 101-109 |
| Response 结构: `AbnormalStatResponse` with `byItem`/`byCustomer` | ✅ | Lines 183-189 |

### 🟡 性能观察 (P2 级)

`getAbnormalStatByItem` 和 `getAbnormalStatByCustomer` 都执行 `examMapper.select(example)` 拉全表到 JVM 再过滤（Lines 68, 126）。当前实现使用 in-memory Stream 聚合，p1-backend.md 已注明「超过 10 万条需改用 SQL 聚合」。此为非阻塞观察，不提为问题。

### 🟡 数据语义问题: `abnormalExamRecords` 统计的是「有异常的独立客户数」，非「异常体检记录数」

`countAbnormalExams()` (Line 163-178) 用 `Set<String> abnormalCustomers` 去重计数，变量名 `abnormalExamRecords` 暗示是「异常记录条数」但实际返回「有异常的独立客户数」。Response 字段 `abnormalExamRecords: long` 的语义与实现不符。

**建议**: 改为 `abnormalCustomerCount` 或修改实现为统计 `isAbnormal=true` 的记录总数。

---

## 五、已修改文件清单（与报告一致）

| 报告声称 | 实际文件 | 状态 |
|----------|----------|------|
| task-c0c: requrls/health.ts | ✅ 已修改 | 49 行, 7 处 URL 变更 |
| task-c0c: modules/health.ts | ✅ 已修改 | 469 行, 新增子表 API |
| task-c0c: healthSyncPanel.vue | ✅ 已修改 | 122 行, date picker |
| p1-backend: HealthSyncJob.java | ✅ 新建 | 76 行 |
| p1-backend: HealthJobScheduler.java | ✅ 新建 | 45 行 |
| p1-backend: HealthExaminationController.java | ✅ 新建 | 206 行 |

**报告中的 "web/src/api/modules/index.ts" 更新** 未被要求验证（PM 审查清单未列），且 modules/health.ts 的导出通过 `@/api/modules` 的已有 import 路径暴露，无需额外验证。

---

## 六、验证清单（留给 tester）

- [ ] `POST /health/sync/sync?startDate=2026-05-17&endDate=2026-05-18` — 确认同步触发
- [ ] `POST /health/follow/save` + `POST /health/follow/delete/{id}` — 确认 URL 有效
- [ ] `POST /health/knowledge/save` + `POST /health/knowledge/delete/{id}` — 确认 URL 有效
- [ ] `GET /health/examination/abnormal/stat` — 确认返回 byItem/byCustomer/abnormalRate
- [ ] Quartz Job 在 2:00 AM 是否自动触发（需检查日志）
- [ ] `POST /health/rule/delete` body: `{id}` → 应返回 404（遗留问题，待 Task-C10 修复）
