package com.gov.grid.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GridMemberStatusVO {

    private Long userId;

    private String userName;

    private String phone;

    private Long gridId;

    private String gridName;

    private BigDecimal lng;

    private BigDecimal lat;

    private Integer onDuty;

    private String lastReportTime;

    private Integer battery;
}
