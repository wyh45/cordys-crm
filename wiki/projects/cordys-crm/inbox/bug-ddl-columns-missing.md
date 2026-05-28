# Bug-DDL-Columns-01: DDL 声明的列未 ALTER 到数据库（已修复）

## 基本信息
- 发现时间: 2026-05-18
- 修复时间: 2026-05-18
- 修复者: bug-fix Agent
- 严重程度: critical（阻塞 Task-C3/C4 前端开发）
- 模块: 数据库 / Java Entity

## 根因分析

**❌ 原假设错误**：Bug 报告声称「DDL 未 ALTER 到数据库」，实际 DB 列已存在。

**✅ 真实根因**：Java Entity 缺少字段，而非 DB 缺列。

验证：
- `health_archive` DB 有 `archive_no`, `blood_pressure`, `heart_rate` 列
- `health_examination` DB 有 `result_flag` 列
- `health_medical_history` DB 有 `family_history` 列

但 Java Entity 无对应字段 → MyBatis 查询时这些列被忽略（读不出），插入时报 SQL 错。

## 修复内容

### 1. HealthArchive.java（+3 字段）
```java
// 已添加
private String archiveNo;   // -> archive_no VARCHAR(32)
private String bloodPressure; // -> blood_pressure VARCHAR(32)
private Integer heartRate;    // -> heart_rate INT
```

### 2. HealthExamination.java（+1 字段）
```java
// 已添加
private String resultFlag;  // -> result_flag VARCHAR(8)
```

### 3. HealthMedicalHistory.java（+1 字段）
```java
// 已添加
private String familyHistory; // -> family_history TEXT
```

## 修改文件
- `backend/crm/src/main/java/cn/cordys/crm/health/domain/HealthArchive.java`
- `backend/crm/src/main/java/cn/cordys/crm/health/domain/HealthExamination.java`
- `backend/crm/src/main/java/cn/cordys/crm/health/domain/HealthMedicalHistory.java`

## 验证步骤
1. 后端重新编译部署
2. 调用 `POST /health/archive/add` 带 archiveNo/bloodPressure/heartRate 字段，确认写入成功
3. 调用 `GET /health/archive/get/{id}` 确认这些字段能返回
4. 对 examination/medical_history 同理验证

## 状态
- [x] 已修复（2026-05-18，Entity 补字段，非 DB 缺列）
- [ ] 待验证（需重新部署后端 + tester 验证）
