package com.gov.grid.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventHeatForecastVO {

    private Long gridId;

    private String gridName;

    private String gridCode;

    private LocalDateTime forecastTime;

    private Integer heatLevel;

    private String heatLevelDesc;

    private Double heatScore;

    private List<EventTypeForecastVO> eventTypeForecasts;

    private String weatherCondition;

    private Boolean isHoliday;

    private String suggestion;

    private Integer predictedEventCount;
}
