# CordysCRM 健康管理模块 — 规划审查报告

**审查者**: reviewer Agent
**审查日期**: 2026-05-18
**审查对象**: `wiki/projects/cordys-crm/tasks.md` (开发计划) + `decisions.md` (13 条决策)
**参照**: context.md（含需求文档内容）+ 现有 29 个 Java 文件 + 9 个前端文件

---

## 一、总体评估

**计划严重不通过** — 已有代码量大且存在大量 API 契约不一致，当前 tasks.md 既低估了现有进度又遗漏了关键修复任务。

核心问题：**前端 API 调用层和组件已大量存在（9+ 文件），但 API URL、请求参数、数据类型与后端全部不匹配**。tasks.md 声称「无前端」导致计划未包含契约对齐这一 P0 前置任务——若按现有计划执行，所有前端页面将全部 404/参数错误。

---

## 二、问题列表（P0 → P1 → P2）

### 🔴 P0 — 阻塞发布（必须修复才能推进）

#### P0-1: 前端 API URL 与后端 Controller 大面积不匹配
**位置**: `frontend/packages/lib-shared/api/requrls/health.ts` vs `backend/.../health/controller/*.java`

| 前端期望 URL | HTTP | 后端实际 | HTTP | 匹配？ |
|---|---|---|---|---|
| `/health/sync/trigger` | POST | `/health/sync/sync` | POST | ❌ URL 不同 |
| `/health/sync/status/{id}` | GET | 不存在 | — | ❌ 端点缺失 |
| `/health/follow/add` | POST | `/health/follow/save` | POST | ❌ URL 不同 |
| `/health/follow/update` | POST | `/health/follow/save` | POST | ❌ URL 不同（后端 add/update 合一） |
| `/health/follow/delete` | POST | `/health/follow/delete/{id}` | POST | ❌ ID 传递方式不同 |
| `/health/knowledge/add` | POST | `/health/knowledge/save` | POST | ❌ URL 不同 |
| `/health/knowledge/update` | POST | `/health/knowledge/save` | POST | ❌ URL 不同（后端 add/update 合一） |
| `/health/knowledge/delete` | POST | `/health/knowledge/delete/{id}` | POST | ❌ ID 传递方式不同 |
| `/health/rule/get/{id}` | GET | 不存在 | — | ❌ 端点缺失 |
| `/health/rule/delete` | POST | 不存在 | — | ❌ 端点缺失 |

**建议**: 创建 **Task-C0: API 契约对齐**（P0，在 Task-C1 之前）。两方协商：要么前端改 URL 匹配后端，要么后端加 missing endpoints。推荐方案是前端改 URL（后端 RESTful 风格更优）。

#### P0-2: 同步接口请求参数完全不一致
**位置**:
- 前端: `frontend/packages/lib-shared/api/modules/health.ts:65` — `HealthSyncParams = { customerId: string, syncType: 'FULL'|'INCREMENTAL' }`
- 后端: `HealthSyncController.java:28-31` — `POST /health/sync/sync?startDate=xxx&endDate=xxx`

前端的 `healthSyncPanel.vue` 组件已完整实现（153 行），但调用签名与后端完全不同。这是**前端已存在但不可用**的典型案例。

#### P0-3: 随访规则 ID 类型不一致（String vs number）
**位置**: 
- `frontend/.../api/modules/health.ts:166` — `HealthFollowRule.id: number | null`
- `backend/.../domain/BaseModel.java:14` — `private String id`（雪花 ID，String 类型）

所有随访规则 API 调用（getFollowRule, saveFollowRule, deleteFollowRule, enableFollowRule, disableFollowRule）的参数类型都是 `number`，但后端是 `String`。TypeScript 编译能过，但运行时 axios 会传 `number` 而 Java 后端收到 `"123"` vs `123` 类型不匹配。

#### P0-4: `HealthSyncService.CollectionUtils = null` — 运行时 NPE
**文件**: `backend/.../health/service/HealthSyncService.java:350`
```java
private static final org.apache.commons.collections4.CollectionUtils CollectionUtils = null;
```
**使用处**: Line 171 `if (CollectionUtils.isNotEmpty(existing))` — `isNotEmpty()` 是实例方法，调在 `null` 上会 NPE。
**修复**: 
```java
// 删除 line 350
// Line 171 改为:
if (existing != null && !existing.isEmpty()) {
```

#### P0-5: requirements 目录为空 — 3 个需求文档缺失
**位置**: `wiki/projects/cordys-crm/requirements/`
- `数据接口文档.md` — 不存在
- `difi接口.txt` — 不存在  
- `腾讯短信SDK集成指南.md` — 不存在

需求内容散落在 context.md 的「数据源」节（第 68-147 行）。无独立需求文档 = 无法追溯需求变更。

#### P0-6: 数据库设计缺失 DDL
tasks.md 第二节有 10 张表的逻辑设计（字段/类型/索引），但没有 SQL DDL 文件。这是 Task-C1 的前置依赖，缺少可执行的 DDL 意味着数据库无法建表。

---

### 🟡 P1 — 核心功能

#### P1-1: 前端 HealthArchive 字段定义与后端不一致
**位置**: 
- 前端 `health.ts:35-49` 定义了 `allergies`, `pastMedicalHistory`, `familyHistory`, `bloodPressure`, `heartRate`, `archiveNo` → 后端 `HealthArchive.java` 全无这些字段
- 后端有 `gender`, `age`, `phone`, `idcardNo` → 前端类型定义全无

前端调用 `/health/archive/add` 时 POST 的 body 字段不与后端 Entity 匹配，MyBatis 会忽略未知字段，但会导致数据丢失（用户填了 allergies 却存不了）。

#### P1-2: HealthExamination 字段名与 DB 设计不符
**位置**:
- 后端实体: `HealthExamination.java:18` — `private String customerId`（实际被 HealthSyncService 用作 archive_id）
- DB 设计 (tasks.md Line 75): `archive_id VARCHAR(32) NOT NULL, FK`
- DB 设计 (tasks.md Line 76): `exam_date DATETIME` — 实体是 `Long examDate`（时间戳）

字段名 `customerId` 语义错误且缺少 `archive_id`。这会导致 MyBatis 映射失败或数据错存。

#### P1-3: 推送记录 push_status 值不一致
**位置**:
- 后端 `HealthPushService.java:218` — `record.setPushStatus("SENT")`
- DB 设计 (tasks.md Line 152): `PENDING/SUCCESS/FAILED`
- 前端 API 类型: 无明确枚举，但推送记录 pushStatus 是 string

后端写 "SENT"，DB 设计期望 "SUCCESS"/"FAILED"/"PENDING"。不一致且缺少异步回调更新状态（当前是 fire-and-forget）。

#### P1-4: 缺失后端端点（前端已调用）
前端 API 层已定义但后端无实现：
| 前端调用 | 端点 | 后端状态 |
|---|---|---|
| `getHealthSyncStatus(syncId)` | `GET /health/sync/status/{id}` | ❌ 缺失 |
| `getFollowRule(id)` | `GET /health/rule/get/{id}` | ❌ 缺失 |
| `deleteFollowRule(id)` | `POST /health/rule/delete` | ❌ 缺失（Controller 无） |

#### P1-5: SMS 推送用 SmsSender 而非腾讯云 SDK 直调
**位置**: `HealthPushService.java:57` — `private SmsSender smsSender`
context.md 和 decisions.md 声称「腾讯云 SMS SDK 已引入」，但代码实际走 `cn.cordys.crm.system.utils.SmsSender`（CordysCRM 内部封装）。如果 SmsSender 是腾讯云 SDK 的 wrapper 就无妨，但需在文档中注明。

---

### 🟢 P2 — 增强/观察

#### P2-1: tasks.md 对前端进度评估严重不准确
tasks.md Line 23-24：
> "过敏史/病史/疫苗 | HealthArchiveController 子接口 | ❌ 缺失 | 有 API 但无独立管理页面"
> 
> "P0 | 前端菜单 | ❌ 缺失"
> "P0 | 前端框架 | ❌ 缺失"

实际上：
- 前端路由已注册: `frontend/.../router/routes/modules/health.ts`（32 行，含 `/health` 路由 + `HEALTH:READ` 权限）
- API 层已完整: `lib-shared/api/modules/health.ts`（375 行，覆盖 7 个模块全部 CRUD）
- 组件已存在: 6 个 Vue 组件（syncPanel/pushPanel/archiveTable/followTable/knowledgeTable/followRuleTable）
- 入口页面: `index.vue` 存在

正确状态应为「⚠️ 前端已有大量代码但 API 契约未对齐」。需更新 gap analysis。

#### P2-2: 菜单结构缺少对应前端页面
tasks.md 的菜单设计（第四节）包含 8 个菜单项，但前端现有仅为 1 个入口 `/health/index`。缺少对应每个子菜单的独立 Vue 页面路由（archive/detail, sync, exam-abnormal, ai-interpret, follow, follow-rule, knowledge, push）。

#### P2-3: 缺少定时任务实现
Task-C6（同步后端增强）和 Task-C14（定时随访提醒）计划使用 Quartz，但当前 HealthSyncService 只是手动触发。tasks.md 中 Task-C6 依赖 Task-C1 正确，但应增加 Redisson 分布式锁实现细节。

---

## 三、现有代码优点

1. ✅ **后端 API 设计合理** — Controller 层 RESTful 风格一致（/page, /get/{id}, /save, /delete/{id}），比前端 URL 设计好
2. ✅ **HealthSyncService 同步逻辑完整** — 两步拉取（列表+详情）、去重、签名生成、错误处理覆盖到位
3. ✅ **HealthAiService 健壮** — 5 种格式兜底解析 Dify 响应，超时配置 120s
4. ✅ **HealthFollowRuleService 规则引擎设计良好** — AND/OR 组合条件 + between/contains 操作符 + 变量模板替换
5. ✅ **前端组件质量高** — healthSyncPanel.vue 使用 Naive UI + TypeScript + i18n，代码规范
6. ✅ **13 条技术决策已记录** — decisions.md 覆盖 ORM/签名/去重/AI/推送/前端/ID/路由/定时任务/页面架构/异常统计/HTTP/配置

---

## 四、建议的修复顺序

```
P0 修复阶段:
  1. Task-C0: 创建 requirements/ 目录，从 context.md 提取 3 个需求文档
  2. Task-C0b: 修复 HealthSyncService.CollectionUtils NPE
  3. Task-C0c: API 契约对齐 — 统一前后端 URL/参数/ID 类型
  4. Task-C0d: 输出 DDL SQL 文件

P0 基础阶段 (现有 Task-C1~C4):
  5. Task-C1: 数据库建表（需 DDL）
  6. Task-C2: 前端菜单骨架 + 子页面路由（复用已有组件）
  7. Task-C3: 健康档案页面（修复字段定义后接上已有 archiveTable）
  8. Task-C4: 体检同步页面（修复参数签名后接上已有 syncPanel）

P1~P3 可延后
```

**关键变更**: 必须在 tasks.md 增加 Task-C0（API 契约对齐）作为所有 P0 前端任务的 blocker。

---

## 五、验证清单（留给 tester）

- [ ] 前端 `POST /health/sync/trigger` → 后端应返回 404，确认 URL 不匹配
- [ ] 后端 `HealthSyncService.syncByDateRange("2024-01-01", "2024-01-31")` 在无映射表的空库上运行 → 应报 NPE（CollectionUtils）
- [ ] 前端调用 `getFollowRule(1)` → TypeScript 传 number 但后端期望 String，确认 axios 实际传了什么
- [ ] SMS 推送实际调用了腾讯云 SDK 还是 CordysCRM 内部 SmsSender？需运行时验证
