# CordysCRM 健康管理模块 — 项目上下文

## 项目定位

CordysCRM 健康管理增值服务模块：在开源 CordysCRM 系统上二次开发，为医疗机构/体检中心提供电子健康档案、AI 解读、健康干预（随访）、健康宣教（知识库+推送）四大能力。

**模块路径**: `backend/crm/src/main/java/cn/cordys/crm/health/`
**前端路径**: `frontend/packages/web/src/views/health/`（已存在 9+ 文件，⚠️ API 契约待对齐）

---

## 技术栈

### 后端

| 项目 | 版本 | 说明 |
|------|------|------|
| Java | 21 | LTS |
| Spring Boot | 3.5.11 | parent pom |
| MyBatis | 3.0.5 (mybatis-starter) | ORM via `BaseMapper<T>` + JPA `@Table` 注解混用 |
| Shiro | 2.1.0 | 鉴权 |
| Redisson | 3.52.0 | 分布式锁/缓存 |
| Quartz | 1.0.0 | 定时任务 |
| FastExcel | 1.3.0 | Excel 导入导出 |
| PageHelper | 6.1.1 | 分页 |
| Swagger | springdoc-openapi 2.8.13 | API 文档 |
| 腾讯云 SMS SDK | 3.1.1445 | `com.tencentcloudapi:tencentcloud-sdk-java` |

**ORM 策略**: 实体类用 JPA `@Table` 指定表名，字段用 MyBatis Plus `BaseMapper<T>` CRUD，**不使用 JPA Repository**。所有 Service 层通过 `BaseMapper<T>` 操作数据库。

### 前端（已存在 9+ 文件，质量良好，但 API 契约全部不匹配）

|| 项目 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.22 | 组合式 API |
| TypeScript | 5.9.3 | |
| Node | v22.16.0 | |
| pnpm | 10.4.1 | workspace monorepo |
| Vue Router | 4.6.4 | hash 模式 |
| 布局组件 | Naive UI | 已有 UI 库 |

**前端实际文件清单**（经 reviewer 审查确认）：

| 文件 | 路径 | 状态 |
|------|------|------|
| 路由注册 | `router/routes/modules/health.ts` (32行) | ✅ 已注册 `/health` + `HEALTH:READ` 权限 |
| 入口页面 | `views/health/index.vue` (56行) | ✅ Tab 布局 6 个模块 |
| 同步面板 | `components/healthSyncPanel.vue` (153行) | ✅ 表单完整，⚠️ 参数签名与后端不匹配 |
| 档案表格 | `components/healthArchiveTable.vue` (361行) | ✅ CRUD 完整，⚠️ 字段定义与后端不匹配 |
| 随访表格 | `components/healthFollowTable.vue` (232行) | ✅ CRUD 完整 |
| 知识库表格 | `components/healthKnowledgeTable.vue` | ✅ 有 |
| 推送面板 | `components/healthPushPanel.vue` | ✅ 有 |
| 随访规则表格 | `components/healthFollowRuleTable.vue` | ✅ 有 |
| API 层 | `lib-shared/api/modules/health.ts` (375行) | ❌ **9个URL中7个与后端不匹配** |
| URL 常量 | `lib-shared/api/requrls/health.ts` (40行) | ❌ **全部需对齐** |
| 国际化 | `locale/zh-CN.ts` + `locale/en-US.ts` (89行) | ✅ i18n key 完整 |

**⚠️ 前端核心问题**：API URL、请求参数、ID 类型与后端全部不一致。Task-C0c（API契约对齐）是所有前端任务的前置依赖。

### 数据库

使用已有 CordysCRM MySQL 数据库。健康模块新建以下表（需 DBA 执行）：
- `health_archive` — 健康档案主表
- `health_examination` — 体检记录明细表
- `health_archive_mapping` — 体检号↔档案映射（已有 domain，未建表）
- `health_follow_record` — 随访记录（已有 domain，未建表）
- `health_follow_rule` — 随访规则模板（已有 domain，未建表）
- `health_knowledge` — 健康知识库（已有 domain，未建表）
- `health_push_record` — 推送记录（已有 domain，未建表）
- `health_allergy` — 过敏史（已有 domain，未建表）
- `health_medical_history` — 病史（已有 domain，未建表）
- `health_vaccination` — 疫苗接种（已有 domain，未建表）

---

## 数据源

### 体检记录 API（数据提供方）

**Base URL**: `http://newreport.jkhaopy.com:9002`

#### 接口一：按时间段获取体检记录摘要

```
POST /health/physical/ai/get/record
```

| 参数 | 说明 |
|------|------|
| appId | `166660309689670637991` |
| startCheckDate | 开始日期 `yyyy-MM-dd` |
| endCheckDate | 结束日期 `yyyy-MM-dd` |
| sign | `MD5(appId + idCardNo + secret)`，idCardNo 用空字符串查全量 |

**返回字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| userName | String | 客户姓名 |
| mobileNo | String | 手机号 |
| genderId | String | 性别 ID（"2"=女，其他=男） |
| age | String | 年龄 |
| workNo | String | 体检单号（唯一标识，格式 `yyyyMMdd...`） |
| productName | String | 套餐名称 |
| idcardNo | String | 身份证号 |

#### 接口二：获取体检报告明细

```
POST /health/physical/ai/report/new/detail
```

| 参数 | 说明 |
|------|------|
| appId | 同上 |
| workNo | 体检单号（来自接口一） |
| sign | 同上 |

**返回字段**:
| 字段 | 类型 | 说明 |
|------|------|------|
| checkIndexName | String | 检查项目名称 |
| resultValue | String | 检查结果值 |
| textRef | String | 参考范围 |
| resultFlag | String | 结果标志（`-` 正常，`↑` 偏高，`↓` 偏低） |

**签名生成规则**: `MD5(appId + idCardNo + secret)`，`idCardNo` 对接口一只传空字符串。

### Dify AI（AI 健康解读）

**Base URL**: `http://220.163.111.166/v1`
**API Key**: `app-05l01mm5RV8dfxUJdIxY6ALG`

```
POST /chat-messages
Authorization: Bearer {key}
Content-Type: application/json
```

| 参数 | 类型 | 说明 |
|------|------|------|
| inputs | object | 体检报告字段（bloodType, height, weight, bloodPressure, heartRate, allergies, pastMedicalHistory, familyHistory, examResults, remark） |
| query | string | 自然语言体检数据描述 |
| response_mode | string | `blocking`（同步等待完整结果） |
| user | string | 用户标识（传 archiveId） |

**响应解析**: 尝试 5 种格式取 `answer` 字段：answer > text > data.content > data.message > result

### 腾讯云短信

- SDK: `com.tencentcloudapi:tencentcloud-sdk-java:3.1.1445`
- 签名方式: HmacSHA256
- 接入地域: `ap-guangzhou`
- 发送方式: `SmsClient.SendSms()` 同步调用
- 配置: 通过 `tencentcloud.sms.*` 配置项管理 SecretId/Key/AppId/SignName/TemplateId

---

## 现有代码骨架（29 个 Java 文件）

```
cn/cordys/crm/health/
├── controller/
│   ├── HealthAiController.java        # POST /health/ai/interpret — AI解读
│   ├── HealthArchiveController.java    # 健康档案 CRUD + 子记录管理
│   ├── HealthFollowController.java    # 随访记录 CRUD + 标记已联系
│   ├── HealthFollowRuleController.java # 随访规则 CRUD + 启用/禁用
│   ├── HealthKnowledgeController.java # 知识库 CRUD + 分类查询
│   ├── HealthPushController.java      # 推送发送 + 推送记录查询
│   └── HealthSyncController.java      # POST /health/sync — 手动触发同步
├── domain/
│   ├── HealthArchive.java             # @Table(name="health_archive"), 档案主表
│   ├── HealthExamination.java         # @Table(name="health_examination"), 体检明细
│   ├── HealthArchiveMapping.java      # @Table(name="health_archive_mapping"), 映射表
│   ├── HealthFollowRecord.java        # @Table(name="health_follow_record"), 随访记录
│   ├── HealthFollowRule.java          # @Table(name="health_follow_rule"), 随访规则
│   ├── HealthKnowledge.java           # @Table(name="health_knowledge"), 知识库
│   ├── HealthPushRecord.java          # @Table(name="health_push_record"), 推送记录
│   ├── HealthAllergy.java             # @Table(name="health_allergy"), 过敏史
│   ├── HealthMedicalHistory.java      # @Table(name="health_medical_history"), 病史
│   └── HealthVaccination.java         # @Table(name="health_vaccination"), 疫苗接种
├── dto/
│   ├── HealthApiRecord.java           # 体检 API 记录 DTO
│   ├── HealthArchivePageRequest.java  # 分页请求
│   ├── HealthFollowPageRequest.java   # 随访分页请求
│   ├── HealthKnowledgePageRequest.java # 知识库分页请求
│   └── HealthPushRequest.java         # 推送请求
└── service/
    ├── HealthSyncService.java          # 两步同步（列表+详情），去重，签名生成
    ├── HealthArchiveService.java       # 档案 CRUD + 子记录管理
    ├── HealthAiService.java            # Dify API 调用，5种响应格式解析
    ├── HealthPushService.java          # 短信+内部通知双通道推送
    ├── HealthFollowService.java        # 随访记录 CRUD
    ├── HealthFollowRuleService.java    # JSON 规则引擎，自动触发随访
    └── HealthKnowledgeService.java      # 知识库 CRUD + 分类/搜索
```

**骨架评估**:
- ✅ 实体类已有 JPA `@Table` 和 Swagger `@Schema` 注解
- ✅ SyncService 已实现完整的两步 API 拉取 + 去重逻辑
- ✅ AI Service 已对接 Dify，响应解析覆盖 5 种格式
- ✅ Push Service 已支持短信+内部通知双通道
- ✅ 7 个 Controller 已定义 REST 接口
- ❌ 数据库表未创建（无 SQL migration）
- ❌ 无前端页面（菜单、前端代码均无）
- ❌ 无定时同步任务（仅手动触发）
- ❌ Domain 实体缺少 `@Entity`/`@Column` 等 JPA 完整注解
- ❌ 异常指标分级功能未实现
- ❌ 没有菜单集成

---

## 项目约束

1. **不改 CordysCRM 核心代码**：健康管理模块是独立子模块，只扩展不修改核心
2. **API Key 安全**：SecretId/Key 通过环境变量或配置文件注入，不硬编码
3. **数据去重**：同一 `workNo` 不重复同步（依赖 `health_archive_mapping` 表）
4. **签名验签**：体检 API 的 sign 生成规则固定为 `MD5(appId + idCardNo + secret)`
5. **Dify 超时**：设置 120s 超时，防止 AI 服务阻塞主线程

---

## 参考文档

- 数据接口文档.md — 体检 API 接口定义
- difi接口.txt — Dify API 接口定义
- 腾讯短信SDK集成指南.md — 腾讯云短信集成说明