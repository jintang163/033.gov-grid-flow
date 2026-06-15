package com.gov.grid.vo;

import lombok.Data;

import java.util.List;

@Data
public class HeatmapCalendarVO {

    private String date;

    private Integer heatValue;

    private String heatLevel;

    private Integer eventCount;

    private Long gridId;

    private String gridName;

    private List<EventTypeForecastVO> topEventTypes;
}
