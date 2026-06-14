package com.gov.grid.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardOverviewVO {

    private Long todayReported;

    private Long todayCompleted;

    private Long pendingCount;

    private Long processingCount;

    private Long completedCount;

    private Double avgHandleTime;

    private Double onlineRate;

    private List<HourlyTrendItem> hourlyTrend;

    @Data
    public static class HourlyTrendItem {
        private Integer hour;
        private Long count;
    }
}
