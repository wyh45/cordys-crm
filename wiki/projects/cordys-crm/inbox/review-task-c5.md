# Review-Task-C5: GET /health/sync/status/{id} 端点审查

## 审查对象
- **任务**: Task-C5 后端产出
- **产出文档**: `wiki/projects/cordys-crm/inbox/task-c5-backend.md`
- **代码文件**: 
  - `backend/crm/src/main/java/cn/cordys/crm/health/service/HealthSyncService.java` (463行)
  - `backend/crm/src/main/java/cn/cordys/crm/health/controller/HealthSyncController.java` (54行)
- **前端消费者**: `frontend/packages/web/src/views/health/components/healthSyncPanel.vue`
- **前端类型定义**: `frontend/packages/lib-shared/api/modules/health.ts` L80-84
- **审查者**: reviewer Agent
- **审查时间**: 2026-05-18

## 审查结果
- [ ] 通过
- [x] **需修改（P0: 2个API契约不匹配，导致功能静默失效）**
- [ ] 拒绝

---

## 逐维审查

### 维度 1: 枚举设计 — ⚠️ P0 不通过

**问题**: Java enum 值全小写，前端 TS 期望全大写，导致所有状态判断静默失效。

| 层级 | 文件:行 | 内容 | 实际值 |
|------|---------|------|--------|
| 后端 enum | `HealthSyncService.java:57-60` | `PENDING("pending")` / `PROCESSING("processing")` / `COMPLETED("completed")` / `FAILED("failed")` | **全小写** |
| 后端序列化 | `HealthSyncService.java:81,97,105,135,142` | `setStatus(SyncStatusEnum.XXX.getValue())` | Jackson 输出 `"processing"` 等 |
| 前端 TS 类型 | `health.ts:82` | `status: 'PENDING' \| 'PROCESSING' \| 'COMPLETED' \| 'FAILED'` | **期望大写** |
| 前端使用 1 | `healthSyncPanel.vue:165` | `result?.status === 'PROCESSING'` | `"processing" === "PROCESSING"` → **FALSE** |
| 前端使用 2 | `healthSyncPanel.vue:168` | `result?.status === 'COMPLETED'` | `"completed" === "COMPLETED"` → **FALSE** |
| 前端使用 3 | `healthSyncPanel.vue:170` | `result?.status === 'FAILED'` | `"failed" === "FAILED"` → **FALSE** |
| 前端轮询判断 | `healthSyncPanel.vue:190,196,198,214` | `status === 'COMPLETED'` 等 | **全部 FALSE** |
| 前端状态徽章 | `healthSyncPanel.vue:229-232` | `map['PENDING'] = 'default'` 等 | `map['pending']` → `undefined` → falls to `'default'` |
| 前端状态标签 | `healthSyncPanel.vue:239-242` | `map['PENDING'] = t(...)` 等 | `map['pending']` → `undefined` → falls to raw `"pending"` |

**根因**: 后端 enum 习惯用小写值（常见 Java 风格），前端 TS 习惯用大写 union literal（模仿 Java 枚举名惯例）。两端独立开发未对齐。

**影响**: 
- `handleRunningSync()` (line 155-181): 触发同步后，status 判断全部为 false，**既不开始轮询也不显示完成/失败** — 用户看到空白状态
- `handleCheckStatus()` (line 183-207): 即使手动触发状态查询，`getHealthSyncStatus` 返回的 status 全部不匹配 UI 分支
- 状态徽章永远显示 `default` 颜色，标签显示原始小写英文
- 轮询循环条件 (line 214) 永远为 false，**3秒轮询完全失效**

### 维度 2: API 契约 — ❌ P0 不通过

#### 对照表

| 后端 SyncResult getter | Jackson 字段 | 前端 HealthSyncResult 字段 | 匹配? |
|------------------------|-------------|--------------------------|-------|
| `getStartDate()` | `startDate` (String) | — | ⚠️ 前端未定义（可用不使用） |
| `getEndDate()` | `endDate` (String) | — | ⚠️ 前端未定义（可用不使用） |
| `getStatus()` | `status` (String, 小写) | `status` (literal union, 大写) | ❌ **值大小写不匹配** |
| `getTotalRecords()` | `totalRecords` (int) | — | ⚠️ 前端未定义 |
| `getSuccessCount()` | `successCount` (int) | — | ⚠️ 前端未定义 |
| `getFailCount()` | `failCount` (int) | — | ⚠️ 前端未定义 |
| `getMessage()` | `message` (String) | `message?` (string) | ✅ |
| — (不存在) | — | `syncId` (string) | ❌ **后端无此字段** |

#### P0-1: 缺失 `syncId` 字段 → 轮询入口短路

`healthSyncPanel.vue:162`:
```typescript
lastSyncId.value = result.syncId ?? null;  // result.syncId === undefined → always null
```

`healthSyncPanel.vue:184`:
```typescript
if (!lastSyncId.value) return;  // ← 永远是 null，函数直接返回
```

**后果**: `handleCheckStatus()` 永远在第一行就 return，后面所有轮询逻辑（状态判断、停止轮询、日志记录）全部不执行。

### 维度 3: 代码质量 — 🟡 通过（有 1 处需改进）

| 检查项 | 结果 | 说明 |
|--------|------|------|
| Controller 端点注册 | ✅ | `@GetMapping("/status/{id}")` 正确 |
| `getSyncStatusById()` 逻辑 | ✅ | 匹配 startDate，查不到返回 PENDING 占位 — 设计合理 |
| syncByDateRange 状态流转 | ✅ | PROCESSING → COMPLETED/FAILED，异常处理完整 |
| 事务注解 | ✅ | `@Transactional(rollbackFor = Exception.class)` |
| volatile 可见性 | ✅ | `private volatile SyncResult lastSyncResult` |
| `lastSyncResult` 赋值点过多 | ⚠️ P2 | 4 处赋值（L98/106/136/143），建议收敛到 finally 块 |
| **N+1 查询** | ⚠️ P1 | `processSingleRecord()` L173 每记录调用 `getSyncedExamNos()` 做全表扫描，应提到循环外 |

**N+1 查询详情** (`HealthSyncService.java:166-174`):
```java
private SyncStatus processSingleRecord(HealthApiRecord record) {
    // ...
    Set<String> synced = getSyncedExamNos();  // ← 每条记录扫一次全表
    if (synced.contains(examNo)) { ... }
    // ...
}
```
`getSyncedExamNos()` 执行 `mappingMapper.select(example)` 无分页全表扫描。同步 100 条记录 = 100 次全表查询。建议在 `syncByDateRange` 的循环前调一次，传入循环。

### 维度 4: 已知限制 — ✅ 可接受

| 限制 | 评估 |
|------|------|
| 状态存 JVM 内存，重启丢失 | ✅ 可接受（MVP 阶段，已在产出文档明确标注） |
| 仅支持单次同步任务（lastSyncResult 单实例） | ✅ 可接受（文档已说明生产需 `health_sync_log` 表） |
| 无持久化 | ✅ 已列为后续改进项 |

---

## 问题汇总

| # | 严重度 | 分类 | 位置 | 问题 |
|---|--------|------|------|------|
| **P0-1** | 🔴 Critical | 枚举大小写 | 后端: `HealthSyncService.java:57-60` 前端: `health.ts:82`, `healthSyncPanel.vue:165-242` | Java enum value 全小写 (`"processing"`)，前端 TS literal union 全大写 (`'PROCESSING'`)。所有 `===` 比较、switch 判断、map 查找全部静默失效 — 同步状态展示/轮询完全不可用 |
| **P0-2** | 🔴 Critical | 缺失字段 | 后端: `HealthSyncService.java:389-412` 前端: `health.ts:81`, `healthSyncPanel.vue:162,184` | 前端期望 `syncId` 字段，后端 `SyncResult` 无对应 getter。`lastSyncId` 永远为 null → `handleCheckStatus()` 第一行 return → 轮询逻辑全部被短路 |
| **P1** | 🟡 Major | 性能 | `HealthSyncService.java:173` | `processSingleRecord()` 循环内调用 `getSyncedExamNos()` → N+1 全表扫描。建议提到循环外 |
| **P2** | 🟢 Minor | 可维护性 | `HealthSyncService.java:98,106,136,143` | `this.lastSyncResult = result` 在 4 个位置赋值，建议收敛 |
| ℹ️ | ℹ️ Info | 字段缺失 | `healthSyncPanel.vue` vs `SyncResult` | 前端未定义 `startDate/endDate/totalRecords/successCount/failCount` — 当前 UI 不需要这些字段，不阻塞但建议添加到 TS 接口以保持契约完整 |

---

## 修复方案

### P0-1 修复（枚举对齐，推荐改后端 1 处）

**方案 A（推荐）**: 后端 enum 改为大写 — 只改 4 行，前端 0 改动:

```java
// HealthSyncService.java:57-60
public enum SyncStatusEnum {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");
    ...
}
```

**方案 B**: 前端 TS literal union 改为小写 — 需改 `health.ts:82` + `healthSyncPanel.vue` 内 ~15 处字符串比较和 2 个 map。

### P0-2 修复（补充 syncId 字段）

后端 `SyncResult` 添加 `syncId` 字段（取值 = startDate），前端轮询用 startDate 标识任务：

```java
// HealthSyncService.java SyncResult 类
private String syncId;

public String getSyncId() { return syncId; }
public void setSyncId(String syncId) { this.syncId = syncId; }
```

`syncByDateRange` 构造 result 后调用 `result.setSyncId(startDate)`。`getSyncStatusById` 返回的 pending 占位也设置 `syncId = startDate`。

### P1 修复（N+1 查询）

在 `syncByDateRange` 循环前:
```java
Set<String> synced = getSyncedExamNos();  // 移到这里，只查一次
for (HealthApiRecord record : records) {
    // 传入 processSingleRecord 或改用内联检查
}
```

---

## 修复后验证点

1. ✅ 触发同步后 `result.status` 值为 `"PROCESSING"` (Jackson 输出大写)
2. ✅ `result.syncId` 有值（= startDate），`lastSyncId` 不为 null
3. ✅ 轮询启动（3秒后 `handleCheckStatus` 正常执行）
4. ✅ 状态徽章颜色正确（processing=蓝色, completed=绿色, failed=红色）
5. ✅ 同步完成后轮询自动停止

---

## 优点

- 端点设计简洁（id = startDate），RESTful 语义清晰
- `getSyncStatusById` 的兜底逻辑好（查无记录返回 PENDING 占位，不抛异常也不返回 null）
- 错误处理完整（syncByDateRange 所有异常路径都设置了 FAILED 状态和 message）
- 已知限制文档化充分（内存存储、单任务、缺持久化均明确标注）

---

## 总结

两个 P0 问题（枚举大小写 + 缺失 syncId）导致前后端完全对不上，**同步状态轮询功能整体静默失效** — 所有 UI 状态判断和轮询逻辑被无声短路，用户看到的是空白状态面板。这不是「功能不完备」，是「看起来接了但实际不工作」的静默失败型 bug。

**判定: ❌ 不通过，退回修复 P0-1 和 P0-2。**
