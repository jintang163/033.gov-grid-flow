package com.gov.grid.vo;

import lombok.Data;

@Data
public class EventTrendVO {

    private String date;

    private Long count;

    private Long completedCount;
}
