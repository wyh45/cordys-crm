# P0-7/8/9 修复计划

**审查**: reviewer（review-cordyscrm-plan-v2.md）
**修复**: bug-fix Agent
**日期**: 2026-05-18
**状态**: 🔧 修复中

---

## P0-7: Section VI API 端点修正（3处错误）

**根因**: Section VI 从 v1 继承时未与后端 Controller 注解逐条核对，导致 3 处错误。

### 错误 1 — `POST /health/archive/save`（不存在）

| tasks.md Section VI | 后端实际（HealthArchiveController.java） |
|---------------------|----------------------------------------|
| `POST /health/archive/save` — ✅ | `POST /add` (line 51) + `POST /update` (line 57)，**分开** |
| `POST /health/archive/delete/{id}` — ✅ | `POST /delete` (line 63)，**ID 在 body** `{id}`，非 path |

**修复 tasks.md Section VI**：

```
删除：
  POST /health/archive/save  (line 306)

替换为：
  POST /health/archive/add    (新建档案)
  POST /health/archive/update (更新档案)
  POST /health/archive/delete (删除档案，body: {id})
```

### 错误 2 — `/health/rule/get/{id}` 和 `/health/rule/delete` 标记「待补充」

| tasks.md Section VI | 后端实际（HealthFollowRuleController.java） |
|---------------------|----------------------------------------|
| `/health/rule/get/{id}` — ❌ 标"待补充" | ✅ **已存在** line 33 |
| `/health/rule/delete` — ❌ 标"待补充" | ✅ **已存在** 为 `POST /delete/{id}` line 45 |

**修复 tasks.md Section VI**：

从"待补充"表中删除这 2 个，移入"已有端点"表，标记 ✅。

### 错误 3 — `POST /health/examination/page` 不存在

| tasks.md Section VI | 后端实际情况 |
|---------------------|------------|
| `POST /health/examination/page` — ✅ | ❌ **不存在**。体检记录通过 `GET /health/archive/examination/{archiveId}` 获取（HealthArchiveController line 81）。无独立 examination Controller。 |

**修复 tasks.md Section VI**：

删除 `POST /health/examination/page` 行。

### 修正后 Section VI 应为（与后端逐条对齐）

**已有端点（26个）**：
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/health/archive/page` | POST | 档案分页 | ✅ |
| `/health/archive/get/{id}` | GET | 档案详情 | ✅ |
| `/health/archive/add` | POST | 新增档案 | ✅ |
| `/health/archive/update` | POST | 更新档案 | ✅ |
| `/health/archive/delete` | POST | 删除档案（body:{id}） | ✅ |
| `/health/archive/examination/{archiveId}` | GET | 档案关联体检记录 | ✅ |
| `/health/archive/allergy/save` | POST | 保存过敏史 | ✅ |
| `/health/archive/allergy/{archiveId}` | GET | 过敏史列表 | ✅ |
| `/health/archive/history/save` | POST | 保存病史 | ✅ |
| `/health/archive/history/{archiveId}` | GET | 病史列表 | ✅ |
| `/health/archive/vaccination/save` | POST | 保存疫苗接种 | ✅ |
| `/health/archive/vaccination/{archiveId}` | GET | 疫苗接种列表 | ✅ |
| `/health/sync/sync` | POST | 按日期范围同步（query: startDate, endDate） | ✅ |
| `/health/sync/sync-day` | POST | 同步单日（query: date） | ✅ |
| `/health/follow/page` | POST | 随访分页 | ✅ |
| `/health/follow/get/{id}` | GET | 随访详情 | ✅ |
| `/health/follow/save` | POST | 新建/更新随访 | ✅ |
| `/health/follow/delete/{id}` | POST | 删除随访 | ✅ |
| `/health/follow/mark-contacted/{archiveId}` | POST | 标记已联系 | ✅ |
| `/health/follow/by-archive/{archiveId}` | GET | 按客户查随访 | ✅ |
| `/health/rule/page` | POST | 规则分页 | ✅ |
| `/health/rule/get/{id}` | GET | 规则详情 | ✅ |
| `/health/rule/save` | POST | 新建/更新规则 | ✅ |
| `/health/rule/delete/{id}` | POST | 删除规则 | ✅ |
| `/health/rule/enable/{id}` | POST | 启用规则 | ✅ |
| `/health/rule/disable/{id}` | POST | 禁用规则 | ✅ |
| `/health/knowledge/page` | POST | 知识分页 | ✅ |
| `/health/knowledge/get/{id}` | GET | 知识详情 | ✅ |
| `/health/knowledge/save` | POST | 新建/更新知识 | ✅ |
| `/health/knowledge/delete/{id}` | POST | 删除知识 | ✅ |
| `/health/knowledge/categories` | GET | 获取所有分类 | ✅ |
| `/health/ai/interpret` | POST | AI解读 | ✅ |
| `/health/push/send` | POST | 发送推送 | ✅ |
| `/health/push/page` | POST | 推送分页 | ✅ |
| `/health/push/customer/{customerId}` | GET | 按客户查推送 | ✅ |
| `/health/push/knowledge/{knowledgeId}` | GET | 按知识查推送 | ✅ |

**待补充端点（1个）**：
| 端点 | 方法 | 说明 |
|------|------|------|
| `/health/sync/status/{id}` | GET | 同步状态（HealthSyncController 无此端点，需新增） |

---

## P0-8: 数据库表设计最终化（确认 10 表）

**根因**: v2 tasks.md 静默替换了 3 张表（`health_allergy` → `health_ai_interpret` 等），与后端实际 Domain 实体脱节。

### 后端实际 Domain 实体（共 10 个，均有 @Table）

| Domain 类 | 表名 | 状态 |
|-----------|------|------|
| `HealthArchive` | `health_archive` | ✅ |
| `HealthExamination` | `health_examination` | ✅ |
| `HealthFollowRecord` | `health_follow`（v2 已改名） | ✅ |
| `HealthFollowRule` | `health_follow_rule` | ✅ |
| `HealthKnowledge` | `health_knowledge` | ✅ |
| `HealthPushRecord` | `health_push_record` | ✅ |
| `HealthAllergy` | `health_allergy` | ✅ |
| `HealthMedicalHistory` | `health_medical_history` | ✅ |
| `HealthVaccination` | `health_vaccination` | ✅ |
| `HealthArchiveMapping` | `health_archive_mapping` | ✅ |
| `HealthAiInterpret` | `health_ai_interpret` | ✅（存在于 backend/domain，未在 tasks.md v2 表中）|

### 修复方案

tasks.md 第三节"数据库设计（10张表）"应替换为以下最终表清单：

| # | 表名 | 对应 Domain | 说明 |
|---|------|------------|------|
| 1 | `health_archive` | HealthArchive | 电子健康档案主表 |
| 2 | `health_examination` | HealthExamination | 体检记录表 |
| 3 | `health_follow` | HealthFollowRecord | 随访记录表（v2 已改名） |
| 4 | `health_follow_rule` | HealthFollowRule | 随访规则表 |
| 5 | `health_knowledge` | HealthKnowledge | 健康知识库表 |
| 6 | `health_push_record` | HealthPushRecord | 推送记录表 |
| 7 | `health_allergy` | HealthAllergy | 过敏史表（v2 消失 → 恢复） |
| 8 | `health_medical_history` | HealthMedicalHistory | 病史表（v2 消失 → 恢复） |
| 9 | `health_vaccination` | HealthVaccination | 疫苗接种表（v2 消失 → 恢复） |
| 10 | `health_archive_mapping` | HealthArchiveMapping | 档案-体检映射表（去重） |

> v2 的 `health_ai_interpret`（第3张）和 `health_abnormal_index`、`health_sync_log` 不在 10 表清单内——若需，应作为额外表在 decisions.md 记录理由。10 表范围按「与现有 Domain 实体一一对应」原则确定。

---

## P0-9: HealthArchive 字段对齐范围澄清

**根因**: Task-C0c 说「后端补全前端已有字段（allergies, pastMedicalHistory, familyHistory, bloodPressure, heartRate, archiveNo）」，但未说明这些字段存哪张表、涉及多大修改范围。

### 前端 HealthArchive 字段（来自 health.ts）

```
已有: id, customerId, customerName, archiveNo, bloodType,
      allergies, pastMedicalHistory, familyHistory,
      height, weight, bloodPressure, heartRate,
      createTime, updateTime
```

### 后端 HealthArchive 字段（来自 HealthArchive.java）

```
已有: id(BaseModel), customerId, customerName, gender, age,
      bloodType, height, weight, phone, idcardNo
缺少: gender(后独有), age(后独有),
      allergies(前独有), pastMedicalHistory(前独有),
      familyHistory(前独有), bloodPressure(前独有),
      heartRate(前独有), archiveNo(前独有),
      phone(后独有), idcardNo(后独有)
```

### 字段存储方案

| 字段 | 存储位置 | 操作 |
|------|----------|------|
| `allergies` | `health_allergy` 表（已有 Domain/Controller） | 前端改用 `/health/archive/allergy/save` |
| `pastMedicalHistory` | `health_medical_history` 表（已有 Domain/Controller） | 前端改用 `/health/archive/history/save` |
| `familyHistory` | `health_medical_history` 表（需扩展 Domain） | 需扩展 health_medical_history 表加字段 |
| `bloodPressure` | `health_archive` 表 | 后端 Domain + DDL 加列 |
| `heartRate` | `health_archive` 表 | 后端 Domain + DDL 加列 |
| `archiveNo` | `health_archive` 表 | 后端 Domain + DDL 加列 |
| `gender` | `health_archive` 表 | 后端已有，前端 health.ts 类型补全 |
| `age` | `health_archive` 表 | 后端已有，前端 health.ts 类型补全 |
| `phone` | `health_archive` 表 | 后端已有，前端 health.ts 类型补全 |
| `idcardNo` | `health_archive` 表 | 后端已有，前端 health.ts 类型补全 |

### Task-C0c 需更新的具体步骤

在 Task-C0c 末尾追加：

```
- **HealthArchive 字段对齐（具体步骤）**:
  1. 后端 HealthArchive.java 加字段: `archiveNo`, `bloodPressure`, `heartRate`
  2. DDL: `ALTER TABLE health_archive ADD COLUMN archive_no VARCHAR(32)` 等
  3. 后端 health_medical_history.java 加字段: `familyHistory`
  4. 前端 health.ts: HealthArchive 接口补全 `gender`, `age`, `phone`, `idcardNo`
  5. 前端 healthArchiveTable.vue: allergies/pastMedicalHistory/familyHistory/bloodPressure/heartRate/archiveNo 字段改用子表 API（`/archive/allergy/save`, `/archive/history/save`）
  6. 不再在 health_archive 主表 CRUD 中传这三个字段
```

---

## tasks.md 修改汇总

| 位置 | 修改内容 |
|------|----------|
| Section III（数据库 10 表） | 恢复 `health_allergy`, `health_medical_history`, `health_vaccination`；移除 `health_ai_interpret`, `health_abnormal_index`, `health_sync_log`；保留 `health_follow`（已改名） |
| Section VI（API 端点汇总） | 删除 `POST /health/archive/save`；拆为 `/add` + `/update`；`delete/{id}` 改为 `delete`（body）；删除 `/examination/page`；`/rule/get/{id}` 和 `/rule/delete/{id}` 从"待补充"移入"已有"；补充 13 个子表端点（allergy/history/vaccination CRUD 等）；`/follow/mark-contacted`, `/follow/by-archive`, `/knowledge/categories` 等补入 |
| Section VI 待补充 | 只保留 `/health/sync/status/{id}` 一个 |
| Task-C0c | 追加 HealthArchive 字段对齐的 6 步具体操作说明 |

---

## 验证步骤

1. **P0-7 验证**: 对照 HealthArchiveController.java、HealthFollowRuleController.java 等 7 个 Controller，逐条确认 Section VI 端点存在且路径/HTTP方法匹配
2. **P0-8 验证**: 10 张表与 backend/domain/ 下 10 个 Health* Domain 类一一对应（有 @Table 注解）
3. **P0-9 验证**: Task-C0c 新增 6 步覆盖了所有前端独有字段的存储位置和 API 调用方式变更
