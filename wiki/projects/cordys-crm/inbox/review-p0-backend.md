# CordysCRM P0 后端 — 审查报告

**审查者**: reviewer Agent
**审查日期**: 2026-05-18 15:45
**审查对象**: `inbox/p0-backend.md` + `HealthSyncService.java` + `sql/schema.sql`
**审查项**: Task-C0a (NPE Fix) / Task-C0d (DDL) / Task-C1 (DB Execution)

---

## 一、总体判决

**🟢 PASS** — 3 个 P0 后端任务全部符合要求。NPE 修复精确，DDL 覆盖 10 表 + 索引 + 初始数据，DB 执行报告完整。

余 2 个 P1 级文档不一致（tasks.md 表名 / p0-backend.md 列注释），不阻塞通过。

---

## 二、逐项验证

### ✅ Task-C0a: NPE 修复

| 检查项 | 文件:行 | 状态 |
|--------|---------|------|
| 旧 Line 350 (`CollectionUtils = null`) 已删除 | HealthSyncService.java:348 | ✅ 行号 348 现在是 `public static class SyncResult {` |
| Line 171 改为 `existing != null && !existing.isEmpty()` | HealthSyncService.java:171 | ✅ 精确匹配 |
| 不再 import `CollectionUtils` | HealthSyncService.java:1-22 | ✅ import 列表中没有 `org.apache.commons.collections4` |
| 无新增 import | — | ✅ 未引入新依赖 |

**结论**: NPE 根因已消除。空库/空映射表场景下调用 `getOrCreateArchive()` 不会抛出 NullPointerException。

---

### ✅ Task-C0d: DDL SQL

| 检查项 | 要求 | 实际 | 状态 |
|--------|------|------|------|
| 表数量 | 10 张 | 10 张 (archive, examination, follow_record, follow_rule, knowledge, push_record, allergy, medical_history, vaccination, archive_mapping) | ✅ |
| 字符集 | utf8mb4 | `utf8mb4` + `utf8mb4_unicode_ci` | ✅ |
| 主键策略 | 雪花 ID (String) | `VARCHAR(64) NOT NULL COMMENT '主键ID（雪花算法）'` | ✅ |
| `uk_exam_no` | 唯一索引 | Line 52 (health_examination) + Line 204 (health_archive_mapping) | ✅ |
| `idx_idcard_no` | 普通索引 | Line 32 (health_archive) | ✅ |
| `idx_customer_id` | 普通索引 | 6 张表均有 (exam/follow_record/push_record/allergy/medical_history/vaccination) | ✅ |
| 初始数据 | 2 条随访规则 | Lines 213-241: init-rule-001 (O型血) + init-rule-002 (50岁以上) | ✅ |
| 字段预置 | archive_no/blood_pressure/heart_rate 在 health_archive | Lines 26-28 | ✅ (Task-C0c 步骤1+2 的 DDL 部分已预执行) |
| 字段预置 | family_history 在 health_medical_history | Line 162 | ✅ (Task-C0c 步骤3 的 DDL 部分已预执行) |
| health_examination.result_flag | 标记列 | Line 49 `result_flag VARCHAR(8)` | ✅ |

**额外发现**: 
- health_knowledge 有 FULLTEXT 索引 (Line 113) — 任务计划未要求，但属于合理增强
- 所有表均使用 `DROP TABLE IF EXISTS` — 幂等安全

---

### ✅ Task-C1: DB 执行

| 检查项 | p0-backend.md 声称 | 判定 |
|--------|-------------------|------|
| 9 张表已存在 | 所列 9 表均在 DDL 中定义 | ✅ 一致 |
| health_follow_rule 新建 | 表结构 + 2 条初始数据 | ✅ DDL 支持 |
| 连接信息 | 127.0.0.1:3306 / cordys_crm | ⚠️ 无法验证（reviewer 不连 DB） |

> **注意**: DB 实际执行状态需 tester 连接验证。本审查仅确认 DDL 与报告一致。

---

## 三、发现的文档不一致（P1 级）

### 🟡 不一致 1: tasks.md 表名 `health_follow` vs 实际 `health_follow_record`

| 来源 | 表名 |
|------|------|
| tasks.md Section III (Line 97) | `health_follow` |
| Domain `@Table` 注解 (HealthFollowRecord.java:14) | `health_follow_record` |
| DDL schema.sql (Line 60) | `health_follow_record` |
| p0-backend.md (Line 47) | `health_follow_record` |

**影响**: tasks.md 写了简化名 `health_follow`，但 Domain 和 DDL 一致用 `health_follow_record`。不阻塞通过，但 tasks.md Section III 应更正为 `health_follow_record`。

### 🟡 不一致 2: p0-backend.md Line 98 说缺少 result_flag

> "health_examination 表缺少 result_flag 列（Domain 有），Task-C0c 时补充"

但 DDL (Line 49) 已包含 `result_flag VARCHAR(8)` 列。p0-backend.md 此条注释可能过时（DDL 后来添加了该列）。

---

## 四、验证清单（留给 tester）

- [ ] 连接 `127.0.0.1:3306/cordys_crm`，确认 10 张表全部存在
- [ ] `SELECT * FROM health_follow_rule` — 应返回 2 行初始数据
- [ ] 触发同步接口 → 不再报 NPE（验证 Task-C0a 修复）
- [ ] `SHOW CREATE TABLE health_examination` → 确认 `result_flag` 列存在
- [ ] `SHOW CREATE TABLE health_archive` → 确认 `archive_no`, `blood_pressure`, `heart_rate` 列存在
