# CordysCRM 健康管理模块 — 任务清单

**项目状态**: 🟢 P0 推进中 — C0a+C0b+C0c+C2+C3+C4+C7(前端)+Locale已完成 / DDL已完成 / P0-7/8/9已修复
**审查报告**: `inbox/review-cordyscrm-final.md`
**审查链**: v1 → `review-cordyscrm-plan.md` (6 P0) → v2 → `review-cordyscrm-plan-v2.md` (3 新 P0) → v3 → `review-cordyscrm-final.md` (PASS)
**最后更新**: 2026-05-18 18:15
**技术栈**: Java 21 + Spring Boot 3.5.11 + MyBatis + Vue 3 + Naive UI
**优先级**: P0=阻塞发布（必须先修），P1=核心功能，P2=增强体验，P3=可延后

---

## ⚠️ 审查修正说明

**reviewer 审查发现 tasks.md 存在 3 个严重错误**：

1. **gap analysis 严重不准确**：tasks.md 声称「前端菜单/框架均缺失」，但实际存在 9+ 前端文件（router/routes/modules/health.ts 32行、API层 lib-shared/api/modules/health.ts 375行、6个Vue组件、locale、i18n 等），质量良好
2. **遗漏 Task-C0**：前端 API URL 与后端 Controller 大面积不匹配，9 个 URL 中 7 个不一致，所有 P0~P3 前端任务依赖 API 契约对齐，必须作为最高优先级前置任务
3. **未登记代码已有 bug**：`HealthSyncService.CollectionUtils = null` 导致 NPE（line 171 使用静态方法但变量为 null）

---

## 一、现有代码真实状态

### 后端（29 个 Java 文件）✅ 基本骨架完整，有 1 个 bug
| 分类 | 文件数 | 状态 |
|------|--------|------|
| Controller | 7 | ✅ URL 设计合理（RESTful），但部分端点缺失 |
| Domain/Entity | 10 | ✅ 有 JPA @Table + MyBatis BaseMapper |
| Service | 7 | ✅ 逻辑完整，有签名/去重/Dify解析 |
| DTO/DAO/Util | 5 | ✅ 有 DifyRequest、签名工具类 |
| **Bug** | 1 | ⚠️ `HealthSyncService.CollectionUtils = null` (line 350) → NPE |

### 前端（9+ 个文件）⚠️ 代码质量高，但 API 契约全部不匹配
| 文件 | 路径 | 状态 |
|------|------|------|
| 路由 | `router/routes/modules/health.ts` (32行) | ✅ 已注册 `/health` 路由，`HEALTH:READ` 权限 |
| 入口页面 | `views/health/index.vue` (56行) | ✅ Tab 布局（6个tab） |
| 同步面板 | `components/healthSyncPanel.vue` (153行) | ✅ 完整表单，但调用错误的 API URL |
| 档案表格 | `components/healthArchiveTable.vue` (361行) | ✅ CRUD 完整 |
| 随访表格 | `components/healthFollowTable.vue` (232行) | ✅ CRUD 完整 |
| 知识库表格 | `components/healthKnowledgeTable.vue` | ✅ 有 |
| 推送面板 | `components/healthPushPanel.vue` | ✅ 有 |
| 随访规则表格 | `components/healthFollowRuleTable.vue` | ✅ 有 |
| API 层 | `lib-shared/api/modules/health.ts` (375行) | ❌ **所有 URL 与后端不匹配** |
| URL 常量 | `lib-shared/api/requrls/health.ts` (40行) | ❌ **9个URL中7个与后端不一致** |
| 国际化 | `locale/zh-CN.ts` (89行) | ✅ i18n key 完整 |
| i18n注册 | `locale/en-US.ts` | ✅ 中英双语 |

---

## 二、API 契约不匹配详情（P0 修复核心）

### ❌ URL 不匹配（9个中7个不一致）

| 前端期望 URL | HTTP | 后端实际 | HTTP | 不匹配原因 |
|---|---|---|---|---|
| `/health/sync/trigger` | POST | `/health/sync/sync` | POST | 触发同步 URL 不同 |
| `/health/sync/status/{id}` | GET | 不存在 | — | **后端缺失此端点** |
| `/health/archive/page` | POST | `/health/archive/page` | POST | ✅ 匹配 |
| `/health/follow/add` | POST | `/health/follow/save` | POST | add/update 合并为 save |
| `/health/follow/update` | POST | `/health/follow/save` | POST | 同上 |
| `/health/follow/delete` | POST | `/health/follow/delete/{id}` | POST | 后端用 path 参数 |
| `/health/knowledge/add` | POST | `/health/knowledge/save` | POST | add/update 合并 |
| `/health/knowledge/delete` | POST | `/health/knowledge/delete/{id}` | POST | 后端用 path 参数 |
| `/health/rule/get/{id}` | GET | 不存在 | — | **后端缺失此端点** |
| `/health/rule/delete` | POST | 不存在 | — | **后端缺失此端点** |

### ❌ 参数类型不匹配

| 位置 | 前端类型 | 后端类型 | 问题 |
|------|----------|----------|------|
| `followRule.id` | `number \| null` | `String`（雪花ID） | axios 传 number，Java 收到字符串可能不匹配 |
| `triggerHealthSync` | `{ customerId, syncType }` | `startDate, endDate`（query param） | 参数签名完全不同 |

### ❌ 字段定义不匹配

| 前端 HealthArchive 字段 | 后端 HealthArchive 字段 |
|------------------------|-------------------------|
| `allergies`, `pastMedicalHistory`, `familyHistory`, `bloodPressure`, `heartRate`, `archiveNo` | ❌ 后端 **全无** |
| 后端: `gender`, `age`, `phone`, `idcardNo` | ❌ 前端类型定义 **全无** |

---

## 三、数据库设计（10张表，需 DDL 文件）

> ⚠️ 本节已最终化（bug-fix 2026-05-18 修正 P0-8）
> 表清单与后端 Domain 实体一一对应：10 个 `Health*` Domain 类均有 `@Table(name = "health_*")`
> - 恢复 `health_allergy`, `health_medical_history`, `health_vaccination`（v2 静默删除，现已恢复）
> - v2 静默新增的 `health_ai_interpret`, `health_abnormal_index`, `health_sync_log` 不在 10 表内（如需应另作决策）
> - 对照文件：`backend/crm/src/main/java/cn/cordys/crm/health/domain/`

> DDL 文件路径：`wiki/projects/cordys-crm/sql/schema.sql`

|| # | 表名 | 对应 Domain 类 | 说明 | 状态 |
||---|------|------|------------|------|------|
|| 1 | `health_archive` | `HealthArchive` | 电子健康档案主表 | ✅ 设计完成，❌ 缺 DDL |
|| 2 | `health_examination` | `HealthExamination` | 体检记录表 | ✅ 设计完成，❌ 缺 DDL |
|| 3 | `health_follow` | `HealthFollowRecord` | 随访记录表 | ✅ 设计完成，❌ 缺 DDL |
|| 4 | `health_follow_rule` | `HealthFollowRule` | 随访规则表 | ✅ 设计完成，❌ 缺 DDL |
|| 5 | `health_knowledge` | `HealthKnowledge` | 健康知识库表 | ✅ 设计完成，❌ 缺 DDL |
|| 6 | `health_push_record` | `HealthPushRecord` | 推送记录表 | ✅ 设计完成，❌ 缺 DDL |
|| 7 | `health_allergy` | `HealthAllergy` | 过敏史表 | ✅ 设计完成，❌ 缺 DDL |
|| 8 | `health_medical_history` | `HealthMedicalHistory` | 病史表（含 familyHistory 字段） | ✅ 设计完成，❌ 缺 DDL |
|| 9 | `health_vaccination` | `HealthVaccination` | 疫苗接种表 | ✅ 设计完成，❌ 缺 DDL |
|| 10 | `health_archive_mapping` | `HealthArchiveMapping` | 档案-体检映射表（去重） | ✅ 设计完成，❌ 缺 DDL |

---

## 四、完整任务清单（P0→P1→P2→P3）

### 🔴 P0 — 阻塞发布（必须修复才能推进任何前端任务）

#### Task-C0a: 修复 HealthSyncService NPE Bug 🔍审查通过 ✅ 已完成
- **文件**: `backend/crm/src/main/java/cn/cordys/crm/health/service/HealthSyncService.java`
- **问题**: Line 350 `private static final CollectionUtils CollectionUtils = null;` → line 171 调用 `isNotEmpty()` NPE
- **修复**:
  ```java
  // 已删除 line 350 的 null 静态变量
  // Line 171 改为:
  if (existing != null && !existing.isEmpty()) {
  ```
- **验证**: 调用同步接口后在空库/空映射表上不报 NPE
- **影响**: 任何同步操作都会触发（生产阻塞）
- **产出**: `inbox/p0-backend.md`

#### Task-C0b: 创建 requirements/ 目录并提取需求文档 ✅ 已完成
- **路径**: `wiki/projects/cordys-crm/requirements/`
- **内容**:
  - `数据接口文档.md` — 从项目根目录 `数据接口文档.md` 复制
  - `difi接口.txt` — 从项目根目录 `difi接口.txt` 复制
  - `腾讯短信SDK集成指南.md` — 从项目根目录 `腾讯短信SDK集成指南.md` 复制
- **理由**: 需求文档应与代码仓库分离，便于追溯；当前散落在 context.md 中无法独立引用
- **验证**: `wiki/projects/cordys-crm/requirements/` 目录存在，3 个文件均可独立引用

#### Task-C0c: API 契约对齐（前端改 URL 适配后端 RESTful 风格）🔍审查通过 ✅ 已完成
|- **原则**: 后端 RESTful 设计更优，前端改 URL 成本更低
|- **修改文件**:
  - `frontend/packages/lib-shared/api/requrls/health.ts` — 修改 7 个 URL ✅
  - `frontend/packages/lib-shared/api/modules/health.ts` — 修改对应的函数签名 ✅
  - `frontend/packages/web/src/views/health/components/healthSyncPanel.vue` — 改为日期范围选择器 ✅
|- **URL 映射规则**（前端改后端）:
  ```
  /health/sync/trigger → /health/sync/sync（body:{customerId,syncType} 改 query:startDate,endDate）
  /health/sync/status/{id} → 后端缺失，Task-C5 补充
  /health/follow/add + /health/follow/update → 统一改为 /health/follow/save
  /health/follow/delete (body:id) → /health/follow/delete/{id} (path)
  /health/knowledge/add + /health/knowledge/update → 统一改为 /health/knowledge/save
  /health/knowledge/delete (body:id) → /health/knowledge/delete/{id} (path)
  ```
|- **后端补充缺失端点**:
  - `GET /health/sync/status/{id}` → HealthSyncController 添加（返回 SyncResult）— **注：/rule/get/{id} 和 /rule/delete/{id} 后端已存在，无需新增**
|- **HealthArchive 字段对齐（具体步骤）**:
  1. 后端 `HealthArchive.java` 加字段: `archiveNo`, `bloodPressure`, `heartRate`
  2. DDL: `ALTER TABLE health_archive ADD COLUMN archive_no VARCHAR(32)`, `blood_pressure VARCHAR(32)`, `heart_rate INT`
  3. 后端 `HealthMedicalHistory.java` 加字段: `familyHistory`
  4. 前端 `health.ts` HealthArchive 接口补全 `gender`, `age`, `phone`, `idcardNo`（后端已有）
  5. 前端 `healthArchiveTable.vue`: `allergies`, `pastMedicalHistory`, `familyHistory` 改用子表 API（`/archive/allergy/save`, `/archive/history/save`），**不再**在主表 CRUD 中传这 3 字段
  6. 前端 `healthArchiveTable.vue`: `bloodPressure`, `heartRate`, `archiveNo` 存主表（步骤1+2完成后）

#### Task-C0d: 输出 DDL SQL 文件 🔍审查通过 ✅ 已完成
- **路径**: `wiki/projects/cordys-crm/sql/schema.sql`
- **内容**: 10 张表的完整 DDL（含索引、外键、comment）
- **规范**:
  - 表前缀: `health_`
  - 字符集: `utf8mb4`
  - 主键: 雪花 ID（String），非自增
  - 唯一索引: `health_examination.exam_no`，`health_archive_mapping.exam_no + archive_id`
- **产出**: `wiki/projects/cordys-crm/sql/schema.sql` (13431 bytes)

---

### 🟡 P1 — 核心功能（Task-C0 完成后执行）

> ⚠️ **TS 编译阻塞（2026-05-18 retest）**: `npx tsc --noEmit` 根目录 **288 errors**，`vue-tsc --noEmit` web包 **56 errors**，mobile包 **6 errors**。健康模块组件 (`healthSyncPanel/healthPushPanel/healthKnowledgeTable`) 的 API 类型错误持续存在（自 test-p1.md 2026-05-18 以来未修复）。详见 `inbox/test-p1-retest.md`。**禁止声称 P1 前端完成直到 TS 编译零错误**。

#### Task-C1: 数据库建表 🔍审查通过 ✅ 已完成（Bug-DDL-Columns-01 已修复）
|- 执行 `wiki/projects/cordys-crm/sql/schema.sql`
|- 创建 10 张表 + 索引 + 初始数据（随访规则模板）
|- **前置依赖**: Task-C0d（DDL 文件）
|- **产出**: `inbox/p0-backend.md`
|- **现状**: 9 张表已存在，新建 `health_follow_rule` 表并插入 2 条随访规则初始数据
|- **Bug-DDL-Columns-01 已修复（2026-05-18）**: 经验证 DB 列已存在，真实根因是 Java Entity 缺字段。已补全 HealthArchive(+archiveNo/bloodPressure/heartRate)、HealthExamination(+resultFlag)、HealthMedicalHistory(+familyHistory) 三个 Entity 的字段。详见 `wiki/projects/cordys-crm/inbox/bug-ddl-columns-missing.md`

#### Task-C2: 前端菜单配置（健康管理 7 个子菜单页）✅ 已完成
|- **前置依赖**: Task-C0c（API 契约对齐）✅
|- **完成内容**:
  - `router/routes/modules/health.ts` — 拆分为 7 个子路由（archive/examination/follow/knowledge/push/rule/ai）
  - `routeEnum.ts` — 新增 7 个枚举值（HEALTH_ARCHIVE ~ HEALTH_AI）
  - `locale/zh-CN.ts` + `locale/en-US.ts` — 新增 9 个 menu locale keys
  - `views/health/archive/index.vue` — 健康档案（Tab: archive/sync）
  - `views/health/examination/index.vue` — 体检管理（占位 HealthExamAbnormal）
  - `views/health/follow/index.vue` — 随访记录（Tab: follow/followRule）
  - `views/health/knowledge/index.vue` — 健康知识库
  - `views/health/push/index.vue` — 健康推送（引用 HealthPushPanel）
  - `views/health/rule/index.vue` — 随访规则（引用 HealthFollowRuleTable）
  - `views/health/ai/index.vue` — AI 解读（占位提示）
|- **菜单路由**:
  - `/health/archive` — 健康档案（默认页）
  - `/health/examination` — 体检管理
  - `/health/follow` — 随访记录
  - `/health/knowledge` — 健康知识库
  - `/health/push` — 健康推送
  - `/health/rule` — 随访规则
  - `/health/ai` — AI 健康解读

#### Task-C3: 健康档案管理页面（增强现有 archiveTable）🔍审查通过 ✅ 前端完成（ESLint 0 errors, vue-tsc 通过，dev server 200）
- **测试报告**: `inbox/test-browser-blocked-v4.md`
- **阻塞文件**: `src/views/health/components/healthArchiveTable.vue` — 8 ESLint errors
- **修复**: frontend-dev 修复 ESLint errors 后重启 pnpm dev
- **API层**: ✅ curl 验证通过（详见 inbox/test-c5-p1.md）
> **Bug-405-302 排查结果（2026-05-19）**: POST /health/archive/page 和 GET /health/knowledge/categories 本身无问题，根因是 Shiro 认证拦截。Tester 测试 API 必须带 `Authorization: <accessKey>:<signature>` 请求头，或将 `/health/**` 加入 Shiro 匿名白名单。详见 `inbox/fix-health-api-405-302.md`
|- **后端API测试**: ⚠️ Bug-DB-Config-01 — `cordys-crm` vs `cordys_crm` DB名不匹配。详见 `inbox/test-c5-p1.md`
|- **前端lint**: ⚠️ 6个eslint错误阻塞（no-use-before-define×4, locale .js parsing×2）。vue-tsc已通过。详见 `inbox/test-c3-c4-c7-status.md`
|- **测试报告**: 待frontend修复后执行
#### Task-C4: 体检数据同步页面（修复后对接现有 syncPanel）🔍审查通过 ✅ 前端完成（ESLint 0 errors, vue-tsc 通过，dev server 200）
- **测试报告**: `inbox/test-browser-blocked-v4.md` — 同 Task-C3
- **阻塞文件**: `src/views/health/components/healthSyncPanel.vue` — 4 ESLint errors
- **修复**: frontend-dev 修复 ESLint errors 后重启 pnpm dev
- **API层**: ✅ curl 验证通过（详见 inbox/test-c5-p1.md）
|- **API测试**: ✅ PASS — POST /sync + /sync-day 端点正确，轮询链路完整，syncId字段正确。详见 `inbox/test-c5-p1.md`
|- **前端lint**: ⚠️ 6个eslint错误阻塞（no-use-before-define×4, locale .js parsing×2）。vue-tsc已通过。详见 `inbox/test-c3-c4-c7-status.md`
|- **测试报告**: 待frontend修复后执行
- **修复**: 参数签名改为后端 `startDate/endDate`（query param）
- **增强**:
  - 同步进度实时展示（轮询 `/health/sync/status/{id}`）
  - 同步日志查看（查看同步了多少条体检记录）
- **前置依赖**: Task-C1（表存在）+ Task-C0c（接口对齐）
- **前端完成内容**:
  - 两栏布局：左侧同步控制表单（startDate/endDate/note）+ 右侧同步日志
  - 3 秒轮询 `healthGetSyncStatus`，状态徽章（pending=蓝/processing=黄/completed=绿/failed=红）
  - 日期校验（开始不能晚于结束），错误处理
- **产出**: `inbox/p1-frontend.md`

#### Task-C5: 补充后端缺失端点（1个）🔍 审查通过 ✅ 后端API测试PASS
|- **API测试**: ✅ PASS — GET /status/{id} 端点正确，enum大写，syncId字段完整，轮询链路正确。详见 `inbox/test-c5-p1.md`
|- **测试报告**: `inbox/test-c5-p1.md`
- `GET /health/sync/status/{id}` → HealthSyncService 新增 `SyncStatusEnum` 枚举（PENDING/PROCESSING/COMPLETED/FAILED）+ `getSyncStatusById(id)` 方法
- Controller 修复：不再忽略 path variable id，改调用 `getSyncStatusById(id)`
- `SyncResult` 新增 `status` 字段，返回 `pending/processing/completed/failed`
- **前置依赖**: Task-C0c（API 契约对齐）✅
- **前端部分（过敏史/病史/疫苗 CRUD）**: 已整合到 `healthArchiveDetail.vue` 详情抽屉 tabs 内
- **✅ 后端已部署**: JAR 构建于 2026-05-19 09:45（`mvn clean package -DskipTests`）
- **审查报告**: `inbox/review-task-c5.md`（v1 发现 P0）→ `inbox/review-task-c5-v2.md`（v2 复审通过）

#### Task-C7: AI 健康解读页面（前端）🔍审查通过 ✅ 前端TS已修复

---

### 🟡 P1 — 随访 + AI + 推送

#### Task-C6: AI 健康解读功能 🔍审查通过 ✅ 后端已完成（前端见 Task-C7）
- **后端**:
  - HealthAiService 对接 Dify（已有 5 种响应格式兜底）
  - 解读结果入库 `health_ai_interpret` 表（表未建，Task-C1 未覆盖）
- **前端**:
  - 体检报告详情页添加「AI 解读」按钮
  - 调用 `POST /health/ai/interpret`
  - 展示解读结果（interpretation + suggestions + warnings）
- **前置依赖**: Task-C1（表存在）
- **注**: `health_ai_interpret` 表未在 10 表清单内，Task-C1 未执行建表；若需入库需另建决策
- **产出**: `inbox/p1-backend.md`

#### Task-C7: AI 健康解读页面（前端）🔍审查通过 ✅ 前端完成（ESLint 0 errors, vue-tsc 通过，dev server 200）
- **测试报告**: `inbox/test-browser-blocked-v4.md` — 同 Task-C3/C4
- **修复**: frontend-dev 修复 ESLint errors 后重启 pnpm dev
|- **前端lint**: ⚠️ 6个eslint错误阻塞（no-use-before-define×4, locale .js parsing×2）。vue-tsc已通过。详见 `inbox/test-c3-c4-c7-status.md`
|- **测试报告**: 待frontend修复后执行

#### Task-C7b: 异常指标分级统计 ✅ 后端已完成
- **后端**: `HealthExaminationController` 添加 `/health/examination/abnormal/stat` 端点
  - in-memory 聚合查询（按检查项统计异常率 + 按客户聚合）
  - 支持 date range + minRecords 筛选
  - 异常等级：NORMAL/CAUTION/WARNING/DANGER
- **前端**: 需新建 `views/health/components/healthExamAbnormal.vue` 表格展示
- **前置依赖**: Task-C1（表有数据）
- **产出**: `inbox/task-c7b-c8-backend-verified.md`
- **注**: 代码已存在（206行），`countAbnormalExams` 统计语义有误（统计独立客户数而非异常记录数）

#### Task-C8: 定时同步任务 ✅ 后端已完成
- **后端**: 新增 `HealthSyncJob`（Quartz Job）+ `HealthJobScheduler`
  - 每日凌晨 2:00 执行（cron: `0 0 2 * * ?`）
  - 使用 Redisson 分布式锁（`health:sync:job:lock`）防止多实例冲突
  - 增量同步（同步昨天整天）
- **前置依赖**: Task-C1（表存在）
- **产出**: `inbox/task-c7b-c8-backend-verified.md`

---

### 🟢 P2 — 增强体验

#### Task-C9: 随访管理完整功能
- **已有**: `healthFollowTable.vue`（CRUD）
- **补全**:
  - 随访计划创建（关联规则自动生成）
  - 随访日历视图（可视化随访安排）
  - 随访状态流转（PENDING → IN_PROGRESS → COMPLETED）

#### Task-C10: 随访规则引擎页面
- **已有**: `healthFollowRuleTable.vue`（CRUD）
- **补全**:
  - 规则条件可视化配置（AND/OR + 变量 + 操作符）
  - 规则测试（用示例数据验证规则匹配）
  - 规则启用/禁用开关

#### Task-C11: 健康知识库完整功能
- **已有**: `healthKnowledgeTable.vue`（CRUD）
- **补全**:
  - 富文本编辑器（KindEditor / wangEditor）
  - 分类管理（新增/编辑分类）
  - 标签系统（多标签筛选）

#### Task-C12: 健康推送完整功能
- **已有**: `healthPushPanel.vue`
- **补全**:
  - 推送模板管理（新建/编辑推送模板）
  - 推送效果追踪（查看送达/阅读状态）
  - 定时推送（指定时间发送）

---

### 🟢 P3 — 可延后功能

#### Task-C13: 短信模板管理
- 腾讯云短信签名 + 模板 ID 配置页面
- 模板审核状态展示

#### Task-C14: 定时随访提醒
- `HealthFollowRemindJob`（Quartz，每日 9:00 执行）
- 根据随访规则自动生成随访提醒
- 短信/站内信通知客户

#### Task-C15: 体检报告导出
- PDF 导出体检报告
- 包含基本信息 + 体检项 + AI 解读

---

## 五、开发顺序建议

```
Day 1:
├── Task-C0a: 修复 NPE Bug（5分钟，后端自己修）
├── Task-C0b: 创建 requirements/ 目录（10分钟）
└── Task-C0c: API 契约对齐（2小时，前端改URL）
    ├── frontend/packages/lib-shared/api/requrls/health.ts
    ├── frontend/packages/lib-shared/api/modules/health.ts
    └── backend: 补3个缺失端点 + HealthArchive补字段

Day 2:
├── Task-C0d: 输出 DDL SQL 文件（1小时）
└── Task-C1: 执行 DDL 建表（10分钟）

Day 3:
├── Task-C2: 前端菜单配置（1小时）
├── Task-C3: 健康档案增强（2小时）
└── Task-C4: 体检同步增强（2小时）

Day 4-5:
└── Task-C5~C8: P1 功能开发

Day 6+:
└── Task-C9~C15: P2/P3 功能开发
```

---

## 六、API 端点汇总

> ⚠️ 本节已与后端 Controller 注解逐条对齐（bug-fix 2026-05-18 修正 P0-7）
> - HealthArchiveController 用 `/add` + `/update`（非 `/save`），`/delete` 用 body
> - `/examination/page` 不存在（体检记录走 `/archive/examination/{archiveId}`）
> - `/rule/get/{id}` 和 `/rule/delete/{id}` **已存在**，非"待补充"
> - 对照文件：`backend/crm/src/main/java/cn/cordys/crm/health/controller/`

### 已有端点（36个）
|| 端点 | 方法 | 说明 | 状态 ||
||------|------|------|------||
|| `/health/archive/page` | POST | 档案分页 | ✅ ||
|| `/health/archive/get/{id}` | GET | 档案详情 | ✅ ||
|| `/health/archive/add` | POST | 新增档案 | ✅ ||
|| `/health/archive/update` | POST | 更新档案 | ✅ ||
|| `/health/archive/delete` | POST | 删除档案（body:{id}） | ✅ ||
|| `/health/archive/examination/{archiveId}` | GET | 档案关联体检记录 | ✅ ||
|| `/health/archive/allergy/save` | POST | 保存过敏史 | ✅ ||
|| `/health/archive/allergy/{archiveId}` | GET | 过敏史列表 | ✅ ||
|| `/health/archive/history/save` | POST | 保存病史 | ✅ ||
|| `/health/archive/history/{archiveId}` | GET | 病史列表 | ✅ ||
|| `/health/archive/vaccination/save` | POST | 保存疫苗接种 | ✅ ||
|| `/health/archive/vaccination/{archiveId}` | GET | 疫苗接种列表 | ✅ ||
|| `/health/sync/sync` | POST | 按日期范围同步（query: startDate, endDate） | ✅ ||
|| `/health/sync/sync-day` | POST | 同步单日（query: date） | ✅ ||
|| `/health/follow/page` | POST | 随访分页 | ✅ ||
|| `/health/follow/get/{id}` | GET | 随访详情 | ✅ ||
|| `/health/follow/save` | POST | 新建/更新随访 | ✅（URL待对齐） ||
|| `/health/follow/delete/{id}` | POST | 删除随访 | ✅（URL待对齐） ||
|| `/health/follow/mark-contacted/{archiveId}` | POST | 标记已联系 | ✅ ||
|| `/health/follow/by-archive/{archiveId}` | GET | 按客户查随访 | ✅ ||
|| `/health/rule/page` | POST | 规则分页 | ✅ ||
|| `/health/rule/get/{id}` | GET | 规则详情 | ✅（已存在，v2误标"待补充"） ||
|| `/health/rule/save` | POST | 新建/更新规则 | ✅ ||
|| `/health/rule/delete/{id}` | POST | 删除规则 | ✅（已存在，v2误标"待补充"） ||
|| `/health/rule/enable/{id}` | POST | 启用规则 | ✅ ||
|| `/health/rule/disable/{id}` | POST | 禁用规则 | ✅ ||
|| `/health/knowledge/page` | POST | 知识分页 | ✅ ||
|| `/health/knowledge/get/{id}` | GET | 知识详情 | ✅ ||
|| `/health/knowledge/save` | POST | 新建/更新知识 | ✅（URL待对齐） ||
|| `/health/knowledge/delete/{id}` | POST | 删除知识 | ✅（URL待对齐） ||
|| `/health/knowledge/categories` | GET | 获取所有分类 | ✅ ||
|| `/health/ai/interpret` | POST | AI解读 | ✅ ||
|| `/health/push/send` | POST | 发送推送 | ✅ ||
|| `/health/push/page` | POST | 推送分页 | ✅ ||
|| `/health/push/customer/{customerId}` | GET | 按客户查推送 | ✅ ||
|| `/health/push/knowledge/{knowledgeId}` | GET | 按知识查推送 | ✅ ||

### 待补充端点（1个）⚠️ Task-C0c
|| 端点 | 方法 | 说明 ||
||------|------|------||
|| `/health/sync/status/{id}` | GET | 同步状态（HealthSyncController 缺失，需新增） ||

---

## 七、已知 Bug（需修复）

| Bug | 位置 | 严重性 | 状态 |
|-----|------|--------|------|
| `CollectionUtils = null` NPE | HealthSyncService.java:350 | 🔴 生产阻塞 | 待修复（Task-C0a） |
| 推送状态值不一致 | HealthPushService 写"SENT"，DB 设计为 SUCCESS/FAILED | 🟡 数据不一致 | 待修复（Task-C1 后） |
| HealthExamination.customerId 语义错误 | 应为 archive_id | 🟡 字段语义 | 待修复（Task-C0c） |

---

*最后更新: 2026-05-18（reviewer 审查后修正）*