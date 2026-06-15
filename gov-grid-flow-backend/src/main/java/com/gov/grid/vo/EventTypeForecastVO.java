package com.gov.grid.vo;

import lombok.Data;

@Data
public class EventTypeForecastVO {

    private String eventType;

    private String eventTypeName;

    private Double probability;

    private Integer predictedCount;

    private Integer rank;

    private String trend;
}
