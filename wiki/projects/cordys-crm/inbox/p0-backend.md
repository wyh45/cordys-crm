# CordysCRM P0 后端完成报告

**生成时间**: 2026-05-18
**执行者**: backend-dev
**状态**: ✅ 全部完成

---

## ✅ Task-C0a: 修复 HealthSyncService NPE

**文件**: `backend/crm/src/main/java/cn/cordys/crm/health/service/HealthSyncService.java`

**根因**: Line 350 声明 `private static final CollectionUtils CollectionUtils = null;` 静态变量为 null，Line 171 调用 `CollectionUtils.isNotEmpty(existing)` 导致 NPE。

**修复**:
1. 删除 Line 350 的 `private static final org.apache.commons.collections4.CollectionUtils CollectionUtils = null;`
2. Line 171 改为: `if (existing != null && !existing.isEmpty())`

**验证**: 空库/空映射表上调用同步接口不再 NPE。

---

## ✅ Task-C0d: 输出 DDL SQL

**文件**: `wiki/projects/cordys-crm/sql/schema.sql`

10 张表的完整 DDL，包含：
- 表前缀: `health_`
- 字符集: `utf8mb4`
- 主键: 雪花 ID（String）
- 索引: `idx_idcard_no`, `uk_exam_no`, `idx_customer_id` 等
- 外键: 无（解耦设计）
- 初始数据: 2 条随访规则模板

---

## ✅ Task-C1: 执行 DDL 建表

**数据库**: `cordys_crm` (MySQL 127.0.0.1:3306)

**现状**: 10 张表中 9 张已存在，新增 `health_follow_rule` 表。

| 表 | 状态 |
|---|---|
| health_archive | ✅ 已存在 |
| health_examination | ✅ 已存在 |
| health_follow_record | ✅ 已存在 |
| **health_follow_rule** | ✅ **新建** |
| health_knowledge | ✅ 已存在 |
| health_push_record | ✅ 已存在 |
| health_allergy | ✅ 已存在 |
| health_medical_history | ✅ 已存在 |
| health_vaccination | ✅ 已存在 |
| health_archive_mapping | ✅ 已存在 |

**新建表结构** (health_follow_rule):
- `id` VARCHAR(64) PK
- `name` VARCHAR(128) NOT NULL
- `description` VARCHAR(512)
- `condition_type` VARCHAR(32) — 触发条件类型
- `condition_expr` TEXT — JSON 条件表达式
- `follow_type` VARCHAR(32)
- `follow_result_template` TEXT
- `follow_interval_days` INT
- `enabled` TINYINT(1) DEFAULT 1
- `priority` INT DEFAULT 100
- `create_time` BIGINT
- `update_time` BIGINT

**初始数据** (2 条随访规则):
- `init-rule-001`: O型血客户季度随访（90天间隔）
- `init-rule-002`: 50岁以上年度体检随访（365天间隔）

---

## 已修改文件清单

| 文件 | 操作 |
|---|---|
| `backend/.../HealthSyncService.java` | 删除 NPE 行 + 修复调用 |
| `wiki/projects/cordys-crm/sql/schema.sql` | 新建（13431 bytes）|

---

## 数据库连接信息

- Host: `127.0.0.1:3306`
- Database: `cordys_crm`
- User: `root`
- Password: `CordysCRM@mysql`

---

## 依赖方提示

- `health_follow_rule` 表现已存在，Task-C5 后端补充端点可直接使用 Mapper CRUD
- 10 张表与 Domain 类 `@Table(name = "health_*")` 全部对齐
- `health_examination` 表缺少 `result_flag` 列（Domain 有），Task-C0c 时补充

---

*backend-dev 完成，P0 后端任务全部收尾*