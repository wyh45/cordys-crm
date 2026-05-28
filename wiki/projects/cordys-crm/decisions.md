# CordysCRM 健康管理模块 — 技术决策

**最后更新**: 2026-05-18
**状态**: 已决策（待实现验证）

---

## 决策 1: ORM 策略 — MyBatis + JPA 注解混用

### 背景

CordysCRM 后端使用 MyBatis Plus 作为 ORM，实体类需要 `BaseMapper<T>` 实现 CRUD。但实体类上有 JPA `@Table` 注解，导致两种框架混用。

### 决策

**维持现状：MyBatis `BaseMapper<T>` + JPA `@Table` 注解混用**

- JPA `@Table` 仅用于指定表名（`name = "xxx"`），不做 JPA Repository
- MyBatis `BaseMapper<T>` 提供所有 CRUD 方法（`select`, `insert`, `update`, `delete`）
- 字段映射通过 MyBatis 反射自动完成，不依赖 JPA `@Column`
- Swagger `@Schema` 注解继续保留（不影响 ORM）

### 理由

- 现有 29 个 Java 文件均使用此模式，改造风险大
- `BaseMapper<T>` 已覆盖所有常用 CRUD，无需迁移到 JPA Repository
- JPA 注解仅作为表名声明，不引入额外依赖冲突

### 影响

- **风险**: 字段名与列名不一致时需手动处理（通过 MyBatis XML 或 `@TableId`）
- **替代方案**: 纯 JPA Repository（改造大，暂不考虑）

---

## 决策 2: 体检 API 签名生成 — `MD5(appId + idCardNo + secret)`

### 背景

体检 API 的 sign 字段生成规则：`MD5(appId + idCardNo + secret)`，key 按升序排列后拼接。

- 接口一（列表）：`sign = MD5(appId + "" + secret)`
- 接口二（详情）：`sign = MD5(appId + "" + secret)`

两个接口都用空字符串作为 idCardNo 参数签名。

### 决策

**保持现有实现：签名生成在 `HealthSyncService` 内部，不外露**

- `generateSign()` 方法封装在 `HealthSyncService` 中
- Secret 通过 `@Value("${health.api.secret}")` 配置注入，不硬编码
- 签名结果每次请求动态生成，不缓存

### 理由

- 现有 `HealthSyncService.generateSign()` 已实现此逻辑
- secret 存在配置文件或环境变量，符合安全要求
- 两个接口的签名逻辑一致（都用空 idCardNo）

### 影响

- **风险**: secret 泄露后所有签名失效 → 立即轮换 secret
- **替代方案**: 动态 idCardNo（需身份证号）→ 暂不支持（接口一只返回列表无身份证加密）

---

## 决策 3: 体检数据去重策略 — 基于 `exam_no` 唯一索引

### 背景

同一客户可能有多次体检（当天或跨天），需要避免重复同步。

### 决策

**使用 `health_archive_mapping.exam_no` 唯一索引去重**

- 每次同步前查询 `health_archive_mapping` 表中已存在的 `exam_no`
- 如果 `exam_no` 已存在，跳过（返回 `SyncStatus.SKIPPED`）
- 同步成功后插入新记录（`syncStatus=1`）

### 理由

- `exam_no` 是体检单号，在体检 API 中全局唯一
- `health_archive_mapping` 表已设计唯一索引约束
- 现有 `HealthSyncService.getSyncedExamNos()` 已实现此逻辑

### 影响

- **风险**: `exam_no` 格式不稳定（`yyyyMMdd...`）→ 可能解析失败 → 依赖接口保证
- **替代方案**: 基于 `idcard_no + exam_date` 组合去重（更复杂，暂不需要）

---

## 决策 4: Dify AI 响应解析 — 5 种格式兜底

### 背景

Dify API 返回格式不确定（chat 模式 vs workflow 模式，blocking vs streaming）。

### 决策

**5 层兜底解析，按优先级尝试**

```
1. root["answer"]         → chat blocking 模式
2. root["text"]           → workflow blocking 模式
3. root["data"]["text"]   → streaming data.text
4. root["data"]["content"] → streaming data.content
5. root["data"]["message"] → streaming data.message
6. root["message"]        → 通用 message 字段
7. root["result"]          → 其他 result 字段
8. fallback: 返回原始 body   → 解析失败时降级
```

### 理由

- Dify 文档未明确规范响应格式，多版本共存
- 现有 `HealthAiService.parseDifyResponse()` 已实现此逻辑
- blocking 模式通常返回 `answer`，优先检查

### 影响

- **风险**: Dify API 格式变更 → 解析失败 → 降级返回原始文本
- **替代方案**: 统一要求 Dify 返回固定 JSON 格式（需 Dify 应用侧配置）

---

## 决策 5: 推送通道 — 短信+内部通知双通道

### 背景

健康推送需要支持多种渠道：短信（腾讯云）和内部通知（INSITE/EMAIL/DINGTALK/LARK/WECOM）。

### 决策

**短信优先，内部通知兜底**

- `HealthPushService.pushKnowledge()` 优先使用短信通道
- 短信失败时降级到 `NoticeSendService.send()` 内部通知
- 推送记录写入 `health_push_record` 表（`push_channel` 标记实际使用的通道）

### 理由

- 短信触达率高（客户手机号直接触达）
- 腾讯云 SMS SDK 已引入（`com.tencentcloudapi:tencentcloud-sdk-java:3.1.1445`）
- 内部通知通过已有 `NoticeSendService` 复用，无需额外开发

### 影响

- **风险**: 短信发送失败（签名未审核/模板未通过）→ 降级通知可能也无法送达
- **替代方案**: 推送失败重试机制（已部分实现，暂无重试队列）

---

## 决策 6: 前端技术选型 — Vue 3 + TypeScript + Naive UI

### 背景

CordysCRM 前端已有技术栈：Vue 3 + TypeScript + Naive UI（monorepo pnpm）。

### 决策

**复用现有技术栈，不引入新框架**

- Vue 3 组合式 API（`<script setup>`）
- TypeScript 类型定义（复用项目已有 `*.d.ts` 规范）
- Naive UI 组件库（复用项目已有组件风格）
- 路由：Hash 模式（复用 `createWebHashHistory()`）
- 页面目录：`frontend/packages/web/src/views/health/`

### 理由

- 保持与 CordysCRM 主项目风格一致
- 不引入额外学习成本（已有 Naive UI 组件使用经验）
- monorepo 结构已验证（pnpm workspace）

### 影响

- **风险**: Naive UI 功能限制 → 使用原生 HTML 或自行封装
- **替代方案**: Element Plus（功能更全，但样式不一致）

---

## 决策 7: 数据库 ID 生成策略 — 雪花 ID（String）

### 背景

10 张表的主键 ID 需要统一生成策略。

### 决策

**使用 `cn.cordys.common.uid.IDGenerator.nextStr()` 生成雪花 ID（String 类型）**

- 不使用数据库自增（AUTO_INCREMENT）
- 不使用 UUID（无序，索引碎片化）
- 雪花 ID 字符串化后作为主键

### 理由

- CordysCRM 已有 `IDGenerator` 工具类
- 现有 `HealthSyncService` 等多处已使用此方式
- 雪花 ID 有序，对索引友好

### 影响

- **风险**: IDGenerator 单点故障 → 依赖 CordysCRM 基础设施
- **替代方案**: 数据库自增（简单但分布式不友好）

---

## 决策 8: 前端页面路由 — Hash 模式 + `/health/` 前缀

### 背景

健康管理模块需要在前端注册路由。

### 决策

**Hash 模式 + `/health/` 前缀**

```
/health/archive          → 健康档案列表
/health/archive/:id       → 档案详情
/health/sync             → 体检数据同步
/health/exam-abnormal     → 异常指标统计
/health/ai-interpret      → AI 健康解读
/health/follow            → 随访管理
/health/follow-rule       → 随访规则
/health/knowledge        → 健康知识库
/health/push              → 健康推送
```

### 理由

- CordysCRM 前端使用 Hash 模式（`createWebHashHistory()`）
- `/health/` 前缀统一管理健康模块路由，避免与其他模块冲突
- 菜单通过后端权限系统配置（复用 CordysCRM 权限体系）

### 影响

- **风险**: 权限系统与健康模块集成 → 需确认 CordysCRM 权限配置方式
- **替代方案**: 后端权限系统配置菜单（已有机制，沿用）

---

## 决策 9: 定时任务方案 — Quartz（已引入）

### 背景

体检数据需要每日凌晨自动同步（增量拉取）。

### 决策

**使用已有 Quartz 定时任务框架**

- 引入：`cn.cordys:quartz-starter:1.0.0`
- 同步任务：`HealthSyncJob`（每日凌晨 2:00 执行）
- 随访提醒任务：`HealthFollowRemindJob`（每日 9:00 执行）

### 理由

- CordysCRM 已引入 Quartz（`quartz-starter.version=1.0.0`）
- 复用已有定时任务基础设施，无需额外引入
- Quartz 支持 cron 表达式（灵活配置执行时间）

### 影响

- **风险**: 定时任务与手动同步冲突 → 使用 Redisson 分布式锁（`redisson-starter:3.52.0`）保证同一时刻只有一个同步任务执行
- **替代方案**: Spring `@Scheduled`（简单但不支持分布式）

---

## 决策 10: 前端页面架构 — 列表页 + 详情页分离

### 背景

健康模块需要多个管理页面（档案/随访/知识库/推送等）。

### 决策

**每个模块使用「列表页 + 详情页/表单页」分离架构**

```
views/health/{module}/
├── index.vue             # 列表页（分页+搜索+操作按钮）
├── detail.vue             # 详情页（只读展示）
└── form.vue               # 表单页（新建/编辑）
```

### 理由

- 符合 CordysCRM 主项目已有的页面组织方式
- 列表页和详情页职责分离，便于复用和扩展
- 表单页独立支持新建和编辑两种场景（通过 query 参数区分）

### 影响

- **风险**: 页面过多（7 个模块 × 2~3 个页面 = 14~21 个 Vue 文件）
- **替代方案**: 详情页和表单页合并（复杂度提升，暂不采用）

---

## 决策 11: 异常指标分级 — 数据库聚合查询

### 背景

医生需要统计异常指标（按检查项维度 + 客户维度）。

### 决策

**后端 SQL 聚合查询 + 前端图表展示**

```sql
-- 按检查项统计异常率
SELECT check_index_name, COUNT(*) as total,
       SUM(CASE WHEN is_abnormal=1 THEN 1 ELSE 0 END) as abnormal_count,
       ROUND(SUM(CASE WHEN is_abnormal=1 THEN 1 ELSE 0 END)/COUNT(*)*100, 2) as abnormal_rate
FROM health_examination
WHERE exam_date BETWEEN ? AND ?
GROUP BY check_index_name
ORDER BY abnormal_rate DESC;
```

### 理由

- 聚合查询在数据库层完成，性能优于内存计算
- 前端用简单表格展示（暂不引入 ECharts，保持轻量）
- 支持按时间段筛选（前端 date range picker）

### 影响

- **风险**: 大数据量时聚合查询慢 → 考虑增加索引或定时计算
- **替代方案**: 预计算表（增加复杂度，暂不需要）

---

## 决策 12: 外部 API 调用 — RestTemplate + 超时控制

### 背景

体检 API 和 Dify API 都需要 HTTP 调用，需要统一超时控制。

### 决策

**统一使用 RestTemplate + 超时配置**

- 体检 API：`health.api.timeout`（默认 30s）
- Dify API：`dify.api.timeout`（默认 120s，超时较长因为是 AI 生成）

```java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(5000);   // 5s
factory.setReadTimeout(timeoutMs); // 可配置
restTemplate.setRequestFactory(factory);
```

### 理由

- RestTemplate 是 Spring 标准库，无需引入额外 HTTP 客户端
- 超时控制防止外部 API 阻塞主线程
- 两个 Service 各自配置超时（Dify 需更长）

### 影响

- **风险**: 外部 API 超时 → 记录日志 + 返回错误信息
- **替代方案**: WebClient（响应式，但改造大，暂不采用）

---

## 决策 13: 配置管理 — 外部化配置（不硬编码）

### 背景

API 凭证（SecretId/Key/Dify API Key）不能硬编码。

### 决策

**通过 `@Value` 注解从配置文件注入，敏感值从环境变量读取**

```properties
# 体检 API
health.api.base-url=http://newreport.jkhaopy.com:9002
health.api.app-id=166660309689670637991
health.api.secret=${HEALTH_API_SECRET:}

# Dify AI
dify.api.url=http://220.163.111.166/v1
dify.api.key=${DIFY_API_KEY:}
dify.api.timeout=120000

# 腾讯云短信
tencentcloud.sms.secret-id=${TENCENTCLOUD_SECRET_ID:}
tencentcloud.sms.secret-key=${TENCENTCLOUD_SECRET_KEY:}
tencentcloud.sms.sms-sdk-app-id=${TENCENTCLOUD_SMS_APP_ID:}
```

### 理由

- 环境变量优先级高于配置文件（本机开发可覆盖）
- 敏感信息不进入代码仓库（gitignore）
- 现有 `HealthSyncService` 和 `HealthAiService` 已使用此模式

### 影响

- **风险**: 环境变量未配置 → 启动失败 → 需明确提示配置项
- **替代方案**: 配置中心（Apollo/Nacos，增加运维复杂度，暂不需要）

---

## 未决事项

|| 事项 | 状态 | 说明 |
|------|------|------|
| CordysCRM 权限系统与健康菜单集成 | ⏳ 待确认 | 需确认菜单配置方式（数据库/代码） |
| 数据库表空间和编码规范 | ⏳ 待确认 | 需 DBA 提供现有表空间配置 |
| Dify 应用 workflow 配置 | ⏳ 待确认 | 需 Dify 应用侧返回固定 JSON 格式 |
| 腾讯云短信签名/模板审核状态 | ⏳ 待确认 | 需确认生产环境签名和模板是否已审核 |

---

## 决策 14: API 契约对齐策略 — 前端改 URL 适配后端

### 背景

reviewer 审查发现前端 9 个 API URL 中 7 个与后端不匹配，且后端已有部分缺失端点。

### 决策

**前端改 URL 适配后端，不改造后端**（后端 RESTful 风格更优）

| 前端改 | 后端保持 | 原因 |
|--------|----------|------|
| `/health/sync/trigger` → `/health/sync/sync` | `/health/sync/sync` | 后端 URL 更 RESTful |
| `/health/follow/add` + `/health/follow/update` → `/health/follow/save` | `/health/follow/save` | 后端 add/update 合一 |
| `/health/knowledge/add` + `/health/knowledge/update` → `/health/knowledge/save` | `/health/knowledge/save` | 同上 |
| DELETE 参数从 body 改为 path | `/{id}` | 后端用 path param |

### 补充后端缺失端点（3个）
- `GET /health/sync/status/{id}` — 同步状态查询
- `GET /health/rule/get/{id}` — 规则详情
- `POST /health/rule/delete` — 删除规则（body: { id }）

### 理由

- 后端 Controller 设计更 RESTful，且 29 个 Java 文件均已按此模式编写
- 前端改 7 个 URL 成本远低于改造后端
- 缺失端点仅 3 个，补全风险低

---

## 决策 15: HealthFollowRule.id 类型 — `string`（雪花ID）

### 背景

前端 TypeScript 定义 `HealthFollowRule.id: number | null`，后端 `BaseModel.id` 是 `String` 类型（雪花 ID）。

### 决策

**前端改为 `string | null`，与后端一致**

```typescript
// 修改前（错误）
id: number | null;

// 修改后（正确）
id: string | null;
```

### 理由

- 雪花 ID 是 19 位数字，Java 中是 `String` 类型
- axios 传 `number` 时大数精度丢失
- 所有随访规则 API 调用（getFollowRule/saveFollowRule/deleteFollowRule 等）统一改为 `string`

---