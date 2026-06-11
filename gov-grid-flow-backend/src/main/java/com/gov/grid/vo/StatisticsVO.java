package com.gov.grid.vo;

import lombok.Data;

@Data
public class StatisticsVO {

    private Long totalEvents;

    private Long pendingCount;

    private Long processingCount;

    private Long completedCount;

    private Double avgHandleTime;

    private Double avgScore;

    private Double reworkRate;
}
