package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.service.HealthSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 健康数据同步接口
 * 提供手动触发从体检接口拉取数据的功能
 */
@Tag(name = "健康管理-数据同步")
@RestController
@RequestMapping("/health/sync")
public class HealthSyncController {

    @Resource
    private HealthSyncService healthSyncService;

    /**
     * 按日期范围同步体检数据
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束日期 yyyy-MM-dd
     */
    @PostMapping("/sync")
    @Operation(summary = "按日期范围同步体检数据")
    public HealthSyncService.SyncResult sync(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return healthSyncService.syncByDateRange(startDate, endDate);
    }

    /**
     * 同步指定日期的数据（一天）
     * @param date 日期 yyyy-MM-dd
     */
    @PostMapping("/sync-day")
    @Operation(summary = "同步单日体检数据")
    public HealthSyncService.SyncResult syncDay(@RequestParam String date) {
        return healthSyncService.syncByDateRange(date, date);
    }

    /**
     * 查询同步状态（轮询进度）
     * @param id 同步记录ID（对应 startDate，用于标识同步任务）
     */
    @GetMapping("/status/{id}")
    @Operation(summary = "查询同步状态")
    public HealthSyncService.SyncResult getSyncStatus(@PathVariable String id) {
        // id 即 startDate，用于前端轮询时标识本次同步任务
        return healthSyncService.getSyncStatusById(id);
    }
}
