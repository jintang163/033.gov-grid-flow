package com.gov.grid.quartz;

import com.gov.grid.service.impl.EventHeatWarningServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventHeatWarningJob extends QuartzJobBean {

    @Autowired
    private EventHeatWarningServiceImpl eventHeatWarningService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("开始执行【事件热度预警扫描】定时任务");
        long startTime = System.currentTimeMillis();
        try {
            int pushedCount = eventHeatWarningService.scanAndPushHighWarning();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("【事件热度预警扫描】定时任务执行完成，成功推送 {} 个网格预警，耗时 {} ms", pushedCount, costTime);
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("【事件热度预警扫描】定时任务执行异常，耗时 {} ms", costTime, e);
        }
    }
}
