package com.gov.grid.quartz;

import com.gov.grid.service.EventUrgeService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class EventUrgeJob extends QuartzJobBean {

    @Autowired
    private EventUrgeService eventUrgeService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("开始执行事件超时催办定时任务");
        int count = eventUrgeService.scanAndUrge();
        log.info("事件超时催办定时任务执行完成，处理了{}个事件", count);
    }
}
