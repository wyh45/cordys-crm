# Task-C5 后端产出：GET /health/sync/status/{id} 端点

## 改动文件

### 1. `backend/crm/src/main/java/cn/cordys/crm/health/service/HealthSyncService.java`

**改动 1：新增 SyncStatusEnum 枚举** (line 55-65)
```java
public enum SyncStatusEnum {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");
    ...
}
```

**改动 2：替换 `getLastSyncResult()` 为 `getSyncStatusById(startDate)`** (line 70-84)
- 旧的：直接返回内存缓存，忽略 id 参数
- 新的：按 startDate 匹配，匹配不到返回 PENDING 状态

**改动 3：`SyncResult` 新增 `status` 字段** (line 386+)
```java
private String status;  // pending / processing / completed / failed
public String getStatus() { return status; }
public void setStatus(String status) { this.status = status; }
```

**改动 4：`syncByDateRange` 设置状态**:
- 初始 → `PROCESSING`
- 空记录返回 → `COMPLETED`
- 处理完成 → `COMPLETED`
- 异常 → `FAILED`

### 2. `backend/crm/src/main/java/cn/cordys/crm/health/controller/HealthSyncController.java`

**改动：getSyncStatus 端点改调 `getSyncStatusById(id)`** (line 50-52)
```java
@GetMapping("/status/{id}")
public HealthSyncService.SyncResult getSyncStatus(@PathVariable String id) {
    return healthSyncService.getSyncStatusById(id);
}
```

## API 响应格式

```json
GET /health/sync/status/{id}
Path: id = startDate (yyyy-MM-dd)

Response:
{
  "startDate": "2026-05-18",
  "endDate": "2026-05-18",
  "status": "completed",  // pending | processing | completed | failed
  "totalRecords": 10,
  "successCount": 8,
  "failCount": 2,
  "message": "成功 8 条, 失败 2 条"
}
```

## 验证

```bash
# 触发同步
curl -X POST "http://localhost:8080/health/sync/sync?startDate=2026-05-01&endDate=2026-05-18"

# 轮询状态（id = startDate）
curl "http://localhost:8080/health/sync/status/2026-05-01"
```

## 已知限制

- 状态存储在 JVM 内存 (`lastSyncResult`)，服务重启后丢失
- 仅支持查询最近一次同步任务（`lastSyncResult` 单实例）
- 若需生产级持久化，需建 `health_sync_log` 表存储每次同步记录

## 影响范围

- Task-C4 前端体检同步页面依赖此端点做 3 秒轮询
- Task-C7 AI 解读页面（无直接依赖）

## 审查状态

✅ 后端完成，等待审查
