package cn.cordys.crm.health.job;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务调度配置
 * 健康模块定时任务：
 *   - HealthSyncJob: 每日凌晨 2:00 增量同步体检数据
 */
@Configuration
public class HealthJobScheduler {

    /**
     * 体检同步任务 JobDetail
     * durably: true 保证 JobDetail 持久化到 DB（石英表）
     * requestsRecovery: true 节点崩溃时由 Quartz 重新执行
     */
    @Bean
    public JobDetail healthSyncJobDetail() {
        return JobBuilder.newJob(HealthSyncJob.class)
                .withIdentity("healthSyncJob", "health")
                .withDescription("每日凌晨2点增量同步体检数据")
                .storeDurably()
                .requestRecovery()
                .build();
    }

    /**
     * 触发器：每日凌晨 2:00 执行
     * cron: 秒 分 时 日 月 周 年
     * 0 0 2 * * ? = 每天上午2点0分0秒
     */
    @Bean
    public Trigger healthSyncJobTrigger(JobDetail healthSyncJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(healthSyncJobDetail)
                .withIdentity("healthSyncTrigger", "health")
                .withDescription("每日凌晨2点触发器")
                .withSchedule(CronScheduleBuilder
                        .cronSchedule("0 0 2 * * ?")
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}