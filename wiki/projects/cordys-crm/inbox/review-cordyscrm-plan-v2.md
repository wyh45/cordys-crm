# CordysCRM 健康管理模块 — 规划审查 v2（重新审查）

**审查者**: reviewer Agent
**审查日期**: 2026-05-18 14:45
**审查对象**: `wiki/projects/cordys-crm/tasks.md` (修正版) + `inbox/review-response.md`
**参照**: 后端 7 个 Controller 实际端点 + 10 个 Domain 实体 + 前端 9+ 文件

---

## 一、总体评估

**🔴 不通过** — review-response 声称修复了原审查的 6 个 P0 问题，但修正版 tasks.md 引入了 **3 个新的严重错误**，且 reviews-response 中 1 个承诺未兑现。

| 审查维度 | v1 状态 | v2 状态 | 变化 |
|----------|--------|--------|------|
| 6 个 P0 问题 | 全部未修 | 6 个全部登记为 Task-C0a~C0d | ✅ 改进 |
| decisions.md | 13 条决策 | 15 条（新增 14+15） | ✅ 改进 |
| tasks.md API 端点表 | 无 | **有 3 处与后端实际不符** | 🔴 回归 |
| 数据库表设计 | 10 张（含 allergy 等） | **10 张但内容改变（3 表消失 3 表新增）** | 🔴 回归 |
| context.md | "前端代码均无" | **未更新** | 🔴 遗漏 |

---

## 二、v1 审查 6 个 P0 问题 — 处理情况

| # | v1 P0 问题 | v2 处理 | 判定 |
|---|-----------|---------|------|
| P0-1 | API URL 不匹配 (10 处) | Task-C0c 列出 7 个 URL 映射 | ✅ 已登记 |
| P0-2 | 同步参数不一致 | Task-C0c 提及修改 healthSyncPanel.vue | ✅ 已登记 |
| P0-3 | 规则 ID 类型不一致 | Decision 15 明确改为 string | ✅ 已登记 |
| P0-4 | NPE bug (CollectionUtils=null) | Task-C0a 精确到行号 + 修复代码 | ✅ 已登记 |
| P0-5 | requirements/ 缺失 | Task-C0b 登记创建 3 文档 | ✅ 已登记 |
| P0-6 | DDL 缺失 | Task-C0d 登记输出 schema.sql | ✅ 已登记 |

**结论**: 6 个 P0 问题均已登记为任务。Task-C0a~C0d 作为新 P0 任务全部合理。

---

## 三、v2 新发现的严重问题

### 🔴 P0-7: tasks.md 第六节「API 端点汇总」与后端实际不符（3 处错误）

**来源**: 审查 review-response 时发现该节声称的端点与后端 Controller 注解不一致。

#### 错误 1: HealthArchiveController 被错误同化为 `/save` 模式

| tasks.md Section VI 声称 | 后端实际 (HealthArchiveController.java) | 
|---|---|
| `POST /health/archive/save` | ❌ 不存在。实际是 `POST /add` (line 51) + `POST /update` (line 57) |
| `POST /health/archive/delete/{id}` | ❌ 不存在。实际是 `POST /delete` (line 63)，ID 在 body: `{id}` |

**根因**: HealthArchiveController 与 follow/knowledge/rule 的 URL 风格不同（用 `/add` + `/update` 而非 `/save`），tasks.md 错误地将 archive 同化为统一模式。

#### 错误 2: HealthFollowRuleController 端点被标记为「待补充」但实际已存在

| tasks.md Section VI 声称 | 后端实际 (HealthFollowRuleController.java) |
|---|---|
| `GET /health/rule/get/{id}` — ❌ "待补充" | ✅ **已存在** (line 33) |
| `POST /health/rule/delete` — ❌ "待补充" | ✅ **已存在** 为 `POST /delete/{id}` (line 45) |

**根因**: 原 v1 审查未读 HealthFollowRuleController，误判缺失。v2 修正时未核实代码即复制了原错误判断。

#### 错误 3: `/health/examination/page` 端点声称存在

tasks.md Line 325: `POST /health/examination/page` — 标记 ✅。但 HealthArchiveController 无此端点。体检记录通过 `GET /health/archive/examination/{archiveId}` (line 81) 获取，非独立 page 端点。

### 🔴 P0-8: 数据库表设计静默变更（3 表消失，3 表新增，无变更说明）

| v1 tasks.md (原 10 表) | v2 tasks.md (新 10 表) | 变化 |
|---|---|---|
| health_archive | health_archive | 保留 |
| health_examination | health_examination | 保留 |
| health_archive_mapping | health_ai_interpret | **替换** |
| health_follow_record | health_follow | **重命名** |
| health_follow_rule | health_follow_rule | 保留 |
| health_knowledge | health_knowledge | 保留 |
| health_push_record | health_push_record | 保留 |
| health_allergy | health_abnormal_index | **替换** |
| health_medical_history | health_archive_mapping | **替换** |
| health_vaccination | health_sync_log | **替换** |

**消失的 3 张表** (`health_allergy`, `health_medical_history`, `health_vaccination`) 在代码中仍有对应 Domain 实体（`HealthAllergy.java`, `HealthMedicalHistory.java`, `HealthVaccination.java`）和 Controller 端点（`/allergy/save`, `/history/save`, `/vaccination/save`）。

**新增的 3 张表** (`health_ai_interpret`, `health_abnormal_index`, `health_sync_log`) 在代码中无对应 Domain 实体。

此变更导致 DB 设计与 Java 实体完全脱节，且无变更说明、无决策记录。

### 🟡 P1-6: context.md 未按 review-response 承诺更新

review-response Line 48-49 明确说需更新 context.md，将"前端代码均无"改为"⚠️ 已有 9+ 文件"。但 context.md Line 204 仍写：
```
❌ 无前端页面（菜单、前端代码均无）
```
**当前状态**: 未修改。review-response 的承诺未兑现。

---

## 四、v1 遗留问题（v2 仍未解决）

### P0-9: HealthArchive 字段对齐范围不明确

Task-C0c 说「后端补全前端已有字段（allergies, pastMedicalHistory, familyHistory, bloodPressure, heartRate, archiveNo）」，但现有 HealthArchive domain 仅有 11 个字段（含 BaseModel 继承）。新增 6 个字段意味着：
1. 修改 Domain 实体
2. 同步修改 DB DDL
3. 同步修改 HealthArchiveService 所有 CRUD 方法

当前 Task-C0c 未说明这些字段存到哪张表、涉及的修改范围多大。

### P0-10: HealthExamination.customerId 语义错误仍存

Entity 字段名是 `customerId` 但实际存 `archive_id`。tasks.md 第七节 (Line 342) 列为已知 Bug 但标记为「P1 — 待修复（Task-C0c）」。然而 Task-C0c 的任务描述只提及 URL 修改和 HealthArchive 字段对齐，未包含 HealthExamination 字段修正。

---

## 五、review-response 质量评估

| 承诺 | 兑现？ | 说明 |
|------|--------|------|
| 重写 gap analysis | ✅ 已执行 | 新增"一、现有代码真实状态"节 |
| 登记 Task-C0 系列 | ✅ 已执行 | C0a~C0d 4 个 P0 前置任务 |
| 新增 Decision 14+15 | ✅ 已执行 | decisions.md 已追加 |
| 更新 context.md | ❌ 未兑现 | context.md 仍写"前端代码均无" |
| API 端点汇总 | ❌ 有 3 处错误 | 见 P0-7 |
| DB 设计完整性 | ❌ 静默变更 | 见 P0-8 |

---

## 六、修正建议（按优先级）

### 立即修正（P0）

1. **P0-7**: 修订 tasks.md Section VI — 核实全部 23+3 个端点与后端 Controller 注解一致
   - `POST /health/archive/save` → 拆为 `POST /health/archive/add` + `POST /health/archive/update`
   - `POST /health/archive/delete/{id}` → `POST /health/archive/delete` (body: {id})
   - 删除 `POST /health/examination/page`（不存在）
   - `GET /health/rule/get/{id}` → 移出"待补充"，标记 ✅
   - `POST /health/rule/delete` → 改为 `POST /health/rule/delete/{id}`，标记 ✅

2. **P0-8**: 决定最终 10 张表 — 如果保留 allergy/medical_history/vaccination，则恢复表并新增 ai_interpret/abnormal_index/sync_log；如果删除前者，则需同步删除 Domain 实体和 Controller 端点，并在 decisions.md 记录决策 16

3. **P0-9**: 明确 HealthArchive 字段对齐的范围：
   - 新增字段存 health_archive 表（需 DDL 加列）
   - 还是独立子表（allergy/medical_history 已有表）？
   - 需写进 Task-C0c 的具体步骤

### 后续修正（P1）

4. **P1-6**: 更新 context.md Line 204 — 改为"⚠️ 前端已有 9+ 文件，但 API 契约未对齐"
5. **P0-10**: Task-C0c 增加 `HealthExamination.customerId → archiveId` 字段重命名

---

## 七、最终判决

**🔴 FAIL** — v2 修正引入了 3 个新的 P0 错误（API 端点表不实 + DB 设计静默变更 + context.md 未更新），抵消了 v1 审查的修复价值。

**通过条件**: P0-7、P0-8、P0-9 三个问题修正后，tasks.md Section VI 与后端 Controller 注解逐条一致，DB 表设计最终确定。

**建议**: tasks.md 的 Section VI（API 端点汇总）应直接从 Controller 注解自动生成，而非手写。手写端点清单极易过时。
