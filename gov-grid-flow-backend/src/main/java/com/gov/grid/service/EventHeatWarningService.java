package com.gov.grid.service;

import com.gov.grid.vo.EventHeatForecastVO;
import com.gov.grid.vo.HeatmapCalendarVO;

import java.time.LocalDate;
import java.util.List;

public interface EventHeatWarningService {

    List<EventHeatForecastVO> getGridHeatForecast(Integer hours);

    EventHeatForecastVO getGridHeatForecastByGridId(Long gridId, Integer hours);

    List<HeatmapCalendarVO> getCalendarHeatmap(Integer days, Long gridId);

    List<HeatmapCalendarVO> getCalendarHeatmapByMonth(Integer year, Integer month, Long gridId);

    boolean pushWarningNotification(Long gridId);

    List<EventHeatForecastVO> getHighWarningGrids();
}
