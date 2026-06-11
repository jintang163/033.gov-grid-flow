package com.gov.grid.vo;

import lombok.Data;

@Data
public class DeptStatsVO {

    private Long deptId;

    private String deptName;

    private Long handleCount;

    private Double avgDuration;

    private Double reworkRate;

    private Double avgScore;
}
