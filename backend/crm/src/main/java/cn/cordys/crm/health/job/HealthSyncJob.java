package cn.cordys.crm.health.job;

import cn.cordys.crm.health.service.HealthSyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class HealthSyncJob implements Job {

    @Resource
    private HealthSyncService healthSyncService;

    @Override
    public void execute(JobExecutionContext context) {
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        log.info("[HealthSyncJob] Auto-syncing exam data for {}", yesterday);
        try {
            HealthSyncService.SyncResult result = healthSyncService.syncByDateRange(yesterday, yesterday);
            log.info("[HealthSyncJob] Synced {}: total={} success={} fail={}",
                yesterday, result.getTotalRecords(), result.getSuccessCount(), result.getFailCount());
        } catch (Exception e) {
            log.error("[HealthSyncJob] Sync failed for {}: {}", yesterday, e.getMessage());
        }
    }
}
