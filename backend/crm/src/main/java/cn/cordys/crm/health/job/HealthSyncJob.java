package cn.cordys.crm.health.job;

import cn.cordys.crm.health.service.HealthSyncService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 体检数据定时同步任务
 * - 每日凌晨 2:00 执行 (cron: 0 0 2 * * ?)
 * - 使用 Redisson 分布式锁防止多实例冲突
 * - 增量同步：只同步当天有更新的记录
 */
@Component
@Slf4j
public class HealthSyncJob implements Job {

    private static final String LOCK_KEY = "health:sync:job:lock";
    private static final long LOCK_LEASE_SECONDS = 30 * 60; // 锁最多持有30分钟

    @Resource
    private HealthSyncService healthSyncService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 尝试获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean locked = false;

        try {
            // tryLock 非阻塞，获取不到立即返回 false
            locked = lock.tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.info("[HealthSyncJob] Another instance is running, skip this execution");
                return;
            }

            log.info("[HealthSyncJob] Started, acquiring distributed lock");
            long startMs = System.currentTimeMillis();

            // 执行增量同步：昨天整天
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            String startDate = yesterday.toString();
            String endDate = yesterday.toString();

            log.info("[HealthSyncJob] Syncing exam records for date: {}", startDate);
            HealthSyncService.SyncResult result = healthSyncService.syncByDateRange(startDate, endDate);

            long elapsedMs = System.currentTimeMillis() - startMs;
            log.info("[HealthSyncJob] Completed in {}ms, total={}, success={}, fail={}, message={}",
                    elapsedMs, result.getTotalRecords(), result.getSuccessCount(),
                    result.getFailCount(), result.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[HealthSyncJob] Interrupted while waiting for lock", e);
        } catch (Exception e) {
            log.error("[HealthSyncJob] Execution failed", e);
            throw new JobExecutionException(e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[HealthSyncJob] Lock released");
            }
        }
    }
}