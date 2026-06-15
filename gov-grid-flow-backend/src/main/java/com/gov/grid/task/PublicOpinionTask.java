package com.gov.grid.task;

import com.gov.grid.service.PublicOpinionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicOpinionTask {

    private final PublicOpinionService publicOpinionService;

    @Scheduled(cron = "0 30 1 * * ?")
    public void calculateDailyStatistics() {
        log.info("开始执行每日舆情统计定时任务");
        try {
            publicOpinionService.calculateDailyStatistics();
            log.info("每日舆情统计任务执行完成");
        } catch (Exception e) {
            log.error("每日舆情统计任务执行失败", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void reprocessPendingEvaluations() {
        log.info("开始执行待处理评价情感分析补算任务");
        try {
            publicOpinionService.reprocessAllEvaluations();
            log.info("待处理评价情感分析补算完成");
        } catch (Exception e) {
            log.error("待处理评价情感分析补算失败", e);
        }
    }
}
