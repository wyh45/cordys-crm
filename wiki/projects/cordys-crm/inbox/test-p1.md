# CordysCRM P1 动态验证报告

**测试者**: tester Agent
**测试时间**: 2026-05-18
**报告**: `inbox/test-p1.md`

---

## 一、验证 1: DDL 列修复状态

**验证命令**: `mysql -h 127.0.0.1 -P 3306 -u root -p'CordysCRM@mysql' cordys_crm`

| 表 | 列 | 状态 |
|----|----|------|
| health_archive | archive_no | ✅ 已添加 |
| health_archive | blood_pressure | ✅ 已添加 |
| health_archive | heart_rate | ✅ 已添加 |
| health_examination | result_flag | ✅ 已添加 |
| health_medical_history | family_history | ✅ 已添加 |

**结论**: ✅ **PASS** — fix-ddl-columns.md 报告的修复已全部生效。

---

## 二、验证 2: Quartz 同步任务

### 代码存在性检查

| 文件 | 路径 | 状态 |
|------|------|------|
| HealthSyncJob.java | `backend/crm/src/main/java/cn/cordys/crm/health/job/` | ✅ 存在 |
| HealthJobScheduler.java | `backend/crm/src/main/java/cn/cordys/crm/health/job/` | ✅ 存在 |
| cron 表达式 | `0 0 2 * * ?`（每日2:00） | ✅ 正确 |
| Redisson 分布式锁 | `health:sync:job:lock` | ✅ 已实现 |
| 锁 lease 时间 | 30 分钟 | ✅ 合理 |

### 运行时验证

**⚠️ CANNOT VERIFY** — 后端 JAR 构建于 2026-04-17，健康模块 P1 代码（job/）在 JAR 中不存在。当前运行的 8081 后端是旧版本。

**根因**: 后端 p1-backend.md 声称完成任务，但 JAR 未重新构建（backend-dev 只提交了 Java 源文件，未执行 `mvn package`）。

**静态验证**: HealthSyncJob.java 代码逻辑正确（分布式锁、增量同步、日志完整），但运行时是否正常工作**无法验证**。

**结论**: ⚠️ **CANNOT VERIFY** — 代码存在，但未构建/未部署，无法测试实际运行。

---

## 三、验证 3: 异常指标统计 API

### 代码存在性检查

| 检查项 | 文件 | 状态 |
|--------|------|------|
| 端点 | `HealthExaminationController.java:33` `@GetMapping("/abnormal/stat")` | ✅ 存在 |
| 响应 DTO | `AbnormalStatResponse`, `ExamItemStat`, `CustomerAbnormalStat` | ✅ 内部类存在 |
| byItem 聚合 | `getAbnormalStatByItem()` | ✅ 存在 |
| byCustomer 聚合 | `getAbnormalStatByCustomer()` | ✅ 存在 |
| 异常等级判定 | NORMAL/CAUTION/WARNING/DANGER | ✅ 符合报告 |

### 运行时验证

**❌ BLOCKED** — 后端 JAR 未重建，API 不存在于运行中的服务。

**curl 测试**（旧 JAR）：
```
GET /api/v1/health/examination/abnormal/stat → 404 (端点不存在)
```

**结论**: ⚠️ **CODE EXISTS, NOT DEPLOYED** — 代码正确，但未构建到 JAR，无法通过 HTTP 验证实际响应。

---

## 四、验证 4: 前端页面 JS 错误

### 编译验证（vue-tsc --noEmit）

**执行**: `cd frontend/packages/web && pnpm vue-tsc --noEmit`

**结果**: ❌ **COMPILE FAIL** — TypeScript 编译失败，10+ 个错误，**前端无法构建**。

### 错误详情

#### healthSyncPanel.vue（7 个错误）
| 行 | 错误 | 类型 |
|----|------|------|
| 96 | `HealthSyncResult` not exported from `@/api/modules` | ❌ import 失败 |
| 162-194 | `.data` property does not exist on `HealthSyncResult` | ❌ 属性不存在 |
| 165 | `.data` property does not exist | ❌ 同上 |

#### healthPushPanel.vue（5 个错误）
| 行 | 错误 | 类型 |
|----|------|------|
| 113 | `getHealthPushPage` not exported from `@/api/modules` | ❌ API 未定义 |
| 113 | `HealthPushRecord` not exported | ❌ 类型未导出 |
| 113 | `pushHealthKnowledge` not exported (建议用 `saveHealthKnowledge`) | ❌ 名称错误 |
| 166 | CustomerTableParams 缺少 `viewId` 必填字段 | ❌ 参数不匹配 |

#### healthKnowledgeTable.vue（8+ 个错误）
| 行 | 错误 | 类型 |
|----|------|------|
| 68 | `HealthKnowledgeListItem` not exported from `@/api/modules` | ❌ 类型不存在 |
| 90 | `propsRes`, `propsEvent`, `filterConfigList` 不存在 | ❌ 引用错误 |
| 91 | `ComputedRef<FormDesignKeyEnum>` not assignable to `FormKey` | ❌ 类型不兼容 |
| 191 | `searchData` property does not exist | ❌ 引用错误 |
| 195 | `FilterFormItem[]` 与 `FilterResult` 类型不兼容 | ❌ 参数类型错误 |

#### mobile 包（3 个错误，来自 web/src/components/business/crm-form-create/config.ts）
| 行 | 错误 | 类型 |
|----|------|------|
| 755 | `healthArchive`, `healthFollow`, `healthKnowledge` 缺失 | ❌ FormDesignKeyEnum 未覆盖 |
| 799 | 同上 | ❌ 同上 |
| 843 | 同上 | ❌ 同上 |

### 根因分析

p1-frontend.md 报告"前端开发完成"，但存在**严重的类型不匹配问题**：

1. **API 层未同步**: `lib-shared/api/modules/` 中缺少 `HealthSyncResult` 类型、`getHealthPushPage` API、`HealthKnowledgeListItem` 类型等
2. **组件引用不存在的 Hook 属性**: `propsRes`/`propsEvent`/`filterConfigList`/`searchData` 在 `useHealthKnowledgeTable` 或 `useTableRes` 中不存在
3. **参数类型不匹配**: `FilterFormItem[]` vs `FilterResult`、customer params 缺 `viewId`
4. **mobile FormDesignKeyEnum 未扩展**: 健康模块 3 个 key 未加入枚举

**影响**: 前端构建失败 → 健康模块所有页面**无法部署**。

---

## 五、验证结果汇总

| 验证项 | 结论 | 严重程度 |
|--------|------|----------|
| DDL 列修复 | ✅ PASS | — |
| Quartz Job 代码 | ✅ 代码存在 | — |
| Quartz Job 运行时 | ⚠️ CANNOT VERIFY（未部署） | medium |
| 异常指标 API 代码 | ✅ 代码存在 | — |
| 异常指标 API 运行时 | ⚠️ CANNOT VERIFY（未部署） | medium |
| 前端编译 | ❌ **COMPILE FAIL** | **critical** |

### 发现的新 Bug

| Bug ID | 文件 | 描述 | 严重性 |
|--------|------|------|--------|
| Bug-Frontend-TS-01 | healthSyncPanel.vue | `HealthSyncResult` 未从 API 模块导出，`.data` 属性引用错误 | critical |
| Bug-Frontend-TS-02 | healthPushPanel.vue | `getHealthPushPage`/`HealthPushRecord`/`pushHealthKnowledge` API 未定义 | critical |
| Bug-Frontend-TS-03 | healthKnowledgeTable.vue | `HealthKnowledgeListItem` 类型不存在，`propsRes`/`propsEvent` 等属性引用错误 | critical |
| Bug-Frontend-TS-04 | healthKnowledgeTable.vue | filter 函数签名与 `useFilter` 不匹配 | critical |
| Bug-Frontend-TS-05 | crm-form-create/config.ts | FormDesignKeyEnum 缺少 healthArchive/healthFollow/healthKnowledge | high |
| Bug-Backend-01 | backend JAR | P1 代码未构建，Job/API 无法验证运行时 | critical |

---

## 六、建议

1. **立即**: 前端 dev 修复所有 TS 编译错误（API 导出补全、类型对齐）
2. **立即**: 后端 dev 执行 `mvn package` 重建 JAR，部署后验证 Job 和 API
3. **顺序**: 前端编译通过 → 后端部署 → 前端页面联调

---

## 状态

- [ ] 待修复
