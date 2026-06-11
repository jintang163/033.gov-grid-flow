package com.gov.grid.vo;

import lombok.Data;

@Data
public class GridStatsVO {

    private Long gridId;

    private String gridName;

    private String gridCode;

    private Long totalCount;

    private Long pendingCount;

    private Long processingCount;

    private Long completedCount;

    private Double avgScore;
}
