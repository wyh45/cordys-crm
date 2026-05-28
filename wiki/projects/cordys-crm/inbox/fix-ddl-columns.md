# Bug-DDL-Columns-01 Fix: ALTER TABLE 补列

**修复时间**: 2026-05-18
**执行者**: backend-dev
**Bug**: `inbox/bug-ddl-columns-missing.md`

---

## 修复内容

执行 3 条 ALTER TABLE，补齐 DDL 中声明但未创建的列：

### 1. health_archive — 新增 3 列 ✅

```sql
ALTER TABLE health_archive
  ADD COLUMN archive_no VARCHAR(32) COMMENT '档案编号',
  ADD COLUMN blood_pressure VARCHAR(32) COMMENT '血压',
  ADD COLUMN heart_rate INT COMMENT '心率（次/分）';
```

### 2. health_examination — 新增 1 列 ✅

```sql
ALTER TABLE health_examination
  ADD COLUMN result_flag VARCHAR(8) COMMENT '结果标志（-正常 ↑偏高 ↓偏低）';
```

### 3. health_medical_history — 新增 1 列 ✅

```sql
ALTER TABLE health_medical_history
  ADD COLUMN family_history TEXT COMMENT '家族病史';
```

---

## 验证结果（SHOW CREATE TABLE）

### health_archive ✅
```
archive_no       varchar(32)  DEFAULT NULL COMMENT '档案编号'
blood_pressure   varchar(32)  DEFAULT NULL COMMENT '血压'
heart_rate       int          DEFAULT NULL COMMENT '心率（次/分）'
```

### health_examination ✅
```
result_flag  varchar(8)  DEFAULT NULL COMMENT '结果标志（-正常 ↑偏高 ↓偏低）'
```

### health_medical_history ✅
```
family_history  text  DEFAULT NULL COMMENT '家族病史'
```

---

## 状态

- [x] 已修复
- [x] 已验证（SHOW CREATE TABLE 确认）

---

*backend-dev 修复完成*