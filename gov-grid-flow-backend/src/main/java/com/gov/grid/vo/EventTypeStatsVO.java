package com.gov.grid.vo;

import lombok.Data;

@Data
public class EventTypeStatsVO {

    private String eventType;

    private String eventTypeName;

    private Long count;

    private Double percentage;
}
