# CordysCRM P0 Bug Fix 报告

**日期**: 2026-05-18
**执行**: bug-fix Agent
**参照**: tasks.md v3 final + `inbox/review-cordyscrm-final.md`

---

## Task-C0b 执行情况 ✅ 完成

**操作**: 创建 `wiki/projects/cordys-crm/requirements/` 目录，复制 3 个原始需求文档：

| 文件 | 来源 | 状态 |
|------|------|------|
| `requirements/数据接口文档.md` | 项目根目录复制 | ✅ |
| `requirements/difi接口.txt` | 项目根目录复制 | ✅ |
| `requirements/腾讯短信SDK集成指南.md` | 项目根目录复制 | ✅ |

---

## Task-C0a 验证（修复 NPE Bug）

### tasks.md 描述 vs 实际代码

| 项目 | tasks.md 描述 | 实际代码（HealthSyncService.java） |
|------|--------------|-----------------------------------|
| 问题行 | Line 350 `private static final CollectionUtils CollectionUtils = null` | ✅ **确认存在**（line 350） |
| 触发行 | Line 171 调用 `isNotEmpty()` | ✅ **确认存在**（line 171: `CollectionUtils.isNotEmpty(existing)`） |
| 修复方案 | 删除 line 350，line 171 改为 `existing != null && !existing.isEmpty()` | ✅ 代码中 `existing` 已是 `List<HealthArchive>`，`existing.isEmpty()` 是标准 Java 方法 |

### 根因确认

`private static final org.apache.commons.collections4.CollectionUtils CollectionUtils = null` — 这行声明了一个**名为 `CollectionUtils` 的静态常量**，但值为 `null`。然后 line 171 调用 `CollectionUtils.isNotEmpty(existing)`，等价于 `null.isNotEmpty(existing)`，必然 NPE。

**正确的修复**：删除 line 350 这行无意义的声明，line 171 改为 `existing != null && !existing.isEmpty()`（已有 null 检查则直接用 `!existing.isEmpty()`）。

### 结论

**Task-C0a 描述准确** ✅，修复方案可行。执行修复时注意：`existing` 类型是 `List<HealthArchive>`，Java `List` 本身有 `isEmpty()` 方法，不需要任何工具类。

---

## Task-C0c 验证（API 契约对齐）

### Section II URL 不匹配 — 逐条核对

| # | 前端 requrls/health.ts | 后端 Controller | 匹配? | tasks.md 描述 | 准确性 |
|---|----------------------|----------------|-------|--------------|--------|
| 1 | `/health/sync/trigger` (POST) | `POST /health/sync/sync` (query: startDate/endDate) | ❌ | `/health/sync/trigger → /health/sync/sync（body→query）` | ✅ 准确 |
| 2 | `/health/sync/status/{id}` | **不存在**（HealthSyncController 无此端点） | ❌ | 待补充端点 | ✅ 准确 |
| 3 | `/health/archive/page` | `POST /health/archive/page` | ✅ | — | ✅ |
| 4 | `/health/follow/add` | `POST /health/follow/save` | ❌ | `/follow/add+update → /follow/save` | ✅ 准确 |
| 5 | `/health/follow/update` | `POST /health/follow/save` | ❌ | 同上 | ✅ 准确 |
| 6 | `/health/follow/delete` | `POST /health/follow/delete/{id}` | ❌ | `/follow/delete body→path` | ✅ 准确 |
| 7 | `/health/knowledge/add` | `POST /health/knowledge/save` | ❌ | `/knowledge/add+update → /knowledge/save` | ✅ 准确 |
| 8 | `/health/knowledge/update` | `POST /health/knowledge/save` | ❌ | 同上 | ✅ 准确 |
| 9 | `/health/knowledge/delete` | `POST /health/knowledge/delete/{id}` | ❌ | `/knowledge/delete body→path` | ✅ 准确 |
| 10 | `/health/rule/get/{id}` | `GET /health/rule/get/{id}` | ✅ | ~~"待补充"~~ → 实际已存在 | ⚠️ tasks.md v3 已修正 |
| 11 | `/health/rule/delete` | `POST /health/rule/delete/{id}` | ❌ | ~~"待补充"~~ → 实际已存在但 path 方式不同 | ⚠️ tasks.md v3 已修正 |

**总计**: 9 个不匹配中 7 个前端错误（与 tasks.md 描述一致），2 个已由前次修复修正。

### 额外发现：前端 health.ts 中仍有 2 个 API 未在 tasks.md Section II 中列出

| API | requrls 常量 | modules/health.ts 函数 | tasks.md Section II 提及? |
|-----|-------------|----------------------|--------------------------|
| `GET /health/archive/get/{id}` | `GetHealthArchiveUrl` = `/health/archive/get` | `getHealthArchive(id)` | ❌ 未列入 URL 不匹配表 |
| `GET /health/follow/get/{id}` | `GetHealthFollowUrl` = `/health/follow/get` | `getHealthFollow(id)` | ❌ 未列入 |
| `GET /health/knowledge/get/{id}` | `GetHealthKnowledgeUrl` = `/health/knowledge/get` | `getHealthKnowledge(id)` | ❌ 未列入 |

**问题**: 前端 `GetHealthArchiveUrl = '/health/archive/get'` → 缺少 `/{id}` 后缀，但 `getHealthArchive(id)` 函数拼接了 `/${id}`，所以实际 URL 是正确的。`follow` 和 `knowledge` 同理。

结论：**这 3 个 URL 实际是匹配的**，tasks.md Section II 准确。

### 参数类型不匹配 — 核对

| 项目 | tasks.md 描述 | 实际情况 |
|------|--------------|----------|
| `followRule.id` | 前端 `number\|null`，后端 `String`（雪花ID） | ✅ `HealthFollowRule.java` 中 `id` 是 `String`（继承 BaseModel），但 `health.ts` 定义为 `number \| null` — **类型不匹配确实存在** |
| `triggerHealthSync` | 前端 `{customerId, syncType}` body，后端 `startDate/endDate` query | ✅ 确认，见 `HealthSyncController.sync()` 注解 |

### HealthArchive 字段对齐 — 核对

| 前端字段 | 后端 HealthArchive.java 有? | 后端存储位置 |
|----------|----------------------------|-------------|
| `allergies` | ❌ 无 | 子表 `health_allergy`（`/archive/allergy/save`） |
| `pastMedicalHistory` | ❌ 无 | 子表 `health_medical_history`（`/archive/history/save`） |
| `familyHistory` | ❌ 无 | 子表 `health_medical_history`（需加字段） |
| `bloodPressure` | ❌ 无 | 需加 `health_archive` 列 |
| `heartRate` | ❌ 无 | 需加 `health_archive` 列 |
| `archiveNo` | ❌ 无 | 需加 `health_archive` 列 |
| `gender` | ✅ 有 | `health_archive` |
| `age` | ✅ 有 | `health_archive` |
| `phone` | ✅ 有 | `health_archive` |
| `idcardNo` | ✅ 有 | `health_archive` |

**结论**: tasks.md C0c 字段对齐步骤准确 ✅。3 个子表 API 路径与后端一致（`/archive/allergy/save`、`/archive/history/save`）。

---

## 关联需求文档关键信息提取

### 数据接口文档.md（体检第三方 API）
- **Base URL**: `http://newreport.jkhaopy.com:9002`
- **接口一**（时间段体检记录）: `POST /health/physical/ai/get/record`，参数 `appId`、`startCheckDate`、`endCheckDate`、`sign`
- **接口二**（体检报告详情）: `POST /health/physical/ai/report/new/detail`，参数 `appId`、`workNo`、`sign`
- **签名算法**: `MD5(appId + idCardNo + secret)`，idCardNo 为空查全量时用空字符串
- **第三方 appId**: `166660309689670637991`
- **去重依据**: `workNo`（体检单号），存在当天多次体检场景

### difi接口.txt（AI 健康解读 Dify API）
- **Base URL**: `http://220.163.111.166/v1`
- **认证**: `Authorization: Bearer <API-Key>`
- **API-Key**: `app-05l01mm5RV8dfxUJdIxY6ALG`
- **端点**: `POST /chat-messages`
- **核心参数**: `query`（用户输入）、`inputs`（变量）、`response_mode`（streaming/blocking）、`files`（图片上传）
- **已有 SDK 集成**: `backend/crm/src/main/java/cn/cordys/crm/health/service/HealthAiService.java` ✅

### 腾讯短信SDK集成指南.md
- **SDK**: `tencentcloud-sdk-java-tencentcloud` v3.1.764
- **发送端点**: `POST /sms/v20210111/SmsClient.SendSms`
- **已有集成**: `backend/crm/src/main/java/cn/cordys/crm/system/utils/SmsSender.java`、`backend/crm/src/main/java/cn/cordys/crm/system/notice/sender/sms/SmsNoticeSender.java`
- **Task-C8/C13/C14**（定时随访提醒、短信模板管理）与此文档直接相关

---

## 修复摘要

| 任务 | 状态 | 说明 |
|------|------|------|
| Task-C0b | ✅ 已完成 | requirements/ 目录已创建，3 文档已复制 |
| Task-C0a | ✅ 描述准确 | NPE 根因确认，修复方案可行 |
| Task-C0c | ✅ 描述准确 | 7 个 URL 不匹配全部核对正确；字段对齐 6 步与后端一致 |

**待执行**: C0a 和 C0c 的代码修改仍需后端/前端 Agent 实施（本文档已验证其准确性）。
