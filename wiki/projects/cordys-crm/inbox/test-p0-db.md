# CordysCRM P0 DB 动态验证报告

**测试者**: tester Agent
**测试时间**: 2026-05-18
**测试对象**: CordysCRM cordys_crm 数据库
**连接**: mysql -h 127.0.0.1 -P 3306 -u root -p'CordysCRM@mysql' cordys_crm

---

## 验证结果

### ✅ Checklist 1: 10 张 health_* 表存在性

| # | 表名 | 状态 | 备注 |
|---|------|------|------|
| 1 | health_allergy | ✅ 存在 | |
| 2 | health_archive | ✅ 存在 | **缺少 3 列，见下方** |
| 3 | health_archive_field | ⚠️ **额外存在** | **未在 10 表清单中** |
| 4 | health_archive_mapping | ✅ 存在 | |
| 5 | health_examination | ✅ 存在 | **缺少 result_flag，见下方** |
| 6 | health_follow_record | ✅ 存在 | |
| 7 | health_follow_rule | ✅ 存在 | |
| 8 | health_knowledge | ✅ 存在 | |
| 9 | health_medical_history | ✅ 存在 | **缺少 family_history，见下方** |
| 10 | health_push_record | ✅ 存在 | |
| 11 | health_vaccination | ✅ 存在 | |

**结论**: 发现 **11 张**表，比预期多 1 张 `health_archive_field`（未在 DDL/任务清单中定义，但 review-p0-backend.md 验证清单未列出）。10 张核心表全部存在 ✅。

---

### ✅ Checklist 2: health_follow_rule 初始数据

```
id             name                    enabled
init-rule-001  O型血客户季度随访        1
init-rule-002  50岁以上年度体检随访     1
```

**结论**: ✅ 2 条初始数据正确。

---

### ❌ Checklist 3: health_archive 新增列

**实际表结构（SHOW CREATE TABLE）**:
```
id, customer_id, customer_name, gender, age, blood_type,
height, weight, phone, idcard_no, create_time, update_time
```

**缺失列**:
| 列名 | 期望 | 实际 | 状态 |
|------|------|------|------|
| archive_no | VARCHAR(32) | 不存在 | ❌ |
| blood_pressure | VARCHAR(32) | 不存在 | ❌ |
| heart_rate | VARCHAR(32) | 不存在 | ❌ |

> **注意**: `health_archive_field` 表存在（key-value 结构），但新列未加到主表。DDL 声明了这些列但未 ALTER 到数据库。

---

### ❌ Checklist 4: health_examination.result_flag

**实际表结构（SHOW CREATE TABLE）**:
```
id, customer_id, exam_date, exam_type, hospital, exam_items,
conclusion, attachment_urls, create_time, update_time
```

**缺失列**: `result_flag VARCHAR(8)` — **不存在**

---

### ❌ Checklist 5: health_medical_history.family_history

**实际表结构（SHOW CREATE TABLE）**:
```
id, customer_id, disease_name, diagnosis_date,
treatment_status, remarks, create_time
```

**缺失列**: `family_history TEXT` — **不存在**

---

## 问题汇总

| # | 严重程度 | 问题 | 模块 |
|---|----------|------|------|
| 1 | **critical** | health_archive 缺少 archive_no/blood_pressure/heart_rate 列 | DB |
| 2 | **critical** | health_examination 缺少 result_flag 列 | DB |
| 3 | **critical** | health_medical_history 缺少 family_history 列 | DB |
| 4 | **medium** | health_archive_field 表未在任何文档中定义 | DB |

## 根因分析

DDL 文件（sql/schema.sql）包含正确的列定义，但这些 ALTER/CREATE 语句**未实际执行到数据库**。reviewer 的代码审查通过，但 DB 实际状态落后于 schema。

---

## 建议

1. **立即修复**: 对 3 张表执行 ALTER TABLE 补列
2. **确认 health_archive_field 用途**: 是 key-value 扩展字段还是文档未提及的额外设计
3. **重新执行 DDL**: 建议完整跑一遍 schema.sql 确保所有表结构与 DDL 一致

---

## 状态

- [ ] 待修复
